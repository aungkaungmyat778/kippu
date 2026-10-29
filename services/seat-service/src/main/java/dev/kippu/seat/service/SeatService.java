package dev.kippu.seat.service;

import dev.kippu.seat.api.dto.ConfirmResponse;
import dev.kippu.seat.api.dto.HoldResponse;
import dev.kippu.seat.api.dto.SeatDto;
import dev.kippu.seat.api.dto.SeatMapDto;
import dev.kippu.seat.client.CatalogClient;
import dev.kippu.seat.domain.SeatStatus;
import dev.kippu.seat.domain.TripSeat;
import dev.kippu.seat.repository.TripSeatRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The rules of the seat map. The class itself is NOT transactional on purpose: calling the
 * Catalog over HTTP must never happen while a database transaction (and its row locks) is open.
 * Each database step opens its own short transaction through the TransactionTemplate.
 */
@Service
public class SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatService.class);

    private final TripSeatRepository seats;
    private final CatalogClient catalog;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final Duration holdDuration;
    private final int maxSeatsPerHold;

    public SeatService(TripSeatRepository seats,
                       CatalogClient catalog,
                       Clock clock,
                       PlatformTransactionManager txManager,
                       @Value("${kippu.seats.hold-minutes}") long holdMinutes,
                       @Value("${kippu.seats.max-seats-per-hold}") int maxSeatsPerHold) {
        this.seats = seats;
        this.catalog = catalog;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
        this.holdDuration = Duration.ofMinutes(holdMinutes);
        this.maxSeatsPerHold = maxSeatsPerHold;
    }

    // ------------------------------------------------------------------ provisioning

    /** Creates the seat rows for a trip from its layout. Safe to call any number of times. */
    public int provision(UUID tripId) {
        CatalogClient.TripInfo trip = catalog.getTrip(tripId);
        CatalogClient.LayoutInfo layout = catalog.getLayout(trip.layoutId());
        // One transaction: other requests see either no seats or all of them, never half.
        Integer created = tx.execute(status -> {
            int n = 0;
            for (CatalogClient.LayoutSeatInfo s : layout.seats()) {
                n += seats.insertIfAbsent(tripId, s.seatNo(), s.row(), s.column());
            }
            return n;
        });
        int count = created == null ? 0 : created;
        log.info("Provisioned {} seats for trip {}", count, tripId);
        return count;
    }

    private void ensureProvisioned(UUID tripId) {
        if (!seats.existsByTripId(tripId)) {
            provision(tripId);
        }
    }

    // ------------------------------------------------------------------ reads

    public SeatMapDto seatMap(UUID tripId) {
        ensureProvisioned(tripId);
        Instant now = clock.instant();
        List<SeatDto> list = seats.findByTripIdOrderByRowNoAscColLabelAsc(tripId).stream()
                .map(s -> new SeatDto(s.getSeatNo(), s.getRowNo(), s.getColLabel(),
                        SeatStatus.effective(s.getStatus(), s.getHoldExpiresAt(), now)))
                .toList();
        int available = count(list, SeatStatus.AVAILABLE);
        int held = count(list, SeatStatus.HELD);
        int sold = count(list, SeatStatus.SOLD);
        return new SeatMapDto(tripId, list.size(), available, held, sold, list);
    }

    private static int count(List<SeatDto> list, SeatStatus status) {
        return (int) list.stream().filter(s -> s.status() == status).count();
    }

    // ------------------------------------------------------------------ hold

    /**
     * Holds all requested seats for a booking, or none of them.
     * Retrying the same call for the same booking is safe and simply extends the hold.
     */
    public HoldResponse hold(UUID tripId, UUID bookingId, List<String> requested) {
        if (bookingId == null) {
            throw new InvalidHoldRequestException("bookingId is required");
        }
        List<String> seatNos = HoldRules.validate(requested, maxSeatsPerHold);
        ensureProvisioned(tripId);

        Instant now = clock.instant();
        Instant expiresAt = now.plus(holdDuration);

        tx.executeWithoutResult(status -> {
            List<TripSeat> found = seats.findByTripIdAndSeatNoIn(tripId, seatNos);
            if (found.size() != seatNos.size()) {
                List<String> known = found.stream().map(TripSeat::getSeatNo).toList();
                throw new UnknownSeatException(seatNos.stream().filter(n -> !known.contains(n)).toList());
            }
            int taken = seats.hold(tripId, seatNos, bookingId, expiresAt, now);
            if (taken != seatNos.size()) {
                // Throwing rolls back the seats we did manage to take: all-or-nothing.
                throw new SeatConflictException(unavailable(tripId, seatNos, bookingId));
            }
        });
        log.info("Held {} for booking {} on trip {} until {}", seatNos, bookingId, tripId, expiresAt);
        return new HoldResponse(tripId, bookingId, seatNos, expiresAt);
    }

    private List<String> unavailable(UUID tripId, List<String> seatNos, UUID bookingId) {
        return seats.findByTripIdAndSeatNoIn(tripId, seatNos).stream()
                .filter(s -> !(s.getStatus() == SeatStatus.HELD && bookingId.equals(s.getBookingId())))
                .map(TripSeat::getSeatNo)
                .sorted()
                .toList();
    }

    // ------------------------------------------------------------------ confirm / release

    /** Turns the booking's held seats into sold seats. Repeating the call returns the same answer. */
    public ConfirmResponse confirm(UUID bookingId) {
        Instant now = clock.instant();
        List<String> sold = tx.execute(status -> {
            long held = seats.countByBookingIdAndStatus(bookingId, SeatStatus.HELD);
            if (held == 0) {
                List<TripSeat> alreadySold = seats.findByBookingIdAndStatus(bookingId, SeatStatus.SOLD);
                if (alreadySold.isEmpty()) {
                    throw new HoldExpiredException(bookingId);
                }
                return seatNosOf(alreadySold); // a retry of a confirm that already succeeded
            }
            int confirmed = seats.confirm(bookingId, now);
            if (confirmed < held) {
                throw new HoldExpiredException(bookingId); // some holds ran out: sell nothing
            }
            return seatNosOf(seats.findByBookingIdAndStatus(bookingId, SeatStatus.SOLD));
        });
        log.info("Confirmed {} for booking {}", sold, bookingId);
        return new ConfirmResponse(bookingId, sold);
    }

    /** Frees the booking's held seats. Sold seats are never touched. Returns how many were freed. */
    public int release(UUID bookingId) {
        int released = seats.release(bookingId);
        log.info("Released {} held seats for booking {}", released, bookingId);
        return released;
    }

    private static List<String> seatNosOf(List<TripSeat> list) {
        return list.stream().map(TripSeat::getSeatNo).sorted().toList();
    }

    // ------------------------------------------------------------------ cleanup

    /**
     * Frees every expired hold. Expired holds already count as available for new holds,
     * so this only keeps the table tidy.
     * TODO (M5): publish a SeatHoldExpired event per booking (needs UPDATE ... RETURNING booking_id).
     */
    public int sweepExpired() {
        int released = seats.releaseExpired(clock.instant());
        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }
        return released;
    }
}

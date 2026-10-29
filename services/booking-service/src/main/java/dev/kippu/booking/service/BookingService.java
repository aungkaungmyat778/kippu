package dev.kippu.booking.service;

import dev.kippu.booking.domain.Booking;
import dev.kippu.booking.domain.BookingReference;
import dev.kippu.booking.domain.FailureReason;
import dev.kippu.booking.gateway.CatalogGateway;
import dev.kippu.booking.gateway.PaymentGateway;
import dev.kippu.booking.gateway.SeatsGateway;
import dev.kippu.booking.repository.BookingStore;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one booking across Catalog, Seats and Payment (an orchestrated saga).
 *
 * Steps: check the trip, save a PENDING booking, hold the seats, charge the payment, confirm the
 * seats, mark the booking CONFIRMED. If a step fails, earlier steps are undone where possible and
 * the booking is saved as FAILED with a reason.
 *
 * This class is deliberately NOT transactional. Each database write is one short statement, and
 * no database lock is ever held while waiting for another service over HTTP.
 *
 * Not handled yet (M4): the unknown outcome of a payment call, refunding a payment whose seats
 * could not be confirmed, and recovering a booking left PENDING by a crash.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingStore bookings;
    private final CatalogGateway catalog;
    private final SeatsGateway seats;
    private final PaymentGateway payments;
    private final Clock clock;
    private final Duration cutoff;
    private final int maxSeats;
    private final RandomGenerator random = new SecureRandom();

    public BookingService(BookingStore bookings,
                          CatalogGateway catalog,
                          SeatsGateway seats,
                          PaymentGateway payments,
                          Clock clock,
                          @Value("${kippu.booking.cutoff-minutes}") long cutoffMinutes,
                          @Value("${kippu.booking.max-seats-per-booking}") int maxSeats) {
        this.bookings = bookings;
        this.catalog = catalog;
        this.seats = seats;
        this.payments = payments;
        this.clock = clock;
        this.cutoff = Duration.ofMinutes(cutoffMinutes);
        this.maxSeats = maxSeats;
    }

    public Booking createBooking(CreateBookingCommand request) {
        CreateBookingCommand cmd = BookingRules.validate(request, maxSeats);

        // 1. Look at the trip. Nothing is saved yet, so a rejected request leaves no trace.
        CatalogGateway.TripInfo trip = catalog.getTrip(cmd.tripId());
        BookingRules.checkBookable(trip.status(), trip.departureAt(), clock.instant(), cutoff);

        // 2. Save the booking as PENDING.
        UUID id = UUID.randomUUID();
        Booking booking = bookings.save(Booking.pending(id, BookingReference.generate(random), trip.id(),
                cmd.customerName(), cmd.customerEmail(), cmd.seatNos(), trip.fareYen(),
                trip.departureAt(), clock.instant()));
        log.info("Booking {} ({}) created PENDING: trip {}, seats {}, total {} yen",
                id, booking.getReference(), trip.id(), cmd.seatNos(), booking.getTotalYen());

        // 3. Hold the seats (all or nothing).
        try {
            seats.hold(trip.id(), id, cmd.seatNos());
        } catch (SeatConflictException e) {
            throw failed(booking, FailureReason.SEAT_UNAVAILABLE, null,
                    "Some of the seats you chose were just taken", e.getSeatNos());
        } catch (UnknownSeatException e) {
            throw failed(booking, FailureReason.UNKNOWN_SEAT, null,
                    "These seats do not exist on this trip", e.getSeatNos());
        } catch (TripNotFoundException | InvalidBookingRequestException e) {
            // Seats disagrees with what Catalog told us (trip unknown to it, or it rejected the request).
            // Not the customer's fault, and the booking must not stay PENDING.
            releaseQuietly(id);
            log.error("Booking {}: seat-service rejected the hold: {}", id, e.getMessage());
            throw failed(booking, FailureReason.SEAT_SERVICE_UNAVAILABLE, null,
                    "Seat reservation is temporarily unavailable, please try again", List.of());
        } catch (DownstreamUnavailableException e) {
            // The hold may or may not have been made. Try to free it; it expires by itself anyway.
            releaseQuietly(id);
            log.warn("Booking {}: seat-service unavailable while holding: {}", id, e.getMessage());
            throw failed(booking, FailureReason.SEAT_SERVICE_UNAVAILABLE, null,
                    "Seat reservation is temporarily unavailable, please try again", List.of());
        }

        // 4. Charge. The key is fixed per booking, so asking twice can never charge twice.
        PaymentGateway.PaymentResult payment;
        try {
            payment = payments.charge(id, "booking-" + id, booking.getTotalYen(), cmd.paymentMethod());
        } catch (DownstreamUnavailableException e) {
            // We do not know whether the customer was charged. Safe to retry with the same key later (M4).
            releaseQuietly(id);
            log.error("Booking {}: payment outcome UNKNOWN: {}", id, e.getMessage());
            throw failed(booking, FailureReason.PAYMENT_UNAVAILABLE, null,
                    "Payment is temporarily unavailable, please try again", List.of());
        }
        if (!payment.succeeded()) {
            releaseQuietly(id);
            throw failed(booking, FailureReason.PAYMENT_DECLINED, null,
                    "Your payment was declined", List.of());
        }

        // 5. Turn the held seats into sold seats.
        try {
            seats.confirm(id);
        } catch (HoldExpiredException | DownstreamUnavailableException e) {
            // Money has been taken but the seats are not ours. This needs a refund (M4).
            log.error("Booking {} PAID BUT NOT CONFIRMED, payment {}: {}", id, payment.paymentId(), e.getMessage());
            throw failed(booking, FailureReason.PAID_BUT_NOT_CONFIRMED, payment.paymentId(),
                    "Your payment was taken but the seats could not be confirmed. Support will refund you.", List.of());
        }

        // 6. Done.
        Booking confirmed = bookings.save(booking.confirm(payment.paymentId(), clock.instant()));
        log.info("Booking {} CONFIRMED with payment {}", id, payment.paymentId());
        return confirmed;
    }

    public Booking get(UUID id) {
        return bookings.findById(id).orElseThrow(() -> new BookingNotFoundException(id.toString()));
    }

    public Booking getByReference(String reference) {
        String ref = reference == null ? "" : reference.trim().toUpperCase();
        return bookings.findByReference(ref).orElseThrow(() -> new BookingNotFoundException(ref));
    }

    private BookingFailedException failed(Booking booking, String reason, UUID paymentId,
                                          String message, List<String> seatNos) {
        bookings.save(booking.fail(reason, paymentId, clock.instant()));
        log.info("Booking {} FAILED: {}", booking.getId(), reason);
        return new BookingFailedException(reason, message, booking.getId(), seatNos);
    }

    private void releaseQuietly(UUID bookingId) {
        try {
            seats.release(bookingId);
        } catch (RuntimeException e) {
            log.warn("Booking {}: could not release seats now ({}). The hold will expire on its own.",
                    bookingId, e.getMessage());
        }
    }
}

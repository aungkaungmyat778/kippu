package dev.kippu.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.kippu.booking.domain.Booking;
import dev.kippu.booking.domain.BookingStatus;
import dev.kippu.booking.domain.FailureReason;
import dev.kippu.booking.gateway.CatalogGateway;
import dev.kippu.booking.gateway.PaymentGateway;
import dev.kippu.booking.gateway.SeatsGateway;
import dev.kippu.booking.repository.BookingStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the saga with hand-written fakes instead of a mocking library: each fake records what it
 * was asked and can be told to fail. No Spring, no database, no network.
 */
class BookingServiceTest {

    static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    static final UUID TRIP = UUID.randomUUID();
    static final int FARE = 4500;

    // ---- fakes ----

    static class FakeStore implements BookingStore {
        final Map<UUID, Booking> rows = new HashMap<>();
        final List<BookingStatus> savedStatuses = new ArrayList<>();

        public Booking save(Booking b) {
            rows.put(b.getId(), b);
            savedStatuses.add(b.getStatus());
            return b;
        }

        public Optional<Booking> findById(UUID id) {
            return Optional.ofNullable(rows.get(id));
        }

        public Optional<Booking> findByReference(String ref) {
            return rows.values().stream().filter(b -> b.getReference().equals(ref)).findFirst();
        }

        Booking only() {
            assertEquals(1, rows.size());
            return rows.values().iterator().next();
        }
    }

    static class FakeCatalog implements CatalogGateway {
        String status = "SCHEDULED";
        Instant departure = NOW.plus(Duration.ofHours(5));
        RuntimeException failure;

        public TripInfo getTrip(UUID id) {
            if (failure != null) {
                throw failure;
            }
            return new TripInfo(id, status, FARE, departure);
        }
    }

    static class FakeSeats implements SeatsGateway {
        final List<String> calls = new ArrayList<>();
        RuntimeException holdFailure;
        RuntimeException confirmFailure;
        RuntimeException releaseFailure;

        public void hold(UUID trip, UUID booking, List<String> seats) {
            calls.add("hold");
            if (holdFailure != null) {
                throw holdFailure;
            }
        }

        public void confirm(UUID booking) {
            calls.add("confirm");
            if (confirmFailure != null) {
                throw confirmFailure;
            }
        }

        public void release(UUID booking) {
            calls.add("release");
            if (releaseFailure != null) {
                throw releaseFailure;
            }
        }
    }

    static class FakePayments implements PaymentGateway {
        final List<String> keys = new ArrayList<>();
        final List<Integer> amounts = new ArrayList<>();
        boolean decline;
        RuntimeException failure;
        final UUID paymentId = UUID.randomUUID();

        public PaymentResult charge(UUID booking, String key, int amountYen, String method) {
            keys.add(key);
            amounts.add(amountYen);
            if (failure != null) {
                throw failure;
            }
            return decline ? new PaymentResult(UUID.randomUUID(), false, "CARD_DECLINED")
                    : new PaymentResult(paymentId, true, null);
        }
    }

    FakeStore store;
    FakeCatalog catalog;
    FakeSeats seats;
    FakePayments payments;
    BookingService service;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        catalog = new FakeCatalog();
        seats = new FakeSeats();
        payments = new FakePayments();
        service = new BookingService(store, catalog, seats, payments, Clock.fixed(NOW, ZoneOffset.UTC), 30, 4);
    }

    static CreateBookingCommand cmd(String... seatNos) {
        return new CreateBookingCommand(TRIP, List.of(seatNos), "Taro Yamada", "taro@example.com", "mock-ok");
    }

    // ---- happy path ----

    @Test
    void happyPathConfirmsTheBookingAndChargesTheRightAmountOnce() {
        Booking b = service.createBooking(cmd("1A", "1B"));

        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertEquals(payments.paymentId, b.getPaymentId());
        assertNull(b.getFailureReason());
        assertEquals(FARE * 2, b.getTotalYen());
        assertEquals(List.of("hold", "confirm"), seats.calls);
        assertEquals(List.of(FARE * 2), payments.amounts);
        assertEquals(List.of("booking-" + b.getId()), payments.keys, "idempotency key is derived from the booking id");
        assertEquals(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED), store.savedStatuses);
    }

    // ---- rejected before anything is saved ----

    @Test
    void invalidRequestSavesNothingAndCallsNobody() {
        assertThrows(InvalidBookingRequestException.class, () -> service.createBooking(cmd()));
        assertTrue(store.rows.isEmpty());
        assertTrue(seats.calls.isEmpty());
        assertTrue(payments.keys.isEmpty());
    }

    @Test
    void unknownTripSavesNothing() {
        catalog.failure = new TripNotFoundException(TRIP);
        assertThrows(TripNotFoundException.class, () -> service.createBooking(cmd("1A")));
        assertTrue(store.rows.isEmpty());
    }

    @Test
    void catalogDownSavesNothing() {
        catalog.failure = new DownstreamUnavailableException("catalog-service", "down", null);
        assertThrows(DownstreamUnavailableException.class, () -> service.createBooking(cmd("1A")));
        assertTrue(store.rows.isEmpty());
        assertTrue(seats.calls.isEmpty());
    }

    @Test
    void cancelledTripIsRejectedBeforeSaving() {
        catalog.status = "CANCELLED";
        var e = assertThrows(BookingNotAllowedException.class, () -> service.createBooking(cmd("1A")));
        assertEquals("TRIP_NOT_BOOKABLE", e.getCode());
        assertTrue(store.rows.isEmpty());
    }

    @Test
    void tripInsideTheCutoffIsClosed() {
        catalog.departure = NOW.plus(Duration.ofMinutes(10));
        var e = assertThrows(BookingNotAllowedException.class, () -> service.createBooking(cmd("1A")));
        assertEquals("BOOKING_CLOSED", e.getCode());
        assertTrue(store.rows.isEmpty());
    }

    // ---- failures after the booking exists ----

    @Test
    void seatConflictFailsTheBookingWithoutChargingAnyone() {
        seats.holdFailure = new SeatConflictException(List.of("1B"));
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A", "1B")));

        assertEquals(FailureReason.SEAT_UNAVAILABLE, e.getCode());
        assertEquals(List.of("1B"), e.getSeatNos());
        assertEquals(store.only().getId(), e.getBookingId());
        assertEquals(BookingStatus.FAILED, store.only().getStatus());
        assertEquals(FailureReason.SEAT_UNAVAILABLE, store.only().getFailureReason());
        assertTrue(payments.keys.isEmpty(), "never charge when seats could not be held");
        assertEquals(List.of("hold"), seats.calls);
    }

    @Test
    void unknownSeatFailsTheBooking() {
        seats.holdFailure = new UnknownSeatException(List.of("99Z"));
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("99Z")));
        assertEquals(FailureReason.UNKNOWN_SEAT, e.getCode());
        assertEquals(FailureReason.UNKNOWN_SEAT, store.only().getFailureReason());
        assertTrue(payments.keys.isEmpty());
    }

    @Test
    void seatServiceDownDuringHoldFailsTheBookingAndTriesToRelease() {
        seats.holdFailure = new DownstreamUnavailableException("seat-service", "down", null);
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A")));
        assertEquals(FailureReason.SEAT_SERVICE_UNAVAILABLE, e.getCode());
        assertEquals(List.of("hold", "release"), seats.calls);
        assertTrue(payments.keys.isEmpty());
        assertEquals(BookingStatus.FAILED, store.only().getStatus());
    }

    @Test
    void seatServiceRejectingTheHoldNeverLeavesTheBookingPending() {
        seats.holdFailure = new TripNotFoundException(TRIP);
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A")));
        assertEquals(FailureReason.SEAT_SERVICE_UNAVAILABLE, e.getCode());
        assertEquals(BookingStatus.FAILED, store.only().getStatus());
    }

    @Test
    void declinedPaymentReleasesTheHoldAndFailsTheBooking() {
        payments.decline = true;
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A")));

        assertEquals(FailureReason.PAYMENT_DECLINED, e.getCode());
        assertEquals(List.of("hold", "release"), seats.calls, "seats are given back, never confirmed");
        assertEquals(BookingStatus.FAILED, store.only().getStatus());
        assertEquals(FailureReason.PAYMENT_DECLINED, store.only().getFailureReason());
        assertNull(store.only().getPaymentId());
    }

    @Test
    void paymentServiceDownReleasesTheHoldAndRecordsUnknownOutcome() {
        payments.failure = new DownstreamUnavailableException("payment-service", "timeout", null);
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A")));

        assertEquals(FailureReason.PAYMENT_UNAVAILABLE, e.getCode());
        assertEquals(List.of("hold", "release"), seats.calls);
        assertEquals(FailureReason.PAYMENT_UNAVAILABLE, store.only().getFailureReason());
    }

    @Test
    void failedReleaseDoesNotHideTheRealFailure() {
        payments.decline = true;
        seats.releaseFailure = new DownstreamUnavailableException("seat-service", "down", null);
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A")));
        assertEquals(FailureReason.PAYMENT_DECLINED, e.getCode(), "the customer still hears about the declined card");
        assertEquals(BookingStatus.FAILED, store.only().getStatus());
    }

    @Test
    void paidButSeatsNotConfirmedIsRecordedWithThePaymentId() {
        seats.confirmFailure = new HoldExpiredException(UUID.randomUUID());
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A")));

        assertEquals(FailureReason.PAID_BUT_NOT_CONFIRMED, e.getCode());
        Booking b = store.only();
        assertEquals(BookingStatus.FAILED, b.getStatus());
        assertEquals(payments.paymentId, b.getPaymentId(), "the payment id is kept so it can be refunded in M4");
        assertEquals(List.of("hold", "confirm"), seats.calls, "no release: the money question must be settled first");
    }

    @Test
    void seatServiceDownDuringConfirmIsAlsoPaidButNotConfirmed() {
        seats.confirmFailure = new DownstreamUnavailableException("seat-service", "down", null);
        var e = assertThrows(BookingFailedException.class, () -> service.createBooking(cmd("1A")));
        assertEquals(FailureReason.PAID_BUT_NOT_CONFIRMED, e.getCode());
        assertNotNull(store.only().getPaymentId());
    }

    // ---- reads ----

    @Test
    void canFindABookingByIdAndByReferenceCaseInsensitively() {
        Booking b = service.createBooking(cmd("1A"));
        assertEquals(b.getId(), service.get(b.getId()).getId());
        assertEquals(b.getId(), service.getByReference(" " + b.getReference().toLowerCase() + " ").getId());
    }

    @Test
    void unknownBookingIsNotFound() {
        assertThrows(BookingNotFoundException.class, () -> service.get(UUID.randomUUID()));
        assertThrows(BookingNotFoundException.class, () -> service.getByReference("KP-NOPE"));
        assertThrows(BookingNotFoundException.class, () -> service.getByReference(null));
    }
}

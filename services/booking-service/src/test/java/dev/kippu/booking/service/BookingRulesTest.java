package dev.kippu.booking.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingRulesTest {

    private static final UUID TRIP = UUID.randomUUID();

    private static CreateBookingCommand cmd(List<String> seats, String name, String email, String method) {
        return new CreateBookingCommand(TRIP, seats, name, email, method);
    }

    private static CreateBookingCommand ok() {
        return cmd(List.of("1A", "1B"), "Taro Yamada", "taro@example.com", "mock-ok");
    }

    @Test
    void validRequestIsAcceptedAndCleaned() {
        var c = BookingRules.validate(cmd(List.of(" 1A ", "1B"), "  Taro  ", " taro@example.com ", null), 4);
        assertEquals(List.of("1A", "1B"), c.seatNos());
        assertEquals("Taro", c.customerName());
        assertEquals("taro@example.com", c.customerEmail());
        assertEquals("mock-ok", c.paymentMethod(), "defaults to mock-ok");
    }

    @Test
    void rejectsMissingBodyOrTrip() {
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(null, 4));
        assertThrows(InvalidBookingRequestException.class,
                () -> BookingRules.validate(new CreateBookingCommand(null, List.of("1A"), "T", "t@e.com", null), 4));
    }

    @Test
    void rejectsBadSeatLists() {
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(null, "T", "t@e.com", null), 4));
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(List.of(), "T", "t@e.com", null), 4));
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(List.of("1A", "1A"), "T", "t@e.com", null), 4));
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(List.of("1A", " 1A"), "T", "t@e.com", null), 4),
                "duplicates are detected after trimming");
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(List.of(" "), "T", "t@e.com", null), 4));
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(Arrays.asList("1A", null), "T", "t@e.com", null), 4));
        assertThrows(InvalidBookingRequestException.class,
                () -> BookingRules.validate(cmd(List.of("1A", "1B", "1C", "1D", "2A"), "T", "t@e.com", null), 4));
    }

    @Test
    void allowsExactlyTheMaximumNumberOfSeats() {
        assertDoesNotThrow(() -> BookingRules.validate(cmd(List.of("1A", "1B", "1C", "1D"), "T", "t@e.com", null), 4));
    }

    @Test
    void rejectsBadNameAndEmail() {
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(List.of("1A"), " ", "t@e.com", null), 4));
        assertThrows(InvalidBookingRequestException.class, () -> BookingRules.validate(cmd(List.of("1A"), null, "t@e.com", null), 4));
        assertThrows(InvalidBookingRequestException.class,
                () -> BookingRules.validate(cmd(List.of("1A"), "x".repeat(101), "t@e.com", null), 4));
        for (String bad : new String[] {null, "", "plain", "a@b", "a b@c.com", "@c.com", "a@@c.com"}) {
            assertThrows(InvalidBookingRequestException.class,
                    () -> BookingRules.validate(cmd(List.of("1A"), "T", bad, null), 4), "email: " + bad);
        }
    }

    @Test
    void rejectsUnknownPaymentMethodUpFront() {
        assertThrows(InvalidBookingRequestException.class,
                () -> BookingRules.validate(cmd(List.of("1A"), "T", "t@e.com", "bitcoin"), 4));
        assertDoesNotThrow(() -> BookingRules.validate(cmd(List.of("1A"), "T", "t@e.com", "mock-decline"), 4));
    }

    // ---- bookable check ----

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final Duration CUTOFF = Duration.ofMinutes(30);

    @Test
    void scheduledTripWellBeforeDepartureIsBookable() {
        assertDoesNotThrow(() -> BookingRules.checkBookable("SCHEDULED", NOW.plus(Duration.ofHours(3)), NOW, CUTOFF));
    }

    @Test
    void cancelledTripIsRejected() {
        var e = assertThrows(BookingNotAllowedException.class,
                () -> BookingRules.checkBookable("CANCELLED", NOW.plus(Duration.ofHours(3)), NOW, CUTOFF));
        assertEquals("TRIP_NOT_BOOKABLE", e.getCode());
    }

    @Test
    void salesCloseExactlyAtTheCutoff() {
        Instant departure = NOW.plus(CUTOFF);
        var e = assertThrows(BookingNotAllowedException.class,
                () -> BookingRules.checkBookable("SCHEDULED", departure, NOW, CUTOFF));
        assertEquals("BOOKING_CLOSED", e.getCode());
        // one second earlier is still open
        assertDoesNotThrow(() -> BookingRules.checkBookable("SCHEDULED", departure.plusSeconds(1), NOW, CUTOFF));
    }

    @Test
    void departedTripIsClosed() {
        assertThrows(BookingNotAllowedException.class,
                () -> BookingRules.checkBookable("SCHEDULED", NOW.minus(Duration.ofHours(1)), NOW, CUTOFF));
    }
}

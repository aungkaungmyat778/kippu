package dev.kippu.booking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Business rules as pure functions, so they can be tested without Spring, a database or a network. */
public final class BookingRules {

    /** Payment methods the mock payment service understands. */
    public static final Set<String> PAYMENT_METHODS = Set.of("mock-ok", "mock-decline");
    public static final String DEFAULT_PAYMENT_METHOD = "mock-ok";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private BookingRules() {
    }

    /** Returns a cleaned copy of the command, or throws InvalidBookingRequestException. */
    public static CreateBookingCommand validate(CreateBookingCommand c, int maxSeats) {
        if (c == null) {
            throw new InvalidBookingRequestException("Request body is required");
        }
        if (c.tripId() == null) {
            throw new InvalidBookingRequestException("tripId is required");
        }
        List<String> seats = cleanSeats(c.seatNos(), maxSeats);

        String name = c.customerName() == null ? "" : c.customerName().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new InvalidBookingRequestException("customerName is required (1 to 100 characters)");
        }
        String email = c.customerEmail() == null ? "" : c.customerEmail().trim();
        if (email.length() > 200 || !EMAIL.matcher(email).matches()) {
            throw new InvalidBookingRequestException("customerEmail must be a valid email address");
        }
        String method = (c.paymentMethod() == null || c.paymentMethod().isBlank())
                ? DEFAULT_PAYMENT_METHOD : c.paymentMethod().trim();
        if (!PAYMENT_METHODS.contains(method)) {
            throw new InvalidBookingRequestException("paymentMethod must be one of " + PAYMENT_METHODS);
        }
        return new CreateBookingCommand(c.tripId(), seats, name, email, method);
    }

    private static List<String> cleanSeats(List<String> requested, int maxSeats) {
        if (requested == null || requested.isEmpty()) {
            throw new InvalidBookingRequestException("seatNos must contain at least one seat");
        }
        List<String> cleaned = requested.stream().map(s -> s == null ? "" : s.trim()).toList();
        if (cleaned.stream().anyMatch(String::isEmpty)) {
            throw new InvalidBookingRequestException("seatNos must not contain blank values");
        }
        if (new HashSet<>(cleaned).size() != cleaned.size()) {
            throw new InvalidBookingRequestException("seatNos must not contain duplicates");
        }
        if (cleaned.size() > maxSeats) {
            throw new InvalidBookingRequestException("At most " + maxSeats + " seats per booking");
        }
        return cleaned;
    }

    /** Throws BookingNotAllowedException when the trip is cancelled or sales have closed. */
    public static void checkBookable(String tripStatus, Instant departureAt, Instant now, Duration cutoff) {
        if (!"SCHEDULED".equals(tripStatus)) {
            throw new BookingNotAllowedException("TRIP_NOT_BOOKABLE", "This trip is " + tripStatus + " and cannot be booked");
        }
        Instant salesClose = departureAt.minus(cutoff);
        if (!now.isBefore(salesClose)) {
            throw new BookingNotAllowedException("BOOKING_CLOSED",
                    "Sales for this trip closed " + cutoff.toMinutes() + " minutes before departure");
        }
    }
}

package dev.kippu.booking.service;

import java.util.List;
import java.util.UUID;

/**
 * The saga stopped and the booking was saved as FAILED. Carries the booking id so the customer
 * (and the logs) can refer to what happened.
 */
public class BookingFailedException extends RuntimeException {
    private final String code;
    private final UUID bookingId;
    private final List<String> seatNos;

    public BookingFailedException(String code, String message, UUID bookingId, List<String> seatNos) {
        super(message);
        this.code = code;
        this.bookingId = bookingId;
        this.seatNos = seatNos == null ? List.of() : List.copyOf(seatNos);
    }

    public String getCode() {
        return code;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public List<String> getSeatNos() {
        return seatNos;
    }
}

package dev.kippu.booking.service;

import java.util.Objects;

/** The request is valid but the trip cannot be booked right now (cancelled, or sales closed). */
public class BookingNotAllowedException extends RuntimeException {
    private final String code;

    public BookingNotAllowedException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code);
    }

    public String getCode() {
        return code;
    }
}

package dev.kippu.booking.service;

import java.util.UUID;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(UUID bookingId) {
        super("Seat hold for booking " + bookingId + " is no longer valid");
    }
}

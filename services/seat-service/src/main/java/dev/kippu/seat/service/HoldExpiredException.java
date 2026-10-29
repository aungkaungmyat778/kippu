package dev.kippu.seat.service;

import java.util.UUID;

/** Confirm was called but the booking has no live hold (it expired or was released). */
public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(UUID bookingId) {
        super("Booking " + bookingId + " has no live seat hold");
    }
}

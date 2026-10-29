package dev.kippu.booking.service;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(String what) {
        super("Booking not found: " + what);
    }
}

package dev.kippu.booking.service;

import java.util.List;

/** Seats said some of the requested seats are taken. */
public class SeatConflictException extends RuntimeException {
    private final List<String> seatNos;

    public SeatConflictException(List<String> seatNos) {
        super("Seats not available: " + seatNos);
        this.seatNos = seatNos == null ? List.of() : List.copyOf(seatNos);
    }

    public List<String> getSeatNos() {
        return seatNos;
    }
}

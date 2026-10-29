package dev.kippu.seat.service;

import java.util.List;

/** At least one requested seat is taken. Nothing was held (all-or-nothing). */
public class SeatConflictException extends RuntimeException {
    private final List<String> seatNos;

    public SeatConflictException(List<String> seatNos) {
        super("Seats not available: " + seatNos);
        this.seatNos = List.copyOf(seatNos);
    }

    public List<String> getSeatNos() {
        return seatNos;
    }
}

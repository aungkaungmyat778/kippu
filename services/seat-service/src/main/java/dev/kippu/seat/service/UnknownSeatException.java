package dev.kippu.seat.service;

import java.util.List;

/** The caller asked for seat numbers that do not exist on this trip. */
public class UnknownSeatException extends RuntimeException {
    private final List<String> seatNos;

    public UnknownSeatException(List<String> seatNos) {
        super("Unknown seats for this trip: " + seatNos);
        this.seatNos = List.copyOf(seatNos);
    }

    public List<String> getSeatNos() {
        return seatNos;
    }
}

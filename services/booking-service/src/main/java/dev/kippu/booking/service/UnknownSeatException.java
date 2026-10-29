package dev.kippu.booking.service;

import java.util.List;

public class UnknownSeatException extends RuntimeException {
    private final List<String> seatNos;

    public UnknownSeatException(List<String> seatNos) {
        super("Unknown seats for this trip: " + seatNos);
        this.seatNos = seatNos == null ? List.of() : List.copyOf(seatNos);
    }

    public List<String> getSeatNos() {
        return seatNos;
    }
}

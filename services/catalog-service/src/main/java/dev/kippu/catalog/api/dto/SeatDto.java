package dev.kippu.catalog.api.dto;

import dev.kippu.catalog.domain.LayoutSeat;

public record SeatDto(String seatNo, int row, String column) {

    public static SeatDto from(LayoutSeat seat) {
        return new SeatDto(seat.getSeatNo(), seat.getRowNo(), seat.getColLabel());
    }
}

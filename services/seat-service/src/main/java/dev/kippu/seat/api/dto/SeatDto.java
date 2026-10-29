package dev.kippu.seat.api.dto;

import dev.kippu.seat.domain.SeatStatus;

public record SeatDto(String seatNo, int row, String column, SeatStatus status) {
}

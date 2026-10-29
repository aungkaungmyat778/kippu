package dev.kippu.seat.api.dto;

import java.util.List;
import java.util.UUID;

public record SeatMapDto(UUID tripId, int total, int available, int held, int sold, List<SeatDto> seats) {
}

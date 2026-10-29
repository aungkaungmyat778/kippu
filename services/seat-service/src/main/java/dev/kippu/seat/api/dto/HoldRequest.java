package dev.kippu.seat.api.dto;

import java.util.List;
import java.util.UUID;

public record HoldRequest(UUID bookingId, List<String> seatNos) {
}

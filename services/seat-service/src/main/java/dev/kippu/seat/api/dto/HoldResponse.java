package dev.kippu.seat.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HoldResponse(UUID tripId, UUID bookingId, List<String> seatNos, Instant expiresAt) {
}

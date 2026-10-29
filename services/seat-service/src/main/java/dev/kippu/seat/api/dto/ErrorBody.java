package dev.kippu.seat.api.dto;

import java.util.List;

/** Machine-readable code for the Booking service, plus a human message. */
public record ErrorBody(String code, String message, List<String> seatNos) {
}

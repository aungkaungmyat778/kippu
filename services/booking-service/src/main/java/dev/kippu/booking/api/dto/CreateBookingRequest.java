package dev.kippu.booking.api.dto;

import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(UUID tripId, List<String> seatNos, String customerName,
                                   String customerEmail, String paymentMethod) {
}

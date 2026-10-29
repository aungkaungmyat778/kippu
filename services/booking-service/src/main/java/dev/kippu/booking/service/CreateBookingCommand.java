package dev.kippu.booking.service;

import java.util.List;
import java.util.UUID;

public record CreateBookingCommand(UUID tripId, List<String> seatNos, String customerName,
                                   String customerEmail, String paymentMethod) {
}

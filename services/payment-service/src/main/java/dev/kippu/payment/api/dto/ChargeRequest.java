package dev.kippu.payment.api.dto;

import java.util.UUID;

/** method is optional and defaults to mock-ok. */
public record ChargeRequest(UUID bookingId, int amountYen, String method) {
}

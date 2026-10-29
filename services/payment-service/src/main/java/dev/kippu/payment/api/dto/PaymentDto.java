package dev.kippu.payment.api.dto;

import dev.kippu.payment.domain.Payment;
import dev.kippu.payment.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentDto(UUID paymentId, UUID bookingId, int amountYen, String method,
                         PaymentStatus status, String failureReason, Instant createdAt) {

    public static PaymentDto from(Payment p) {
        return new PaymentDto(p.getId(), p.getBookingId(), p.getAmountYen(), p.getMethod(),
                p.getStatus(), p.getFailureReason(), p.getCreatedAt());
    }
}

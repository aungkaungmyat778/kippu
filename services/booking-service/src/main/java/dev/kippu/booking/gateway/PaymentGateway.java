package dev.kippu.booking.gateway;

import java.util.UUID;

/**
 * The Payment service as Booking sees it. A declined card is a normal result (succeeded = false),
 * not an exception. Throws DownstreamUnavailableException when the outcome is unknown.
 */
public interface PaymentGateway {

    PaymentResult charge(UUID bookingId, String idempotencyKey, int amountYen, String method);

    record PaymentResult(UUID paymentId, boolean succeeded, String failureReason) {
    }
}

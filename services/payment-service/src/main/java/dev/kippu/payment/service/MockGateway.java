package dev.kippu.payment.service;

import dev.kippu.payment.domain.PaymentStatus;

/**
 * Stands in for a real card processor. The outcome depends only on the payment method, so
 * the same request always gets the same answer and tests are predictable.
 *
 * mock-ok       the charge succeeds
 * mock-decline  the card is declined
 *
 * A real processor is not instant or side-effect free, so a real version would record a
 * PROCESSING row first and settle it later. That state is added when a timeout scenario exists (M4).
 */
public final class MockGateway {

    public static final String OK = "mock-ok";
    public static final String DECLINE = "mock-decline";

    public record Decision(PaymentStatus status, String failureReason) {
    }

    private MockGateway() {
    }

    public static Decision decide(String method) {
        return switch (method) {
            case OK -> new Decision(PaymentStatus.SUCCEEDED, null);
            case DECLINE -> new Decision(PaymentStatus.DECLINED, "CARD_DECLINED");
            default -> throw new InvalidPaymentRequestException(
                    "Unknown payment method '" + method + "'. Use " + OK + " or " + DECLINE);
        };
    }
}

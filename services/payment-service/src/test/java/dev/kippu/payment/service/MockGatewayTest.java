package dev.kippu.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.kippu.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

class MockGatewayTest {

    @Test
    void okSucceeds() {
        MockGateway.Decision d = MockGateway.decide("mock-ok");
        assertEquals(PaymentStatus.SUCCEEDED, d.status());
        assertEquals(null, d.failureReason());
    }

    @Test
    void declineIsDeclinedWithReason() {
        MockGateway.Decision d = MockGateway.decide("mock-decline");
        assertEquals(PaymentStatus.DECLINED, d.status());
        assertEquals("CARD_DECLINED", d.failureReason());
    }

    @Test
    void unknownMethodIsRejected() {
        assertThrows(InvalidPaymentRequestException.class, () -> MockGateway.decide("visa-1234"));
    }
}

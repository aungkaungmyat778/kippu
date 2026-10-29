package dev.kippu.payment.service;

import dev.kippu.payment.domain.Payment;
import dev.kippu.payment.repository.PaymentRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_KEY_LENGTH = 64;

    /** replayed = true means this key had been used before and we returned the original result. */
    public record ChargeResult(Payment payment, boolean replayed) {
    }

    private final PaymentRepository payments;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, Clock clock) {
        this.payments = payments;
        this.clock = clock;
    }

    /**
     * Charges once per idempotency key. Asking again with the same key and the same request
     * returns the original result without charging again. The same key with a different
     * request is rejected, because it means the caller has a bug.
     */
    public ChargeResult charge(String idempotencyKey, UUID bookingId, int amountYen, String method) {
        String key = idempotencyKey == null ? "" : idempotencyKey.trim();
        if (key.isEmpty()) {
            throw new InvalidPaymentRequestException("Idempotency-Key header is required");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new InvalidPaymentRequestException("Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters");
        }
        if (bookingId == null) {
            throw new InvalidPaymentRequestException("bookingId is required");
        }
        if (amountYen <= 0) {
            throw new InvalidPaymentRequestException("amountYen must be positive");
        }
        String chosenMethod = (method == null || method.isBlank()) ? MockGateway.OK : method.trim();
        MockGateway.Decision decision = MockGateway.decide(chosenMethod);

        int inserted = payments.insertIfAbsent(UUID.randomUUID(), bookingId, key, amountYen, chosenMethod,
                decision.status().name(), decision.failureReason(), clock.instant());
        Payment stored = payments.findByIdempotencyKey(key)
                .orElseThrow(() -> new IllegalStateException("Payment vanished for key " + key));
        boolean replayed = inserted == 0;

        if (replayed && !sameRequest(stored, bookingId, amountYen, chosenMethod)) {
            throw new IdempotencyKeyReusedException(key);
        }
        log.info("Payment {} for booking {}: {} (replayed={})", stored.getId(), bookingId, stored.getStatus(), replayed);
        return new ChargeResult(stored, replayed);
    }

    static boolean sameRequest(Payment stored, UUID bookingId, int amountYen, String method) {
        return stored.getBookingId().equals(bookingId)
                && stored.getAmountYen() == amountYen
                && stored.getMethod().equals(method);
    }

    public Payment get(UUID id) {
        return payments.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}

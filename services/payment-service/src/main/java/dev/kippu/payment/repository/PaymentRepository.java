package dev.kippu.payment.repository;

import dev.kippu.payment.domain.Payment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);

    /**
     * Records the payment unless this idempotency key was already used. Returns 1 when we
     * recorded it, 0 when the key already existed. When two requests with the same key arrive
     * together, the database lets one insert and makes the other wait, then report 0.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO payments (id, booking_id, idempotency_key, amount_yen, method, status, failure_reason, created_at)
            VALUES (:id, :bookingId, :idempotencyKey, :amountYen, :method, :status, :failureReason, :createdAt)
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("bookingId") UUID bookingId,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("amountYen") int amountYen,
                       @Param("method") String method,
                       @Param("status") String status,
                       @Param("failureReason") String failureReason,
                       @Param("createdAt") Instant createdAt);
}

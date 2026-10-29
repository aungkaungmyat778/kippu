package dev.kippu.seat.repository;

import dev.kippu.seat.domain.SeatStatus;
import dev.kippu.seat.domain.TripSeat;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every state change below is ONE SQL statement. The database decides in a single step
 * whether a seat may change hands, so there is no gap between "check" and "update" for
 * another request to slip into.
 *
 * Each statement locks the rows it touches in the same order (trip_id, seat_no) with
 * "ORDER BY ... FOR UPDATE". Two requests that want the same seats in opposite order
 * then queue up instead of deadlocking.
 */
public interface TripSeatRepository extends JpaRepository<TripSeat, UUID> {

    // ---------- reads (Spring Data builds these queries from the method names) ----------

    boolean existsByTripId(UUID tripId);

    List<TripSeat> findByTripIdOrderByRowNoAscColLabelAsc(UUID tripId);

    List<TripSeat> findByTripIdAndSeatNoIn(UUID tripId, Collection<String> seatNos);

    long countByBookingIdAndStatus(UUID bookingId, SeatStatus status);

    List<TripSeat> findByBookingIdAndStatus(UUID bookingId, SeatStatus status);

    @Transactional
    long deleteByTripId(UUID tripId);

    // ---------- writes ----------

    /** Creates a seat unless it already exists, so provisioning a trip twice is harmless. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO trip_seats (trip_id, seat_no, row_no, col_label)
            VALUES (:tripId, :seatNo, :rowNo, :colLabel)
            ON CONFLICT (trip_id, seat_no) DO NOTHING
            """)
    int insertIfAbsent(@Param("tripId") UUID tripId,
                       @Param("seatNo") String seatNo,
                       @Param("rowNo") int rowNo,
                       @Param("colLabel") String colLabel);

    /**
     * Holds the seats for a booking. A seat can be taken when it is available, when its
     * hold has expired, or when this same booking already holds it (a retry).
     * Returns how many seats were taken; the caller compares that with how many it asked for.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE trip_seats
               SET status = 'HELD', booking_id = :bookingId, hold_expires_at = :expiresAt
             WHERE id IN (
                   SELECT id FROM trip_seats
                    WHERE trip_id = :tripId
                      AND seat_no IN (:seatNos)
                      AND (status = 'AVAILABLE'
                           OR (status = 'HELD' AND (hold_expires_at <= :now OR booking_id = :bookingId)))
                    ORDER BY trip_id, seat_no
                      FOR UPDATE)
            """)
    int hold(@Param("tripId") UUID tripId,
             @Param("seatNos") Collection<String> seatNos,
             @Param("bookingId") UUID bookingId,
             @Param("expiresAt") Instant expiresAt,
             @Param("now") Instant now);

    /** Turns this booking's live holds into sales. Expired holds are not confirmed. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE trip_seats
               SET status = 'SOLD', hold_expires_at = NULL
             WHERE id IN (
                   SELECT id FROM trip_seats
                    WHERE booking_id = :bookingId
                      AND status = 'HELD'
                      AND hold_expires_at > :now
                    ORDER BY trip_id, seat_no
                      FOR UPDATE)
            """)
    int confirm(@Param("bookingId") UUID bookingId, @Param("now") Instant now);

    /** Frees this booking's held seats. Sold seats are never touched. Safe to repeat. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE trip_seats
               SET status = 'AVAILABLE', booking_id = NULL, hold_expires_at = NULL
             WHERE id IN (
                   SELECT id FROM trip_seats
                    WHERE booking_id = :bookingId
                      AND status = 'HELD'
                    ORDER BY trip_id, seat_no
                      FOR UPDATE)
            """)
    int release(@Param("bookingId") UUID bookingId);

    /** The cleanup job: frees every hold whose time has run out. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE trip_seats
               SET status = 'AVAILABLE', booking_id = NULL, hold_expires_at = NULL
             WHERE id IN (
                   SELECT id FROM trip_seats
                    WHERE status = 'HELD'
                      AND hold_expires_at <= :now
                    ORDER BY trip_id, seat_no
                      FOR UPDATE)
            """)
    int releaseExpired(@Param("now") Instant now);
}

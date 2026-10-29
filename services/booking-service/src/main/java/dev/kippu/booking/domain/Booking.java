package dev.kippu.booking.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One customer purchase. The only legal moves are PENDING to CONFIRMED and PENDING to FAILED;
 * anything else is a bug and throws.
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, length = 12)
    private String reference;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "failure_reason", length = 60)
    private String failureReason;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 200)
    private String customerEmail;

    @Column(name = "seat_count", nullable = false)
    private int seatCount;

    @Column(name = "fare_yen", nullable = false)
    private int fareYen;

    @Column(name = "total_yen", nullable = false)
    private int totalYen;

    @Column(name = "departure_at", nullable = false)
    private Instant departureAt;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_no", nullable = false, length = 5)
    private List<String> seatNos = new ArrayList<>();

    protected Booking() {
        // required by JPA
    }

    public static Booking pending(UUID id, String reference, UUID tripId, String customerName,
                                  String customerEmail, List<String> seatNos, int fareYen,
                                  Instant departureAt, Instant now) {
        Booking b = new Booking();
        b.id = id;
        b.reference = reference;
        b.tripId = tripId;
        b.status = BookingStatus.PENDING;
        b.customerName = customerName;
        b.customerEmail = customerEmail;
        b.seatNos = new ArrayList<>(seatNos);
        b.seatCount = seatNos.size();
        b.fareYen = fareYen;
        b.totalYen = fareYen * seatNos.size();
        b.departureAt = departureAt;
        b.createdAt = now;
        b.updatedAt = now;
        return b;
    }

    public Booking confirm(UUID paymentId, Instant now) {
        requirePending("confirm");
        this.status = BookingStatus.CONFIRMED;
        this.paymentId = paymentId;
        this.updatedAt = now;
        return this;
    }

    /** paymentId is set only when money was taken (PAID_BUT_NOT_CONFIRMED), otherwise null. */
    public Booking fail(String reason, UUID paymentId, Instant now) {
        requirePending("fail");
        this.status = BookingStatus.FAILED;
        this.failureReason = reason;
        this.paymentId = paymentId;
        this.updatedAt = now;
        return this;
    }

    private void requirePending(String action) {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Cannot " + action + " booking " + id + " in status " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public UUID getTripId() {
        return tripId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public int getFareYen() {
        return fareYen;
    }

    public int getTotalYen() {
        return totalYen;
    }

    public Instant getDepartureAt() {
        return departureAt;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<String> getSeatNos() {
        return List.copyOf(seatNos);
    }
}

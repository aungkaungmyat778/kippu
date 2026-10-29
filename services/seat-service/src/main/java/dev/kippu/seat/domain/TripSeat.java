package dev.kippu.seat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model of a seat. State changes are made with atomic SQL statements in
 * TripSeatRepository, never by editing this object and saving it, so two requests
 * can never both believe they changed the same seat.
 */
@Entity
@Table(name = "trip_seats")
public class TripSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "seat_no", nullable = false, length = 5)
    private String seatNo;

    @Column(name = "row_no", nullable = false)
    private int rowNo;

    @Column(name = "col_label", nullable = false, length = 2)
    private String colLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    protected TripSeat() {
        // required by JPA
    }

    public UUID getTripId() {
        return tripId;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public int getRowNo() {
        return rowNo;
    }

    public String getColLabel() {
        return colLabel;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }
}

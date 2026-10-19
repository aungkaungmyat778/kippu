package dev.kippu.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One real run of a schedule on one date. This is the thing a customer books. */
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_id", nullable = false)
    private BusLayout layout;

    @Column(name = "departure_at", nullable = false)
    private Instant departureAt;

    @Column(name = "arrival_at", nullable = false)
    private Instant arrivalAt;

    @Column(name = "fare_yen", nullable = false)
    private int fareYen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    protected Trip() {
        // required by JPA
    }

    public Trip(Schedule schedule, Route route, BusLayout layout,
                Instant departureAt, Instant arrivalAt, int fareYen) {
        this.schedule = schedule;
        this.route = route;
        this.layout = layout;
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
        this.fareYen = fareYen;
        this.status = TripStatus.SCHEDULED;
    }

    public UUID getId() {
        return id;
    }

    public Route getRoute() {
        return route;
    }

    public BusLayout getLayout() {
        return layout;
    }

    public Instant getDepartureAt() {
        return departureAt;
    }

    public Instant getArrivalAt() {
        return arrivalAt;
    }

    public int getFareYen() {
        return fareYen;
    }

    public TripStatus getStatus() {
        return status;
    }
}

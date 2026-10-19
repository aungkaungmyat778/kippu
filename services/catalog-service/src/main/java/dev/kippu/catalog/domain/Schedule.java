package dev.kippu.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_id", nullable = false)
    private BusLayout layout;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "days_of_week", nullable = false, length = 80)
    private String daysOfWeek;

    @Column(name = "fare_yen", nullable = false)
    private int fareYen;

    @Column(nullable = false)
    private boolean active;

    protected Schedule() {
        // required by JPA
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

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public int getFareYen() {
        return fareYen;
    }

    public boolean isActive() {
        return active;
    }

    /** The days this schedule runs on, parsed from the days_of_week column. */
    public Set<DayOfWeek> days() {
        return DaysOfWeek.parse(daysOfWeek);
    }
}

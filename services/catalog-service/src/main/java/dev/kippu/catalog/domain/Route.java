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
import java.util.UUID;

@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_terminal_id", nullable = false)
    private Terminal origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_terminal_id", nullable = false)
    private Terminal destination;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    protected Route() {
        // required by JPA
    }

    public UUID getId() {
        return id;
    }

    public Terminal getOrigin() {
        return origin;
    }

    public Terminal getDestination() {
        return destination;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }
}

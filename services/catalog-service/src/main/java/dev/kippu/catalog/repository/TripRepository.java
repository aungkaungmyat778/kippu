package dev.kippu.catalog.repository;

import dev.kippu.catalog.domain.Trip;
import dev.kippu.catalog.domain.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    /**
     * Trips between two cities that depart inside [start, end) and have not left yet.
     * The "join fetch" parts load the route, terminals and layout in the same query,
     * so building the response does not fire one extra query per trip (the N+1 problem).
     */
    @Query("""
            select t from Trip t
              join fetch t.route r
              join fetch r.origin o
              join fetch r.destination d
              join fetch t.layout l
            where lower(o.city) = lower(:originCity)
              and lower(d.city) = lower(:destinationCity)
              and t.departureAt >= :start
              and t.departureAt < :end
              and t.departureAt > :now
              and t.status = :status
            order by t.departureAt
            """)
    List<Trip> search(@Param("originCity") String originCity,
                      @Param("destinationCity") String destinationCity,
                      @Param("start") Instant start,
                      @Param("end") Instant end,
                      @Param("now") Instant now,
                      @Param("status") TripStatus status);

    @Query("""
            select t from Trip t
              join fetch t.route r
              join fetch r.origin
              join fetch r.destination
              join fetch t.layout
            where t.id = :id
            """)
    Optional<Trip> findDetailById(@Param("id") UUID id);

    /** Spring Data builds this query from the method name. */
    boolean existsByScheduleIdAndDepartureAt(UUID scheduleId, Instant departureAt);
}

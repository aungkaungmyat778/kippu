package dev.kippu.catalog.repository;

import dev.kippu.catalog.domain.BusLayout;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusLayoutRepository extends JpaRepository<BusLayout, UUID> {

    /** Loads the layout and all its seats in one query. */
    @Query("select l from BusLayout l left join fetch l.seats where l.id = :id")
    Optional<BusLayout> findWithSeatsById(@Param("id") UUID id);
}

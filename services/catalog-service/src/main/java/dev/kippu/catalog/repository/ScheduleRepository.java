package dev.kippu.catalog.repository;

import dev.kippu.catalog.domain.Schedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    @Query("""
            select s from Schedule s
              join fetch s.route
              join fetch s.layout
            where s.active = true
            """)
    List<Schedule> findActiveWithRouteAndLayout();
}

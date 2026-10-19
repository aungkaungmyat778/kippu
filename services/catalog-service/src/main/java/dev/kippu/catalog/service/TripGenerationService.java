package dev.kippu.catalog.service;

import dev.kippu.catalog.domain.Schedule;
import dev.kippu.catalog.domain.Trip;
import dev.kippu.catalog.repository.ScheduleRepository;
import dev.kippu.catalog.repository.TripRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TripGenerationService.class);

    private final ScheduleRepository schedules;
    private final TripRepository trips;
    private final Clock clock;
    private final ZoneId zone;

    public TripGenerationService(ScheduleRepository schedules, TripRepository trips, Clock clock, ZoneId zone) {
        this.schedules = schedules;
        this.trips = trips;
        this.clock = clock;
        this.zone = zone;
    }

    /**
     * Creates the missing trips for every active schedule over the next horizonDays days.
     * Safe to run repeatedly: a trip that already exists is skipped, and the database
     * UNIQUE (schedule_id, departure_at) constraint backs that up.
     *
     * @return how many trips were created
     */
    @Transactional
    public int generate(int horizonDays) {
        if (horizonDays < 1 || horizonDays > 120) {
            throw new InvalidRequestException("days must be between 1 and 120");
        }
        Instant now = clock.instant();
        LocalDate today = now.atZone(zone).toLocalDate();
        int created = 0;

        for (Schedule schedule : schedules.findActiveWithRouteAndLayout()) {
            for (Instant departure : TripPlanner.departures(
                    schedule.getDepartureTime(), schedule.days(), zone, today, horizonDays, now)) {
                if (trips.existsByScheduleIdAndDepartureAt(schedule.getId(), departure)) {
                    continue;
                }
                Instant arrival = TripPlanner.arrival(departure, schedule.getRoute().getDurationMinutes());
                trips.save(new Trip(schedule, schedule.getRoute(), schedule.getLayout(),
                        departure, arrival, schedule.getFareYen()));
                created++;
            }
        }
        log.info("Trip generation finished: {} new trips for the next {} days", created, horizonDays);
        return created;
    }
}

package dev.kippu.catalog.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The date and time rules for generating trips, kept free of Spring and the database
 * so they are easy to test with plain JUnit.
 */
public final class TripPlanner {

    private TripPlanner() {
    }

    /**
     * Departure instants for one schedule over the next horizonDays days, counting from today.
     * A schedule time of 22:30 means 22:30 in the given zone. Departures that are not
     * after "now" are skipped, so today's 22:30 bus is not created at 23:00.
     */
    public static List<Instant> departures(LocalTime departureTime, Set<DayOfWeek> runsOn, ZoneId zone,
                                           LocalDate today, int horizonDays, Instant now) {
        List<Instant> result = new ArrayList<>();
        for (int i = 0; i < horizonDays; i++) {
            LocalDate date = today.plusDays(i);
            if (!runsOn.contains(date.getDayOfWeek())) {
                continue;
            }
            Instant departure = date.atTime(departureTime).atZone(zone).toInstant();
            if (departure.isAfter(now)) {
                result.add(departure);
            }
        }
        return result;
    }

    /** Overnight trips are easy: adding minutes to an Instant rolls over to the next date by itself. */
    public static Instant arrival(Instant departure, int durationMinutes) {
        return departure.plus(Duration.ofMinutes(durationMinutes));
    }
}

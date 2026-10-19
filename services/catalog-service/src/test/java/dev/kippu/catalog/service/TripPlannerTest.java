package dev.kippu.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TripPlannerTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Set<DayOfWeek> DAILY = EnumSet.allOf(DayOfWeek.class);
    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    // 2026-10-12 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    @Test
    void dailyScheduleGivesOneDeparturePerDay() {
        Instant now = MONDAY.atStartOfDay(TOKYO).toInstant();

        List<Instant> departures =
                TripPlanner.departures(LocalTime.of(22, 30), DAILY, TOKYO, MONDAY, 7, now);

        assertEquals(7, departures.size());
        // 22:30 in Tokyo (UTC+9) is 13:30 UTC on the same date.
        assertEquals(Instant.parse("2026-10-12T13:30:00Z"), departures.get(0));
    }

    @Test
    void weekdayScheduleSkipsWeekends() {
        Instant now = MONDAY.atStartOfDay(TOKYO).toInstant();

        List<Instant> departures =
                TripPlanner.departures(LocalTime.of(7, 30), WEEKDAYS, TOKYO, MONDAY, 14, now);

        // Two full weeks starting on a Monday contain ten weekdays.
        assertEquals(10, departures.size());
        for (Instant departure : departures) {
            DayOfWeek day = departure.atZone(TOKYO).getDayOfWeek();
            assertTrue(day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY);
        }
    }

    @Test
    void departureThatAlreadyLeftTodayIsSkipped() {
        // It is 23:00 on Monday, so Monday's 22:30 bus has gone.
        Instant now = LocalDateTime.of(2026, 10, 12, 23, 0).atZone(TOKYO).toInstant();

        List<Instant> departures =
                TripPlanner.departures(LocalTime.of(22, 30), DAILY, TOKYO, MONDAY, 7, now);

        assertEquals(6, departures.size());
        assertEquals(LocalDate.of(2026, 10, 13), departures.get(0).atZone(TOKYO).toLocalDate());
    }

    @Test
    void overnightTripArrivesOnTheNextDate() {
        Instant departure = LocalDateTime.of(2026, 10, 12, 22, 30).atZone(TOKYO).toInstant();

        Instant arrival = TripPlanner.arrival(departure, 540);

        assertEquals(LocalDateTime.of(2026, 10, 13, 7, 30), arrival.atZone(TOKYO).toLocalDateTime());
    }

    @Test
    void scheduleThatRunsOnNoDaysGivesNoDepartures() {
        Instant now = MONDAY.atStartOfDay(TOKYO).toInstant();

        List<Instant> departures =
                TripPlanner.departures(LocalTime.of(22, 30), EnumSet.noneOf(DayOfWeek.class), TOKYO, MONDAY, 30, now);

        assertTrue(departures.isEmpty());
    }
}

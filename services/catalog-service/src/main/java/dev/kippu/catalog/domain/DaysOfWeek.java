package dev.kippu.catalog.domain;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Reads and writes the days_of_week column, for example "MONDAY,FRIDAY". */
public final class DaysOfWeek {

    private DaysOfWeek() {
    }

    /** Throws IllegalArgumentException for a name that is not a day of the week. */
    public static Set<DayOfWeek> parse(String value) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (value == null || value.isBlank()) {
            return days;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .forEach(days::add);
        return days;
    }

    public static String format(Set<DayOfWeek> days) {
        return days.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }
}

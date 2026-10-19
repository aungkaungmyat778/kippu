package dev.kippu.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DaysOfWeekTest {

    @Test
    void parsesCommaSeparatedNamesAndIgnoresSpaces() {
        Set<DayOfWeek> days = DaysOfWeek.parse("MONDAY, FRIDAY");

        assertEquals(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), days);
    }

    @Test
    void blankOrNullMeansNoDays() {
        assertTrue(DaysOfWeek.parse("").isEmpty());
        assertTrue(DaysOfWeek.parse(null).isEmpty());
    }

    @Test
    void formatsInWeekOrder() {
        String text = DaysOfWeek.format(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY));

        assertEquals("MONDAY,FRIDAY", text);
    }

    @Test
    void unknownDayNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DaysOfWeek.parse("MONDAY,FUNDAY"));
    }
}

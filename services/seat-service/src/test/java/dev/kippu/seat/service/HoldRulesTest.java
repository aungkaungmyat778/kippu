package dev.kippu.seat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class HoldRulesTest {

    @Test
    void acceptsOneToFourSeatsAndTrimsThem() {
        assertEquals(List.of("1A", "1B"), HoldRules.validate(List.of(" 1A", "1B "), 4));
        assertEquals(4, HoldRules.validate(List.of("1A", "1B", "2A", "2B"), 4).size());
    }

    @Test
    void rejectsEmptyOrNull() {
        assertThrows(InvalidHoldRequestException.class, () -> HoldRules.validate(null, 4));
        assertThrows(InvalidHoldRequestException.class, () -> HoldRules.validate(List.of(), 4));
    }

    @Test
    void rejectsMoreThanTheLimit() {
        assertThrows(InvalidHoldRequestException.class,
                () -> HoldRules.validate(List.of("1A", "1B", "2A", "2B", "3A"), 4));
    }

    @Test
    void rejectsDuplicatesAfterTrimming() {
        assertThrows(InvalidHoldRequestException.class,
                () -> HoldRules.validate(List.of("1A", " 1A"), 4));
    }

    @Test
    void rejectsBlankAndNullEntries() {
        assertThrows(InvalidHoldRequestException.class, () -> HoldRules.validate(List.of("1A", "  "), 4));
        assertThrows(InvalidHoldRequestException.class, () -> HoldRules.validate(Arrays.asList("1A", null), 4));
    }
}

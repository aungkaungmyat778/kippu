package dev.kippu.booking.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.random.RandomGenerator;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BookingReferenceTest {

    private static final Pattern FORMAT = Pattern.compile("^KP-[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{8}$");

    @Test
    void hasExpectedFormatAndFitsTheColumn() {
        RandomGenerator random = new java.util.Random(42);
        for (int i = 0; i < 200; i++) {
            String ref = BookingReference.generate(random);
            assertTrue(FORMAT.matcher(ref).matches(), ref);
            assertTrue(ref.length() <= 12, ref);
        }
    }

    @Test
    void neverUsesLookAlikeCharacters() {
        RandomGenerator random = new java.util.Random(7);
        for (int i = 0; i < 500; i++) {
            String body = BookingReference.generate(random).substring(3);
            assertTrue(body.chars().noneMatch(c -> "01OIL".indexOf(c) >= 0), body);
        }
    }
}

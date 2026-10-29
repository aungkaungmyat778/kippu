package dev.kippu.booking.domain;

import java.util.random.RandomGenerator;

/** Short customer-facing code such as KP-7H3K9QWM. Avoids 0, O, 1, I and L so it reads clearly on a phone. */
public final class BookingReference {

    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LENGTH = 8;

    private BookingReference() {
    }

    public static String generate(RandomGenerator random) {
        StringBuilder sb = new StringBuilder("KP-");
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}

package dev.kippu.seat.service;

import java.util.HashSet;
import java.util.List;

/** Input rules for a hold request. Pure functions, so they are easy to unit test. */
public final class HoldRules {

    private HoldRules() {
    }

    /** Returns the cleaned seat list, or throws InvalidHoldRequestException. */
    public static List<String> validate(List<String> requested, int maxSeats) {
        if (requested == null || requested.isEmpty()) {
            throw new InvalidHoldRequestException("seatNos must contain at least one seat");
        }
        List<String> cleaned = requested.stream()
                .map(s -> s == null ? "" : s.trim())
                .toList();
        if (cleaned.stream().anyMatch(String::isEmpty)) {
            throw new InvalidHoldRequestException("seatNos must not contain blank values");
        }
        if (new HashSet<>(cleaned).size() != cleaned.size()) {
            throw new InvalidHoldRequestException("seatNos must not contain duplicates");
        }
        if (cleaned.size() > maxSeats) {
            throw new InvalidHoldRequestException("At most " + maxSeats + " seats per hold");
        }
        return cleaned;
    }
}

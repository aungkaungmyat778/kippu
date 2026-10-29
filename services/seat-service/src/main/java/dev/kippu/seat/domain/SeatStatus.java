package dev.kippu.seat.domain;

import java.time.Instant;

public enum SeatStatus {
    AVAILABLE,
    HELD,
    SOLD;

    /**
     * The status a customer should see right now. A hold whose time has run out counts
     * as available even before the cleanup job has released it. The comparison uses the
     * same rule as the SQL (hold_expires_at <= now), so the two never disagree.
     */
    public static SeatStatus effective(SeatStatus stored, Instant holdExpiresAt, Instant now) {
        if (stored == HELD && holdExpiresAt != null && !holdExpiresAt.isAfter(now)) {
            return AVAILABLE;
        }
        return stored;
    }
}

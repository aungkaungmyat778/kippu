package dev.kippu.seat.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SeatStatusTest {

    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");

    @Test
    void liveHoldStaysHeld() {
        assertEquals(SeatStatus.HELD,
                SeatStatus.effective(SeatStatus.HELD, NOW.plusSeconds(1), NOW));
    }

    @Test
    void holdExpiringExactlyNowCountsAsAvailable() {
        // Same rule as the SQL: hold_expires_at <= now means expired.
        assertEquals(SeatStatus.AVAILABLE,
                SeatStatus.effective(SeatStatus.HELD, NOW, NOW));
    }

    @Test
    void expiredHoldShowsAsAvailable() {
        assertEquals(SeatStatus.AVAILABLE,
                SeatStatus.effective(SeatStatus.HELD, NOW.minusSeconds(60), NOW));
    }

    @Test
    void soldAndAvailableNeverChange() {
        assertEquals(SeatStatus.SOLD, SeatStatus.effective(SeatStatus.SOLD, null, NOW));
        assertEquals(SeatStatus.AVAILABLE, SeatStatus.effective(SeatStatus.AVAILABLE, null, NOW));
    }
}

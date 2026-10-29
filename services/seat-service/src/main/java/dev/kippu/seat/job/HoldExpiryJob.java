package dev.kippu.seat.job;

import dev.kippu.seat.service.SeatService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the cleanup on a fixed delay. Several service instances may run it at once; that is
 * safe because the SQL locks rows and only touches rows that are still expired.
 * On AWS this becomes an EventBridge Scheduler rule or delayed SQS messages.
 */
@Component
public class HoldExpiryJob {

    private final SeatService seatService;

    public HoldExpiryJob(SeatService seatService) {
        this.seatService = seatService;
    }

    @Scheduled(fixedDelayString = "${kippu.seats.sweep-interval-ms}")
    public void sweep() {
        seatService.sweepExpired();
    }
}

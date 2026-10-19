package dev.kippu.catalog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * When trip generation runs: once at startup (handy in development) and every night.
 * In the serverless phase this class is replaced by an EventBridge Scheduler rule.
 */
@Component
public class TripGenerationJob {

    private final TripGenerationService generation;
    private final int horizonDays;
    private final boolean runOnStartup;

    public TripGenerationJob(TripGenerationService generation,
                             @Value("${kippu.generation.horizon-days}") int horizonDays,
                             @Value("${kippu.generation.run-on-startup}") boolean runOnStartup) {
        this.generation = generation;
        this.horizonDays = horizonDays;
        this.runOnStartup = runOnStartup;
    }

    @Scheduled(cron = "${kippu.generation.cron}", zone = "${kippu.timezone}")
    public void nightly() {
        generation.generate(horizonDays);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (runOnStartup) {
            generation.generate(horizonDays);
        }
    }
}

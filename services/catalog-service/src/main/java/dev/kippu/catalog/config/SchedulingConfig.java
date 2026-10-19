package dev.kippu.catalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled methods, such as the nightly trip generator. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

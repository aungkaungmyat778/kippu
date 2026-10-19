package dev.kippu.catalog.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Time is a dependency, not a global. Services receive a Clock and a ZoneId,
 * so a test can hand them a fixed clock instead of the real one.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ZoneId serviceZone(@Value("${kippu.timezone}") String zoneId) {
        return ZoneId.of(zoneId);
    }
}

package dev.kippu.booking.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** The one zone used to display times to customers. Stored times are always UTC instants. */
    @Bean
    public ZoneId displayZone(@Value("${kippu.timezone}") String zone) {
        return ZoneId.of(zone);
    }
}

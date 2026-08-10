package be.condorcet.easycarrent.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration that is only active under the {@code demo} profile.
 *
 * <p>It supplies the single {@link Clock} used by {@link DemoDataSeeder} so the
 * demonstration dates are derived from one injectable time source. Under the
 * default profile this configuration is not loaded and no bean is created, so
 * normal application behavior is unchanged.
 */
@Configuration
@Profile("demo")
public class DemoConfig {

    /**
     * The clock used to derive demonstration dates. It is defined only under the
     * {@code demo} profile and can be replaced in tests to make the seed
     * deterministic.
     */
    @Bean
    public Clock demoClock() {
        return Clock.systemDefaultZone();
    }
}

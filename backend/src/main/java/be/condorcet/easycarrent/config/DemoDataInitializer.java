package be.condorcet.easycarrent.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Startup hook that triggers the demonstration dataset once the application
 * context is ready and the schema is available.
 *
 * <p>It is only registered under the {@code demo} profile, so a normal startup
 * runs no demo logic at all. The actual work and its transaction boundary live in
 * {@link DemoDataSeeder}; this runner only delegates to it after startup.
 */
@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoDataSeeder seeder;

    public DemoDataInitializer(DemoDataSeeder seeder) {
        this.seeder = seeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seeder.seed();
    }
}

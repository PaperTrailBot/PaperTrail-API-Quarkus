package io.github.eggy03.papertrail.api.configuration;

import io.github.eggy03.papertrail.api.annotation.UTCClock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

@ApplicationScoped
public class ClockProducer {

    @Produces
    @ApplicationScoped
    @UTCClock
    public Clock clock() {
        return Clock.systemUTC();
    }
}

package com.burcuduzen.dayflow.gmail;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class OAuthAttemptsTests {
    @Test void callbackCanConsumeWithoutTheOriginalBrowserSessionOnlyOnce() {
        var attempts = new OAuthAttempts();
        var attempt = attempts.create();
        assertEquals(attempt.verifier(), attempts.consume(attempt.state()));
        assertThrows(ResponseStatusException.class, () -> attempts.consume(attempt.state()));
        assertThrows(ResponseStatusException.class, () -> attempts.consume("unrelated-state"));
        assertThrows(ResponseStatusException.class, () -> attempts.consume(null));
    }
    @Test void anExpiredAttemptCannotBeUsed() {
        class MutableClock extends Clock {
            Instant now = Instant.parse("2026-10-12T10:00:00Z");
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now; }
        }
        var clock = new MutableClock();
        var attempts = new OAuthAttempts(clock);
        var attempt = attempts.create();
        clock.now = clock.now.plusSeconds(601);
        assertThrows(ResponseStatusException.class, () -> attempts.consume(attempt.state()));
    }
}

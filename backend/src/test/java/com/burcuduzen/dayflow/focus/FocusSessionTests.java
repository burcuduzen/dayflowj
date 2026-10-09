package com.burcuduzen.dayflow.focus;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

class FocusSessionTests {
    private final OffsetDateTime start = OffsetDateTime.parse("2026-10-09T09:00:00Z");

    @Test
    void recordsFocusedAndPausedDurationsAcrossACompleteSession() {
        FocusSession session = new FocusSession(42L, start);

        session.pause(start.plusMinutes(25));
        assertEquals(FocusStatus.PAUSED, session.getStatus());
        assertEquals(1500, session.currentFocusedSeconds(start.plusMinutes(30)));
        assertEquals(300, session.currentPausedSeconds(start.plusMinutes(30)));

        session.resume(start.plusMinutes(30));
        session.complete(start.plusMinutes(45));

        assertEquals(FocusStatus.COMPLETED, session.getStatus());
        assertEquals(2400, session.currentFocusedSeconds(start.plusHours(2)));
        assertEquals(300, session.currentPausedSeconds(start.plusHours(2)));
        assertEquals(start.plusMinutes(45), session.getEndedAt());
    }

    @Test
    void rejectsInvalidStateTransitions() {
        FocusSession session = new FocusSession(null, start);
        assertThrows(IllegalStateException.class, () -> session.resume(start.plusMinutes(1)));
        session.pause(start.plusMinutes(1));
        assertThrows(IllegalStateException.class, () -> session.pause(start.plusMinutes(2)));
        session.complete(start.plusMinutes(3));
        assertThrows(IllegalStateException.class, () -> session.complete(start.plusMinutes(4)));
    }
}

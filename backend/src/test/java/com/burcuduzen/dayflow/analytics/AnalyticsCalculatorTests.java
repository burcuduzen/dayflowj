package com.burcuduzen.dayflow.analytics;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyticsCalculatorTests {
    private final ZoneId istanbul = ZoneId.of("Europe/Istanbul");
    private final LocalDate first = LocalDate.of(2026, 10, 8);

    @Test
    void groupsCompletionsAndFocusSessionsByRequestedTimeZone() {
        var days = AnalyticsCalculator.calculate(first, first.plusDays(1), istanbul,
            List.of(
                new AnalyticsCalculator.Completion(1L, OffsetDateTime.parse("2026-10-08T20:30:00Z")),
                new AnalyticsCalculator.Completion(2L, OffsetDateTime.parse("2026-10-08T22:30:00Z"))
            ),
            List.of(new AnalyticsCalculator.Focus(OffsetDateTime.parse("2026-10-08T21:30:00Z"), 1500)),
            Map.of(1L, 20, 2L, 40));

        assertEquals(2, days.size());
        assertEquals(1, days.get(0).completedTasks());
        assertEquals(20, days.get(0).estimatedMinutes());
        assertEquals(1500, days.get(1).focusedSeconds());
        assertEquals(1, days.get(1).completedTasks());
        assertEquals(40, days.get(1).estimatedMinutes());
    }

    @Test
    void includesEmptyDaysAndIgnoresEntriesOutsideRange() {
        var days = AnalyticsCalculator.calculate(first, first.plusDays(2), ZoneOffset.UTC,
            List.of(new AnalyticsCalculator.Completion(1L, first.minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))),
            List.of(), Map.of(1L, 30));

        assertEquals(3, days.size());
        assertTrue(days.stream().allMatch(day -> day.completedTasks() == 0 && day.focusedSeconds() == 0));
    }
}

package com.burcuduzen.dayflow.progress;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class StreakTests {
    private final LocalDate today = LocalDate.of(2026, 10, 9);
    @Test void yesterdayStreakRemainsAvailableUntilTodayEnds() {
        var result = StreakCalculator.calculate(Set.of(today.minusDays(1), today.minusDays(2)), today);
        assertEquals(2, result.current()); assertFalse(result.completedToday());
    }
    @Test void completedTodayExtendsConsecutiveRun() {
        var result = StreakCalculator.calculate(Set.of(today, today.minusDays(1), today.minusDays(2)), today);
        assertEquals(3, result.current()); assertTrue(result.completedToday());
    }
    @Test void missedDayResetsCurrentButRetainsBest() {
        var result = StreakCalculator.calculate(Set.of(today.minusDays(2), today.minusDays(3)), today);
        assertEquals(0, result.current()); assertEquals(2, result.longest());
    }
    @Test void futureDatesCannotIncreaseStreak() {
        var result = StreakCalculator.calculate(Set.of(today.plusDays(1)), today);
        assertEquals(0, result.current()); assertEquals(0, result.longest());
    }
}

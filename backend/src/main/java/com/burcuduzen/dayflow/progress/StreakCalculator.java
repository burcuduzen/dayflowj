package com.burcuduzen.dayflow.progress;

import java.time.LocalDate;
import java.util.*;

public final class StreakCalculator {
    private StreakCalculator() {}
    public record Result(int current, int longest, boolean completedToday) {}
    public static Result calculate(Set<LocalDate> days, LocalDate today) {
        var sorted = days.stream().filter(d -> !d.isAfter(today)).sorted().toList();
        int longest = 0, run = 0;
        LocalDate previous = null;
        for (LocalDate day : sorted) {
            run = previous != null && previous.plusDays(1).equals(day) ? run + 1 : 1;
            longest = Math.max(longest, run); previous = day;
        }
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int current = 0;
        while (days.contains(cursor)) { current++; cursor = cursor.minusDays(1); }
        return new Result(current, longest, days.contains(today));
    }
}

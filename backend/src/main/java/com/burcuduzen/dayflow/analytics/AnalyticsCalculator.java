package com.burcuduzen.dayflow.analytics;

import java.time.*;
import java.util.*;

final class AnalyticsCalculator {
    record Completion(Long taskId, OffsetDateTime at) {}
    record Focus(OffsetDateTime endedAt, long focusedSeconds) {}
    record Day(LocalDate date, int completedTasks, long focusedSeconds, int estimatedMinutes) {}

    private AnalyticsCalculator() {}

    static List<Day> calculate(LocalDate from, LocalDate to, ZoneId zone,
                               List<Completion> completions, List<Focus> sessions,
                               Map<Long, Integer> estimates) {
        Map<LocalDate, MutableDay> days = new LinkedHashMap<>();
        from.datesUntil(to.plusDays(1)).forEach(date -> days.put(date, new MutableDay()));

        completions.forEach(completion -> {
            MutableDay day = days.get(completion.at().atZoneSameInstant(zone).toLocalDate());
            if (day != null) {
                day.completedTasks++;
                day.estimatedMinutes += Math.max(0, estimates.getOrDefault(completion.taskId(), 0));
            }
        });
        sessions.forEach(session -> {
            MutableDay day = days.get(session.endedAt().atZoneSameInstant(zone).toLocalDate());
            if (day != null) day.focusedSeconds += Math.max(0, session.focusedSeconds());
        });

        return days.entrySet().stream().map(entry -> new Day(entry.getKey(), entry.getValue().completedTasks,
            entry.getValue().focusedSeconds, entry.getValue().estimatedMinutes)).toList();
    }

    private static final class MutableDay {
        int completedTasks;
        long focusedSeconds;
        int estimatedMinutes;
    }
}

package com.burcuduzen.dayflow.analytics;

import java.time.*;
import java.util.*;

final class AnalyticsCalculator {
    record Completion(Long taskId, OffsetDateTime at, int estimatedMinutes) {}
    record Day(LocalDate date, int completedTasks, int estimatedMinutes) {}
    private AnalyticsCalculator() {}

    static List<Day> calculate(LocalDate from, LocalDate to, ZoneId zone, List<Completion> completions) {
        Map<LocalDate, int[]> days = new LinkedHashMap<>();
        from.datesUntil(to.plusDays(1)).forEach(date -> days.put(date, new int[2]));
        completions.forEach(completion -> {
            int[] day = days.get(completion.at().atZoneSameInstant(zone).toLocalDate());
            if (day != null) { day[0]++; day[1] += Math.max(0, completion.estimatedMinutes()); }
        });
        return days.entrySet().stream().map(entry -> new Day(entry.getKey(), entry.getValue()[0], entry.getValue()[1])).toList();
    }
}

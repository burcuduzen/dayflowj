package com.burcuduzen.dayflow.task;

import java.time.*;
import java.time.temporal.ChronoUnit;

public final class ScheduleCalculator {
    private ScheduleCalculator() {}
    public static OffsetDateTime next(OffsetDateTime due, Recurrence recurrence, String zone, Instant now) {
        ZonedDateTime start = due.atZoneSameInstant(ZoneId.of(zone));
        ZonedDateTime current = now.atZone(ZoneId.of(zone));
        long steps = switch (recurrence) {
            case DAILY -> Math.max(1, ChronoUnit.DAYS.between(start.toLocalDate(), current.toLocalDate()));
            case WEEKLY -> Math.max(1, ChronoUnit.DAYS.between(start.toLocalDate(), current.toLocalDate()) / 7);
            case MONTHLY -> Math.max(1, ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(current)));
            default -> throw new IllegalArgumentException("Tekrarlama gerekli.");
        };
        ZonedDateTime candidate;
        do {
            candidate = switch (recurrence) {
                case DAILY -> start.plusDays(steps);
                case WEEKLY -> start.plusWeeks(steps);
                case MONTHLY -> start.plusMonths(steps);
                default -> throw new IllegalArgumentException("Tekrarlama gerekli.");
            };
            steps++;
        } while (!candidate.toInstant().isAfter(now));
        return candidate.toOffsetDateTime();
    }
}

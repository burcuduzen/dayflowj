package com.burcuduzen.dayflow.analytics;

import com.burcuduzen.dayflow.progress.*;
import com.burcuduzen.dayflow.task.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@Transactional(readOnly = true)
public class AnalyticsController {
    public record DayAnalytics(LocalDate date, int completedTasks, int estimatedMinutes) {}
    public record AnalyticsResponse(LocalDate from, LocalDate to, String timeZone,
                                    int completedTasks, int estimatedMinutes, List<DayAnalytics> days) {}

    private final ActivityRepository activities;
    private final TaskRepository tasks;

    public AnalyticsController(ActivityRepository activities, TaskRepository tasks) {
        this.activities = activities;
        this.tasks = tasks;
    }

    @GetMapping
    public AnalyticsResponse get(@RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to,
                                 @RequestParam(defaultValue = "Europe/Istanbul") String timeZone) {
        ZoneId zone = parseZone(timeZone);
        LocalDate effectiveTo = to == null ? LocalDate.now(zone) : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(6) : from;
        validateRange(effectiveFrom, effectiveTo);

        OffsetDateTime start = effectiveFrom.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime until = effectiveTo.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        List<CompletionActivity> completionRows = activities
            .findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(start, until);

        Map<Long, Integer> estimates = new HashMap<>();
        tasks.findAllById(completionRows.stream().map(row -> row.taskId).toList()).forEach(task ->
            estimates.put(task.getId(), task.getEstimatedMinutes() == null ? 0 : task.getEstimatedMinutes()));

        List<AnalyticsCalculator.Day> calculated = AnalyticsCalculator.calculate(effectiveFrom, effectiveTo, zone,
            completionRows.stream().map(row -> new AnalyticsCalculator.Completion(row.taskId, row.completedAt,
                row.estimatedMinutes == null ? estimates.getOrDefault(row.taskId, 0) : row.estimatedMinutes)).toList());
        List<DayAnalytics> days = calculated.stream().map(day -> new DayAnalytics(day.date(), day.completedTasks(), day.estimatedMinutes())).toList();
        int completed = days.stream().mapToInt(DayAnalytics::completedTasks).sum();
        int estimated = days.stream().mapToInt(DayAnalytics::estimatedMinutes).sum();
        return new AnalyticsResponse(effectiveFrom, effectiveTo, zone.getId(), completed, estimated, days);
    }

    private ZoneId parseZone(String value) {
        try { return ZoneId.of(value); }
        catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saat dilimi geçersiz.");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Başlangıç tarihi bitiş tarihinden sonra olamaz.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= 366) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "En fazla 366 günlük analiz istenebilir.");
        }
    }
}

package com.burcuduzen.dayflow.progress;

import com.burcuduzen.dayflow.task.*;
import com.burcuduzen.dayflow.settings.UserSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/progress") @Transactional
public class ProgressController {
    public record DayProgress(LocalDate date, boolean completed) {}
    public record ProgressResponse(int currentStreak, int longestStreak, boolean completedToday,
                                   int dailyTaskGoal, int completedTodayCount, boolean dailyGoalReached,
                                   List<DayProgress> week) {}
    private final ActivityRepository activities;
    private final TaskRepository tasks;
    private final UserSettingsService settings;
    public ProgressController(ActivityRepository activities, TaskRepository tasks, UserSettingsService settings) {
        this.activities = activities; this.tasks = tasks; this.settings = settings;
    }
    @GetMapping public ProgressResponse get(@RequestParam(required = false) String timeZone) {
        var preferences = settings.get();
        ZoneId zone;
        try { zone = ZoneId.of(timeZone == null || timeZone.isBlank() ? preferences.getTimeZone() : timeZone); }
        catch (DateTimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saat dilimi geçersiz."); }
        // Import completion dates from older app versions, preserving the first completion forever.
        tasks.findByStatusOrderByCreatedAtDescIdDesc(TaskStatus.COMPLETED).stream()
            .sorted(Comparator.comparing(Task::getId)).forEach(t -> {
                Task locked = tasks.findForUpdate(t.getId()).orElse(null);
                if (locked != null && locked.isComplete() && locked.getCompletedAt() != null && !activities.existsById(locked.getId())) {
                    activities.save(new CompletionActivity(locked.getId(), locked.getCompletedAt()));
                }
            });
        Set<LocalDate> days = new HashSet<>();
        activities.findAll().forEach(a -> days.add(a.completedAt.atZoneSameInstant(zone).toLocalDate()));
        LocalDate today = LocalDate.now(zone);
        var result = StreakCalculator.calculate(days, today);
        OffsetDateTime todayStart = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime tomorrowStart = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        int completedTodayCount = activities
            .findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(todayStart, tomorrowStart).size();
        int dailyTaskGoal = preferences.getDailyTaskGoal();
        List<DayProgress> week = new ArrayList<>();
        for (int i = 6; i >= 0; i--) { LocalDate day = today.minusDays(i); week.add(new DayProgress(day, days.contains(day))); }
        return new ProgressResponse(result.current(), result.longest(), result.completedToday(), dailyTaskGoal,
            completedTodayCount, completedTodayCount >= dailyTaskGoal, week);
    }
}

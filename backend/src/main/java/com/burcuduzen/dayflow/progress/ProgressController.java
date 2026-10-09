package com.burcuduzen.dayflow.progress;

import com.burcuduzen.dayflow.task.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/progress") @Transactional
public class ProgressController {
    public record DayProgress(LocalDate date, boolean completed) {}
    public record ProgressResponse(int currentStreak, int longestStreak, boolean completedToday, List<DayProgress> week) {}
    private final ActivityRepository activities;
    private final TaskRepository tasks;
    public ProgressController(ActivityRepository activities, TaskRepository tasks) { this.activities = activities; this.tasks = tasks; }
    @GetMapping public ProgressResponse get(@RequestParam(defaultValue = "Europe/Istanbul") String timeZone) {
        ZoneId zone;
        try { zone = ZoneId.of(timeZone); }
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
        List<DayProgress> week = new ArrayList<>();
        for (int i = 6; i >= 0; i--) { LocalDate day = today.minusDays(i); week.add(new DayProgress(day, days.contains(day))); }
        return new ProgressResponse(result.current(), result.longest(), result.completedToday(), week);
    }
}

package com.burcuduzen.dayflow.planning;

import com.burcuduzen.dayflow.calendar.*;
import com.burcuduzen.dayflow.task.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/planning")
@Transactional(readOnly = true)
public class PlanningController {
    public record SuggestionRequest(@NotNull LocalDate from, @NotNull LocalDate to,
                                    @NotNull LocalTime workStart, @NotNull LocalTime workEnd,
                                    @NotBlank @Size(max = 100) String timeZone,
                                    List<@Positive Long> taskIds) {}
    public record SuggestedBlock(Long taskId, String title, OffsetDateTime startAt,
                                 OffsetDateTime endAt, int minutes) {}
    public record UnscheduledTask(Long taskId, String title, String reason) {}
    public record SuggestionResponse(LocalDate from, LocalDate to, String timeZone,
                                     List<SuggestedBlock> blocks, List<UnscheduledTask> unscheduled) {}

    private final TaskRepository tasks;
    private final CalendarEventRepository events;
    private final PlannedBlockRepository blocks;

    public PlanningController(TaskRepository tasks, CalendarEventRepository events, PlannedBlockRepository blocks) {
        this.tasks = tasks;
        this.events = events;
        this.blocks = blocks;
    }

    @PostMapping("/suggestions")
    public SuggestionResponse suggest(@Valid @RequestBody SuggestionRequest request) {
        ZoneId zone = parseZone(request.timeZone());
        validate(request);
        ZonedDateTime rangeStart = request.from().atTime(request.workStart()).atZone(zone);
        ZonedDateTime rangeEnd = request.to().atTime(request.workEnd()).atZone(zone);
        List<Task> openTasks = tasks.findByStatusNotOrderByCreatedAtAscIdAsc(TaskStatus.COMPLETED);
        Set<Long> alreadyPlanned = blocks.findAll().stream().map(PlannedBlock::getTaskId)
            .collect(java.util.stream.Collectors.toSet());
        openTasks = openTasks.stream().filter(task -> !alreadyPlanned.contains(task.getId())).toList();
        if (request.taskIds() != null && !request.taskIds().isEmpty()) {
            Set<Long> selected = new HashSet<>(request.taskIds());
            openTasks = openTasks.stream().filter(task -> selected.contains(task.getId())).toList();
            Set<Long> found = openTasks.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
            if (!found.containsAll(selected)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Seçilen görevlerden biri bulunamadı veya tamamlanmış.");
            }
        }
        List<CalendarEvent> busy = events.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            rangeEnd.toOffsetDateTime(), rangeStart.toOffsetDateTime());
        List<PlannedBlock> planned = blocks.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            rangeEnd.toOffsetDateTime(), rangeStart.toOffsetDateTime());
        OffsetDateTime now = OffsetDateTime.now(zone);
        PlanningEngine.Result result = PlanningEngine.plan(request.from(), request.to(), request.workStart(),
            request.workEnd(), zone, now, openTasks.stream().map(task -> new PlanningEngine.Candidate(
                task.getId(), task.getTitle(), task.getDueDate(), task.getPriority(), task.getEstimatedMinutes())).toList(),
            java.util.stream.Stream.concat(
                busy.stream().map(event -> new PlanningEngine.Busy(event.getStartAt(), event.getEndAt())),
                planned.stream().map(block -> new PlanningEngine.Busy(block.getStartAt(), block.getEndAt())))
                .toList());
        return new SuggestionResponse(request.from(), request.to(), zone.getId(),
            result.blocks().stream().map(block -> new SuggestedBlock(block.taskId(), block.title(), block.startAt(),
                block.endAt(), block.minutes())).toList(),
            result.unscheduled().stream().map(item -> new UnscheduledTask(item.taskId(), item.title(), item.reason())).toList());
    }

    private void validate(SuggestionRequest request) {
        if (request.from().isAfter(request.to())) badRequest("Başlangıç tarihi bitiş tarihinden sonra olamaz.");
        if (ChronoUnit.DAYS.between(request.from(), request.to()) >= 14) badRequest("En fazla 14 günlük plan oluşturulabilir.");
        if (!request.workEnd().isAfter(request.workStart())) badRequest("Çalışma bitişi başlangıçtan sonra olmalıdır.");
    }

    private ZoneId parseZone(String value) {
        try { return ZoneId.of(value); }
        catch (DateTimeException ex) { throw badRequest("Saat dilimi geçersiz."); }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

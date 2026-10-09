package com.burcuduzen.dayflow.task;

import com.burcuduzen.dayflow.task.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class TaskService {
    private final TaskRepository repository;
    private final com.burcuduzen.dayflow.progress.ActivityRepository activities;
    public TaskService(TaskRepository repository, com.burcuduzen.dayflow.progress.ActivityRepository activities) {
        this.repository = repository; this.activities = activities;
    }

    public TaskResponse create(CreateTaskRequest request) {
        validateSchedule(request.dueDate(), request.recurrence(), request.timeZone());
        Task task = new Task(request.title().strip(), request.description(), request.dueDate(),
            request.priority() == null ? TaskPriority.MEDIUM : request.priority(), request.estimatedMinutes());
        task.configureSchedule(request.recurrence(), request.timeZone(), !Boolean.FALSE.equals(request.reminderEnabled()));
        return TaskResponse.from(repository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null ? repository.findAllByOrderByCreatedAtDescIdDesc()
            : repository.findByStatusOrderByCreatedAtDescIdDesc(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) { return TaskResponse.from(find(id)); }

    public TaskResponse update(Long id, UpdateTaskRequest request) {
        validateSchedule(request.dueDate(), request.recurrence(), request.timeZone());
        Task task = findForUpdate(id);
        boolean wasComplete = task.isComplete();
        OffsetDateTime previousDate = task.getDueDate();
        task.update(request.title().strip(), request.description(), request.dueDate(),
            request.priority(), request.estimatedMinutes(), request.status());
        task.configureSchedule(request.recurrence(), request.timeZone(), !Boolean.FALSE.equals(request.reminderEnabled()));
        task.resetReminderForNewDate(previousDate);
        afterStatusChange(task, wasComplete);
        return TaskResponse.from(repository.save(task));
    }

    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = findForUpdate(id);
        boolean wasComplete = task.isComplete();
        task.setStatus(status);
        afterStatusChange(task, wasComplete);
        return TaskResponse.from(repository.save(task));
    }

    public void delete(Long id) { repository.delete(find(id)); }

    private void validateSchedule(OffsetDateTime dueDate, Recurrence recurrence, String timeZone) {
        if (recurrence != null && recurrence != Recurrence.NONE && dueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tekrarlayan görev için son tarih gerekli.");
        }
        try { ZoneId.of(timeZone == null || timeZone.isBlank() ? "Europe/Istanbul" : timeZone); }
        catch (DateTimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saat dilimi geçersiz."); }
    }

    private void afterStatusChange(Task task, boolean wasComplete) {
        if (!task.isComplete()) { if (wasComplete) task.restartReminder(); return; }
        if (!wasComplete && task.getId() != null && !activities.existsById(task.getId())) {
            activities.save(new com.burcuduzen.dayflow.progress.CompletionActivity(task.getId(), task.getCompletedAt()));
        }
        task.dismissReminder();
        if (wasComplete || task.isRecurrenceSpawned() || task.getRecurrence() == Recurrence.NONE || task.getDueDate() == null) return;
        OffsetDateTime next = ScheduleCalculator.next(task.getDueDate(), task.getRecurrence(), task.getTimeZone(), Instant.now());
        Task upcoming = new Task(task.getTitle(), task.getDescription(), next, task.getPriority(), task.getEstimatedMinutes());
        upcoming.configureSchedule(task.getRecurrence(), task.getTimeZone(), task.isReminderEnabled());
        repository.save(upcoming);
        task.markRecurrenceSpawned();
    }

    private Task findForUpdate(Long id) {
        return repository.findForUpdate(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private Task find(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }
}

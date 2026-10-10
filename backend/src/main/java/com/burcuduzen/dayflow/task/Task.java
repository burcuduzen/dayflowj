package com.burcuduzen.dayflow.task;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 5000)
    private String description;
    private OffsetDateTime dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TaskPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TaskStatus status;
    private Integer estimatedMinutes;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private Recurrence recurrence;
    private String timeZone;
    private OffsetDateTime reminderAt;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean reminderEnabled;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int reminderMinutesBefore;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean recurrenceSpawned;

    protected Task() {}

    public Task(String title, String description, OffsetDateTime dueDate,
                TaskPriority priority, Integer estimatedMinutes) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.estimatedMinutes = estimatedMinutes;
        this.status = TaskStatus.TODO;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String title, String description, OffsetDateTime dueDate,
                       TaskPriority priority, Integer estimatedMinutes, TaskStatus status) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.estimatedMinutes = estimatedMinutes;
        setStatus(status);
    }

    public void setStatus(TaskStatus status) {
        if (status == TaskStatus.COMPLETED && this.status != TaskStatus.COMPLETED) {
            this.completedAt = OffsetDateTime.now();
        } else if (status != TaskStatus.COMPLETED) {
            this.completedAt = null;
        }
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    public void configureSchedule(Recurrence recurrence, String timeZone, boolean reminderEnabled) {
        configureSchedule(recurrence, timeZone, reminderEnabled, 0);
    }

    public void configureSchedule(Recurrence recurrence, String timeZone, boolean reminderEnabled,
                                  Integer reminderMinutesBefore) {
        Recurrence next = recurrence == null ? Recurrence.NONE : recurrence;
        String zone = timeZone == null || timeZone.isBlank() ? "Europe/Istanbul" : timeZone;
        ZoneId.of(zone);
        int leadMinutes = reminderMinutesBefore == null ? 0 : reminderMinutesBefore;
        boolean changed = this.reminderEnabled != reminderEnabled || this.reminderMinutesBefore != leadMinutes;
        this.recurrence = next;
        this.timeZone = zone;
        this.reminderEnabled = reminderEnabled;
        this.reminderMinutesBefore = leadMinutes;
        if (isComplete() || !reminderEnabled) this.reminderAt = null;
        else if (changed) this.reminderAt = calculateReminderAt();
    }

    public void resetReminderForNewDate(OffsetDateTime previousDate) {
        if (!Objects.equals(previousDate, this.dueDate)) {
            this.reminderAt = reminderEnabled && !isComplete() ? calculateReminderAt() : null;
        }
    }
    public void restartReminder() { this.reminderAt = reminderEnabled && !isComplete() ? calculateReminderAt() : null; }
    public void dismissReminder() { this.reminderAt = null; }
    public void snoozeReminder(int minutes) { this.reminderAt = OffsetDateTime.now().plusMinutes(minutes); }
    public boolean isComplete() { return status == TaskStatus.COMPLETED; }
    public Recurrence getRecurrence() { return recurrence == null ? Recurrence.NONE : recurrence; }
    public String getTimeZone() { return timeZone == null ? "Europe/Istanbul" : timeZone; }
    public OffsetDateTime getReminderAt() { return reminderAt; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public int getReminderMinutesBefore() { return reminderMinutesBefore; }
    public boolean isRecurrenceSpawned() { return recurrenceSpawned; }
    public void markRecurrenceSpawned() { recurrenceSpawned = true; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public OffsetDateTime getDueDate() { return dueDate; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    private OffsetDateTime calculateReminderAt() {
        return dueDate == null ? null : dueDate.minusMinutes(reminderMinutesBefore);
    }
}

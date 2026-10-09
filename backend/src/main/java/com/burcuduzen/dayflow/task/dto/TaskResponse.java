package com.burcuduzen.dayflow.task.dto;

import com.burcuduzen.dayflow.task.*;
import java.time.OffsetDateTime;

public record TaskResponse(Long id, String title, String description, OffsetDateTime dueDate,
    TaskPriority priority, TaskStatus status, Integer estimatedMinutes,
    OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt,
    Recurrence recurrence, boolean reminderEnabled, OffsetDateTime reminderAt, String timeZone) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(),
            task.getDueDate(), task.getPriority(), task.getStatus(), task.getEstimatedMinutes(),
            task.getCreatedAt(), task.getUpdatedAt(), task.getCompletedAt(), task.getRecurrence(),
            task.isReminderEnabled(), task.getReminderAt(), task.getTimeZone());
    }
}

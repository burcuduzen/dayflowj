package com.burcuduzen.dayflow.task.dto;

import com.burcuduzen.dayflow.task.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import com.burcuduzen.dayflow.task.Recurrence;

public record UpdateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    OffsetDateTime dueDate,
    @NotNull TaskPriority priority,
    @Positive Integer estimatedMinutes,
    @NotNull TaskStatus status,
    Recurrence recurrence,
    Boolean reminderEnabled,
    @Size(max = 100) String timeZone
) {
    public UpdateTaskRequest(String title, String description, OffsetDateTime dueDate,
        TaskPriority priority, Integer estimatedMinutes, TaskStatus status) {
        this(title, description, dueDate, priority, estimatedMinutes, status, Recurrence.NONE, true, "Europe/Istanbul");
    }
}

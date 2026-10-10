package com.burcuduzen.dayflow.task.dto;

import com.burcuduzen.dayflow.task.TaskPriority;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import com.burcuduzen.dayflow.task.Recurrence;

public record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    OffsetDateTime dueDate,
    TaskPriority priority,
    @Positive Integer estimatedMinutes,
    Recurrence recurrence,
    Boolean reminderEnabled,
    @Size(max = 100) String timeZone,
    @Min(0) @Max(10080) Integer reminderMinutesBefore
) {
    public CreateTaskRequest(String title, String description, OffsetDateTime dueDate,
        TaskPriority priority, Integer estimatedMinutes) {
        this(title, description, dueDate, priority, estimatedMinutes, Recurrence.NONE, true, "Europe/Istanbul", 0);
    }
    public CreateTaskRequest(String title, String description, OffsetDateTime dueDate,
        TaskPriority priority, Integer estimatedMinutes, Recurrence recurrence,
        Boolean reminderEnabled, String timeZone) {
        this(title, description, dueDate, priority, estimatedMinutes, recurrence, reminderEnabled, timeZone, 0);
    }
}

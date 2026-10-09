package com.burcuduzen.dayflow.task.dto;

import com.burcuduzen.dayflow.task.TaskPriority;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    OffsetDateTime dueDate,
    TaskPriority priority,
    @Positive Integer estimatedMinutes
) {}

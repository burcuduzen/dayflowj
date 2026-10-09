package com.burcuduzen.dayflow.task.dto;

import com.burcuduzen.dayflow.task.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record UpdateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    OffsetDateTime dueDate,
    @NotNull TaskPriority priority,
    @Positive Integer estimatedMinutes,
    @NotNull TaskStatus status
) {}

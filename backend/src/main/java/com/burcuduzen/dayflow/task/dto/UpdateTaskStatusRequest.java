package com.burcuduzen.dayflow.task.dto;

import com.burcuduzen.dayflow.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {}

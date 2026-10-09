package com.burcuduzen.dayflow.task;

import com.burcuduzen.dayflow.task.dto.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTests {
    private final TaskRepository repository = mock(TaskRepository.class);
    private final TaskService service = new TaskService(repository);

    @Test
    void creationTrimsTitleAndDefaultsPriorityAndStatus() {
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TaskResponse response = service.create(new CreateTaskRequest("  Ders çalış  ", null, null, null, 30));
        assertEquals("Ders çalış", response.title());
        assertEquals(TaskPriority.MEDIUM, response.priority());
        assertEquals(TaskStatus.TODO, response.status());
        assertNull(response.completedAt());
    }

    @Test
    void completionIsIdempotentAndReopeningClearsCompletionDate() {
        Task task = new Task("Görev", null, null, TaskPriority.HIGH, 30);
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);
        TaskResponse completed = service.updateStatus(1L, TaskStatus.COMPLETED);
        assertNotNull(completed.completedAt());
        assertEquals(completed.completedAt(), service.updateStatus(1L, TaskStatus.COMPLETED).completedAt());
        assertNull(service.updateStatus(1L, TaskStatus.TODO).completedAt());
    }

    @Test
    void missingTaskCannotBeDeleted() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(TaskNotFoundException.class, () -> service.delete(42L));
        verify(repository, never()).delete(any(Task.class));
    }

    @Test
    void filteredListUsesRequestedStatus() {
        Task task = new Task("Görev", null, null, TaskPriority.MEDIUM, null);
        when(repository.findByStatusOrderByCreatedAtDescIdDesc(TaskStatus.TODO)).thenReturn(List.of(task));
        assertEquals(1, service.list(TaskStatus.TODO).size());
        verify(repository, never()).findAllByOrderByCreatedAtDescIdDesc();
    }

    @Test
    void invalidTitleAndDurationAreRejectedByValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var errors = factory.getValidator().validate(new CreateTaskRequest(" ", null, null, null, 0));
            assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("title")));
            assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("estimatedMinutes")));
        }
    }
}

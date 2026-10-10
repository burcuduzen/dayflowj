package com.burcuduzen.dayflow.reminder;

import com.burcuduzen.dayflow.task.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReminderTests {
    @Test void earlyReminderIsCalculatedFromDueDate() {
        OffsetDateTime due = OffsetDateTime.parse("2026-10-12T15:00:00+03:00");
        Task task = new Task("Görev", null, due, TaskPriority.MEDIUM, null);
        task.configureSchedule(Recurrence.NONE, "Europe/Istanbul", true, 60);
        assertEquals(due.minusHours(1), task.getReminderAt());
        assertEquals(60, task.getReminderMinutesBefore());
    }

    @Test void snoozePersistsNewTimeAndRejectsInvalidDurations() {
        TaskRepository repository = mock(TaskRepository.class);
        Task task = new Task("Görev", null, OffsetDateTime.now().minusMinutes(1), TaskPriority.MEDIUM, null);
        task.configureSchedule(Recurrence.NONE, "Europe/Istanbul", true);
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(task));
        ReminderController controller = new ReminderController(repository);
        controller.snooze(1L, new ReminderController.SnoozeRequest(10));
        assertTrue(task.getReminderAt().isAfter(OffsetDateTime.now().plusMinutes(9)));
        verify(repository).save(task);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> controller.snooze(1L, new ReminderController.SnoozeRequest(7)));
    }
    @Test void completedTaskCannotBeSnoozed() {
        TaskRepository repository = mock(TaskRepository.class);
        Task task = new Task("Görev", null, OffsetDateTime.now().minusMinutes(1), TaskPriority.MEDIUM, null);
        task.configureSchedule(Recurrence.NONE, "Europe/Istanbul", true);
        task.setStatus(TaskStatus.COMPLETED);
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(task));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> new ReminderController(repository).snooze(1L, new ReminderController.SnoozeRequest(5)));
    }
}

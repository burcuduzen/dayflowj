package com.burcuduzen.dayflow.task;

import com.burcuduzen.dayflow.task.dto.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleTests {
    @Test void dailyRecurrencePreservesLocalTimeAcrossDaylightSaving() {
        var next = ScheduleCalculator.next(OffsetDateTime.parse("2026-03-28T09:00:00+01:00"), Recurrence.DAILY,
            "Europe/Berlin", Instant.parse("2026-03-28T10:00:00Z"));
        assertEquals(OffsetDateTime.parse("2026-03-29T09:00:00+02:00"), next);
    }
    @Test void monthlyRecurrenceHandlesShortMonths() {
        var next = ScheduleCalculator.next(OffsetDateTime.parse("2026-01-31T09:00:00+03:00"), Recurrence.MONTHLY,
            "Europe/Istanbul", Instant.parse("2026-01-31T10:00:00Z"));
        assertEquals(28, next.getDayOfMonth()); assertEquals(2, next.getMonthValue());
    }
    @Test void missedOccurrencesSkipToFuture() {
        var now = Instant.parse("2026-10-09T10:00:00Z");
        var next = ScheduleCalculator.next(OffsetDateTime.parse("2000-01-01T09:00:00+03:00"), Recurrence.DAILY,
            "Europe/Istanbul", now);
        assertTrue(next.toInstant().isAfter(now)); assertEquals(10, next.getDayOfMonth());
    }
    @Test void repeatedCompletionDoesNotGenerateDuplicateNextTasks() {
        TaskRepository repository = mock(TaskRepository.class);
        Task task = new Task("Tekrar", null, OffsetDateTime.now().plusDays(1), TaskPriority.MEDIUM, 20);
        task.configureSchedule(Recurrence.DAILY, "Europe/Istanbul", true);
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
        TaskService service = new TaskService(repository, mock(com.burcuduzen.dayflow.progress.ActivityRepository.class));
        service.updateStatus(1L, TaskStatus.COMPLETED);
        service.updateStatus(1L, TaskStatus.TODO);
        service.updateStatus(1L, TaskStatus.COMPLETED);
        verify(repository, times(1)).save(argThat(t -> t != task));
    }
    @Test void repeatNeedsDateAndValidZone() {
        TaskService service = new TaskService(mock(TaskRepository.class), mock(com.burcuduzen.dayflow.progress.ActivityRepository.class));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.create(new CreateTaskRequest("Tekrar", null, null, null, null, Recurrence.DAILY, true, "Europe/Istanbul")));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.create(new CreateTaskRequest("Görev", null, null, null, null, Recurrence.NONE, true, "invalid-zone")));
    }
    @Test void dismissedReminderStaysDismissedOnUnchangedSchedule() {
        Task task = new Task("Hatırlatma", null, OffsetDateTime.now().plusDays(1), TaskPriority.MEDIUM, null);
        task.configureSchedule(Recurrence.NONE, "Europe/Istanbul", true);
        task.dismissReminder();
        task.configureSchedule(Recurrence.NONE, "Europe/Istanbul", true);
        assertNull(task.getReminderAt());
    }
}

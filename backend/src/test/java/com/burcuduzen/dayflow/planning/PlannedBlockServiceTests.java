package com.burcuduzen.dayflow.planning;

import com.burcuduzen.dayflow.calendar.CalendarEventRepository;
import com.burcuduzen.dayflow.task.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlannedBlockServiceTests {
    private final PlannedBlockRepository blocks = mock(PlannedBlockRepository.class);
    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final PlannedBlockService service = new PlannedBlockService(blocks, events, tasks);
    private final OffsetDateTime start = OffsetDateTime.parse("2026-10-12T09:00:00+03:00");

    @Test
    void approvesMatchingConflictFreeBlock() {
        Task task = new Task("Rapor", null, null, TaskPriority.HIGH, 60);
        when(tasks.findForUpdate(1L)).thenReturn(Optional.of(task));
        when(blocks.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.approve(List.of(new PlannedBlockService.Approval(1L, start, start.plusHours(1))));
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getTaskId());
        verify(blocks).saveAll(anyList());
    }

    @Test
    void rejectsDurationThatDoesNotMatchEstimate() {
        when(tasks.findForUpdate(1L)).thenReturn(Optional.of(
            new Task("Rapor", null, null, TaskPriority.HIGH, 60)));
        assertThrows(ResponseStatusException.class, () -> service.approve(List.of(
            new PlannedBlockService.Approval(1L, start, start.plusMinutes(30)))));
        verify(blocks, never()).saveAll(anyList());
    }

    @Test
    void rejectsOverlappingApprovalBatchBeforeWriting() {
        assertThrows(ResponseStatusException.class, () -> service.approve(List.of(
            new PlannedBlockService.Approval(1L, start, start.plusHours(1)),
            new PlannedBlockService.Approval(2L, start.plusMinutes(30), start.plusHours(2)))));
        verifyNoInteractions(tasks, events);
        verify(blocks, never()).saveAll(anyList());
    }
}

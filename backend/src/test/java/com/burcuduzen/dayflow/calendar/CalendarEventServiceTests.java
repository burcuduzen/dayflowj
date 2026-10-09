package com.burcuduzen.dayflow.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarEventServiceTests {
    private final CalendarEventRepository repository = mock(CalendarEventRepository.class);
    private final CalendarEventService service = new CalendarEventService(repository);
    private final OffsetDateTime start = OffsetDateTime.parse("2026-10-10T09:00:00+03:00");

    @Test
    void createsValidEvent() {
        when(repository.save(any(CalendarEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CalendarEvent event = service.create(" Toplantı ", "Haftalık", start, start.plusHours(1));
        assertEquals("Toplantı", event.getTitle());
        assertEquals(start.plusHours(1), event.getEndAt());
        verify(repository).save(event);
    }

    @Test
    void rejectsEmptyOrReversedTimeRanges() {
        assertThrows(ResponseStatusException.class,
            () -> service.create("Toplantı", null, start, start));
        assertThrows(ResponseStatusException.class,
            () -> service.create("Toplantı", null, start, start.minusMinutes(1)));
        verifyNoInteractions(repository);
    }

    @Test
    void rangeQueryUsesOverlapSemantics() {
        OffsetDateTime end = start.plusDays(7);
        when(repository.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(end, start))
            .thenReturn(List.of());
        service.list(start, end);
        verify(repository).findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(end, start);
    }

    @Test
    void requiresBothQueryBoundsAndLimitsLargeRanges() {
        assertThrows(ResponseStatusException.class, () -> service.list(start, null));
        assertThrows(ResponseStatusException.class, () -> service.list(start, start.plusDays(367)));
        verifyNoInteractions(repository);
    }
}

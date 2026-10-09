package com.burcuduzen.dayflow.planning;

import com.burcuduzen.dayflow.task.TaskPriority;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlanningEngineTests {
    private final ZoneId zone = ZoneId.of("Europe/Istanbul");
    private final LocalDate day = LocalDate.of(2026, 10, 12);
    private final OffsetDateTime earliest = day.atTime(8, 0).atZone(zone).toOffsetDateTime();

    @Test
    void schedulesAroundCalendarEventsAndOrdersByDeadline() {
        var urgent = new PlanningEngine.Candidate(2L, "Acil", day.atTime(12, 0).atZone(zone).toOffsetDateTime(),
            TaskPriority.MEDIUM, 60);
        var later = new PlanningEngine.Candidate(1L, "Sonra", null, TaskPriority.HIGH, 90);
        var meeting = new PlanningEngine.Busy(day.atTime(10, 0).atZone(zone).toOffsetDateTime(),
            day.atTime(11, 0).atZone(zone).toOffsetDateTime());

        var result = PlanningEngine.plan(day, day, LocalTime.of(9, 0), LocalTime.of(17, 0), zone,
            earliest, List.of(later, urgent), List.of(meeting));

        assertEquals(2, result.blocks().size());
        assertEquals(2L, result.blocks().get(0).taskId());
        assertEquals(LocalTime.of(9, 0), result.blocks().get(0).startAt().toLocalTime());
        assertEquals(1L, result.blocks().get(1).taskId());
        assertEquals(LocalTime.of(11, 0), result.blocks().get(1).startAt().toLocalTime());
    }

    @Test
    void reportsMissingEstimateAndTasksThatDoNotFit() {
        var noEstimate = new PlanningEngine.Candidate(1L, "Belirsiz", null, TaskPriority.LOW, null);
        var tooLong = new PlanningEngine.Candidate(2L, "Uzun", null, TaskPriority.HIGH, 600);
        var result = PlanningEngine.plan(day, day, LocalTime.of(9, 0), LocalTime.of(17, 0), zone,
            earliest, List.of(noEstimate, tooLong), List.of());

        assertTrue(result.blocks().isEmpty());
        assertEquals("NO_AVAILABLE_SLOT", result.unscheduled().get(0).reason());
        assertEquals("MISSING_ESTIMATE", result.unscheduled().get(1).reason());
    }

    @Test
    void neverPlacesSuggestionInThePast() {
        OffsetDateTime noon = day.atTime(12, 15).atZone(zone).toOffsetDateTime();
        var task = new PlanningEngine.Candidate(1L, "Görev", null, TaskPriority.MEDIUM, 30);
        var result = PlanningEngine.plan(day, day, LocalTime.of(9, 0), LocalTime.of(17, 0), zone,
            noon, List.of(task), List.of());
        assertEquals(noon, result.blocks().get(0).startAt());
    }
}

package com.burcuduzen.dayflow.task;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class NaturalLanguageTaskParserTests {
    private final NaturalLanguageTaskParser parser = new NaturalLanguageTaskParser();
    private final ZoneId zone = ZoneId.of("Europe/Istanbul");
    private final ZonedDateTime now = ZonedDateTime.of(2026, 10, 10, 12, 0, 0, 0, zone);

    @Test
    void parsesTurkishDateTimePriorityAndDuration() {
        var parsed = parser.parse("Yarın saat 14:30 acil 2 saat sunumu hazırla", zone, now);
        assertEquals("sunumu hazırla", parsed.title());
        assertEquals(TaskPriority.HIGH, parsed.priority());
        assertEquals(120, parsed.estimatedMinutes());
        assertEquals(OffsetDateTime.parse("2026-10-11T14:30:00+03:00"), parsed.dueDate());
    }

    @Test
    void parsesWeeklyRecurrenceAndDefaultTime() {
        var parsed = parser.parse("Her hafta pazartesi raporu gönder", zone, now);
        assertEquals("raporu gönder", parsed.title());
        assertEquals(Recurrence.WEEKLY, parsed.recurrence());
        assertEquals(DayOfWeek.MONDAY, parsed.dueDate().getDayOfWeek());
        assertEquals(LocalTime.of(18, 0), parsed.dueDate().toLocalTime());
    }

    @Test
    void timeWithoutDateMovesToTomorrowWhenTimePassed() {
        var parsed = parser.parse("saat 09:00 kahve siparişi ver", zone, now);
        assertEquals(LocalDate.of(2026, 10, 11), parsed.dueDate().toLocalDate());
    }

    @Test
    void rejectsSentenceWithoutATitle() {
        assertThrows(ResponseStatusException.class, () -> parser.parse("yarın saat 10:00 acil", zone, now));
    }
}

package com.burcuduzen.dayflow.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findAllByOrderByStartAtAscIdAsc();
    List<CalendarEvent> findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
        OffsetDateTime until, OffsetDateTime from);
}

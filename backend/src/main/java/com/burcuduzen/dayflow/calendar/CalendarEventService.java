package com.burcuduzen.dayflow.calendar;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class CalendarEventService {
    private static final Duration MAX_QUERY_RANGE = Duration.ofDays(366);
    private final CalendarEventRepository repository;

    public CalendarEventService(CalendarEventRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<CalendarEvent> list(OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) return repository.findAllByOrderByStartAtAscIdAsc();
        if (from == null || to == null) {
            throw badRequest("Tarih aralığı için from ve to birlikte verilmelidir.");
        }
        validateRange(from, to, true);
        return repository.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(to, from);
    }

    public CalendarEvent create(String title, String description, OffsetDateTime startAt, OffsetDateTime endAt) {
        validateRange(startAt, endAt, false);
        return repository.save(new CalendarEvent(title, description, startAt, endAt));
    }

    public CalendarEvent update(Long id, String title, String description,
                                OffsetDateTime startAt, OffsetDateTime endAt) {
        validateRange(startAt, endAt, false);
        CalendarEvent event = find(id);
        event.update(title, description, startAt, endAt);
        return repository.save(event);
    }

    public void delete(Long id) { repository.delete(find(id)); }

    private void validateRange(OffsetDateTime from, OffsetDateTime to, boolean limitQuery) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw badRequest("Bitiş zamanı başlangıç zamanından sonra olmalıdır.");
        }
        if (limitQuery && Duration.between(from, to).compareTo(MAX_QUERY_RANGE) > 0) {
            throw badRequest("En fazla 366 günlük takvim aralığı istenebilir.");
        }
    }

    private CalendarEvent find(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Takvim etkinliği bulunamadı."));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

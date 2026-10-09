package com.burcuduzen.dayflow.calendar;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/calendar-events")
public class CalendarEventController {
    public record EventRequest(@NotBlank @Size(max = 200) String title,
                               @Size(max = 2000) String description,
                               @NotNull OffsetDateTime startAt,
                               @NotNull OffsetDateTime endAt) {}
    public record EventResponse(Long id, String title, String description, OffsetDateTime startAt,
                                OffsetDateTime endAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        static EventResponse from(CalendarEvent event) {
            return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getStartAt(),
                event.getEndAt(), event.getCreatedAt(), event.getUpdatedAt());
        }
    }

    private final CalendarEventService service;
    public CalendarEventController(CalendarEventService service) { this.service = service; }

    @GetMapping
    public List<EventResponse> list(@RequestParam(required = false) OffsetDateTime from,
                                    @RequestParam(required = false) OffsetDateTime to) {
        return service.list(from, to).stream().map(EventResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        EventResponse response = EventResponse.from(service.create(request.title(), request.description(),
            request.startAt(), request.endAt()));
        return ResponseEntity.created(URI.create("/api/calendar-events/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return EventResponse.from(service.update(id, request.title(), request.description(),
            request.startAt(), request.endAt()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

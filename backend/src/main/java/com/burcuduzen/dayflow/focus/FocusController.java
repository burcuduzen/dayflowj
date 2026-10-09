package com.burcuduzen.dayflow.focus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/focus-sessions")
public class FocusController {
    public record StartFocusRequest(Long taskId) {}
    public record FocusResponse(Long id, Long taskId, FocusStatus status, OffsetDateTime startedAt,
                                OffsetDateTime endedAt, long focusedSeconds, long pausedSeconds) {
        static FocusResponse from(FocusSession session) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            return new FocusResponse(session.getId(), session.getTaskId(), session.getStatus(),
                session.getStartedAt(), session.getEndedAt(), session.currentFocusedSeconds(now),
                session.currentPausedSeconds(now));
        }
    }

    private final FocusService service;
    public FocusController(FocusService service) { this.service = service; }

    @GetMapping
    public List<FocusResponse> list() { return service.list().stream().map(FocusResponse::from).toList(); }

    @GetMapping("/active")
    public ResponseEntity<FocusResponse> active() {
        FocusSession session = service.active();
        return session == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(FocusResponse.from(session));
    }

    @PostMapping
    public ResponseEntity<FocusResponse> start(@RequestBody(required = false) StartFocusRequest request) {
        FocusResponse response = FocusResponse.from(service.start(request == null ? null : request.taskId()));
        return ResponseEntity.created(URI.create("/api/focus-sessions/" + response.id())).body(response);
    }

    @PostMapping("/{id}/pause")
    public FocusResponse pause(@PathVariable Long id) { return FocusResponse.from(service.pause(id)); }

    @PostMapping("/{id}/resume")
    public FocusResponse resume(@PathVariable Long id) { return FocusResponse.from(service.resume(id)); }

    @PostMapping("/{id}/complete")
    public FocusResponse complete(@PathVariable Long id) { return FocusResponse.from(service.complete(id)); }
}

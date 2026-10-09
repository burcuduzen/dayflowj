package com.burcuduzen.dayflow.note;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController @RequestMapping("/api/notes")
public class NoteController {
    public record NoteRequest(@NotBlank @Size(max = 200) String title, @NotNull @Size(max = 20000) String content) {}
    public record NoteResponse(Long id, String title, String content, java.time.OffsetDateTime createdAt, java.time.OffsetDateTime updatedAt) {
        static NoteResponse from(Note n) { return new NoteResponse(n.id, n.title, n.content, n.createdAt, n.updatedAt); }
    }
    private final NoteService service;
    public NoteController(NoteService service) { this.service = service; }
    @GetMapping public List<NoteResponse> list() { return service.list().stream().map(NoteResponse::from).toList(); }
    @PostMapping public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest body) {
        NoteResponse note = NoteResponse.from(service.create(body.title(), body.content()));
        return ResponseEntity.created(URI.create("/api/notes/" + note.id())).body(note);
    }
    @PutMapping("/{id}") public NoteResponse update(@PathVariable Long id, @Valid @RequestBody NoteRequest body) {
        return NoteResponse.from(service.update(id, body.title(), body.content()));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
}

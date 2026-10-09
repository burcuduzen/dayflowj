package com.burcuduzen.dayflow.reminder;

import com.burcuduzen.dayflow.task.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.List;

@RestController @RequestMapping("/api/reminders") @Transactional
public class ReminderController {
    public record ReminderResponse(Long taskId, String title, OffsetDateTime remindAt) {}
    public record SnoozeRequest(@NotNull Integer minutes) {}
    private final TaskRepository repository;
    public ReminderController(TaskRepository repository) { this.repository = repository; }
    @GetMapping @Transactional(readOnly = true)
    public List<ReminderResponse> due() {
        return repository.findByStatusNotAndReminderAtLessThanEqualOrderByReminderAtAsc(TaskStatus.COMPLETED, OffsetDateTime.now())
            .stream().map(t -> new ReminderResponse(t.getId(), t.getTitle(), t.getReminderAt())).toList();
    }
    @PostMapping("/{id}/dismiss") public ResponseEntity<Void> dismiss(@PathVariable Long id) {
        Task task = find(id); task.dismissReminder(); repository.save(task);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/snooze") public ResponseEntity<Void> snooze(@PathVariable Long id, @Valid @RequestBody SnoozeRequest body) {
        if (!List.of(5, 10, 30).contains(body.minutes())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "5, 10 veya 30 dakika seçin.");
        Task task = find(id);
        if (task.isComplete() || !task.isReminderEnabled() || task.getReminderAt() == null || task.getReminderAt().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etkin ve zamanı gelmiş bir hatırlatıcı gerekli.");
        }
        task.snoozeReminder(body.minutes()); repository.save(task); return ResponseEntity.noContent().build();
    }
    private Task find(Long id) { return repository.findForUpdate(id).orElseThrow(() -> new TaskNotFoundException(id)); }
}

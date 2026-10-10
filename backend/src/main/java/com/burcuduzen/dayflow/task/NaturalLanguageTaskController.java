package com.burcuduzen.dayflow.task;

import com.burcuduzen.dayflow.settings.UserSettingsService;
import com.burcuduzen.dayflow.task.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks/natural-language")
public class NaturalLanguageTaskController {
    public record NaturalLanguageRequest(@NotBlank @Size(max = 500) String text,
                                         @Size(max = 100) String timeZone) {}
    public record ParsedTaskResponse(String title, OffsetDateTime dueDate, TaskPriority priority,
                                     Integer estimatedMinutes, Recurrence recurrence,
                                     List<String> recognized) {
        static ParsedTaskResponse from(NaturalLanguageTaskParser.ParsedTask parsed) {
            return new ParsedTaskResponse(parsed.title(), parsed.dueDate(), parsed.priority(),
                parsed.estimatedMinutes(), parsed.recurrence(), parsed.recognized());
        }
    }

    private final NaturalLanguageTaskParser parser;
    private final TaskService tasks;
    private final UserSettingsService settings;

    public NaturalLanguageTaskController(NaturalLanguageTaskParser parser, TaskService tasks,
                                         UserSettingsService settings) {
        this.parser = parser; this.tasks = tasks; this.settings = settings;
    }

    @PostMapping("/preview")
    public ParsedTaskResponse preview(@Valid @RequestBody NaturalLanguageRequest request) {
        ZoneId zone = zone(request.timeZone());
        return ParsedTaskResponse.from(parser.parse(request.text(), zone, ZonedDateTime.now(zone)));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody NaturalLanguageRequest request) {
        ZoneId zone = zone(request.timeZone());
        var parsed = parser.parse(request.text(), zone, ZonedDateTime.now(zone));
        TaskResponse task = tasks.create(new CreateTaskRequest(parsed.title(), null, parsed.dueDate(),
            parsed.priority(), parsed.estimatedMinutes(), parsed.recurrence(), true, zone.getId()));
        return ResponseEntity.created(URI.create("/api/tasks/" + task.id())).body(task);
    }

    private ZoneId zone(String requested) {
        String value = requested == null || requested.isBlank() ? settings.get().getTimeZone() : requested;
        try { return ZoneId.of(value); }
        catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saat dilimi geçersiz.");
        }
    }
}

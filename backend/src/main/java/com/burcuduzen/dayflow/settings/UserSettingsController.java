package com.burcuduzen.dayflow.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {
    public record SettingsRequest(@Min(1) @Max(100) int dailyTaskGoal,
                                  @NotNull LocalTime workStart,
                                  @NotNull LocalTime workEnd,
                                  @NotBlank @Size(max = 100) String timeZone) {}
    public record SettingsResponse(int dailyTaskGoal, LocalTime workStart, LocalTime workEnd,
                                   String timeZone,
                                   OffsetDateTime updatedAt) {
        static SettingsResponse from(UserSettings settings) {
            return new SettingsResponse(settings.getDailyTaskGoal(), settings.getWorkStart(), settings.getWorkEnd(),
                settings.getTimeZone(), settings.getUpdatedAt());
        }
    }

    private final UserSettingsService service;
    public UserSettingsController(UserSettingsService service) { this.service = service; }

    @GetMapping
    public SettingsResponse get() { return SettingsResponse.from(service.get()); }

    @PutMapping
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return SettingsResponse.from(service.update(request.dailyTaskGoal(), request.workStart(), request.workEnd(),
            request.timeZone()));
    }
}

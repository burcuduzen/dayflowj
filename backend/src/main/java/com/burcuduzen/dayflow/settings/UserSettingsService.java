package com.burcuduzen.dayflow.settings;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;

@Service
@Transactional
public class UserSettingsService {
    private static final long SETTINGS_ID = 1L;
    private final UserSettingsRepository repository;

    public UserSettingsService(UserSettingsRepository repository) { this.repository = repository; }

    public UserSettings get() {
        return repository.findById(SETTINGS_ID).orElseGet(() -> repository.save(new UserSettings(SETTINGS_ID)));
    }

    public UserSettings update(int dailyTaskGoal, LocalTime workStart, LocalTime workEnd,
                               int focusMinutes, int breakMinutes, String timeZone) {
        validate(dailyTaskGoal, workStart, workEnd, focusMinutes, breakMinutes, timeZone);
        UserSettings settings = get();
        settings.update(dailyTaskGoal, workStart, workEnd, focusMinutes, breakMinutes, timeZone);
        return repository.save(settings);
    }

    private void validate(int dailyTaskGoal, LocalTime workStart, LocalTime workEnd,
                          int focusMinutes, int breakMinutes, String timeZone) {
        if (dailyTaskGoal < 1 || dailyTaskGoal > 100) badRequest("Günlük görev hedefi 1 ile 100 arasında olmalıdır.");
        if (workStart == null || workEnd == null) badRequest("Çalışma saatleri gereklidir.");
        if (!workEnd.isAfter(workStart)) badRequest("Çalışma bitişi başlangıçtan sonra olmalıdır.");
        if (focusMinutes < 5 || focusMinutes > 240 || breakMinutes < 1 || breakMinutes > 120) {
            badRequest("Odak veya mola süresi izin verilen aralığın dışında.");
        }
        if (focusMinutes <= breakMinutes) badRequest("Odak süresi mola süresinden uzun olmalıdır.");
        if (timeZone == null || timeZone.isBlank()) badRequest("Saat dilimi gereklidir.");
        try { ZoneId.of(timeZone); }
        catch (DateTimeException ex) { badRequest("Saat dilimi geçersiz."); }
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

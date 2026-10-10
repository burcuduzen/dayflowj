package com.burcuduzen.dayflow.settings;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSettingsServiceTests {
    private final UserSettingsRepository repository = mock(UserSettingsRepository.class);
    private final UserSettingsService service = new UserSettingsService(repository);

    @Test
    void createsDefaultsForFirstUse() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserSettings settings = service.get();
        assertEquals(3, settings.getDailyTaskGoal());
        assertEquals(LocalTime.of(9, 0), settings.getWorkStart());
        assertEquals(25, settings.getFocusMinutes());
    }

    @Test
    void updatesValidPreferences() {
        UserSettings existing = new UserSettings(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        UserSettings updated = service.update(5, LocalTime.of(8, 30), LocalTime.of(17, 0),
            50, 10, "Europe/London");
        assertEquals(5, updated.getDailyTaskGoal());
        assertEquals("Europe/London", updated.getTimeZone());
        assertEquals(50, updated.getFocusMinutes());
    }

    @Test
    void rejectsInvalidWorkingHoursAndTimeZone() {
        assertThrows(ResponseStatusException.class, () -> service.update(3, LocalTime.NOON, LocalTime.NOON,
            25, 5, "Europe/Istanbul"));
        assertThrows(ResponseStatusException.class, () -> service.update(3, LocalTime.of(9, 0), LocalTime.of(18, 0),
            25, 5, "Mars/Olympus"));
        verifyNoInteractions(repository);
    }
}

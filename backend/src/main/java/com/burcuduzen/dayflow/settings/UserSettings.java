package com.burcuduzen.dayflow.settings;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {
    @Id
    private Long id;

    @Column(nullable = false)
    private int dailyTaskGoal;

    @Column(nullable = false)
    private LocalTime workStart;

    @Column(nullable = false)
    private LocalTime workEnd;

    @Column(nullable = false, length = 100)
    private String timeZone;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected UserSettings() {}

    UserSettings(Long id) {
        this.id = id;
        update(3, LocalTime.of(9, 0), LocalTime.of(18, 0), "Europe/Istanbul");
    }

    void update(int dailyTaskGoal, LocalTime workStart, LocalTime workEnd,
                String timeZone) {
        this.dailyTaskGoal = dailyTaskGoal;
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.timeZone = timeZone;
        this.updatedAt = OffsetDateTime.now();
    }

    public int getDailyTaskGoal() { return dailyTaskGoal; }
    public LocalTime getWorkStart() { return workStart; }
    public LocalTime getWorkEnd() { return workEnd; }
    public String getTimeZone() { return timeZone; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

package com.burcuduzen.dayflow.focus;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.OffsetDateTime;

@Entity
@Table(name = "focus_sessions")
public class FocusSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;
    private OffsetDateTime lastResumedAt;
    private OffsetDateTime pausedAt;

    @Column(nullable = false)
    private long focusedSeconds;

    @Column(nullable = false)
    private long pausedSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FocusStatus status;

    protected FocusSession() {}

    FocusSession(Long taskId, OffsetDateTime now) {
        this.taskId = taskId;
        this.startedAt = now;
        this.lastResumedAt = now;
        this.status = FocusStatus.RUNNING;
    }

    void pause(OffsetDateTime now) {
        requireStatus(FocusStatus.RUNNING);
        focusedSeconds += elapsed(lastResumedAt, now);
        lastResumedAt = null;
        pausedAt = now;
        status = FocusStatus.PAUSED;
    }

    void resume(OffsetDateTime now) {
        requireStatus(FocusStatus.PAUSED);
        pausedSeconds += elapsed(pausedAt, now);
        pausedAt = null;
        lastResumedAt = now;
        status = FocusStatus.RUNNING;
    }

    void complete(OffsetDateTime now) {
        if (status == FocusStatus.COMPLETED) {
            throw new IllegalStateException("Oturum zaten tamamlandı.");
        }
        if (status == FocusStatus.RUNNING) {
            focusedSeconds += elapsed(lastResumedAt, now);
            lastResumedAt = null;
        } else {
            pausedSeconds += elapsed(pausedAt, now);
            pausedAt = null;
        }
        endedAt = now;
        status = FocusStatus.COMPLETED;
    }

    long currentFocusedSeconds(OffsetDateTime now) {
        return focusedSeconds + (status == FocusStatus.RUNNING ? elapsed(lastResumedAt, now) : 0);
    }

    long currentPausedSeconds(OffsetDateTime now) {
        return pausedSeconds + (status == FocusStatus.PAUSED ? elapsed(pausedAt, now) : 0);
    }

    private void requireStatus(FocusStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(expected == FocusStatus.RUNNING
                ? "Yalnızca çalışan oturum duraklatılabilir."
                : "Yalnızca duraklatılmış oturum devam ettirilebilir.");
        }
    }

    private long elapsed(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to.isBefore(from)) return 0;
        return Duration.between(from, to).getSeconds();
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public long getFocusedSeconds() { return focusedSeconds; }
    public long getPausedSeconds() { return pausedSeconds; }
    public FocusStatus getStatus() { return status; }
}

package com.burcuduzen.dayflow.calendar;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected CalendarEvent() {}

    CalendarEvent(String title, String description, OffsetDateTime startAt, OffsetDateTime endAt) {
        this.createdAt = OffsetDateTime.now();
        update(title, description, startAt, endAt);
        this.createdAt = this.updatedAt;
    }

    void update(String title, String description, OffsetDateTime startAt, OffsetDateTime endAt) {
        this.title = title.strip();
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

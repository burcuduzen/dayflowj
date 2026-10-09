package com.burcuduzen.dayflow.planning;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "planned_blocks", uniqueConstraints = @UniqueConstraint(columnNames = "task_id"))
public class PlannedBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PlannedBlock() {}

    PlannedBlock(Long taskId, OffsetDateTime startAt, OffsetDateTime endAt) {
        this.taskId = taskId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

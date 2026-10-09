package com.burcuduzen.dayflow.progress;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "completion_activity")
public class CompletionActivity {
    @Id public Long taskId;
    @Column(nullable = false) public OffsetDateTime completedAt;
    protected CompletionActivity() {}
    public CompletionActivity(Long taskId, OffsetDateTime completedAt) { this.taskId = taskId; this.completedAt = completedAt; }
}

package com.burcuduzen.dayflow.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Task t where t.id = :id")
    java.util.Optional<Task> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Task> findByStatusNotAndReminderAtLessThanEqualOrderByReminderAtAsc(TaskStatus status, java.time.OffsetDateTime now);
    List<Task> findAllByOrderByCreatedAtDescIdDesc();
    List<Task> findByStatusOrderByCreatedAtDescIdDesc(TaskStatus status);
}

package com.burcuduzen.dayflow.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByCreatedAtDescIdDesc();
    List<Task> findByStatusOrderByCreatedAtDescIdDesc(TaskStatus status);
}

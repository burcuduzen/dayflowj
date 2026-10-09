package com.burcuduzen.dayflow.task;

import com.burcuduzen.dayflow.task.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TaskService {
    private final TaskRepository repository;
    public TaskService(TaskRepository repository) { this.repository = repository; }

    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(request.title().strip(), request.description(), request.dueDate(),
            request.priority() == null ? TaskPriority.MEDIUM : request.priority(), request.estimatedMinutes());
        return TaskResponse.from(repository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null ? repository.findAllByOrderByCreatedAtDescIdDesc()
            : repository.findByStatusOrderByCreatedAtDescIdDesc(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) { return TaskResponse.from(find(id)); }

    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = find(id);
        task.update(request.title().strip(), request.description(), request.dueDate(),
            request.priority(), request.estimatedMinutes(), request.status());
        return TaskResponse.from(repository.save(task));
    }

    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = find(id);
        task.setStatus(status);
        return TaskResponse.from(repository.save(task));
    }

    public void delete(Long id) { repository.delete(find(id)); }

    private Task find(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }
}

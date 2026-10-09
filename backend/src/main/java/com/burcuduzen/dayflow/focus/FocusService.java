package com.burcuduzen.dayflow.focus;

import com.burcuduzen.dayflow.task.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class FocusService {
    private static final List<FocusStatus> ACTIVE = List.of(FocusStatus.RUNNING, FocusStatus.PAUSED);
    private final FocusSessionRepository sessions;
    private final TaskRepository tasks;

    public FocusService(FocusSessionRepository sessions, TaskRepository tasks) {
        this.sessions = sessions;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<FocusSession> list() {
        return sessions.findAllByOrderByStartedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public FocusSession active() {
        return sessions.findFirstByStatusInOrderByStartedAtDesc(ACTIVE).orElse(null);
    }

    public FocusSession start(Long taskId) {
        if (sessions.existsByStatusIn(ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Önce aktif odak oturumunu tamamla.");
        }
        if (taskId != null && !tasks.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Görev bulunamadı.");
        }
        return sessions.save(new FocusSession(taskId, now()));
    }

    public FocusSession pause(Long id) { return change(id, Change.PAUSE); }
    public FocusSession resume(Long id) { return change(id, Change.RESUME); }
    public FocusSession complete(Long id) { return change(id, Change.COMPLETE); }

    private FocusSession change(Long id, Change change) {
        FocusSession session = find(id);
        try {
            switch (change) {
                case PAUSE -> session.pause(now());
                case RESUME -> session.resume(now());
                case COMPLETE -> session.complete(now());
            }
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return sessions.save(session);
    }

    private FocusSession find(Long id) {
        return sessions.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Odak oturumu bulunamadı."));
    }

    OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }
    private enum Change { PAUSE, RESUME, COMPLETE }
}

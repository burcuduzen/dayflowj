package com.burcuduzen.dayflow.planning;

import com.burcuduzen.dayflow.calendar.CalendarEventRepository;
import com.burcuduzen.dayflow.task.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class PlannedBlockService {
    public record Approval(Long taskId, OffsetDateTime startAt, OffsetDateTime endAt) {}
    private final PlannedBlockRepository blocks;
    private final CalendarEventRepository events;
    private final TaskRepository tasks;

    public PlannedBlockService(PlannedBlockRepository blocks, CalendarEventRepository events, TaskRepository tasks) {
        this.blocks = blocks;
        this.events = events;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<PlannedBlock> list(OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) return blocks.findAllByOrderByStartAtAscIdAsc();
        if (from == null || to == null || !to.isAfter(from)) badRequest("Geçerli from ve to birlikte verilmelidir.");
        if (Duration.between(from, to).compareTo(Duration.ofDays(366)) > 0) badRequest("En fazla 366 günlük aralık istenebilir.");
        return blocks.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(to, from);
    }

    public List<PlannedBlock> approve(List<Approval> approvals) {
        if (approvals == null || approvals.isEmpty()) badRequest("Onaylanacak en az bir plan bloğu gerekli.");
        Set<Long> taskIds = new HashSet<>();
        List<Approval> sorted = approvals.stream().sorted(Comparator.comparing(Approval::startAt)).toList();
        for (int index = 0; index < sorted.size(); index++) {
            Approval approval = sorted.get(index);
            validateTimes(approval);
            if (!taskIds.add(approval.taskId())) conflict("Aynı görev için birden fazla plan bloğu gönderilemez.");
            if (index > 0 && sorted.get(index - 1).endAt().isAfter(approval.startAt())) {
                conflict("Onaylanan plan blokları birbiriyle çakışamaz.");
            }
        }

        List<PlannedBlock> accepted = new ArrayList<>();
        for (Approval approval : sorted) {
            Task task = tasks.findForUpdate(approval.taskId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Planlanacak görev bulunamadı."));
            if (task.isComplete()) conflict("Tamamlanmış görev planlanamaz.");
            if (task.getEstimatedMinutes() == null) conflict("Görevin tahmini süresi bulunmuyor.");
            if (Duration.between(approval.startAt(), approval.endAt()).toMinutes() != task.getEstimatedMinutes()) {
                conflict("Plan bloğu süresi görevin tahmini süresiyle eşleşmiyor.");
            }
            if (blocks.existsByTaskId(task.getId())) conflict("Görev zaten planlanmış.");
            if (!events.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                    approval.endAt(), approval.startAt()).isEmpty()) {
                conflict("Plan bloğu sabit bir takvim etkinliğiyle çakışıyor.");
            }
            if (!blocks.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                    approval.endAt(), approval.startAt()).isEmpty()) {
                conflict("Plan bloğu mevcut bir planla çakışıyor.");
            }
            accepted.add(new PlannedBlock(approval.taskId(), approval.startAt(), approval.endAt()));
        }
        return blocks.saveAll(accepted);
    }

    public void delete(Long id) {
        PlannedBlock block = blocks.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan bloğu bulunamadı."));
        blocks.delete(block);
    }

    private void validateTimes(Approval approval) {
        if (approval.taskId() == null || approval.startAt() == null || approval.endAt() == null
            || !approval.endAt().isAfter(approval.startAt())) {
            badRequest("Her plan bloğu geçerli görev, başlangıç ve bitiş içermelidir.");
        }
    }

    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}

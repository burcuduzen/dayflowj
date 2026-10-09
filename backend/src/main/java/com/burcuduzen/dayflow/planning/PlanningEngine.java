package com.burcuduzen.dayflow.planning;

import com.burcuduzen.dayflow.task.TaskPriority;
import java.time.*;
import java.util.*;

final class PlanningEngine {
    record Candidate(Long taskId, String title, OffsetDateTime dueDate,
                     TaskPriority priority, Integer estimatedMinutes) {}
    record Busy(OffsetDateTime startAt, OffsetDateTime endAt) {}
    record Block(Long taskId, String title, OffsetDateTime startAt, OffsetDateTime endAt, int minutes) {}
    record Unscheduled(Long taskId, String title, String reason) {}
    record Result(List<Block> blocks, List<Unscheduled> unscheduled) {}

    private PlanningEngine() {}

    static Result plan(LocalDate from, LocalDate to, LocalTime workStart, LocalTime workEnd,
                       ZoneId zone, OffsetDateTime notBefore, List<Candidate> candidates, List<Busy> busyInput) {
        List<Candidate> sorted = candidates.stream().sorted(Comparator
            .comparing(Candidate::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing((Candidate candidate) -> priorityRank(candidate.priority()), Comparator.reverseOrder())
            .thenComparing(Candidate::taskId)).toList();
        List<Busy> occupied = new ArrayList<>(busyInput);
        List<Block> blocks = new ArrayList<>();
        List<Unscheduled> unscheduled = new ArrayList<>();

        for (Candidate candidate : sorted) {
            if (candidate.estimatedMinutes() == null || candidate.estimatedMinutes() <= 0) {
                unscheduled.add(new Unscheduled(candidate.taskId(), candidate.title(), "MISSING_ESTIMATE"));
                continue;
            }
            Optional<Block> block = findSlot(from, to, workStart, workEnd, zone, notBefore,
                candidate, occupied);
            if (block.isEmpty()) {
                unscheduled.add(new Unscheduled(candidate.taskId(), candidate.title(), "NO_AVAILABLE_SLOT"));
            } else {
                blocks.add(block.get());
                occupied.add(new Busy(block.get().startAt(), block.get().endAt()));
            }
        }
        blocks.sort(Comparator.comparing(Block::startAt));
        return new Result(List.copyOf(blocks), List.copyOf(unscheduled));
    }

    private static Optional<Block> findSlot(LocalDate from, LocalDate to, LocalTime workStart,
                                            LocalTime workEnd, ZoneId zone, OffsetDateTime notBefore,
                                            Candidate candidate, List<Busy> occupied) {
        Duration duration = Duration.ofMinutes(candidate.estimatedMinutes());
        Instant earliestAllowed = notBefore.toInstant();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            ZonedDateTime dayStart = date.atTime(workStart).atZone(zone);
            ZonedDateTime dayEnd = date.atTime(workEnd).atZone(zone);
            Instant cursor = dayStart.toInstant().isAfter(earliestAllowed) ? dayStart.toInstant() : earliestAllowed;
            Instant limit = dayEnd.toInstant();
            if (candidate.dueDate() != null && candidate.dueDate().toInstant().isAfter(earliestAllowed)
                && candidate.dueDate().toInstant().isBefore(limit)) {
                limit = candidate.dueDate().toInstant();
            }
            if (!cursor.isBefore(limit)) continue;

            Instant searchStart = cursor;
            Instant searchLimit = limit;
            List<Busy> dayBusy = occupied.stream()
                .filter(busy -> busy.startAt().toInstant().isBefore(searchLimit)
                    && busy.endAt().toInstant().isAfter(searchStart))
                .sorted(Comparator.comparing(busy -> busy.startAt().toInstant())).toList();
            for (Busy busy : dayBusy) {
                Instant busyStart = busy.startAt().toInstant();
                if (!cursor.plus(duration).isAfter(busyStart)) {
                    return Optional.of(block(candidate, cursor, duration, zone));
                }
                if (busy.endAt().toInstant().isAfter(cursor)) cursor = busy.endAt().toInstant();
                if (!cursor.isBefore(limit)) break;
            }
            if (!cursor.plus(duration).isAfter(limit)) {
                return Optional.of(block(candidate, cursor, duration, zone));
            }
        }
        return Optional.empty();
    }

    private static Block block(Candidate candidate, Instant start, Duration duration, ZoneId zone) {
        OffsetDateTime startAt = start.atZone(zone).toOffsetDateTime();
        return new Block(candidate.taskId(), candidate.title(), startAt,
            start.plus(duration).atZone(zone).toOffsetDateTime(), candidate.estimatedMinutes());
    }

    private static int priorityRank(TaskPriority priority) {
        if (priority == TaskPriority.HIGH) return 3;
        if (priority == TaskPriority.MEDIUM) return 2;
        return 1;
    }
}

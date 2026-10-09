package com.burcuduzen.dayflow.focus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findAllByOrderByStartedAtDescIdDesc();
    Optional<FocusSession> findFirstByStatusInOrderByStartedAtDesc(Collection<FocusStatus> statuses);
    boolean existsByStatusIn(Collection<FocusStatus> statuses);
    List<FocusSession> findByStatusAndEndedAtGreaterThanEqualAndEndedAtLessThan(
        FocusStatus status, OffsetDateTime from, OffsetDateTime until);
}

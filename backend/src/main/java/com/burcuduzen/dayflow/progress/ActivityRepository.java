package com.burcuduzen.dayflow.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface ActivityRepository extends JpaRepository<CompletionActivity, Long> {
    List<CompletionActivity> findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(
        OffsetDateTime from, OffsetDateTime until);
}

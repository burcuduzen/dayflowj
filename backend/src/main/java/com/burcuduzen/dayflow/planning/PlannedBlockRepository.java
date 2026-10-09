package com.burcuduzen.dayflow.planning;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.*;

public interface PlannedBlockRepository extends JpaRepository<PlannedBlock, Long> {
    List<PlannedBlock> findAllByOrderByStartAtAscIdAsc();
    List<PlannedBlock> findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
        OffsetDateTime until, OffsetDateTime from);
    boolean existsByTaskId(Long taskId);
}

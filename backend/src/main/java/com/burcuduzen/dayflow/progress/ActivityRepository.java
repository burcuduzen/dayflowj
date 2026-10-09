package com.burcuduzen.dayflow.progress;

import org.springframework.data.jpa.repository.JpaRepository;
public interface ActivityRepository extends JpaRepository<CompletionActivity, Long> {}

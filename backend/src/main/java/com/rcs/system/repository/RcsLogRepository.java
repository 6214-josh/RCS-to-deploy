package com.rcs.system.repository;

import com.rcs.system.model.RcsLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RcsLogRepository extends JpaRepository<RcsLog, Long> {
    List<RcsLog> findTop100ByOrderByCreatedAtDesc();
}

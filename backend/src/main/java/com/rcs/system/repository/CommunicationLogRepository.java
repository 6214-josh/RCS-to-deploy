package com.rcs.system.repository;

import com.rcs.system.model.CommunicationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long> {
    List<CommunicationLog> findAllByOrderByTimestampDesc(Pageable pageable);
}

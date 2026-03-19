package com.rcs.system.repository;

import com.rcs.system.model.PickingOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PickingOrderRepository extends JpaRepository<PickingOrder, Long> {
    Optional<PickingOrder> findByJobNo(String jobNo);
    Optional<PickingOrder> findByCommandNo(String commandNo);
    boolean existsByCommandNo(String commandNo);
    long countByCommandStatus(String commandStatus);
    List<PickingOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<PickingOrder> findByCommandStatusInOrderByCreatedAtDesc(List<String> statuses);
}

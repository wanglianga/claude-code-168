package com.ruralmarket.repo;

import com.ruralmarket.domain.AdjustmentLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjustmentLedgerRepository extends JpaRepository<AdjustmentLedger, Long> {
    List<AdjustmentLedger> findAllByOrderByCreatedAtDesc();
    List<AdjustmentLedger> findByFairDayIdOrderByCreatedAtDesc(Long fairDayId);
    List<AdjustmentLedger> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    List<AdjustmentLedger> findByAllocationIdOrderByCreatedAtDesc(Long allocationId);
}

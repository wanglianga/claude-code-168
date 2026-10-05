package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.Adjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdjustmentRepository extends JpaRepository<Adjustment, Long> {
    List<Adjustment> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<Adjustment> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    List<Adjustment> findByResultId(Long resultId);
}

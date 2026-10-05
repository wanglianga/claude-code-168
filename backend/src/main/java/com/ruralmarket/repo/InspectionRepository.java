package com.ruralmarket.repo;

import com.ruralmarket.domain.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByFairDayIdOrderByCreatedAtDesc(Long fairDayId);
    List<Inspection> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
}

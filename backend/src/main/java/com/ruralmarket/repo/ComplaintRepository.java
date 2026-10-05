package com.ruralmarket.repo;

import com.ruralmarket.domain.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByFairDayId(Long fairDayId);
    List<Complaint> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
}

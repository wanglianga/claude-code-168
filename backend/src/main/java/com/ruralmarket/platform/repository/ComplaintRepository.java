package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<Complaint> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    List<Complaint> findByResultId(Long resultId);
}

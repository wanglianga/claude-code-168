package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.CreditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditLogRepository extends JpaRepository<CreditLog, Long> {
    List<CreditLog> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
}

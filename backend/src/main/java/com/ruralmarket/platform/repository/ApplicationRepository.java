package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByEventId(Long eventId);
    List<Application> findByEventIdAndStatus(Long eventId, String status);
    List<Application> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    Optional<Application> findByEventIdAndVendorId(Long eventId, Long vendorId);
}

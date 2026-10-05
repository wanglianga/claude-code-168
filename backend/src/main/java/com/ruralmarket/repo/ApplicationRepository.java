package com.ruralmarket.repo;

import com.ruralmarket.domain.ApplyStatus;
import com.ruralmarket.domain.Application;
import com.ruralmarket.domain.FairDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByFairDayOrderById(FairDay fairDay);
    List<Application> findByFairDayAndStatus(FairDay fairDay, ApplyStatus status);
    Optional<Application> findByFairDayIdAndVendorId(Long fairDayId, Long vendorId);
    List<Application> findByVendorIdOrderBySubmittedAtDesc(Long vendorId);
}

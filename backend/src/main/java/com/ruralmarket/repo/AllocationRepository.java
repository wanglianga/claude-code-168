package com.ruralmarket.repo;

import com.ruralmarket.domain.Allocation;
import com.ruralmarket.domain.FairDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByFairDayOrderByStall_StallNo(FairDay fairDay);
    Optional<Allocation> findByFairDayIdAndVendorId(Long fairDayId, Long vendorId);
    Optional<Allocation> findByCheckCode(String checkCode);
    List<Allocation> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    List<Allocation> findByStallId(Long stallId);
}

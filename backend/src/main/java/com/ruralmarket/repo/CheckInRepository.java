package com.ruralmarket.repo;

import com.ruralmarket.domain.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    Optional<CheckIn> findFirstByAllocationIdOrderByCreatedAtDesc(Long allocationId);
    List<CheckIn> findByAllocation_FairDayIdOrderByCreatedAtDesc(Long fairDayId);
}

package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.FoodSample;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodSampleRepository extends JpaRepository<FoodSample, Long> {
    List<FoodSample> findByEventId(Long eventId);
    List<FoodSample> findByVendorIdOrderByRetainedAtDesc(Long vendorId);
    List<FoodSample> findByEventIdAndVendorId(Long eventId, Long vendorId);
}

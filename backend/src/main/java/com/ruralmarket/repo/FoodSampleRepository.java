package com.ruralmarket.repo;

import com.ruralmarket.domain.FoodSample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodSampleRepository extends JpaRepository<FoodSample, Long> {
    List<FoodSample> findByFairDayIdOrderBySampledAtDesc(Long fairDayId);
    List<FoodSample> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
}

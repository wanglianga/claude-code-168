package com.ruralmarket.service;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.domain.Allocation;
import com.ruralmarket.domain.FoodSample;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.AllocationRepository;
import com.ruralmarket.repo.FairDayRepository;
import com.ruralmarket.repo.FoodSampleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FoodSampleService {

    private final FoodSampleRepository sampleRepository;
    private final FairDayRepository fairDayRepository;
    private final AllocationRepository allocationRepository;

    public FoodSampleService(FoodSampleRepository sampleRepository, FairDayRepository fairDayRepository,
                             AllocationRepository allocationRepository) {
        this.sampleRepository = sampleRepository;
        this.fairDayRepository = fairDayRepository;
        this.allocationRepository = allocationRepository;
    }

    @Transactional
    public FoodSample submit(FoodSample payload, Long fairDayId, SysUser vendor) {
        FoodSample s = new FoodSample();
        s.setFairDay(fairDayRepository.findById(fairDayId)
                .orElseThrow(() -> new ApiException("赶集日不存在")));
        s.setVendor(vendor);
        Allocation allocation = allocationRepository
                .findByFairDayIdAndVendorId(fairDayId, vendor.getId()).orElse(null);
        s.setAllocation(allocation);
        s.setFoodName(payload.getFoodName());
        s.setBatchNo(payload.getBatchNo());
        s.setStorageLocation(payload.getStorageLocation());
        s.setSampledAt(payload.getSampledAt() == null ? LocalDateTime.now() : payload.getSampledAt());
        s.setPhotos(payload.getPhotos());
        s.setStorageTemp(payload.getStorageTemp());
        s.setRemark(payload.getRemark());
        if (s.getFoodName() == null || s.getFoodName().isBlank()
                || s.getBatchNo() == null || s.getBatchNo().isBlank()
                || s.getStorageLocation() == null || s.getStorageLocation().isBlank()) {
            throw new ApiException("留样食品名称、批次号、保存位置均为必填");
        }
        return sampleRepository.save(s);
    }
}

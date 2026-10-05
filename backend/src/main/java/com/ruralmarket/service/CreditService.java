package com.ruralmarket.service;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.AllocationRepository;
import com.ruralmarket.repo.FairDayRepository;
import com.ruralmarket.repo.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 信用调整：独立于扣罚的直接信用加减，同样必须说明来源与理由。
 */
@Service
public class CreditService {

    private final SysUserRepository userRepository;
    private final AllocationRepository allocationRepository;
    private final FairDayRepository fairDayRepository;
    private final LedgerService ledgerService;

    public CreditService(SysUserRepository userRepository, AllocationRepository allocationRepository,
                         FairDayRepository fairDayRepository, LedgerService ledgerService) {
        this.userRepository = userRepository;
        this.allocationRepository = allocationRepository;
        this.fairDayRepository = fairDayRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public SysUser adjust(Long vendorId, Long fairDayId, Integer delta, String reason,
                          String sourceType, Long sourceId, SysUser operator) {
        SysUser vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new ApiException("摊主不存在"));
        if (delta == null || delta == 0) {
            throw new ApiException("信用分值变化不能为 0");
        }
        FairDay day = fairDayId == null ? null
                : fairDayRepository.findById(fairDayId).orElse(null);
        Allocation allocation = fairDayId == null ? null
                : allocationRepository.findByFairDayIdAndVendorId(fairDayId, vendorId).orElse(null);
        int before = vendor.getCreditScore();
        ledgerService.record(AdjustType.CREDIT_ADJUST, day, vendor, allocation,
                "信用" + before, "信用" + Math.max(0, Math.min(200, before + delta)),
                null, delta,
                sourceType == null ? "人工核查" : sourceType, sourceId, reason, operator);
        return userRepository.findById(vendorId).orElseThrow();
    }
}

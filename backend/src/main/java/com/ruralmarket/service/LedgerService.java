package com.ruralmarket.service;

import com.ruralmarket.domain.*;
import com.ruralmarket.repo.AdjustmentLedgerRepository;
import com.ruralmarket.repo.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 调整台账：所有改变抽签结果、费用、保证金、信用的动作统一在此落账，
 * 保证“临时调位、费用减免、扣罚、信用调整”必有来源与理由。
 */
@Service
public class LedgerService {

    private final AdjustmentLedgerRepository ledgerRepository;
    private final SysUserRepository userRepository;

    public LedgerService(AdjustmentLedgerRepository ledgerRepository, SysUserRepository userRepository) {
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AdjustmentLedger record(AdjustType type, FairDay day, SysUser vendor, Allocation allocation,
                                   String before, String after, BigDecimal amount, Integer creditDelta,
                                   String sourceType, Long sourceId, String reason, SysUser operator) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("调整必须填写理由（来源说明）");
        }
        AdjustmentLedger ledger = new AdjustmentLedger();
        ledger.setType(type);
        ledger.setFairDay(day);
        ledger.setVendor(vendor);
        ledger.setAllocation(allocation);
        ledger.setBeforeValue(before);
        ledger.setAfterValue(after);
        ledger.setAmount(amount);
        ledger.setCreditDelta(creditDelta);
        ledger.setSourceType(sourceType);
        ledger.setSourceId(sourceId);
        ledger.setReason(reason);
        ledger.setOperator(operator);
        ledgerRepository.save(ledger);

        if (creditDelta != null && creditDelta != 0 && vendor != null) {
            SysUser fresh = userRepository.findById(vendor.getId()).orElseThrow();
            fresh.setCreditScore(Math.max(0, Math.min(200, fresh.getCreditScore() + creditDelta)));
            userRepository.save(fresh);
        }
        return ledger;
    }
}

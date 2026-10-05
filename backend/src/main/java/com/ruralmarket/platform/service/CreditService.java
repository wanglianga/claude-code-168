package com.ruralmarket.platform.service;

import com.ruralmarket.platform.entity.CreditLog;
import com.ruralmarket.platform.entity.User;
import com.ruralmarket.platform.repository.CreditLogRepository;
import com.ruralmarket.platform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditService {
    private final CreditLogRepository creditLogRepository;
    private final UserRepository userRepository;

    public CreditService(CreditLogRepository creditLogRepository, UserRepository userRepository) {
        this.creditLogRepository = creditLogRepository;
        this.userRepository = userRepository;
    }

    /** 调整摊主信用分并留痕 */
    @Transactional
    public CreditLog adjust(User vendor, int delta, String reason, String sourceType, Long sourceRefId, User operator) {
        int after = Math.max(0, Math.min(100, vendor.getCreditScore() + delta));
        int realDelta = after - vendor.getCreditScore();
        vendor.setCreditScore(after);
        userRepository.save(vendor);

        CreditLog log = new CreditLog();
        log.setVendor(vendor);
        log.setDelta(realDelta);
        log.setScoreAfter(after);
        log.setReason(reason);
        log.setSourceType(sourceType);
        log.setSourceRefId(sourceRefId);
        log.setOperator(operator);
        return creditLogRepository.save(log);
    }
}

package com.ruralmarket.platform.service;

import com.ruralmarket.platform.entity.AuditLog;
import com.ruralmarket.platform.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void log(Long actorId, String actorName, String action, String detail) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorName(actorName);
        log.setAction(action);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }
}

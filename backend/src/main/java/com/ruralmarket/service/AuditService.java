package com.ruralmarket.service;

import com.ruralmarket.domain.AuditLog;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(SysUser user, String action, String detail, String ip) {
        AuditLog log = new AuditLog();
        if (user != null) {
            log.setUsername(user.getUsername());
            log.setRole(user.getRole().name());
        }
        log.setAction(action);
        log.setDetail(detail == null ? "" : (detail.length() > 480 ? detail.substring(0, 480) : detail));
        log.setIp(ip);
        repository.save(log);
    }
}

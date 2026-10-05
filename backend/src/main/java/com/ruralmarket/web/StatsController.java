package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.repo.AuditLogRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;
    private final AuditLogRepository auditLogRepository;

    public StatsController(StatsService statsService, AuditLogRepository auditLogRepository) {
        this.statsService = statsService;
        this.auditLogRepository = auditLogRepository;
    }

    /** 复盘看板：高投诉品类、拥堵/违规区域、收费到账、食品安全风险 */
    @GetMapping("/overview/{fairDayId}")
    public R<Map<String, Object>> overview(@PathVariable Long fairDayId) {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅管理人员可查看复盘看板");
        }
        return R.ok(statsService.fairDayOverview(fairDayId));
    }

    @GetMapping("/audit-logs")
    public R<?> auditLogs() {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅管理人员可查看审计日志");
        }
        return R.ok(auditLogRepository.findTop100ByOrderByCreatedAtDesc());
    }
}

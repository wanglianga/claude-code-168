package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.SysUserRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import com.ruralmarket.service.CreditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserRepository userRepository;
    private final CreditService creditService;
    private final AuditService auditService;

    public UserController(SysUserRepository userRepository, CreditService creditService,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.creditService = creditService;
        this.auditService = auditService;
    }

    @GetMapping("/vendors")
    public R<List<SysUser>> vendors() {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅管理人员可查询摊主名录");
        }
        return R.ok(userRepository.findByRole(com.ruralmarket.domain.Role.VENDOR));
    }

    public static class CreditRequest {
        public Long vendorId;
        public Long fairDayId;
        public Integer delta;
        public String reason;
        public String sourceType;
        public Long sourceId;
    }

    /** 信用分调整（须说明来源与理由，进调整台账） */
    @PostMapping("/credit-adjust")
    public R<SysUser> creditAdjust(@RequestBody CreditRequest req, HttpServletRequest request) {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅管理人员可调整信用分");
        }
        SysUser me = CurrentUser.require();
        if (req.reason == null || req.reason.isBlank()) {
            throw new ApiException("信用调整必须填写理由");
        }
        SysUser vendor = creditService.adjust(req.vendorId, req.fairDayId, req.delta,
                req.reason, req.sourceType, req.sourceId, me);
        auditService.log(me, "信用分调整",
                "摊主#" + req.vendorId + " " + (req.delta > 0 ? "+" : "") + req.delta
                        + "：" + req.reason, request.getRemoteAddr());
        return R.ok(vendor);
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> profile(@PathVariable Long id) {
        SysUser me = CurrentUser.require();
        if (me.getRole() == com.ruralmarket.domain.Role.VENDOR && !me.getId().equals(id)) {
            throw new ApiException(403, "只能查看本人信息");
        }
        return R.ok(Map.of("user", userRepository.findById(id)
                .orElseThrow(() -> new ApiException("用户不存在"))));
    }
}

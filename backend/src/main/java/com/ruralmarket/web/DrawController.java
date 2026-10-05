package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.*;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import com.ruralmarket.service.DrawService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/draw")
public class DrawController {

    private final DrawService drawService;
    private final AllocationRepository allocationRepository;
    private final FairDayRepository fairDayRepository;
    private final AuditService auditService;

    public DrawController(DrawService drawService, AllocationRepository allocationRepository,
                          FairDayRepository fairDayRepository, AuditService auditService) {
        this.drawService = drawService;
        this.allocationRepository = allocationRepository;
        this.fairDayRepository = fairDayRepository;
        this.auditService = auditService;
    }

    public static class DrawRequest {
        public String seed;
    }

    /** 组织抽签（公示种子；不传则随机生成） */
    @PostMapping("/{fairDayId}")
    public R<Map<String, Object>> draw(@PathVariable Long fairDayId, @RequestBody(required = false) DrawRequest req,
                                       HttpServletRequest request) {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可组织抽签");
        }
        SysUser me = CurrentUser.require();
        String seed = req == null ? null : req.seed;
        Map<String, Object> result = drawService.draw(fairDayId, seed, me);
        auditService.log(me, "组织抽签",
                "集市#" + fairDayId + " 种子=" + result.get("seed")
                        + " 中签" + result.get("allocatedCount") + " 候补" + result.get("waitlistCount"),
                request.getRemoteAddr());
        return R.ok(result);
    }

    /** 当场抽签结果（公示，所有登录用户可见） */
    @GetMapping("/{fairDayId}/allocations")
    public R<List<Allocation>> allocations(@PathVariable Long fairDayId) {
        FairDay day = fairDayRepository.findById(fairDayId).orElseThrow(() -> new ApiException("赶集日不存在"));
        return R.ok(allocationRepository.findByFairDayOrderByStall_StallNo(day));
    }

    /** 我的抽签结果 */
    @GetMapping("/mine/{fairDayId}")
    public R<Allocation> mine(@PathVariable Long fairDayId) {
        SysUser me = CurrentUser.require();
        return R.ok(allocationRepository.findByFairDayIdAndVendorId(fairDayId, me.getId()).orElse(null));
    }
}

package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.ApplicationRepository;
import com.ruralmarket.repo.FairDayRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.ApplicationService;
import com.ruralmarket.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final FairDayRepository fairDayRepository;
    private final AuditService auditService;

    public ApplicationController(ApplicationRepository applicationRepository,
                                 ApplicationService applicationService,
                                 FairDayRepository fairDayRepository, AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.fairDayRepository = fairDayRepository;
        this.auditService = auditService;
    }

    /** 摊主提交/修改报名 */
    @PostMapping
    public R<Application> submit(@RequestBody Application payload, HttpServletRequest request) {
        SysUser me = CurrentUser.require();
        if (me.getRole() != Role.VENDOR) {
            throw new ApiException(403, "仅摊主可提交报名");
        }
        if (payload.getFairDay() == null || payload.getFairDay().getId() == null) {
            throw new ApiException("缺少赶集日");
        }
        Application saved = applicationService.submit(payload.getFairDay().getId(), payload, me);
        auditService.log(me, "提交报名",
                saved.getFairDay().getName() + " " + saved.getCategory(), request.getRemoteAddr());
        return R.ok(saved);
    }

    /** 我的报名记录 */
    @GetMapping("/mine")
    public R<List<Application>> mine() {
        SysUser me = CurrentUser.require();
        return R.ok(applicationRepository.findByVendorIdOrderBySubmittedAtDesc(me.getId()));
    }

    /** 某场报名列表（管理端） */
    @GetMapping
    public R<List<Application>> list(@RequestParam(required = false) Long fairDayId) {
        requireStaff();
        if (fairDayId == null) {
            return R.ok(applicationRepository.findAll());
        }
        FairDay day = fairDayRepository.findById(fairDayId).orElseThrow(() -> new ApiException("赶集日不存在"));
        return R.ok(applicationRepository.findByFairDayOrderById(day));
    }

    public static class ReviewRequest {
        public boolean approved;
        public String note;
        public Integer historyCount;
    }

    /** 审核报名 */
    @PutMapping("/{id}/review")
    public R<Application> review(@PathVariable Long id, @RequestBody ReviewRequest req,
                                 HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        Application app = applicationService.review(id, req.approved, req.note, req.historyCount, me);
        auditService.log(me, (req.approved ? "审核通过报名" : "审核驳回报名"),
                app.getVendor().getName() + " " + app.getCategory()
                        + (req.historyCount != null ? "，核实赶集次数=" + req.historyCount : ""),
                request.getRemoteAddr());
        return R.ok(app);
    }

    private void requireStaff() {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可操作");
        }
    }
}

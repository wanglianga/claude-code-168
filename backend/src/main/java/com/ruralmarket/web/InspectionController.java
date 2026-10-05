package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.Inspection;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.InspectionRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import com.ruralmarket.service.OperationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspections")
public class InspectionController {

    private final OperationService operationService;
    private final InspectionRepository inspectionRepository;
    private final AuditService auditService;

    public InspectionController(OperationService operationService,
                                InspectionRepository inspectionRepository, AuditService auditService) {
        this.operationService = operationService;
        this.inspectionRepository = inspectionRepository;
        this.auditService = auditService;
    }

    public static class CreateRequest {
        public Long fairDayId;
        public Long vendorId;
        public Long allocationId;
        public Inspection inspection;
    }

    /** 工作人员巡查记录：卫生、明码标价、缺斤少两、占道、用电安全 */
    @PostMapping
    public R<Inspection> create(@RequestBody CreateRequest req, HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        if (req.fairDayId == null || req.vendorId == null || req.inspection == null) {
            throw new ApiException("缺少集市、摊主或巡查内容");
        }
        Inspection saved = operationService.createInspection(
                req.inspection, req.fairDayId, req.vendorId, req.allocationId, me);
        auditService.log(me, "巡查记录",
                "摊主#" + req.vendorId + " 问题=" + saved.getIssues(), request.getRemoteAddr());
        return R.ok(saved);
    }

    @GetMapping
    public R<List<Inspection>> list(@RequestParam(required = false) Long fairDayId,
                                    @RequestParam(required = false) Long vendorId) {
        if (fairDayId != null) {
            return R.ok(inspectionRepository.findByFairDayIdOrderByCreatedAtDesc(fairDayId));
        }
        if (vendorId != null) {
            SysUser me = CurrentUser.require();
            if (me.getRole() == com.ruralmarket.domain.Role.VENDOR && !me.getId().equals(vendorId)) {
                throw new ApiException(403, "只能查看本人记录");
            }
            return R.ok(inspectionRepository.findByVendorIdOrderByCreatedAtDesc(vendorId));
        }
        requireStaff();
        return R.ok(inspectionRepository.findAll());
    }

    private void requireStaff() {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可操作");
        }
    }
}

package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.ComplaintRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import com.ruralmarket.service.OperationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final OperationService operationService;
    private final AuditService auditService;

    public ComplaintController(ComplaintRepository complaintRepository,
                               OperationService operationService, AuditService auditService) {
        this.complaintRepository = complaintRepository;
        this.operationService = operationService;
        this.auditService = auditService;
    }

    /** 登记投诉（工作人员代消费者登记；如腹泻类重点标记） */
    @PostMapping
    public R<Complaint> create(@RequestBody Complaint payload, HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        if (payload.getType() == null || payload.getComplainant() == null || payload.getContent() == null) {
            throw new ApiException("投诉类型、投诉人、投诉内容必填");
        }
        payload.setId(null);
        payload.setStatus(ComplaintStatus.RECEIVED);
        Complaint saved = complaintRepository.save(payload);
        auditService.log(me, "登记投诉",
                saved.getType() + " 投诉人" + saved.getComplainant(), request.getRemoteAddr());
        return R.ok(saved);
    }

    @GetMapping
    public R<List<Complaint>> list(@RequestParam(required = false) Long fairDayId,
                                   @RequestParam(required = false) Long vendorId) {
        if (fairDayId != null) return R.ok(complaintRepository.findByFairDayId(fairDayId));
        if (vendorId != null) return R.ok(complaintRepository.findByVendorIdOrderByCreatedAtDesc(vendorId));
        return R.ok(complaintRepository.findAllByOrderByCreatedAtDesc());
    }

    public static class HandleRequest {
        public ComplaintStatus status;
        public String handleResult;
        public boolean deduct;
        public java.math.BigDecimal deductAmount;
        public Integer creditDelta;
        public Long allocationId;
        public String reason;
    }

    /** 投诉处置：办结/驳回，可联动保证金扣罚与信用扣分（全部进台账） */
    @PutMapping("/{id}/handle")
    public R<Complaint> handle(@PathVariable Long id, @RequestBody HandleRequest req,
                               HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        if (req.status == null) throw new ApiException("请选择处置结果");
        Complaint c = operationService.handleComplaint(id, req.status, req.handleResult,
                req.deduct, req.deductAmount, req.creditDelta, req.allocationId, req.reason, me);
        auditService.log(me, "处置投诉",
                "投诉#" + id + " -> " + req.status.getLabel()
                        + (req.deduct ? "，已联动扣罚保证金" : ""), request.getRemoteAddr());
        return R.ok(c);
    }

    private void requireStaff() {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可操作");
        }
    }
}

package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.AllocationRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import com.ruralmarket.service.OperationService;
import com.ruralmarket.util.QrCodeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

    private final AllocationRepository allocationRepository;
    private final OperationService operationService;
    private final AuditService auditService;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public AllocationController(AllocationRepository allocationRepository,
                                OperationService operationService, AuditService auditService) {
        this.allocationRepository = allocationRepository;
        this.operationService = operationService;
        this.auditService = auditService;
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Allocation a = allocationRepository.findById(id).orElseThrow(() -> new ApiException("抽签记录不存在"));
        SysUser me = CurrentUser.require();
        if (me.getRole() == Role.VENDOR && !a.getVendor().getId().equals(me.getId())) {
            throw new ApiException(403, "只能查看本人抽签结果");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("allocation", a);
        data.put("outstanding", operationService.outstanding(a));
        data.put("qr", QrCodeUtil.toDataUri(a.getCheckCode(), 240));
        return R.ok(data);
    }

    /** 根据核销码查询（工作人员扫码用） */
    @GetMapping("/by-code/{code}")
    public R<Map<String, Object>> byCode(@PathVariable String code) {
        requireStaff();
        Allocation a = operationService.lookupByCode(code);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("allocation", a);
        data.put("outstanding", operationService.outstanding(a));
        return R.ok(data);
    }

    public static class PayRequest {
        public BigDecimal amount;
    }

    /** 确认收费到账 */
    @PostMapping("/{id}/pay")
    public R<Allocation> pay(@PathVariable Long id, @RequestBody PayRequest req, HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        Allocation a = operationService.payFee(id, req.amount, me);
        auditService.log(me, "确认收费到账",
                a.getVendor().getName() + " 缴费" + req.amount + "元，已缴" + a.getPaidAmount(),
                request.getRemoteAddr());
        return R.ok(a);
    }

    public static class WaiveRequest {
        public BigDecimal amount;
        public String reason;
        public String sourceType;
        public Long sourceId;
    }

    @PostMapping("/{id}/waive")
    public R<Allocation> waive(@PathVariable Long id, @RequestBody WaiveRequest req,
                               HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        if (req.reason == null || req.reason.isBlank()) {
            throw new ApiException("减免必须填写理由与来源");
        }
        Allocation a = operationService.waiveFee(id, req.amount, req.reason, req.sourceType, req.sourceId, me);
        auditService.log(me, "费用减免",
                a.getVendor().getName() + " 减免" + req.amount + "元：" + req.reason, request.getRemoteAddr());
        return R.ok(a);
    }

    public static class DeductRequest {
        public BigDecimal amount;
        public Integer creditDelta;
        public String reason;
        public String sourceType;
        public Long sourceId;
    }

    @PostMapping("/{id}/deduct")
    public R<Allocation> deduct(@PathVariable Long id, @RequestBody DeductRequest req,
                                HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        if (req.reason == null || req.reason.isBlank()) {
            throw new ApiException("扣罚必须填写事实依据与来源");
        }
        Allocation a = operationService.deductDeposit(id, req.amount, req.creditDelta,
                req.reason, req.sourceType, req.sourceId, me);
        auditService.log(me, "保证金扣罚",
                a.getVendor().getName() + " 扣" + req.amount + "元 信用" + req.creditDelta
                        + "：" + req.reason, request.getRemoteAddr());
        return R.ok(a);
    }

    public static class RefundRequest {
        public BigDecimal amount;
        public String reason;
    }

    @PostMapping("/{id}/refund")
    public R<Allocation> refund(@PathVariable Long id, @RequestBody(required = false) RefundRequest req,
                                HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        Allocation a = operationService.refundDeposit(id,
                req == null ? null : req.amount,
                req == null ? null : req.reason, me);
        auditService.log(me, "保证金退还",
                a.getVendor().getName() + " 退还保证金", request.getRemoteAddr());
        return R.ok(a);
    }

    public static class MoveRequest {
        public Long targetStallId;
        public String reason;
        public String sourceType;
        public Long sourceId;
    }

    @PostMapping("/{id}/move")
    public R<Allocation> move(@PathVariable Long id, @RequestBody MoveRequest req,
                              HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        if (req.reason == null || req.reason.isBlank()) {
            throw new ApiException("临时调位必须填写原因（雨天/纠纷/消防等）");
        }
        Allocation a = operationService.moveStall(id, req.targetStallId, req.reason,
                req.sourceType, req.sourceId, me);
        auditService.log(me, "临时调位",
                a.getVendor().getName() + " 调至 " + a.getStall().getStallNo() + "：" + req.reason,
                request.getRemoteAddr());
        return R.ok(a);
    }

    @PostMapping("/{id}/no-show")
    public R<Void> noShow(@PathVariable Long id, HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        operationService.markNoShow(id, me);
        auditService.log(me, "标记未到场", "抽签记录#" + id, request.getRemoteAddr());
        return R.ok();
    }

    private void requireStaff() {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可操作");
        }
    }
}

package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.CheckIn;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.CheckInRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import com.ruralmarket.service.OperationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/check-ins")
public class CheckInController {

    private final OperationService operationService;
    private final CheckInRepository checkInRepository;
    private final AuditService auditService;

    public CheckInController(OperationService operationService, CheckInRepository checkInRepository,
                             AuditService auditService) {
        this.operationService = operationService;
        this.checkInRepository = checkInRepository;
        this.auditService = auditService;
    }

    public static class CheckInRequest {
        public String code;
        public String setupPhotos;
        public String remark;
    }

    /** 工作人员扫码核验入场并拍摄摊位布置 */
    @PostMapping
    public R<CheckIn> checkIn(@RequestBody CheckInRequest req, HttpServletRequest request) {
        requireStaff();
        SysUser me = CurrentUser.require();
        if (req.code == null || req.code.isBlank()) {
            throw new ApiException("缺少核销码");
        }
        if (req.setupPhotos == null || req.setupPhotos.isBlank()) {
            throw new ApiException("入场须拍摄摊位布置照片");
        }
        CheckIn checkIn = operationService.checkInByCode(req.code, req.setupPhotos, req.remark, me);
        auditService.log(me, "扫码入场核验",
                checkIn.getAllocation().getVendor().getName()
                        + " 摊位" + checkIn.getAllocation().getStall().getStallNo(),
                request.getRemoteAddr());
        return R.ok(checkIn);
    }

    @GetMapping
    public R<List<CheckIn>> list(@RequestParam(required = false) Long fairDayId) {
        requireStaff();
        if (fairDayId == null) {
            return R.ok(checkInRepository.findAll());
        }
        return R.ok(checkInRepository.findByAllocation_FairDayIdOrderByCreatedAtDesc(fairDayId));
    }

    private void requireStaff() {
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可操作");
        }
    }
}

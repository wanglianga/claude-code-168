package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.FoodSample;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.FoodSampleRepository;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import com.ruralmarket.service.FoodSampleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food-samples")
public class FoodSampleController {

    private final FoodSampleService sampleService;
    private final FoodSampleRepository sampleRepository;
    private final AuditService auditService;

    public FoodSampleController(FoodSampleService sampleService, FoodSampleRepository sampleRepository,
                                AuditService auditService) {
        this.sampleService = sampleService;
        this.sampleRepository = sampleRepository;
        this.auditService = auditService;
    }

    /** 摊主上传留样：照片、批次、保存位置 */
    @PostMapping
    public R<FoodSample> submit(@RequestBody FoodSample payload, HttpServletRequest request) {
        SysUser me = CurrentUser.require();
        if (payload.getFairDay() == null || payload.getFairDay().getId() == null) {
            throw new ApiException("缺少赶集日");
        }
        FoodSample saved = sampleService.submit(payload, payload.getFairDay().getId(), me);
        auditService.log(me, "上传食品留样",
                saved.getFoodName() + " 批次" + saved.getBatchNo(), request.getRemoteAddr());
        return R.ok(saved);
    }

    @GetMapping("/mine")
    public R<List<FoodSample>> mine() {
        SysUser me = CurrentUser.require();
        return R.ok(sampleRepository.findByVendorIdOrderByCreatedAtDesc(me.getId()));
    }

    @GetMapping
    public R<List<FoodSample>> list(@RequestParam(required = false) Long fairDayId) {
        if (fairDayId != null) {
            return R.ok(sampleRepository.findByFairDayIdOrderBySampledAtDesc(fairDayId));
        }
        if (!CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可查看全部留样");
        }
        return R.ok(sampleRepository.findAll());
    }
}

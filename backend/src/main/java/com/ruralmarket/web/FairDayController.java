package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.*;
import com.ruralmarket.service.FairDayService;
import com.ruralmarket.service.OperationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fair-days")
public class FairDayController {

    private final FairDayRepository fairDayRepository;
    private final MarketZoneRepository zoneRepository;
    private final StallRepository stallRepository;
    private final FairDayService fairDayService;
    private final OperationService operationService;
    private final com.ruralmarket.service.AuditService auditService;

    public FairDayController(FairDayRepository fairDayRepository, MarketZoneRepository zoneRepository,
                             StallRepository stallRepository, FairDayService fairDayService,
                             OperationService operationService,
                             com.ruralmarket.service.AuditService auditService) {
        this.fairDayRepository = fairDayRepository;
        this.zoneRepository = zoneRepository;
        this.stallRepository = stallRepository;
        this.fairDayService = fairDayService;
        this.operationService = operationService;
        this.auditService = auditService;
    }

    @GetMapping
    public R<List<FairDay>> list() {
        return R.ok(fairDayRepository.findAllByOrderByMarketDateDesc());
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        FairDay day = fairDayRepository.findById(id).orElseThrow(() -> new ApiException("赶集日不存在"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fairDay", day);
        data.put("zones", zoneRepository.findByFairDayOrderByCode(day));
        data.put("stalls", stallRepository.findByFairDayOrderByStallNo(day));
        return R.ok(data);
    }

    @PostMapping
    public R<FairDay> create(@RequestBody FairDay payload, HttpServletRequest request) {
        requireStaff();
        FairDay day = fairDayService.create(payload);
        auditService.log(com.ruralmarket.security.CurrentUser.get(), "创建集市",
                day.getName() + " " + day.getMarketDate(), request.getRemoteAddr());
        return R.ok(day);
    }

    @PutMapping("/{id}/status")
    public R<FairDay> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        requireStaff();
        DayStatus status = DayStatus.valueOf(body.get("status"));
        FairDay day = operationService.updateDayStatus(id, status);
        auditService.log(com.ruralmarket.security.CurrentUser.get(), "集市状态变更",
                day.getName() + " -> " + status.getLabel(), request.getRemoteAddr());
        return R.ok(day);
    }

    @PostMapping("/{id}/zones")
    public R<MarketZone> addZone(@PathVariable Long id, @RequestBody MarketZone zone,
                                 HttpServletRequest request) {
        requireStaff();
        MarketZone saved = fairDayService.addZone(id, zone);
        auditService.log(com.ruralmarket.security.CurrentUser.get(), "新增区域",
                saved.getCode() + " " + saved.getName(), request.getRemoteAddr());
        return R.ok(saved);
    }

    public static class StallGenRequest {
        public Long zoneId;
        public String prefix;
        public int count;
        public BigDecimal length;
        public BigDecimal width;
        public int electricityCount;
        public List<Integer> fireLaneNos;
    }

    @PostMapping("/{id}/stalls/generate")
    public R<Integer> generateStalls(@PathVariable Long id, @RequestBody StallGenRequest req,
                                     HttpServletRequest request) {
        requireStaff();
        int n = fairDayService.generateStalls(id, req.zoneId, req.prefix, req.count,
                req.length, req.width, req.electricityCount, req.fireLaneNos);
        auditService.log(com.ruralmarket.security.CurrentUser.get(), "批量生成摊位",
                "生成 " + n + " 个摊位", request.getRemoteAddr());
        return R.ok(n);
    }

    @PutMapping("/stalls/{stallId}")
    public R<Stall> updateStall(@PathVariable Long stallId, @RequestBody Stall payload) {
        requireStaff();
        Stall stall = stallRepository.findById(stallId).orElseThrow(() -> new ApiException("摊位不存在"));
        if (payload.getEnabled() != null) stall.setEnabled(payload.getEnabled());
        if (payload.getHasElectricity() != null) stall.setHasElectricity(payload.getHasElectricity());
        if (payload.getNearFireLane() != null) stall.setNearFireLane(payload.getNearFireLane());
        if (payload.getNote() != null) stall.setNote(payload.getNote());
        if (payload.getLength() != null) stall.setLength(payload.getLength());
        if (payload.getWidth() != null) stall.setWidth(payload.getWidth());
        return R.ok(stallRepository.save(stall));
    }

    private void requireStaff() {
        if (!com.ruralmarket.security.CurrentUser.isStaffOrAdmin()) {
            throw new ApiException(403, "仅市场管理人员可操作");
        }
    }
}

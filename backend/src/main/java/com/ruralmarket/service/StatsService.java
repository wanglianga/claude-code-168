package com.ruralmarket.service;

import com.ruralmarket.domain.*;
import com.ruralmarket.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 乡镇管理者复盘：高投诉品类、拥堵/违规高发区域、收费到账、食品安全风险。
 */
@Service
public class StatsService {

    private final ComplaintRepository complaintRepository;
    private final InspectionRepository inspectionRepository;
    private final AllocationRepository allocationRepository;
    private final FoodSampleRepository foodSampleRepository;
    private final ApplicationRepository applicationRepository;
    private final CheckInRepository checkInRepository;
    private final FairDayRepository fairDayRepository;

    public StatsService(ComplaintRepository complaintRepository, InspectionRepository inspectionRepository,
                        AllocationRepository allocationRepository, FoodSampleRepository foodSampleRepository,
                        ApplicationRepository applicationRepository, CheckInRepository checkInRepository,
                        FairDayRepository fairDayRepository) {
        this.complaintRepository = complaintRepository;
        this.inspectionRepository = inspectionRepository;
        this.allocationRepository = allocationRepository;
        this.foodSampleRepository = foodSampleRepository;
        this.applicationRepository = applicationRepository;
        this.checkInRepository = checkInRepository;
        this.fairDayRepository = fairDayRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> fairDayOverview(Long fairDayId) {
        FairDay day = fairDayRepository.findById(fairDayId).orElse(null);
        List<Allocation> allocations = fairDayId == null
                ? List.of() : allocationRepository.findByFairDayOrderByStall_StallNo(day);
        List<Application> applications = fairDayId == null
                ? List.of() : applicationRepository.findByFairDayOrderById(day);
        List<Complaint> complaints = fairDayId == null
                ? List.of() : complaintRepository.findByFairDayId(fairDayId);
        List<Inspection> inspections = fairDayId == null
                ? List.of() : inspectionRepository.findByFairDayIdOrderByCreatedAtDesc(fairDayId);
        List<FoodSample> samples = fairDayId == null
                ? List.of() : foodSampleRepository.findByFairDayIdOrderBySampledAtDesc(fairDayId);
        List<CheckIn> checkIns = fairDayId == null
                ? List.of() : checkInRepository.findByAllocation_FairDayIdOrderByCreatedAtDesc(fairDayId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fairDayId", fairDayId);
        data.put("fairDayName", day == null ? "全部集市" : day.getName());

        // 报名/抽签概览
        Map<String, Object> signup = new LinkedHashMap<>();
        signup.put("total", applications.size());
        signup.put("approved", applications.stream().filter(a -> a.getStatus() == ApplyStatus.APPROVED).count());
        signup.put("rejected", applications.stream().filter(a -> a.getStatus() == ApplyStatus.REJECTED).count());
        signup.put("pending", applications.stream().filter(a -> a.getStatus() == ApplyStatus.SUBMITTED).count());
        data.put("signup", signup);

        long allocated = allocations.stream().filter(a -> a.getStatus() != AllocationStatus.WAITLIST).count();
        Map<String, Object> draw = new LinkedHashMap<>();
        draw.put("allocated", allocated);
        draw.put("waitlist", allocations.stream().filter(a -> a.getStatus() == AllocationStatus.WAITLIST).count());
        draw.put("checkedIn", allocations.stream().filter(a -> a.getStatus() == AllocationStatus.CHECKED_IN
                || a.getStatus() == AllocationStatus.ADJUSTED).count());
        draw.put("noShow", allocations.stream().filter(a -> a.getStatus() == AllocationStatus.NO_SHOW).count());
        draw.put("checkInCount", checkIns.size());
        data.put("draw", draw);

        // 收费到账
        BigDecimal total = allocations.stream().map(Allocation::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = allocations.stream().map(Allocation::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal waived = allocations.stream().map(Allocation::getWaivedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deducted = allocations.stream().map(Allocation::getDeductedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunded = allocations.stream().map(Allocation::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = total.subtract(paid).subtract(waived).max(BigDecimal.ZERO);
        Map<String, Object> fee = new LinkedHashMap<>();
        fee.put("receivable", total);
        fee.put("paid", paid);
        fee.put("waived", waived);
        fee.put("outstanding", outstanding);
        fee.put("depositDeducted", deducted);
        fee.put("depositRefunded", refunded);
        fee.put("paidRate", total.compareTo(BigDecimal.ZERO) == 0 ? 0
                : paid.add(waived).multiply(BigDecimal.valueOf(100))
                .divide(total, 1, java.math.RoundingMode.HALF_UP));
        data.put("fee", fee);

        // 高投诉品类：按报名品类归因
        Map<String, Integer> complaintByCategory = new LinkedHashMap<>();
        Map<String, Integer> complaintByType = new LinkedHashMap<>();
        for (Complaint c : complaints) {
            complaintByType.merge(c.getType(), 1, Integer::sum);
            String cat = "未关联品类";
            if (c.getAllocation() != null && c.getAllocation().getApplication() != null) {
                cat = c.getAllocation().getApplication().getCategory();
            }
            complaintByCategory.merge(cat, 1, Integer::sum);
        }
        data.put("complaintTotal", complaints.size());
        data.put("complaintByType", sortMap(complaintByType));
        data.put("complaintByCategory", sortMap(complaintByCategory));

        // 巡查问题与违规高发区域
        Map<String, Integer> issuesCount = new LinkedHashMap<>();
        Map<String, Map<String, Object>> zoneAgg = new LinkedHashMap<>();
        for (Inspection ins : inspections) {
            if (ins.getIssues() != null) {
                for (String tag : ins.getIssues().split(",")) {
                    String t = tag.trim();
                    if (!t.isEmpty()) issuesCount.merge(t, 1, Integer::sum);
                }
            }
            String zoneName = "未知区域";
            String stallNo = "";
            if (ins.getAllocation() != null && ins.getAllocation().getStall() != null) {
                Stall st = ins.getAllocation().getStall();
                zoneName = st.getZone().getName();
                stallNo = st.getStallNo();
            }
            Map<String, Object> z = zoneAgg.computeIfAbsent(zoneName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("inspections", 0);
                m.put("problemInspections", 0);
                m.put("scoreSum", 0);
                m.put("scoreCount", 0);
                m.put("stalls", new HashSet<String>());
                return m;
            });
            z.put("inspections", (Integer) z.get("inspections") + 1);
            int scoreSum = avgScore(ins);
            z.put("scoreSum", (Integer) z.get("scoreSum") + scoreSum);
            z.put("scoreCount", (Integer) z.get("scoreCount") + 1);
            if (Boolean.TRUE.equals(ins.getRequireRectify())
                    || (ins.getIssues() != null && !ins.getIssues().isBlank())) {
                z.put("problemInspections", (Integer) z.get("problemInspections") + 1);
            }
            @SuppressWarnings("unchecked")
            Set<String> stalls = (Set<String>) z.get("stalls");
            if (!stallNo.isEmpty()) stalls.add(stallNo);
        }
        List<Map<String, Object>> zoneList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : zoneAgg.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>(e.getValue());
            m.put("zone", e.getKey());
            int count = (Integer) m.get("scoreCount");
            int sum = (Integer) m.get("scoreSum");
            m.put("avgScore", count == 0 ? 0 : Math.round(sum * 10.0 / count) / 10.0);
            @SuppressWarnings("unchecked")
            Set<String> stalls = (Set<String>) m.remove("stalls");
            m.put("involvedStalls", stalls.size());
            zoneList.add(m);
        }
        zoneList.sort((a, b) -> (Integer) b.get("problemInspections") - (Integer) a.get("problemInspections"));
        data.put("issues", sortMap(issuesCount));
        data.put("zones", zoneList);

        // 食品安全风险：留样数量、熟食摊位数量、留样覆盖率、腹泻类投诉
        long cookedStalls = allocations.stream()
                .filter(a -> a.getApplication() != null && Boolean.TRUE.equals(a.getApplication().getCookedFood()))
                .count();
        long diarrhea = complaints.stream().filter(c -> c.getType() != null && c.getType().contains("腹泻")).count();
        long lowHygiene = inspections.stream().filter(i -> i.getHygieneScore() <= 3).count();
        Map<String, Object> foodRisk = new LinkedHashMap<>();
        foodRisk.put("cookedStalls", cookedStalls);
        foodRisk.put("sampleCount", samples.size());
        foodRisk.put("sampleCoverage", cookedStalls == 0 ? 100
                : Math.min(100, Math.round(samples.size() * 100.0 / cookedStalls)));
        foodRisk.put("diarrheaComplaints", diarrhea);
        foodRisk.put("lowHygieneInspections", lowHygiene);
        foodRisk.put("riskLevel", (diarrhea > 0 || (cookedStalls > 0 && samples.size() < cookedStalls))
                ? "高" : (lowHygiene > 0 ? "中" : "低"));
        data.put("foodRisk", foodRisk);

        return data;
    }

    private int avgScore(Inspection ins) {
        return safe(ins.getHygieneScore()) + safe(ins.getPriceTagScore())
                + safe(ins.getMeasureScore()) + safe(ins.getAisleScore()) + safe(ins.getElectricScore());
    }

    private int safe(Integer v) { return v == null ? 5 : v; }

    private List<Map<String, Object>> sortMap(Map<String, Integer> map) {
        List<Map<String, Object>> list = new ArrayList<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    list.add(m);
                });
        return list;
    }
}

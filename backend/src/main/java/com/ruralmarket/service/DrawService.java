package com.ruralmarket.service;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.util.*;

/**
 * 摊位抽签服务。
 *
 * 规则（当场公示，结果可复算）：
 * 1. 区域规则：活禽→活禽隔离区，熟食/凉菜/糕点→熟食冷链区，其余→普通区。
 * 2. 消防通道：邻近消防通道的摊位不安排熟食明火与活禽。
 * 3. 尺寸匹配：摊位长宽须容纳申报车辆/摊棚尺寸。
 * 4. 用电：需要用电的摊主只能分配带电源的摊位。
 * 5. 老摊主优先：优先级 = 历史赶集次数*3 + (信用分-100)，高优先级先抽；
 *    同一优先级比随机数（小者中签），随机数由当场公示种子按确定顺序产生，可复算。
 * 6. 未匹配到摊位者列入候补（WAITLIST）。
 */
@Service
public class DrawService {

    private final FairDayRepository fairDayRepository;
    private final ApplicationRepository applicationRepository;
    private final StallRepository stallRepository;
    private final AllocationRepository allocationRepository;
    private final CheckInRepository checkInRepository;

    public DrawService(FairDayRepository fairDayRepository, ApplicationRepository applicationRepository,
                       StallRepository stallRepository, AllocationRepository allocationRepository,
                       CheckInRepository checkInRepository) {
        this.fairDayRepository = fairDayRepository;
        this.applicationRepository = applicationRepository;
        this.stallRepository = stallRepository;
        this.allocationRepository = allocationRepository;
        this.checkInRepository = checkInRepository;
    }

    private static class Candidate {
        Application application;
        SysUser vendor;
        int priority;
        long randomValue;
        ZoneType targetZone;
    }

    @Transactional
    public Map<String, Object> draw(Long fairDayId, String seedInput, SysUser operator) {
        FairDay day = fairDayRepository.findById(fairDayId)
                .orElseThrow(() -> new ApiException("赶集日不存在"));

        List<Allocation> existing = allocationRepository.findByFairDayOrderByStall_StallNo(day);
        if (!existing.isEmpty()) {
            boolean hasCheckin = existing.stream().anyMatch(a -> a.getStatus() == AllocationStatus.CHECKED_IN);
            if (hasCheckin) {
                throw new ApiException("已有摊主入场，不能重新抽签；如需调整请使用“临时调位”");
            }
            allocationRepository.deleteAll(existing);
            allocationRepository.flush();
        }

        List<Application> approved = applicationRepository
                .findByFairDayAndStatus(day, ApplyStatus.APPROVED);
        if (approved.isEmpty()) {
            throw new ApiException("没有审核通过的报名，无法抽签");
        }

        List<Stall> stalls = stallRepository.findByFairDayAndEnabledTrueOrderByStallNo(day);
        if (stalls.isEmpty()) {
            throw new ApiException("当场集市尚未设置摊位");
        }

        String seed = (seedInput == null || seedInput.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : seedInput.trim();
        long seedHash = hashSeed(fairDayId + "|" + seed);
        Random rng = new Random(seedHash);

        // 1. 计算优先级，抽签顺序按优先级降序；同优先级随机数小者先选
        List<Candidate> candidates = new ArrayList<>();
        for (Application app : approved) {
            Candidate c = new Candidate();
            c.application = app;
            c.vendor = app.getVendor();
            c.priority = app.getHistoryCount() * 3
                    + (c.vendor.getCreditScore() == null ? 0 : c.vendor.getCreditScore() - 100);
            c.randomValue = rng.nextLong();
            c.targetZone = app.getLivePoultry() ? ZoneType.POULTRY
                    : (Boolean.TRUE.equals(app.getCookedFood()) ? ZoneType.COOKED : ZoneType.NORMAL);
            candidates.add(c);
        }
        // 确定顺序：优先级降序、随机值升序（同优先级比随机数）
        candidates.sort(Comparator.comparingInt((Candidate c) -> c.priority).reversed()
                .thenComparing(c -> c.randomValue));

        // 剩余可用摊位
        List<Stall> available = new ArrayList<>(stalls);
        // 区域内已分配序号（用于错峰入场）
        Map<ZoneType, Integer> zoneSeq = new EnumMap<>(ZoneType.class);

        int allocated = 0;
        int waitlist = 0;
        List<Map<String, Object>> traces = new ArrayList<>();

        for (Candidate c : candidates) {
            List<Stall> eligible = new ArrayList<>();
            for (Stall s : available) {
                if (isEligible(s, c)) {
                    eligible.add(s);
                }
            }
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("vendor", c.vendor.getName());
            trace.put("category", c.application.getCategory());
            trace.put("priority", c.priority);
            trace.put("random", c.randomValue);
            trace.put("targetZone", c.targetZone.name());

            Allocation allocation = new Allocation();
            allocation.setFairDay(day);
            allocation.setVendor(c.vendor);
            allocation.setApplication(c.application);
            allocation.setPriorityScore(c.priority);
            allocation.setDrawRandom(c.randomValue);

            if (eligible.isEmpty()) {
                allocation.setStatus(AllocationStatus.WAITLIST);
                allocation.setStallFee(BigDecimal.ZERO);
                allocation.setElectricityFee(BigDecimal.ZERO);
                allocation.setDeposit(BigDecimal.ZERO);
                allocation.setTotalAmount(BigDecimal.ZERO);
                allocation.setProhibitions("候补：暂无符合条件的摊位（"
                        + unmetReason(available, c) + "）");
                allocation.setCheckCode("WAIT-" + fairDayId + "-" + c.vendor.getId());
                waitlist++;
                trace.put("result", "WAITLIST");
                trace.put("reason", allocation.getProhibitions());
            } else {
                // 在符合条件的摊位中再次随机
                Stall chosen = eligible.get(rng.nextInt(eligible.size()));
                available.remove(chosen);
                MarketZone zone = chosen.getZone();

                allocation.setStall(chosen);
                allocation.setStatus(AllocationStatus.ALLOCATED);
                BigDecimal elecFee = Boolean.TRUE.equals(c.application.getNeedElectricity())
                        ? zone.getElectricityFee() : BigDecimal.ZERO;
                allocation.setStallFee(zone.getStallFee());
                allocation.setElectricityFee(elecFee);
                allocation.setDeposit(zone.getDepositStandard());
                allocation.setTotalAmount(zone.getStallFee().add(elecFee).add(zone.getDepositStandard()));
                allocation.setProhibitions(buildProhibitions(c.application, chosen));

                int seq = zoneSeq.merge(c.targetZone, 1, Integer::sum);
                allocation.setEntryTime(staggeredEntry(c.targetZone, seq));
                allocation.setCheckCode("CHK" + fairDayId + "-" + c.vendor.getId() + "-"
                        + Math.abs(seedHash ^ c.vendor.getId()));
                allocated++;
                trace.put("result", chosen.getStallNo());
                trace.put("entryTime", allocation.getEntryTime().toString());
            }
            allocationRepository.save(allocation);
            traces.add(trace);
        }

        day.setStatus(DayStatus.DRAWN);
        day.setDrawSeed(seed);
        day.setDrawnAt(java.time.LocalDateTime.now());
        fairDayRepository.save(day);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("seed", seed);
        result.put("approvedCount", approved.size());
        result.put("allocatedCount", allocated);
        result.put("waitlistCount", waitlist);
        result.put("traces", traces);
        return result;
    }

    private boolean isEligible(Stall stall, Candidate c) {
        Application a = c.application;
        if (stall.getZone().getType() != c.targetZone) {
            return false;
        }
        // 车辆/摊棚尺寸（允许 0.2 米误差）
        BigDecimal pad = new BigDecimal("0.2");
        if (a.getVehicleLength().compareTo(BigDecimal.ZERO) > 0
                && a.getVehicleLength().compareTo(stall.getLength().add(pad)) > 0) {
            return false;
        }
        if (a.getVehicleWidth().compareTo(BigDecimal.ZERO) > 0
                && a.getVehicleWidth().compareTo(stall.getWidth().add(pad)) > 0) {
            return false;
        }
        // 用电
        if (Boolean.TRUE.equals(a.getNeedElectricity()) && !Boolean.TRUE.equals(stall.getHasElectricity())) {
            return false;
        }
        // 消防通道：熟食明火、活禽不得安排在紧邻消防通道的摊位
        if (Boolean.TRUE.equals(stall.getNearFireLane())
                && (Boolean.TRUE.equals(a.getCookedFood()) || Boolean.TRUE.equals(a.getLivePoultry()))) {
            return false;
        }
        return true;
    }

    private String unmetReason(List<Stall> remaining, Candidate c) {
        Application a = c.application;
        List<Stall> zoneStalls = remaining.stream()
                .filter(s -> s.getZone().getType() == c.targetZone).toList();
        if (zoneStalls.isEmpty()) {
            return c.targetZone.getLabel() + "摊位已抽满";
        }
        List<String> reasons = new ArrayList<>();
        boolean sizeOk = zoneStalls.stream().anyMatch(s ->
                a.getVehicleLength().compareTo(s.getLength().add(new BigDecimal("0.2"))) <= 0
                        && a.getVehicleWidth().compareTo(s.getWidth().add(new BigDecimal("0.2"))) <= 0);
        if (!sizeOk) reasons.add("车辆尺寸超出该区域摊位");
        if (Boolean.TRUE.equals(a.getNeedElectricity())
                && zoneStalls.stream().noneMatch(s -> Boolean.TRUE.equals(s.getHasElectricity()))) {
            reasons.add("带电源摊位已满");
        }
        if (reasons.isEmpty()) reasons.add("符合条件摊位已抽满");
        return String.join("、", reasons);
    }

    private String buildProhibitions(Application a, Stall stall) {
        List<String> items = new ArrayList<>();
        items.add("按划定黄线经营，不得越线占道");
        items.add("明码标价，使用检定合格计量器具，不得缺斤少两");
        if (Boolean.TRUE.equals(a.getCookedFood())) {
            items.add("熟食冷链保存，逐批次留样48小时并上传照片");
            items.add("操作人员持健康证上岗，佩戴口罩手套");
        }
        if (Boolean.TRUE.equals(a.getLivePoultry())) {
            items.add("活禽限于隔离区内经营，不得串区，及时消毒并密封处理排泄物");
        }
        if (Boolean.TRUE.equals(stall.getNearFireLane())) {
            items.add("摊位紧邻消防通道，严禁占用通道、严禁明火作业");
        }
        if (Boolean.TRUE.equals(a.getNeedElectricity())) {
            items.add("用电只能接入市场统一电源，禁止私拉乱接，收摊断电");
        }
        return String.join("；", items) + "。";
    }

    private LocalTime staggeredEntry(ZoneType zone, int seq) {
        LocalTime base = switch (zone) {
            case COOKED -> LocalTime.of(5, 30);
            case NORMAL -> LocalTime.of(6, 0);
            case POULTRY -> LocalTime.of(6, 30);
        };
        // 每批次 8 个摊位，递增 10 分钟，最晚 8:00
        int offsetMin = ((seq - 1) / 8) * 10;
        LocalTime t = base.plusMinutes(offsetMin);
        return t.isAfter(LocalTime.of(8, 0)) ? LocalTime.of(8, 0) : t;
    }

    private long hashSeed(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (digest[i] & 0xffL);
            }
            return h;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

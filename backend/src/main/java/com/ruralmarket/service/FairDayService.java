package com.ruralmarket.service;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.FairDayRepository;
import com.ruralmarket.repo.MarketZoneRepository;
import com.ruralmarket.repo.StallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FairDayService {

    private final FairDayRepository fairDayRepository;
    private final MarketZoneRepository zoneRepository;
    private final StallRepository stallRepository;

    public FairDayService(FairDayRepository fairDayRepository, MarketZoneRepository zoneRepository,
                          StallRepository stallRepository) {
        this.fairDayRepository = fairDayRepository;
        this.zoneRepository = zoneRepository;
        this.stallRepository = stallRepository;
    }

    @Transactional
    public FairDay create(FairDay payload) {
        if (payload.getName() == null || payload.getMarketDate() == null) {
            throw new ApiException("集市名称和赶集日期必填");
        }
        if (payload.getStatus() == null) {
            payload.setStatus(DayStatus.OPEN);
        }
        if (payload.getRulesText() == null || payload.getRulesText().isBlank()) {
            payload.setRulesText("""
                    本场抽签规则：1.按品类分区（熟食冷链区/活禽隔离区/普通区）；
                    2.邻近消防通道摊位不安排熟食明火与活禽；3.用电摊主安排带电源摊位；
                    4.车辆尺寸须与摊位匹配；5.老摊主（历史赶集次数）与信用分高者优先抽签；
                    6.抽签使用当场公示随机种子，结果可复算；7.临时调位、减免、扣罚一律台账公示。""");
        }
        return fairDayRepository.save(payload);
    }

    @Transactional
    public MarketZone addZone(Long fairDayId, MarketZone zone) {
        FairDay day = fairDayRepository.findById(fairDayId).orElseThrow(() -> new ApiException("赶集日不存在"));
        zone.setFairDay(day);
        return zoneRepository.save(zone);
    }

    /**
     * 批量生成摊位。
     * body: {zoneId, prefix, count, length, width, electricityCount, fireLaneNos:[...]}
     */
    @Transactional
    public int generateStalls(Long fairDayId, Long zoneId, String prefix, int count,
                              BigDecimal length, BigDecimal width,
                              int electricityCount, List<Integer> fireLaneNos) {
        FairDay day = fairDayRepository.findById(fairDayId).orElseThrow(() -> new ApiException("赶集日不存在"));
        MarketZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ApiException("区域不存在"));
        List<Stall> existing = stallRepository.findByFairDayOrderByStallNo(day);
        int maxSeq = existing.stream()
                .filter(s -> s.getStallNo().startsWith(prefix))
                .mapToInt(s -> {
                    try {
                        return Integer.parseInt(s.getStallNo().substring(prefix.length()));
                    } catch (Exception e) {
                        return 0;
                    }
                }).max().orElse(0);
        int created = 0;
        for (int i = 1; i <= count; i++) {
            int seq = maxSeq + i;
            Stall stall = new Stall();
            stall.setFairDay(day);
            stall.setZone(zone);
            stall.setStallNo(prefix + String.format("%02d", seq));
            stall.setLength(length == null ? new BigDecimal("4.00") : length);
            stall.setWidth(width == null ? new BigDecimal("3.00") : width);
            stall.setHasElectricity(i <= electricityCount);
            stall.setNearFireLane(fireLaneNos != null && fireLaneNos.contains(i));
            stallRepository.save(stall);
            created++;
        }
        return created;
    }
}

package com.ruralmarket.platform.controller;

import com.ruralmarket.platform.entity.MarketEvent;
import com.ruralmarket.platform.entity.StallZone;
import com.ruralmarket.platform.repository.MarketEventRepository;
import com.ruralmarket.platform.repository.StallZoneRepository;
import com.ruralmarket.platform.service.Constants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 公共元数据：品类字典、报名中的集市、区域信息 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {
    private final MarketEventRepository eventRepository;
    private final StallZoneRepository zoneRepository;

    public MetaController(MarketEventRepository eventRepository, StallZoneRepository zoneRepository) {
        this.eventRepository = eventRepository;
        this.zoneRepository = zoneRepository;
    }

    @GetMapping("/categories")
    public Map<String, String> categories() {
        return Constants.CATEGORIES;
    }

    @GetMapping("/events")
    public List<MarketEvent> events() {
        return eventRepository.findAllByOrderByMarketDateDesc();
    }

    @GetMapping("/zones")
    public List<StallZone> zones() {
        return zoneRepository.findAll();
    }
}

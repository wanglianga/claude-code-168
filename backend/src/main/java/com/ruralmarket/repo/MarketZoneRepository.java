package com.ruralmarket.repo;

import com.ruralmarket.domain.MarketZone;
import com.ruralmarket.domain.FairDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketZoneRepository extends JpaRepository<MarketZone, Long> {
    List<MarketZone> findByFairDayOrderByCode(FairDay fairDay);
}

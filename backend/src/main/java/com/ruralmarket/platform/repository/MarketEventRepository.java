package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.MarketEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MarketEventRepository extends JpaRepository<MarketEvent, Long> {
    List<MarketEvent> findAllByOrderByMarketDateDesc();
}

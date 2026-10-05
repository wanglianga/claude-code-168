package com.ruralmarket.repo;

import com.ruralmarket.domain.DayStatus;
import com.ruralmarket.domain.FairDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FairDayRepository extends JpaRepository<FairDay, Long> {
    List<FairDay> findAllByOrderByMarketDateDesc();
    List<FairDay> findByStatus(DayStatus status);
}

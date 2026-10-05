package com.ruralmarket.repo;

import com.ruralmarket.domain.FairDay;
import com.ruralmarket.domain.Stall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StallRepository extends JpaRepository<Stall, Long> {
    List<Stall> findByFairDayAndEnabledTrueOrderByStallNo(FairDay fairDay);
    List<Stall> findByFairDayOrderByStallNo(FairDay fairDay);
}

package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.LotteryRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LotteryRunRepository extends JpaRepository<LotteryRun, Long> {
    List<LotteryRun> findByEventIdOrderByExecutedAtDesc(Long eventId);
}

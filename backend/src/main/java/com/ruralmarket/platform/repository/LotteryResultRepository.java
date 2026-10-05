package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.LotteryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LotteryResultRepository extends JpaRepository<LotteryResult, Long> {
    List<LotteryResult> findByRunId(Long runId);
    List<LotteryResult> findByApplicationEventId(Long eventId);
    Optional<LotteryResult> findByQrToken(String qrToken);
    Optional<LotteryResult> findByApplicationId(Long applicationId);
}

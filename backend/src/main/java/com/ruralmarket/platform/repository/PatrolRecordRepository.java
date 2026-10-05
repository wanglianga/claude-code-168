package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.PatrolRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatrolRecordRepository extends JpaRepository<PatrolRecord, Long> {
    List<PatrolRecord> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<PatrolRecord> findByResultId(Long resultId);
}

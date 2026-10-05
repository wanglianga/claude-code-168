package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.EntryCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EntryCheckRepository extends JpaRepository<EntryCheck, Long> {
    Optional<EntryCheck> findByResultId(Long resultId);
    List<EntryCheck> findByResultApplicationEventId(Long eventId);
}

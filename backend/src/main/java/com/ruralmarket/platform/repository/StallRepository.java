package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.Stall;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StallRepository extends JpaRepository<Stall, Long> {
    List<Stall> findByActiveTrueOrderByStallNo();
}

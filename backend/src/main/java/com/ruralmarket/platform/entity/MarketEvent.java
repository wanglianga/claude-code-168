package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "market_events")
public class MarketEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 赶集日 */
    @Column(nullable = false)
    private LocalDate marketDate;

    /** DRAFT 筹备 / OPEN 报名中 / CLOSED 报名截止 / LOTTERY_DONE 已抽签 / IN_PROGRESS 集市进行中 / FINISHED 已结束 */
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

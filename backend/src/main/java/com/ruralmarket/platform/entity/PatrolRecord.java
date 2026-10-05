package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 巡查记录：卫生、明码标价、缺斤少两、占道、用电安全。
 */
@Data
@Entity
@Table(name = "patrol_records")
public class PatrolRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private MarketEvent event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "result_id", nullable = false)
    private LotteryResult result;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    /** 卫生评分 1-5 */
    @Column(nullable = false)
    private Integer hygieneScore;

    /** 是否明码标价 */
    @Column(nullable = false)
    private Boolean priceMarked = true;

    /** 是否发现缺斤少两 */
    @Column(nullable = false)
    private Boolean shortWeight = false;

    /** 是否占道经营 */
    @Column(nullable = false)
    private Boolean occupyingLane = false;

    /** 用电是否安全 */
    @Column(nullable = false)
    private Boolean electricSafe = true;

    @Column(length = 1000)
    private String photos;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 抽签结果（摊位分配单）：摊位号、入场时间、收费项目、保证金、禁止事项。
 */
@Data
@Entity
@Table(name = "lottery_results")
public class LotteryResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "run_id", nullable = false)
    private LotteryRun run;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stall_id", nullable = false)
    private Stall stall;

    @Column(nullable = false, length = 20)
    private String stallNo;

    /** 入场时间，如 06:30 */
    @Column(nullable = false, length = 10)
    private String entryTime;

    /** 收费项目 JSON：[{item, amount}] */
    @Column(nullable = false, length = 2000)
    private String feeItems;

    /** 保证金（元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deposit = BigDecimal.ZERO;

    /** 禁止事项 JSON 数组 */
    @Column(nullable = false, length = 2000)
    private String prohibitedItems;

    /** 入场核验二维码令牌 */
    @Column(nullable = false, unique = true, length = 64)
    private String qrToken;

    /** ASSIGNED 已分配 / RELOCATED 已调位 / CANCELLED 已取消 */
    @Column(nullable = false, length = 20)
    private String status = "ASSIGNED";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

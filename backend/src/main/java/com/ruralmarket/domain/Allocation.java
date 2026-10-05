package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 抽签结果：摊位号、入场时间、收费项目、保证金、禁止事项
 */
@Getter
@Setter
@Entity
@Table(name = "allocation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fair_day_id", "vendor_id"}))
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fair_day_id")
    private FairDay fairDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    private SysUser vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id")
    private Stall stall;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AllocationStatus status = AllocationStatus.ALLOCATED;

    /** 抽签优先级（越大越优先）：历史次数权重 + 信用权重 */
    private Integer priorityScore = 0;

    /** 抽签随机数（种子可复现），公示用 */
    private Long drawRandom;

    /** 入场时间（分时段错峰） */
    private LocalTime entryTime;

    /** 摊位费 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stallFee = BigDecimal.ZERO;

    /** 用电附加费 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityFee = BigDecimal.ZERO;

    /** 保证金 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deposit = BigDecimal.ZERO;

    /** 合计应收（初始） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 减免金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal waivedAmount = BigDecimal.ZERO;

    /** 已缴金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** 保证金扣罚金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deductedAmount = BigDecimal.ZERO;

    /** 保证金已退金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeeStatus feeStatus = FeeStatus.UNPAID;

    /** 禁止事项：据摊位与品类生成，如“禁明火、禁占用消防通道” */
    @Column(length = 500)
    private String prohibitions;

    /** 核销码：二维码内容，入场扫码核验 */
    @Column(length = 40, unique = true)
    private String checkCode;

    private LocalDateTime createdAt = LocalDateTime.now();
}

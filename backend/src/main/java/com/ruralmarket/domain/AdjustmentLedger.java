package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调整台账：临时调位、费用减免、保证金扣罚/退还、信用调整。
 * 每一条都必须写明来源（投诉/巡查/天气/摊主申请等）与理由，
 * 任何人都不能在不留痕的情况下改变抽签结果或费用。
 */
@Getter
@Setter
@Entity
@Table(name = "adjustment_ledger")
public class AdjustmentLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fair_day_id")
    private FairDay fairDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private SysUser vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustType type;

    /** 调整前 */
    @Column(length = 100)
    private String beforeValue;

    /** 调整后 */
    @Column(length = 100)
    private String afterValue;

    /** 金额（减免/扣罚/退还时使用） */
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    /** 信用分变化（正/负） */
    private Integer creditDelta;

    /** 来源单据类型：投诉/巡查/天气调整/摊主申请/抽签规则等 */
    @Column(nullable = false, length = 30)
    private String sourceType;

    /** 来源单据 id */
    private Long sourceId;

    /** 理由说明（必填，公示口径） */
    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private SysUser operator;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

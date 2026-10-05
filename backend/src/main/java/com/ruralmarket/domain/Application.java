package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 赶集报名：经营品类、车辆尺寸、用电需求、是否熟食、证照照片、历史经营记录
 */
@Getter
@Setter
@Entity
@Table(name = "application",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fair_day_id", "vendor_id"}))
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fair_day_id")
    private FairDay fairDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    private SysUser vendor;

    /** 经营品类，如 卤味/蔬菜/服装/活禽/糕点 */
    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, length = 200)
    private String goodsDesc;

    /** 是否售卖熟食/凉菜等需冷链与留样的食品 */
    @Column(nullable = false)
    private Boolean cookedFood = false;

    /** 是否售卖活禽 */
    @Column(nullable = false)
    private Boolean livePoultry = false;

    /** 车辆/摊棚尺寸 长(米) */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal vehicleLength = BigDecimal.ZERO;

    /** 宽(米) */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal vehicleWidth = BigDecimal.ZERO;

    /** 是否需要用电 */
    @Column(nullable = false)
    private Boolean needElectricity = false;

    /** 用电功率（瓦） */
    private Integer powerWatt = 0;

    /** 证照照片（URL 列表，逗号分隔）：营业执照/健康证/小餐饮备案 */
    @Column(length = 1000)
    private String licensePhotos;

    /** 历史经营记录摘要：赶集次数、曾获奖惩等 */
    @Column(length = 1000)
    private String historyRecord;

    /** 历史赶集次数（审核时确认，作为老摊主优先依据） */
    @Column(nullable = false)
    private Integer historyCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApplyStatus status = ApplyStatus.SUBMITTED;

    @Column(length = 500)
    private String rejectReason;

    private LocalDateTime submittedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private SysUser reviewer;

    /** 审核备注：对老摊主身份、活禽资质等的核实说明 */
    @Column(length = 500)
    private String reviewNote;
}

package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "market_zone",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fair_day_id", "code"}))
public class MarketZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fair_day_id")
    private FairDay fairDay;

    /** 区域编码 A/B/C */
    @Column(nullable = false, length = 8)
    private String code;

    @Column(nullable = false, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ZoneType type;

    /** 单个摊位费（元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stallFee = BigDecimal.ZERO;

    /** 用电附加费（元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityFee = BigDecimal.ZERO;

    /** 保证金标准（元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal depositStandard = BigDecimal.ZERO;

    @Column(length = 200)
    private String note;
}

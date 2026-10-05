package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "stall",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fair_day_id", "stall_no"}))
public class Stall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fair_day_id")
    private FairDay fairDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private MarketZone zone;

    /** 摊位号，例如 A01 */
    @Column(name = "stall_no", nullable = false, length = 12)
    private String stallNo;

    /** 摊位尺寸（米），用于与车辆尺寸匹配 */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal length = new BigDecimal("4.00");

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal width = new BigDecimal("3.00");

    /** 是否可接电 */
    @Column(nullable = false)
    private Boolean hasElectricity = false;

    /** 是否紧邻消防通道（禁明火熟食、活禽、超大车） */
    @Column(nullable = false)
    private Boolean nearFireLane = false;

    /** 是否停用（积水/维修等） */
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 200)
    private String note;
}

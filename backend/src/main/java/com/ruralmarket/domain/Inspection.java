package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 市场巡查记录：卫生、明码标价、缺斤少两、占道、用电安全
 */
@Getter
@Setter
@Entity
@Table(name = "inspection")
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fair_day_id")
    private FairDay fairDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    private SysUser vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id")
    private SysUser staff;

    /** 卫生 0-5 分 */
    @Column(nullable = false)
    private Integer hygieneScore = 5;

    /** 明码标价 0-5 分 */
    @Column(nullable = false)
    private Integer priceTagScore = 5;

    /** 计量合规（缺斤少两）0-5 分 */
    @Column(nullable = false)
    private Integer measureScore = 5;

    /** 不占道 0-5 分 */
    @Column(nullable = false)
    private Integer aisleScore = 5;

    /** 用电安全 0-5 分；不涉电记满分 */
    @Column(nullable = false)
    private Integer electricScore = 5;

    /** 发现问题（标签逗号分隔） */
    @Column(length = 300)
    private String issues;

    /** 现场照片 */
    @Column(length = 1000)
    private String photos;

    @Column(length = 500)
    private String remark;

    /** 责令当场整改 */
    @Column(nullable = false)
    private Boolean requireRectify = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 食品留样：卤味、糕点、凉菜等；照片、批次、保存位置
 */
@Getter
@Setter
@Entity
@Table(name = "food_sample")
public class FoodSample {

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
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    /** 留样食品名称 */
    @Column(nullable = false, length = 60)
    private String foodName;

    /** 批次号 */
    @Column(nullable = false, length = 40)
    private String batchNo;

    /** 保存位置，如 市场留样柜B-2 */
    @Column(nullable = false, length = 100)
    private String storageLocation;

    /** 留样时间 */
    @Column(nullable = false)
    private LocalDateTime sampledAt;

    /** 留样照片 */
    @Column(length = 1000)
    private String photos;

    /** 冷藏温度 ℃ */
    private Double storageTemp;

    @Column(length = 300)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

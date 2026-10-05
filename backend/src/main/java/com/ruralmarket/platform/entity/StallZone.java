package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "stall_zones")
public class StallZone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 区域代码，如 A/B/C */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    /** 允许的经营品类，逗号分隔；* 表示不限（熟食/活禽仍受下面开关约束） */
    @Column(nullable = false, length = 500)
    private String allowedCategories = "*";

    /** 是否允许熟食（冷链区） */
    @Column(nullable = false)
    private Boolean allowsCookedFood = false;

    /** 是否允许活禽（隔离区） */
    @Column(nullable = false)
    private Boolean allowsLivePoultry = false;

    /** 基础摊位费（元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee = BigDecimal.ZERO;

    /** 入场时间偏移分钟数（相对 06:00，用于错峰入场） */
    @Column(nullable = false)
    private Integer entryOffsetMinutes = 0;

    @Column(length = 200)
    private String remark;
}

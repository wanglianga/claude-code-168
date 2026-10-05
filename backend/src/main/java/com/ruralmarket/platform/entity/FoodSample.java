package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 食品留样记录：卤味、糕点、凉菜等食品摊主上传。
 */
@Data
@Entity
@Table(name = "food_samples")
public class FoodSample {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private MarketEvent event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "result_id")
    private LotteryResult result;

    /** 食品名称 */
    @Column(nullable = false, length = 100)
    private String productName;

    /** 批次 */
    @Column(nullable = false, length = 50)
    private String batchNo;

    /** 留样照片 */
    @Column(nullable = false, length = 1000)
    private String photo;

    /** 保存位置（如：1号冷藏柜） */
    @Column(nullable = false, length = 200)
    private String storageLocation;

    /** KEPT 已留样 / DISPOSED 已处置 / ABNORMAL 异常 */
    @Column(nullable = false, length = 20)
    private String status = "KEPT";

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime retainedAt = LocalDateTime.now();
}

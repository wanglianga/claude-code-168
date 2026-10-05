package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 信用变动记录：每次扣分/加分都有原因与来源单据。
 */
@Data
@Entity
@Table(name = "credit_logs")
public class CreditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    /** 变动分值（负为扣分） */
    @Column(nullable = false)
    private Integer delta;

    @Column(nullable = false)
    private Integer scoreAfter;

    @Column(nullable = false, length = 500)
    private String reason;

    /** 来源类型：PATROL 巡查 / COMPLAINT 投诉 / ADJUSTMENT 人工调整 / SYSTEM 系统 */
    @Column(nullable = false, length = 20)
    private String sourceType;

    private Long sourceRefId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id")
    private User operator;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

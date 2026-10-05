package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 全局审计日志：抽签、调整、投诉处理、缴费变更等敏感操作全部留痕。
 */
@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String actorName;

    private Long actorId;

    /** 操作类型，如 LOTTERY_RUN / ADJUSTMENT / COMPLAINT_HANDLE / PAYMENT_CHANGE */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 2000)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

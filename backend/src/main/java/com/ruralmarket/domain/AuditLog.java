package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 操作审计日志：登录、抽签、审核、收费等关键动作 */
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
    private String username;

    @Column(length = 16)
    private String role;

    @Column(nullable = false, length = 200)
    private String action;

    @Column(length = 500)
    private String detail;

    @Column(length = 60)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

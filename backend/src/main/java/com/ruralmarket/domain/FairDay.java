package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "fair_day")
public class FairDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 赶集日名称，例如“2026-10-08 重阳大集” */
    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private LocalDate marketDate;

    /** 报名截止时间 */
    private LocalDateTime signupDeadline;

    /** 计划抽签时间 */
    private LocalTime drawTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DayStatus status = DayStatus.PLANNED;

    /** 抽签随机种子；同一种子结果可复现，保证公开可核 */
    @Column(length = 64)
    private String drawSeed;

    private LocalDateTime drawnAt;

    /** 当场集市公示的规则说明 */
    @Column(length = 1000)
    private String rulesText;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 一次抽签执行的完整留痕：随机种子 + 规则快照 + 输入快照，
 * 任何人可凭此复核抽签结果，保证公开公平、可追溯。
 */
@Data
@Entity
@Table(name = "lottery_runs")
public class LotteryRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private MarketEvent event;

    /** 随机种子（抽签前由管理员公布/录入，抽签后任何人可复核） */
    @Column(nullable = false)
    private Long seed;

    /** 抽签规则快照（JSON） */
    @Column(nullable = false, length = 4000)
    private String ruleSnapshot;

    /** 输入快照（JSON）：参与抽签的报名ID按优先级排序后的列表 + 可用摊位ID列表 */
    @Column(nullable = false, length = 8000)
    private String inputSnapshot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    @Column(nullable = false)
    private LocalDateTime executedAt = LocalDateTime.now();

    @Column(length = 500)
    private String remark;
}

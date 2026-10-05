package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 调整记录（可追溯核心）：临时调位、费用减免、保证金扣罚、信用调整、品类变更。
 * 每条记录必须填写原因与来源，向摊主公开，避免"规则被人为改变"的质疑。
 */
@Data
@Entity
@Table(name = "adjustments")
public class Adjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private MarketEvent event;

    /** 涉及的摊位分配单 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "result_id")
    private LotteryResult result;

    /** 涉及的摊主 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    /** RELOCATE 临时调位 / RAIN_RELOCATE 雨天调位 / FEE_WAIVER 费用减免 /
     *  DEPOSIT_PENALTY 保证金扣罚 / CREDIT_ADJUST 信用调整 / CATEGORY_CHANGE 品类变更 */
    @Column(nullable = false, length = 30)
    private String type;

    /** 调整前内容（如 原摊位号/原金额/原信用分/原品类） */
    @Column(length = 1000)
    private String beforeValue;

    /** 调整后内容 */
    @Column(length = 1000)
    private String afterValue;

    /** 调整原因（必填，向摊主公开） */
    @Column(nullable = false, length = 1000)
    private String reason;

    /** 来源依据：RAIN 雨天 / COMPLAINT 投诉单号 / PATROL 巡查记录 / DISPUTE 争议处理 /
     *  REQUEST 摊主申请 / POLICY 政策减免 / OTHER 其他 */
    @Column(nullable = false, length = 30)
    private String source;

    /** 来源单据ID（投诉ID/巡查ID等，可空） */
    private Long sourceRefId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

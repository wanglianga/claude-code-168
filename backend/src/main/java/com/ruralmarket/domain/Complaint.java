package com.ruralmarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消费者投诉：如腹泻、缺斤少两、价格纠纷等。
 * 投诉可触发保证金扣罚、信用调整，均走调整台账留痕。
 */
@Getter
@Setter
@Entity
@Table(name = "complaint")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fair_day_id")
    private FairDay fairDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private SysUser vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    /** 投诉类型：腹泻疑似食物中毒/缺斤少两/价格纠纷/卫生/服务态度/其他 */
    @Column(nullable = false, length = 40)
    private String type;

    /** 投诉人（消费者） */
    @Column(nullable = false, length = 40)
    private String complainant;

    @Column(length = 20)
    private String contact;

    @Column(nullable = false, length = 500)
    private String content;

    /** 证据照片 */
    @Column(length = 1000)
    private String evidencePhotos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplaintStatus status = ComplaintStatus.RECEIVED;

    @Column(length = 500)
    private String handleResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id")
    private SysUser handler;

    private LocalDateTime handledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

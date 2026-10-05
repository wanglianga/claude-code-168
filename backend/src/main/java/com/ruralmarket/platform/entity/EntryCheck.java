package com.ruralmarket.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 入场核验记录：工作人员扫码核验并拍摄摊位布置。
 */
@Data
@Entity
@Table(name = "entry_checks")
public class EntryCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "result_id", nullable = false)
    private LotteryResult result;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    /** 摊位布置照片，逗号分隔 */
    @Column(length = 2000)
    private String photos;

    /** 实际经营品类（核验时确认，用于发现临时换品类） */
    @Column(length = 30)
    private String actualCategory;

    /** 是否按抽签位置摆摊 */
    @Column(nullable = false)
    private Boolean atAssignedStall = true;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime checkedAt = LocalDateTime.now();
}

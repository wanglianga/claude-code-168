package com.ruralmarket.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /** VENDOR 摊主 / STAFF 市场工作人员 / ADMIN 乡镇管理者 */
    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    /** 信用分，满分 100，低于 60 取消老摊主资格 */
    @Column(nullable = false)
    private Integer creditScore = 100;

    /** 历史经营次数（老摊主认定依据，>=3 次为老摊主） */
    @Column(nullable = false)
    private Integer historyCount = 0;

    /** 历史经营记录说明 */
    @Column(length = 1000)
    private String historyNotes;

    @Column(length = 500)
    private String licensePhoto;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

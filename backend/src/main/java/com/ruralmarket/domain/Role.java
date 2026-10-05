package com.ruralmarket.domain;

public enum Role {
    VENDOR("摊主"),
    STAFF("市场管理人员"),
    ADMIN("乡镇管理者");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}

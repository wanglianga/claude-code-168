package com.ruralmarket.domain;

public enum FeeStatus {
    UNPAID("未缴"),
    PAID("已缴"),
    WAIVED("已减免");

    private final String label;

    FeeStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}

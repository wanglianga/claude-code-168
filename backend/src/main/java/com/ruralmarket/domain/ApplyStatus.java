package com.ruralmarket.domain;

public enum ApplyStatus {
    SUBMITTED("已提交待审核"),
    APPROVED("审核通过"),
    REJECTED("审核不通过");

    private final String label;

    ApplyStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}

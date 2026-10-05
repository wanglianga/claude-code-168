package com.ruralmarket.domain;

public enum AllocationStatus {
    ALLOCATED("已中签待入场"),
    CHECKED_IN("已入场"),
    ADJUSTED("已临时调位"),
    NO_SHOW("未到场"),
    CANCELLED("已取消"),
    WAITLIST("候补未中签");

    private final String label;

    AllocationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}

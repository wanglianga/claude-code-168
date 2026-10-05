package com.ruralmarket.domain;

public enum DayStatus {
    PLANNED("筹备中"),
    OPEN("报名中"),
    DRAWN("已抽签"),
    ONGOING("赶集进行中"),
    CLOSED("已结束");

    private final String label;

    DayStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}

package com.ruralmarket.domain;

public enum ComplaintStatus {
    RECEIVED("已受理"),
    PROCESSING("处理中"),
    RESOLVED("已办结"),
    DISMISSED("驳回");

    private final String label;

    ComplaintStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}

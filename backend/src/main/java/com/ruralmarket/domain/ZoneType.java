package com.ruralmarket.domain;

public enum ZoneType {
    NORMAL("普通干货区"),
    COOKED("熟食冷链区"),
    POULTRY("活禽隔离区");

    private final String label;

    ZoneType(String label) { this.label = label; }

    public String getLabel() { return label; }
}

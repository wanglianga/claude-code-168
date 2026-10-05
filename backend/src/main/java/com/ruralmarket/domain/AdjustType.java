package com.ruralmarket.domain;

/**
 * 调整台账类型：现场每一次灵活处置都必须落账并解释来源，
 * 是“公开公平”与“现场灵活”之间可追溯的关键。
 */
public enum AdjustType {
    STALL_MOVE("临时调位"),
    FEE_WAIVER("费用减免"),
    DEPOSIT_DEDUCT("保证金扣罚"),
    DEPOSIT_REFUND("保证金退还"),
    CREDIT_ADJUST("信用分调整");

    private final String label;

    AdjustType(String label) { this.label = label; }

    public String getLabel() { return label; }
}

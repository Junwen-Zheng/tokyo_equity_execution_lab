package com.junwenzheng.execution.order;

public enum ChildOrderStatus {
    NEW,
    ACKNOWLEDGED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED
}

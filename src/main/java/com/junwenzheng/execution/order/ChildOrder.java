package com.junwenzheng.execution.order;

public record ChildOrder(
        String parentOrderId,
        String symbol,
        Side side,
        int quantity,
        long timestampMs,
        String reason
) {
    public ChildOrder {
        if (quantity <= 0) throw new IllegalArgumentException("child quantity must be positive");
    }
}

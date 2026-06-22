package com.junwenzheng.execution.order;

public record Fill(
        String parentOrderId,
        String symbol,
        Side side,
        int quantity,
        double price,
        long timestampMs,
        String strategy,
        String reason
) {
    public double notional() {
        return quantity * price;
    }
}

package com.junwenzheng.execution.order;

public record Fill(
        String childOrderId,
        String parentOrderId,
        String symbol,
        Side side,
        int quantity,
        double price,
        long timestampMs,
        String strategy,
        String reason
) {
    public Fill {
        if (
                childOrderId == null
                        || childOrderId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "childOrderId is required"
            );
        }

        if (
                parentOrderId == null
                        || parentOrderId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "parentOrderId is required"
            );
        }

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "symbol is required"
            );
        }

        if (side == null) {
            throw new IllegalArgumentException(
                    "side is required"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "fill quantity must be positive"
            );
        }

        if (
                !Double.isFinite(price)
                        || price <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "fill price must be finite and positive"
            );
        }

        if (timestampMs < 0) {
            throw new IllegalArgumentException(
                    "timestampMs must be non-negative"
            );
        }

        if (
                strategy == null
                        || strategy.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "strategy is required"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "reason is required"
            );
        }

        childOrderId = childOrderId.trim();
        parentOrderId = parentOrderId.trim();
        symbol = symbol.trim();
        strategy = strategy.trim();
        reason = reason.trim();
    }

    public double notional() {
        return quantity * price;
    }
}

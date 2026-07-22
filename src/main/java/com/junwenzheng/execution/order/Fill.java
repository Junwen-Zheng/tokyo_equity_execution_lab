package com.junwenzheng.execution.order;

public record Fill(
        String childOrderId,
        String parentOrderId,
        String symbol,
        Side side,
        int quantity,
        double price,
        double referenceMidPrice,
        double spreadCostBps,
        double impactCostBps,
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

        if (
                !Double.isFinite(referenceMidPrice)
                        || referenceMidPrice <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "reference mid price must be "
                            + "finite and positive"
            );
        }

        if (
                !Double.isFinite(spreadCostBps)
                        || spreadCostBps < 0.0
        ) {
            throw new IllegalArgumentException(
                    "spread cost must be finite "
                            + "and non-negative"
            );
        }

        if (
                !Double.isFinite(impactCostBps)
                        || impactCostBps < 0.0
        ) {
            throw new IllegalArgumentException(
                    "impact cost must be finite "
                            + "and non-negative"
            );
        }

        if (timestampMs < 0L) {
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

    public double totalCostBps() {
        return spreadCostBps + impactCostBps;
    }

    public double notional() {
        return quantity * price;
    }
}

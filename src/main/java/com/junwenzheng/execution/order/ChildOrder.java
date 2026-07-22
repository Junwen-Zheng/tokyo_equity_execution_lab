package com.junwenzheng.execution.order;

import java.util.UUID;

public final class ChildOrder {
    private final String childOrderId;
    private final String parentOrderId;
    private final String symbol;
    private final Side side;
    private final int quantity;
    private final long timestampMs;
    private final String reason;

    private int filledQuantity;
    private long lastUpdateTimestampMs;
    private ChildOrderStatus status;

    public ChildOrder(
            String parentOrderId,
            String symbol,
            Side side,
            int quantity,
            long timestampMs,
            String reason
    ) {
        this(
                UUID.randomUUID().toString(),
                parentOrderId,
                symbol,
                side,
                quantity,
                timestampMs,
                reason
        );
    }

    public ChildOrder(
            String childOrderId,
            String parentOrderId,
            String symbol,
            Side side,
            int quantity,
            long timestampMs,
            String reason
    ) {
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
                    "child quantity must be positive"
            );
        }

        if (timestampMs < 0) {
            throw new IllegalArgumentException(
                    "timestampMs must be non-negative"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "reason is required"
            );
        }

        this.childOrderId = childOrderId.trim();
        this.parentOrderId = parentOrderId.trim();
        this.symbol = symbol.trim();
        this.side = side;
        this.quantity = quantity;
        this.timestampMs = timestampMs;
        this.reason = reason.trim();
        this.lastUpdateTimestampMs = timestampMs;
        this.status = ChildOrderStatus.NEW;
    }

    public String childOrderId() {
        return childOrderId;
    }

    public String parentOrderId() {
        return parentOrderId;
    }

    public String symbol() {
        return symbol;
    }

    public Side side() {
        return side;
    }

    public int quantity() {
        return quantity;
    }

    public long timestampMs() {
        return timestampMs;
    }

    public String reason() {
        return reason;
    }

    public int filledQuantity() {
        return filledQuantity;
    }

    public int remainingQuantity() {
        return quantity - filledQuantity;
    }

    public long lastUpdateTimestampMs() {
        return lastUpdateTimestampMs;
    }

    public ChildOrderStatus status() {
        return status;
    }

    public boolean isTerminal() {
        return status == ChildOrderStatus.FILLED
                || status == ChildOrderStatus.CANCELLED
                || status == ChildOrderStatus.REJECTED;
    }

    public void acknowledge(long updateTimestampMs) {
        requireStatus(
                ChildOrderStatus.NEW,
                "acknowledge"
        );

        advanceTimestamp(updateTimestampMs);
        status = ChildOrderStatus.ACKNOWLEDGED;
    }

    public void applyFill(
            int fillQuantity,
            long updateTimestampMs
    ) {
        if (fillQuantity <= 0) {
            throw new IllegalArgumentException(
                    "fill quantity must be positive"
            );
        }

        if (
                status != ChildOrderStatus.ACKNOWLEDGED
                        && status
                        != ChildOrderStatus.PARTIALLY_FILLED
        ) {
            throw new IllegalStateException(
                    "cannot apply fill while child is "
                            + status
            );
        }

        if (fillQuantity > remainingQuantity()) {
            throw new IllegalArgumentException(
                    "fill quantity exceeds child remaining quantity"
            );
        }

        advanceTimestamp(updateTimestampMs);
        filledQuantity += fillQuantity;

        status = remainingQuantity() == 0
                ? ChildOrderStatus.FILLED
                : ChildOrderStatus.PARTIALLY_FILLED;
    }

    public void cancel(long updateTimestampMs) {
        if (
                status != ChildOrderStatus.ACKNOWLEDGED
                        && status
                        != ChildOrderStatus.PARTIALLY_FILLED
        ) {
            throw new IllegalStateException(
                    "cannot cancel child while it is "
                            + status
            );
        }

        advanceTimestamp(updateTimestampMs);
        status = ChildOrderStatus.CANCELLED;
    }

    public void reject(long updateTimestampMs) {
        requireStatus(
                ChildOrderStatus.NEW,
                "reject"
        );

        advanceTimestamp(updateTimestampMs);
        status = ChildOrderStatus.REJECTED;
    }

    private void advanceTimestamp(
            long updateTimestampMs
    ) {
        if (updateTimestampMs < lastUpdateTimestampMs) {
            throw new IllegalArgumentException(
                    "lifecycle timestamp cannot move backwards"
            );
        }

        lastUpdateTimestampMs = updateTimestampMs;
    }

    private void requireStatus(
            ChildOrderStatus expected,
            String operation
    ) {
        if (status != expected) {
            throw new IllegalStateException(
                    "cannot "
                            + operation
                            + " child while it is "
                            + status
            );
        }
    }
}

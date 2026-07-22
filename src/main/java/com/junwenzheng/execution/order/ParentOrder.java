package com.junwenzheng.execution.order;

import java.util.UUID;

public final class ParentOrder {
    private final String orderId;
    private final String symbol;
    private final Side side;
    private final int quantity;
    private final double arrivalPrice;

    private int filledQuantity;
    private OrderStatus status;

    public ParentOrder(
            String symbol,
            Side side,
            int quantity,
            double arrivalPrice
    ) {
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
                    "quantity must be positive"
            );
        }

        if (
                !Double.isFinite(arrivalPrice)
                        || arrivalPrice <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "arrival price must be finite and positive"
            );
        }

        this.orderId = UUID.randomUUID().toString();
        this.symbol = symbol.trim();
        this.side = side;
        this.quantity = quantity;
        this.arrivalPrice = arrivalPrice;
        this.status = OrderStatus.NEW;
    }

    public String orderId() {
        return orderId;
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

    public double arrivalPrice() {
        return arrivalPrice;
    }

    public int filledQuantity() {
        return filledQuantity;
    }

    public int remainingQuantity() {
        return quantity - filledQuantity;
    }

    public OrderStatus status() {
        return status;
    }

    public boolean isTerminal() {
        return status == OrderStatus.FILLED
                || status == OrderStatus.CANCELLED
                || status == OrderStatus.REJECTED;
    }

    public void markWorking() {
        requireStatus(
                OrderStatus.NEW,
                "mark working"
        );

        status = OrderStatus.WORKING;
    }

    public void applyFill(int fillQuantity) {
        if (fillQuantity <= 0) {
            throw new IllegalArgumentException(
                    "fill quantity must be positive"
            );
        }

        if (
                status != OrderStatus.WORKING
                        && status
                        != OrderStatus.PARTIALLY_FILLED
        ) {
            throw new IllegalStateException(
                    "cannot apply fill while parent is "
                            + status
            );
        }

        if (fillQuantity > remainingQuantity()) {
            throw new IllegalArgumentException(
                    "fill quantity exceeds parent remaining quantity"
            );
        }

        filledQuantity += fillQuantity;

        status = remainingQuantity() == 0
                ? OrderStatus.FILLED
                : OrderStatus.PARTIALLY_FILLED;
    }

    public void cancel() {
        if (
                status != OrderStatus.NEW
                        && status != OrderStatus.WORKING
                        && status
                        != OrderStatus.PARTIALLY_FILLED
        ) {
            throw new IllegalStateException(
                    "cannot cancel parent while it is "
                            + status
            );
        }

        status = OrderStatus.CANCELLED;
    }

    public void reject() {
        requireStatus(
                OrderStatus.NEW,
                "reject"
        );

        status = OrderStatus.REJECTED;
    }

    private void requireStatus(
            OrderStatus expected,
            String operation
    ) {
        if (status != expected) {
            throw new IllegalStateException(
                    "cannot "
                            + operation
                            + " parent while it is "
                            + status
            );
        }
    }
}

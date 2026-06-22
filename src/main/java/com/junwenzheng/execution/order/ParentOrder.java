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

    public ParentOrder(String symbol, Side side, int quantity, double arrivalPrice) {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (arrivalPrice <= 0) throw new IllegalArgumentException("arrival price must be positive");
        this.orderId = UUID.randomUUID().toString();
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.arrivalPrice = arrivalPrice;
        this.status = OrderStatus.NEW;
    }

    public String orderId() { return orderId; }
    public String symbol() { return symbol; }
    public Side side() { return side; }
    public int quantity() { return quantity; }
    public double arrivalPrice() { return arrivalPrice; }
    public int filledQuantity() { return filledQuantity; }
    public int remainingQuantity() { return Math.max(0, quantity - filledQuantity); }
    public OrderStatus status() { return status; }

    public void markWorking() {
        if (status == OrderStatus.NEW) status = OrderStatus.WORKING;
    }

    public void applyFill(int quantity) {
        if (quantity <= 0) return;
        filledQuantity = Math.min(this.quantity, filledQuantity + quantity);
        status = filledQuantity >= this.quantity ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }
}

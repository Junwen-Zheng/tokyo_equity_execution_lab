package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.order.ChildOrder;

public final class RiskManager {
    private final int maxChildQty;
    private final double maxNotional;

    public RiskManager(int maxChildQty, double maxNotional) {
        this.maxChildQty = maxChildQty;
        this.maxNotional = maxNotional;
    }

    public boolean isAllowed(ChildOrder order, double referencePrice) {
        if (order.quantity() <= 0) return false;
        if (order.quantity() > maxChildQty) return false;
        return order.quantity() * referencePrice <= maxNotional;
    }
}

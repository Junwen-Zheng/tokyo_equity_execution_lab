package com.junwenzheng.execution.algo;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;

public final class VwapAlgorithm implements ExecutionAlgorithm {
    private final int maxSliceQty;

    public VwapAlgorithm(int maxSliceQty) {
        this.maxSliceQty = maxSliceQty;
    }

    @Override
    public String name() {
        return "VWAP";
    }

    @Override
    public ExecutionDecision onEvent(ParentOrder parentOrder, MarketEvent event, ReplayProgress progress) {
        int targetCumulative = (int) Math.ceil(parentOrder.quantity() * progress.volumeFraction());
        int desired = targetCumulative - parentOrder.filledQuantity();
        int childQty = Math.min(parentOrder.remainingQuantity(), Math.min(maxSliceQty, Math.max(0, desired)));
        if (childQty <= 0) return ExecutionDecision.none("already at volume-curve target");
        return new ExecutionDecision(childQty, "volume-curve catch-up");
    }
}

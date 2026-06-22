package com.junwenzheng.execution.algo;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;

public final class TwapAlgorithm implements ExecutionAlgorithm {
    private final int minSliceQty;

    public TwapAlgorithm(int minSliceQty) {
        this.minSliceQty = minSliceQty;
    }

    @Override
    public String name() {
        return "TWAP";
    }

    @Override
    public ExecutionDecision onEvent(ParentOrder parentOrder, MarketEvent event, ReplayProgress progress) {
        int targetCumulative = (int) Math.ceil(parentOrder.quantity() * progress.progressFraction());
        int desired = targetCumulative - parentOrder.filledQuantity();
        int childQty = Math.min(parentOrder.remainingQuantity(), Math.max(0, desired));
        if (childQty <= 0) return ExecutionDecision.none("already at time-schedule target");
        return new ExecutionDecision(Math.max(Math.min(childQty, minSliceQty), Math.min(childQty, minSliceQty)), "time-schedule catch-up");
    }
}

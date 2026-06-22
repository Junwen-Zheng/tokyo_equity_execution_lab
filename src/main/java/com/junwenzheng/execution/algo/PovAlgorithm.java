package com.junwenzheng.execution.algo;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;

public final class PovAlgorithm implements ExecutionAlgorithm {
    private final double participationRate;
    private final int maxSliceQty;

    public PovAlgorithm(double participationRate, int maxSliceQty) {
        if (participationRate <= 0 || participationRate > 1) throw new IllegalArgumentException("participationRate must be in (0, 1]");
        this.participationRate = participationRate;
        this.maxSliceQty = maxSliceQty;
    }

    @Override
    public String name() {
        return "POV";
    }

    @Override
    public ExecutionDecision onEvent(ParentOrder parentOrder, MarketEvent event, ReplayProgress progress) {
        int childQty = (int) Math.floor(event.volume() * participationRate);
        childQty = Math.min(parentOrder.remainingQuantity(), Math.min(maxSliceQty, childQty));
        if (childQty <= 0) return ExecutionDecision.none("no executable participation quantity");
        return new ExecutionDecision(childQty, "participation of event volume");
    }
}

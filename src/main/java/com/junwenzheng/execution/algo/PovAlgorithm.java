package com.junwenzheng.execution.algo;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;

public final class PovAlgorithm
        implements ExecutionAlgorithm {
    private final double participationRate;
    private final int maxSliceQty;

    public PovAlgorithm(
            double participationRate,
            int maxSliceQty
    ) {
        if (
                !Double.isFinite(participationRate)
                        || participationRate <= 0.0
                        || participationRate > 1.0
        ) {
            throw new IllegalArgumentException(
                    "participationRate must be in (0, 1]"
            );
        }

        if (maxSliceQty <= 0) {
            throw new IllegalArgumentException(
                    "maxSliceQty must be positive"
            );
        }

        this.participationRate = participationRate;
        this.maxSliceQty = maxSliceQty;
    }

    @Override
    public String name() {
        return "POV";
    }

    @Override
    public ExecutionDecision onEvent(
            ParentOrder parentOrder,
            MarketEvent event,
            ReplayProgress progress
    ) {
        long uncappedTarget =
                (long) Math.floor(
                        progress.cumulativeVolume()
                                * participationRate
                );

        int targetCumulative =
                (int) Math.min(
                        parentOrder.quantity(),
                        uncappedTarget
                );

        int participationDeficit =
                targetCumulative
                        - parentOrder.filledQuantity();

        if (participationDeficit <= 0) {
            return ExecutionDecision.none(
                    "already at participation target"
            );
        }

        int childQuantity =
                Math.min(
                        parentOrder.remainingQuantity(),
                        Math.min(
                                maxSliceQty,
                                participationDeficit
                        )
                );

        return new ExecutionDecision(
                childQuantity,
                "cumulative participation catch-up"
        );
    }
}

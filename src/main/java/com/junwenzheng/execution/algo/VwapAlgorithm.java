package com.junwenzheng.execution.algo;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;

public final class VwapAlgorithm
        implements ExecutionAlgorithm {
    private final int maxSliceQty;

    public VwapAlgorithm(
            int maxSliceQty
    ) {
        if (maxSliceQty <= 0) {
            throw new IllegalArgumentException(
                    "maxSliceQty must be positive"
            );
        }

        this.maxSliceQty = maxSliceQty;
    }

    @Override
    public String name() {
        return "VWAP_ORACLE";
    }

    @Override
    public ExecutionDecision onEvent(
            ParentOrder parentOrder,
            MarketEvent event,
            ReplayProgress progress
    ) {
        int targetCumulative =
                (int) Math.ceil(
                        parentOrder.quantity()
                                * progress
                                .oracleVolumeFraction()
                );

        int scheduleDeficit =
                targetCumulative
                        - parentOrder.filledQuantity();

        if (scheduleDeficit <= 0) {
            return ExecutionDecision.none(
                    "already at oracle volume target"
            );
        }

        int childQuantity =
                Math.min(
                        parentOrder.remainingQuantity(),
                        Math.min(
                                maxSliceQty,
                                scheduleDeficit
                        )
                );

        return new ExecutionDecision(
                childQuantity,
                "oracle volume-curve catch-up"
        );
    }
}

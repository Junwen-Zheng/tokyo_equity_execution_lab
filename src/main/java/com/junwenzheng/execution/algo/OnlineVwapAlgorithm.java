package com.junwenzheng.execution.algo;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;

public final class OnlineVwapAlgorithm
        implements ExecutionAlgorithm {
    private final int maxSliceQty;
    private final long forecastTotalVolume;

    public OnlineVwapAlgorithm(
            int maxSliceQty,
            long forecastTotalVolume
    ) {
        if (maxSliceQty <= 0) {
            throw new IllegalArgumentException(
                    "maxSliceQty must be positive"
            );
        }

        if (forecastTotalVolume <= 0L) {
            throw new IllegalArgumentException(
                    "forecastTotalVolume must be positive"
            );
        }

        this.maxSliceQty = maxSliceQty;
        this.forecastTotalVolume =
                forecastTotalVolume;
    }

    @Override
    public String name() {
        return "VWAP_ONLINE";
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
                                .observedVolumeFraction(
                                        forecastTotalVolume
                                )
                );

        int scheduleDeficit =
                targetCumulative
                        - parentOrder.filledQuantity();

        if (scheduleDeficit <= 0) {
            return ExecutionDecision.none(
                    "already at online volume target"
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
                "online volume-forecast catch-up"
        );
    }
}

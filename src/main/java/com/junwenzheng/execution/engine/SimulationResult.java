package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ChildOrderStatus;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;

import java.util.List;

public record SimulationResult(
        String strategyName,
        ParentOrder parentOrder,
        MarketDataReplay replay,
        List<ChildOrder> childOrders,
        List<Fill> fills
) {
    public SimulationResult {
        if (
                strategyName == null
                        || strategyName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "strategyName is required"
            );
        }

        if (parentOrder == null) {
            throw new IllegalArgumentException(
                    "parentOrder is required"
            );
        }

        if (replay == null) {
            throw new IllegalArgumentException(
                    "replay is required"
            );
        }

        if (childOrders == null) {
            throw new IllegalArgumentException(
                    "childOrders are required"
            );
        }

        if (fills == null) {
            throw new IllegalArgumentException(
                    "fills are required"
            );
        }

        strategyName = strategyName.trim();
        childOrders = List.copyOf(childOrders);
        fills = List.copyOf(fills);
    }

    public int rejectedChildren() {
        return Math.toIntExact(
                childOrders.stream()
                        .filter(
                                child ->
                                        child.status()
                                                == ChildOrderStatus.REJECTED
                        )
                        .count()
        );
    }
}

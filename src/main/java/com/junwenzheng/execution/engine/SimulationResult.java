package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ChildOrderStatus;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.routing.RoutingDecision;

import java.util.List;

public record SimulationResult(
        String strategyName,
        ParentOrder parentOrder,
        MarketDataReplay replay,
        List<ChildOrder> childOrders,
        List<Fill> fills,
        List<LatencyEvent> latencyEvents,
        List<RiskDecision> riskDecisions,
        List<RoutingDecision> routingDecisions
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

        if (latencyEvents == null) {
            throw new IllegalArgumentException(
                    "latencyEvents are required"
            );
        }

        if (riskDecisions == null) {
            throw new IllegalArgumentException(
                    "riskDecisions are required"
            );
        }

        if (routingDecisions == null) {
            throw new IllegalArgumentException(
                    "routingDecisions are required"
            );
        }

        strategyName = strategyName.trim();
        childOrders = List.copyOf(childOrders);
        fills = List.copyOf(fills);
        latencyEvents = List.copyOf(latencyEvents);
        riskDecisions = List.copyOf(riskDecisions);
        routingDecisions =
                List.copyOf(routingDecisions);
    }

    public SimulationResult(
            String strategyName,
            ParentOrder parentOrder,
            MarketDataReplay replay,
            List<ChildOrder> childOrders,
            List<Fill> fills,
            List<LatencyEvent> latencyEvents,
            List<RiskDecision> riskDecisions
    ) {
        this(
                strategyName,
                parentOrder,
                replay,
                childOrders,
                fills,
                latencyEvents,
                riskDecisions,
                List.of()
        );
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

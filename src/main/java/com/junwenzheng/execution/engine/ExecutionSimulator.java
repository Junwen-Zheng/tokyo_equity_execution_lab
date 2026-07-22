package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.market.DeterministicEventClock;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;

import java.util.ArrayList;
import java.util.List;

public final class ExecutionSimulator {
    private final RiskManager riskManager;
    private final FillModel fillModel;

    public ExecutionSimulator(
            RiskManager riskManager,
            FillModel fillModel
    ) {
        if (riskManager == null) {
            throw new IllegalArgumentException(
                    "riskManager is required"
            );
        }

        if (fillModel == null) {
            throw new IllegalArgumentException(
                    "fillModel is required"
            );
        }

        this.riskManager = riskManager;
        this.fillModel = fillModel;
    }

    public SimulationResult run(
            ParentOrder parentOrder,
            MarketDataReplay replay,
            ExecutionAlgorithm algorithm
    ) {
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

        if (algorithm == null) {
            throw new IllegalArgumentException(
                    "algorithm is required"
            );
        }

        parentOrder.markWorking();

        List<ChildOrder> childOrders =
                new ArrayList<>();

        List<Fill> fills =
                new ArrayList<>();

        PositionTracker positions =
                new PositionTracker();

        long cumulativeVolume = 0L;

        List<MarketEvent> events =
                replay.events();

        DeterministicEventClock clock =
                new DeterministicEventClock();

        for (
                int i = 0;
                i < events.size()
                        && parentOrder.remainingQuantity() > 0;
                i++
        ) {
            MarketEvent event = events.get(i);

            long eventTimeMs =
                    clock.advanceTo(event);

            cumulativeVolume += event.volume();

            ReplayProgress progress =
                    new ReplayProgress(
                            i,
                            events.size(),
                            cumulativeVolume,
                            replay.totalVolume()
                    );

            ExecutionDecision decision =
                    algorithm.onEvent(
                            parentOrder,
                            event,
                            progress
                    );

            if (!decision.shouldTrade()) {
                continue;
            }

            ChildOrder childOrder =
                    new ChildOrder(
                            parentOrder.orderId(),
                            parentOrder.symbol(),
                            parentOrder.side(),
                            Math.min(
                                    decision.childQuantity(),
                                    parentOrder
                                            .remainingQuantity()
                            ),
                            eventTimeMs,
                            decision.reason()
                    );

            childOrders.add(childOrder);

            if (
                    !riskManager.isAllowed(
                            childOrder,
                            event.mid()
                    )
            ) {
                childOrder.reject(eventTimeMs);
                continue;
            }

            childOrder.acknowledge(eventTimeMs);

            Fill fill = fillModel.tryFill(
                    childOrder,
                    event,
                    algorithm.name()
            );

            validateFill(
                    childOrder,
                    fill
            );

            childOrder.applyFill(
                    fill.quantity(),
                    fill.timestampMs()
            );

            parentOrder.applyFill(
                    fill.quantity()
            );

            fills.add(fill);
            positions.apply(fill);

            if (childOrder.remainingQuantity() > 0) {
                childOrder.cancel(
                        fill.timestampMs()
                );
            }
        }

        if (
                parentOrder.remainingQuantity() > 0
                        && !parentOrder.isTerminal()
        ) {
            parentOrder.cancel();
        }

        return new SimulationResult(
                algorithm.name(),
                parentOrder,
                replay,
                childOrders,
                fills
        );
    }

    private static void validateFill(
            ChildOrder childOrder,
            Fill fill
    ) {
        if (fill == null) {
            throw new IllegalStateException(
                    "fill model returned null"
            );
        }

        if (
                !fill.childOrderId().equals(
                        childOrder.childOrderId()
                )
        ) {
            throw new IllegalStateException(
                    "fill childOrderId does not match child"
            );
        }

        if (
                !fill.parentOrderId().equals(
                        childOrder.parentOrderId()
                )
        ) {
            throw new IllegalStateException(
                    "fill parentOrderId does not match child"
            );
        }

        if (
                !fill.symbol().equals(
                        childOrder.symbol()
                )
        ) {
            throw new IllegalStateException(
                    "fill symbol does not match child"
            );
        }

        if (fill.side() != childOrder.side()) {
            throw new IllegalStateException(
                    "fill side does not match child"
            );
        }

        if (
                fill.quantity()
                        > childOrder.remainingQuantity()
        ) {
            throw new IllegalStateException(
                    "fill exceeds child remaining quantity"
            );
        }

        if (
                fill.timestampMs()
                        < childOrder.lastUpdateTimestampMs()
        ) {
            throw new IllegalStateException(
                    "fill timestamp precedes child lifecycle"
            );
        }
    }
}

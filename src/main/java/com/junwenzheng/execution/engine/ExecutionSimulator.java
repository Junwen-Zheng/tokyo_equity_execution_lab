package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.market.DeterministicEventClock;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.engine.FillOutcome;
import com.junwenzheng.execution.order.ParentOrder;

import java.util.ArrayList;
import java.util.List;

public final class ExecutionSimulator {
    private final RiskManager riskManager;
    private final FillModel fillModel;
    private final DeterministicLatencyPipeline latencyPipeline;

    public ExecutionSimulator(
            RiskManager riskManager,
            FillModel fillModel
    ) {
        this(
                riskManager,
                fillModel,
                LatencyProfile.deterministicBaseline()
        );
    }

    public ExecutionSimulator(
            RiskManager riskManager,
            FillModel fillModel,
            LatencyProfile latencyProfile
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

        if (latencyProfile == null) {
            throw new IllegalArgumentException(
                    "latencyProfile is required"
            );
        }

        this.riskManager = riskManager;
        this.fillModel = fillModel;
        this.latencyPipeline =
                new DeterministicLatencyPipeline(
                        latencyProfile
                );
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

        List<LatencyEvent> latencyEvents =
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

            long decisionTimeMs =
                    latencyPipeline
                            .decisionTimestampMs(
                                    eventTimeMs
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
                            decisionTimeMs,
                            decision.reason()
                    );

            childOrders.add(childOrder);

            latencyEvents.add(
                    new LatencyEvent(
                            childOrder.childOrderId(),
                            LatencyStage.MARKET_EVENT,
                            eventTimeMs
                    )
            );

            latencyEvents.add(
                    new LatencyEvent(
                            childOrder.childOrderId(),
                            LatencyStage.DECISION,
                            decisionTimeMs
                    )
            );

            long riskTimeMs =
                    latencyPipeline.riskTimestampMs(
                            decisionTimeMs
                    );

            latencyEvents.add(
                    new LatencyEvent(
                            childOrder.childOrderId(),
                            LatencyStage.RISK_CHECK,
                            riskTimeMs
                    )
            );

            if (
                    !riskManager.isAllowed(
                            childOrder,
                            event.mid()
                    )
            ) {
                childOrder.reject(riskTimeMs);

                latencyEvents.add(
                        new LatencyEvent(
                                childOrder.childOrderId(),
                                LatencyStage.REJECTION,
                                riskTimeMs
                        )
                );

                continue;
            }

            long acknowledgementTimeMs =
                    latencyPipeline
                            .acknowledgementTimestampMs(
                                    riskTimeMs
                            );

            childOrder.acknowledge(
                    acknowledgementTimeMs
            );

            latencyEvents.add(
                    new LatencyEvent(
                            childOrder.childOrderId(),
                            LatencyStage.ACKNOWLEDGEMENT,
                            acknowledgementTimeMs
                    )
            );

            long fillTimeMs =
                    latencyPipeline.fillTimestampMs(
                            acknowledgementTimeMs
                    );

            FillOutcome fillOutcome =
                    fillModel.tryFill(
                            childOrder,
                            event,
                            algorithm.name(),
                            fillTimeMs
                    );

            if (
                    fillOutcome
                            instanceof FillOutcome.NoFill
            ) {
                long cancellationTimeMs =
                        latencyPipeline
                                .cancellationTimestampMs(
                                        fillTimeMs
                                );

                childOrder.cancel(
                        cancellationTimeMs
                );

                latencyEvents.add(
                        new LatencyEvent(
                                childOrder.childOrderId(),
                                LatencyStage.CANCELLATION,
                                cancellationTimeMs
                        )
                );

                continue;
            }

            Fill fill =
                    (
                            (FillOutcome.Filled)
                                    fillOutcome
                    ).fill();

            validateFill(
                    childOrder,
                    fill
            );

            latencyEvents.add(
                    new LatencyEvent(
                            childOrder.childOrderId(),
                            LatencyStage.FILL,
                            fill.timestampMs()
                    )
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
                long cancellationTimeMs =
                        latencyPipeline
                                .cancellationTimestampMs(
                                        fill.timestampMs()
                                );

                childOrder.cancel(
                        cancellationTimeMs
                );

                latencyEvents.add(
                        new LatencyEvent(
                                childOrder.childOrderId(),
                                LatencyStage.CANCELLATION,
                                cancellationTimeMs
                        )
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
                fills,
                latencyEvents
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

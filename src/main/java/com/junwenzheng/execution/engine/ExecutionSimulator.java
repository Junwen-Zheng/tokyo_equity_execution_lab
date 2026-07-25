package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.market.DeterministicEventClock;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.routing.RouteAllocation;
import com.junwenzheng.execution.routing.RoutingDecision;
import com.junwenzheng.execution.routing.SmartOrderRouter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExecutionSimulator {
    private static final String CONSOLIDATED_VENUE =
            "CONSOLIDATED";

    private final RiskManager riskManager;
    private final FillModel fillModel;
    private final DeterministicLatencyPipeline
            latencyPipeline;
    private final SmartOrderRouter smartOrderRouter;

    public ExecutionSimulator(
            RiskManager riskManager,
            FillModel fillModel
    ) {
        this(
                riskManager,
                fillModel,
                LatencyProfile.deterministicBaseline(),
                null
        );
    }

    public ExecutionSimulator(
            RiskManager riskManager,
            FillModel fillModel,
            LatencyProfile latencyProfile
    ) {
        this(
                riskManager,
                fillModel,
                latencyProfile,
                null
        );
    }

    private ExecutionSimulator(
            RiskManager riskManager,
            FillModel fillModel,
            LatencyProfile latencyProfile,
            SmartOrderRouter smartOrderRouter
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
        this.smartOrderRouter = smartOrderRouter;
    }

    public static ExecutionSimulator routed(
            RiskManager riskManager,
            FillModel fillModel,
            SmartOrderRouter smartOrderRouter
    ) {
        return routed(
                riskManager,
                fillModel,
                LatencyProfile.deterministicBaseline(),
                smartOrderRouter
        );
    }

    public static ExecutionSimulator routed(
            RiskManager riskManager,
            FillModel fillModel,
            LatencyProfile latencyProfile,
            SmartOrderRouter smartOrderRouter
    ) {
        if (smartOrderRouter == null) {
            throw new IllegalArgumentException(
                    "smartOrderRouter is required"
            );
        }

        return new ExecutionSimulator(
                riskManager,
                fillModel,
                latencyProfile,
                smartOrderRouter
        );
    }

    public SimulationResult run(
            ParentOrder parentOrder,
            MarketDataReplay replay,
            ExecutionAlgorithm algorithm
    ) {
        validateRunInputs(
                parentOrder,
                replay,
                algorithm
        );

        if (smartOrderRouter == null) {
            return runSingleVenue(
                    parentOrder,
                    replay,
                    algorithm
            );
        }

        return runRouted(
                parentOrder,
                replay,
                algorithm
        );
    }

    private SimulationResult runSingleVenue(
            ParentOrder parentOrder,
            MarketDataReplay replay,
            ExecutionAlgorithm algorithm
    ) {
        parentOrder.markWorking();

        List<ChildOrder> childOrders =
                new ArrayList<>();

        List<Fill> fills =
                new ArrayList<>();

        List<LatencyEvent> latencyEvents =
                new ArrayList<>();

        List<RiskDecision> riskDecisions =
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

            executeChild(
                    parentOrder,
                    event,
                    algorithm.name(),
                    childOrder,
                    fills,
                    latencyEvents,
                    riskDecisions,
                    positions
            );
        }

        cancelResidualParent(parentOrder);

        return new SimulationResult(
                algorithm.name(),
                parentOrder,
                replay,
                childOrders,
                fills,
                latencyEvents,
                riskDecisions,
                List.of()
        );
    }

    private SimulationResult runRouted(
            ParentOrder parentOrder,
            MarketDataReplay replay,
            ExecutionAlgorithm algorithm
    ) {
        parentOrder.markWorking();

        List<ChildOrder> childOrders =
                new ArrayList<>();

        List<Fill> fills =
                new ArrayList<>();

        List<LatencyEvent> latencyEvents =
                new ArrayList<>();

        List<RiskDecision> riskDecisions =
                new ArrayList<>();

        List<RoutingDecision> routingDecisions =
                new ArrayList<>();

        PositionTracker positions =
                new PositionTracker();

        List<List<MarketEvent>> snapshots =
                snapshotsForSymbol(
                        replay,
                        parentOrder.symbol()
                );

        long totalVolume =
                snapshots.stream()
                        .flatMap(List::stream)
                        .mapToLong(MarketEvent::volume)
                        .sum();

        long cumulativeVolume = 0L;

        DeterministicEventClock clock =
                new DeterministicEventClock();

        for (
                int i = 0;
                i < snapshots.size()
                        && parentOrder.remainingQuantity() > 0;
                i++
        ) {
            List<MarketEvent> snapshot =
                    snapshots.get(i);

            MarketEvent decisionEvent =
                    consolidatedEvent(snapshot);

            long eventTimeMs =
                    clock.advanceTo(decisionEvent);

            long snapshotVolume =
                    snapshot.stream()
                            .mapToLong(
                                    MarketEvent::volume
                            )
                            .sum();

            cumulativeVolume += snapshotVolume;

            ReplayProgress progress =
                    new ReplayProgress(
                            i,
                            snapshots.size(),
                            cumulativeVolume,
                            totalVolume
                    );

            long decisionTimeMs =
                    latencyPipeline
                            .decisionTimestampMs(
                                    eventTimeMs
                            );

            ExecutionDecision decision =
                    algorithm.onEvent(
                            parentOrder,
                            decisionEvent,
                            progress
                    );

            if (!decision.shouldTrade()) {
                continue;
            }

            int requestedQuantity =
                    Math.min(
                            decision.childQuantity(),
                            parentOrder
                                    .remainingQuantity()
                    );

            RoutingDecision routingDecision =
                    smartOrderRouter.route(
                            parentOrder.side(),
                            requestedQuantity,
                            snapshot
                    );

            routingDecisions.add(routingDecision);

            for (
                    RouteAllocation allocation :
                    routingDecision.allocations()
            ) {
                if (
                        parentOrder.remainingQuantity()
                                == 0
                ) {
                    break;
                }

                int childQuantity =
                        Math.min(
                                allocation.quantity(),
                                parentOrder
                                        .remainingQuantity()
                        );

                ChildOrder childOrder =
                        ChildOrder.routed(
                                parentOrder.orderId(),
                                parentOrder.symbol(),
                                allocation.venue(),
                                parentOrder.side(),
                                childQuantity,
                                decisionTimeMs,
                                decision.reason()
                                        + "; routed to "
                                        + allocation.venue()
                        );

                childOrders.add(childOrder);

                executeChild(
                        parentOrder,
                        allocation.event(),
                        algorithm.name(),
                        childOrder,
                        fills,
                        latencyEvents,
                        riskDecisions,
                        positions
                );
            }
        }

        cancelResidualParent(parentOrder);

        return new SimulationResult(
                algorithm.name(),
                parentOrder,
                replay,
                childOrders,
                fills,
                latencyEvents,
                riskDecisions,
                routingDecisions
        );
    }

    private void executeChild(
            ParentOrder parentOrder,
            MarketEvent event,
            String strategyName,
            ChildOrder childOrder,
            List<Fill> fills,
            List<LatencyEvent> latencyEvents,
            List<RiskDecision> riskDecisions,
            PositionTracker positions
    ) {
        long eventTimeMs =
                event.timestampMs();

        long decisionTimeMs =
                childOrder.timestampMs();

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

        RiskDecision riskDecision =
                riskManager.evaluate(
                        childOrder,
                        event.mid(),
                        positions.position(
                                childOrder.symbol()
                        )
                );

        riskDecisions.add(riskDecision);

        if (!riskDecision.allowed()) {
            childOrder.reject(riskTimeMs);

            latencyEvents.add(
                    new LatencyEvent(
                            childOrder.childOrderId(),
                            LatencyStage.REJECTION,
                            riskTimeMs
                    )
            );

            return;
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
                        strategyName,
                        fillTimeMs
                );

        if (
                fillOutcome
                        instanceof FillOutcome.NoFill
        ) {
            cancelChildAfter(
                    childOrder,
                    fillTimeMs,
                    latencyEvents
            );

            return;
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
            cancelChildAfter(
                    childOrder,
                    fill.timestampMs(),
                    latencyEvents
            );
        }
    }

    private void cancelChildAfter(
            ChildOrder childOrder,
            long precedingTimestampMs,
            List<LatencyEvent> latencyEvents
    ) {
        long cancellationTimeMs =
                latencyPipeline
                        .cancellationTimestampMs(
                                precedingTimestampMs
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

    private static List<List<MarketEvent>>
    snapshotsForSymbol(
            MarketDataReplay replay,
            String symbol
    ) {
        Map<Long, List<MarketEvent>> byTimestamp =
                new LinkedHashMap<>();

        for (MarketEvent event : replay.events()) {
            if (!event.symbol().equals(symbol)) {
                continue;
            }

            byTimestamp.computeIfAbsent(
                    event.timestampMs(),
                    ignored -> new ArrayList<>()
            ).add(event);
        }

        if (byTimestamp.isEmpty()) {
            throw new IllegalArgumentException(
                    "no replay events for parent symbol "
                            + symbol
            );
        }

        return byTimestamp.values()
                .stream()
                .map(List::copyOf)
                .toList();
    }

    private static MarketEvent consolidatedEvent(
            List<MarketEvent> snapshot
    ) {
        if (snapshot == null || snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "snapshot is required"
            );
        }

        MarketEvent first = snapshot.getFirst();

        double bestBid =
                snapshot.stream()
                        .mapToDouble(MarketEvent::bid)
                        .max()
                        .orElseThrow();

        double bestAsk =
                snapshot.stream()
                        .mapToDouble(MarketEvent::ask)
                        .min()
                        .orElseThrow();

        if (bestAsk < bestBid) {
            double lockedPrice =
                    (bestBid + bestAsk) / 2.0;

            bestBid = lockedPrice;
            bestAsk = lockedPrice;
        }

        long totalVolume =
                snapshot.stream()
                        .mapToLong(MarketEvent::volume)
                        .sum();

        long totalQueueDepth =
                snapshot.stream()
                        .mapToLong(MarketEvent::queueDepth)
                        .sum();

        double lastPrice =
                consolidatedLast(
                        snapshot,
                        totalVolume
                );

        MarketEventType type = first.type();

        for (MarketEvent event : snapshot) {
            if (
                    !event.symbol()
                            .equals(first.symbol())
            ) {
                throw new IllegalArgumentException(
                        "snapshot symbols must match"
                );
            }

            if (
                    event.timestampMs()
                            != first.timestampMs()
            ) {
                throw new IllegalArgumentException(
                        "snapshot timestamps must match"
                );
            }

            if (event.type() != type) {
                throw new IllegalArgumentException(
                        "snapshot event types must match"
                );
            }
        }

        return new MarketEvent(
                first.timestampMs(),
                first.sourceSequence(),
                type,
                first.symbol(),
                CONSOLIDATED_VENUE,
                bestBid,
                bestAsk,
                lastPrice,
                totalVolume,
                totalQueueDepth
        );
    }

    private static double consolidatedLast(
            List<MarketEvent> snapshot,
            long totalVolume
    ) {
        if (totalVolume == 0L) {
            return snapshot.stream()
                    .mapToDouble(MarketEvent::mid)
                    .average()
                    .orElseThrow();
        }

        double notional = 0.0;

        for (MarketEvent event : snapshot) {
            notional +=
                    event.last()
                            * event.volume();
        }

        return notional / totalVolume;
    }

    private static void cancelResidualParent(
            ParentOrder parentOrder
    ) {
        if (
                parentOrder.remainingQuantity() > 0
                        && !parentOrder.isTerminal()
        ) {
            parentOrder.cancel();
        }
    }

    private static void validateRunInputs(
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

        if (
                !fill.venue().equals(
                        childOrder.venue()
                )
        ) {
            throw new IllegalStateException(
                    "fill venue does not match child"
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

package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.engine.RiskDecisionReason;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.order.ChildOrderStatus;
import com.junwenzheng.execution.order.OrderStatus;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.routing.SmartOrderRouter;
import com.junwenzheng.execution.routing.VenueConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmartOrderRoutingIntegrationTest {

    @Test
    void splitsExecutionAcrossBestPricedVenues() {
        SimulationResult result =
                routedSimulator(
                        SmartOrderRouter.unconstrained(),
                        new RiskManager(
                                1_000,
                                1_000_000.0,
                                10_000
                        )
                ).run(
                        parent(150),
                        replay(
                                event(
                                        1_000L,
                                        0L,
                                        "PTS_A",
                                        99.90,
                                        100.00,
                                        500L,
                                        60L
                                ),
                                event(
                                        1_000L,
                                        1L,
                                        "TSE",
                                        100.00,
                                        100.10,
                                        500L,
                                        100L
                                )
                        ),
                        fixedQuantityAlgorithm(150)
                );

        assertEquals(1, result.routingDecisions().size());

        assertTrue(
                result.routingDecisions()
                        .getFirst()
                        .fullyRouted()
        );

        assertEquals(
                List.of("PTS_A", "TSE"),
                result.routingDecisions()
                        .getFirst()
                        .allocations()
                        .stream()
                        .map(allocation ->
                                allocation.venue())
                        .toList()
        );

        assertEquals(
                List.of(60, 90),
                result.routingDecisions()
                        .getFirst()
                        .allocations()
                        .stream()
                        .map(allocation ->
                                allocation.quantity())
                        .toList()
        );

        assertEquals(
                List.of("PTS_A", "TSE"),
                result.childOrders()
                        .stream()
                        .map(child -> child.venue())
                        .toList()
        );

        assertEquals(
                List.of("PTS_A", "TSE"),
                result.fills()
                        .stream()
                        .map(fill -> fill.venue())
                        .toList()
        );

        assertEquals(150, result.parentOrder().filledQuantity());
        assertEquals(OrderStatus.FILLED, result.parentOrder().status());
    }

    @Test
    void venueFeesCanChangeExecutedDestination() {
        SmartOrderRouter router =
                new SmartOrderRouter(
                        List.of(
                                new VenueConfiguration(
                                        "TSE",
                                        3.0,
                                        1.0,
                                        0.0
                                ),
                                new VenueConfiguration(
                                        "PTS_A",
                                        0.0,
                                        1.0,
                                        0.0
                                )
                        )
                );

        SimulationResult result =
                routedSimulator(
                        router,
                        new RiskManager(
                                1_000,
                                1_000_000.0,
                                10_000
                        )
                ).run(
                        parent(100),
                        replay(
                                event(
                                        1_000L,
                                        0L,
                                        "TSE",
                                        99.90,
                                        100.00,
                                        500L,
                                        100L
                                ),
                                event(
                                        1_000L,
                                        1L,
                                        "PTS_A",
                                        99.95,
                                        100.01,
                                        500L,
                                        100L
                                )
                        ),
                        fixedQuantityAlgorithm(100)
                );

        assertEquals(1, result.childOrders().size());

        assertEquals(
                "PTS_A",
                result.childOrders()
                        .getFirst()
                        .venue()
        );

        assertEquals(
                "PTS_A",
                result.fills()
                        .getFirst()
                        .venue()
        );
    }

    @Test
    void unallocatedQuantityLeavesParentResidual() {
        SmartOrderRouter router =
                new SmartOrderRouter(
                        List.of(
                                new VenueConfiguration(
                                        "TSE",
                                        0.0,
                                        0.10,
                                        0.0
                                )
                        )
                );

        SimulationResult result =
                routedSimulator(
                        router,
                        new RiskManager(
                                1_000,
                                1_000_000.0,
                                10_000
                        )
                ).run(
                        parent(250),
                        replay(
                                event(
                                        1_000L,
                                        0L,
                                        "TSE",
                                        99.90,
                                        100.00,
                                        1_000L,
                                        500L
                                )
                        ),
                        fixedQuantityAlgorithm(250)
                );

        assertEquals(
                100,
                result.routingDecisions()
                        .getFirst()
                        .routedQuantity()
        );

        assertEquals(
                150,
                result.routingDecisions()
                        .getFirst()
                        .unallocatedQuantity()
        );

        assertEquals(100, result.parentOrder().filledQuantity());
        assertEquals(150, result.parentOrder().remainingQuantity());

        assertEquals(
                OrderStatus.CANCELLED,
                result.parentOrder().status()
        );
    }

    @Test
    void riskUsesPositionsAccumulatedAcrossVenues() {
        SimulationResult result =
                routedSimulator(
                        SmartOrderRouter.unconstrained(),
                        new RiskManager(
                                100,
                                1_000_000.0,
                                100
                        )
                ).run(
                        parent(120),
                        replay(
                                event(
                                        1_000L,
                                        0L,
                                        "TSE",
                                        99.90,
                                        100.00,
                                        500L,
                                        60L
                                ),
                                event(
                                        1_000L,
                                        1L,
                                        "PTS_A",
                                        100.00,
                                        100.10,
                                        500L,
                                        60L
                                )
                        ),
                        fixedQuantityAlgorithm(120)
                );

        assertEquals(2, result.childOrders().size());
        assertEquals(1, result.fills().size());
        assertEquals(2, result.riskDecisions().size());

        assertEquals(
                ChildOrderStatus.FILLED,
                result.childOrders()
                        .getFirst()
                        .status()
        );

        assertEquals(
                ChildOrderStatus.REJECTED,
                result.childOrders()
                        .getLast()
                        .status()
        );

        assertEquals(
                RiskDecisionReason.ALLOWED,
                result.riskDecisions()
                        .getFirst()
                        .reason()
        );

        assertEquals(
                RiskDecisionReason.MAX_ABSOLUTE_POSITION,
                result.riskDecisions()
                        .getLast()
                        .reason()
        );

        assertEquals(
                60,
                result.riskDecisions()
                        .getLast()
                        .currentPosition()
        );

        assertEquals(
                120L,
                result.riskDecisions()
                        .getLast()
                        .projectedPosition()
        );

        assertEquals(60, result.parentOrder().filledQuantity());
        assertEquals(60, result.parentOrder().remainingQuantity());
        assertEquals(OrderStatus.CANCELLED, result.parentOrder().status());
    }

    @Test
    void routingDecisionResultsAreImmutable() {
        SimulationResult result =
                routedSimulator(
                        SmartOrderRouter.unconstrained(),
                        new RiskManager(
                                1_000,
                                1_000_000.0,
                                10_000
                        )
                ).run(
                        parent(50),
                        replay(
                                event(
                                        1_000L,
                                        0L,
                                        "TSE",
                                        99.90,
                                        100.00,
                                        500L,
                                        100L
                                )
                        ),
                        fixedQuantityAlgorithm(50)
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.routingDecisions().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.routingDecisions()
                        .getFirst()
                        .allocations()
                        .clear()
        );
    }

    @Test
    void legacySimulatorDoesNotRecordRoutingDecisions() {
        ExecutionSimulator simulator =
                new ExecutionSimulator(
                        new RiskManager(
                                1_000,
                                1_000_000.0,
                                10_000
                        ),
                        new FillModel(
                                1.0,
                                0.0,
                                0.0
                        ),
                        LatencyProfile.zero()
                );

        SimulationResult result =
                simulator.run(
                        parent(50),
                        replay(
                                event(
                                        1_000L,
                                        0L,
                                        "PRIMARY",
                                        99.90,
                                        100.00,
                                        500L,
                                        100L
                                )
                        ),
                        fixedQuantityAlgorithm(50)
                );

        assertTrue(result.routingDecisions().isEmpty());
        assertEquals(1, result.fills().size());
    }

    private static ExecutionSimulator routedSimulator(
            SmartOrderRouter router,
            RiskManager riskManager
    ) {
        return ExecutionSimulator.routed(
                riskManager,
                new FillModel(
                        1.0,
                        0.0,
                        0.0
                ),
                LatencyProfile.zero(),
                router
        );
    }

    private static ParentOrder parent(
            int quantity
    ) {
        return new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                quantity,
                100.0
        );
    }

    private static MarketDataReplay replay(
            MarketEvent... events
    ) {
        return MarketDataReplay.of(
                List.of(events)
        );
    }

    private static MarketEvent event(
            long timestampMs,
            long sourceSequence,
            String venue,
            double bid,
            double ask,
            long volume,
            long queueDepth
    ) {
        return new MarketEvent(
                timestampMs,
                sourceSequence,
                MarketEventType.CONTINUOUS,
                "JPXDEMO",
                venue,
                bid,
                ask,
                (bid + ask) / 2.0,
                volume,
                queueDepth
        );
    }

    private static ExecutionAlgorithm fixedQuantityAlgorithm(
            int quantity
    ) {
        return new ExecutionAlgorithm() {
            @Override
            public String name() {
                return "ROUTED_FIXED";
            }

            @Override
            public ExecutionDecision onEvent(
                    ParentOrder parentOrder,
                    MarketEvent event,
                    ReplayProgress progress
            ) {
                return new ExecutionDecision(
                        quantity,
                        "fixed routed quantity"
                );
            }
        };
    }
}

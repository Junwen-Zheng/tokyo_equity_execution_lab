package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.FillOutcome;
import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.OrderStatus;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.routing.RouteAllocation;
import com.junwenzheng.execution.routing.SmartOrderRouter;
import com.junwenzheng.execution.rules.TokyoEquityRules;
import com.junwenzheng.execution.rules.TokyoSessionSchedule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TokyoExecutionIntegrationTest {

    @Test
    void normalizesSingleVenueChildToBoardLot() {
        SimulationResult result =
                tokyoSimulator().run(
                        parent(500),
                        replay(
                                event(
                                        time(10, 0),
                                        0L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_499.5,
                                        2_500.0,
                                        1_000L,
                                        1_000L
                                )
                        ),
                        fixedQuantityAlgorithm(250)
                );

        assertEquals(1, result.childOrders().size());
        assertEquals(
                200,
                result.childOrders()
                        .getFirst()
                        .quantity()
        );

        assertEquals(1, result.fills().size());
        assertEquals(
                200,
                result.fills()
                        .getFirst()
                        .quantity()
        );

        assertEquals(
                200,
                result.parentOrder().filledQuantity()
        );

        assertEquals(
                OrderStatus.CANCELLED,
                result.parentOrder().status()
        );
    }

    @Test
    void skipsLunchAndPreCloseEvents() {
        long validTimestamp =
                time(13, 0);

        SimulationResult result =
                tokyoSimulator().run(
                        parent(100),
                        replay(
                                event(
                                        time(12, 0),
                                        0L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_499.5,
                                        2_500.0,
                                        1_000L,
                                        1_000L
                                ),
                                event(
                                        time(15, 25),
                                        1L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_499.5,
                                        2_500.0,
                                        1_000L,
                                        1_000L
                                ),
                                event(
                                        validTimestamp,
                                        2L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_499.5,
                                        2_500.0,
                                        1_000L,
                                        1_000L
                                )
                        ),
                        fixedQuantityAlgorithm(100)
                );

        assertEquals(1, result.childOrders().size());

        assertEquals(
                validTimestamp,
                result.childOrders()
                        .getFirst()
                        .timestampMs()
        );

        assertEquals(
                100,
                result.parentOrder().filledQuantity()
        );

        assertEquals(
                OrderStatus.FILLED,
                result.parentOrder().status()
        );
    }

    @Test
    void executesAfternoonClosingAuction() {
        long closingTimestamp =
                time(15, 30);

        SimulationResult result =
                tokyoSimulator().run(
                        parent(100),
                        replay(
                                event(
                                        closingTimestamp,
                                        0L,
                                        MarketEventType.CLOSING_AUCTION,
                                        "TSE",
                                        2_499.5,
                                        2_500.0,
                                        1_000L,
                                        1_000L
                                )
                        ),
                        fixedQuantityAlgorithm(100)
                );

        assertEquals(1, result.fills().size());

        assertEquals(
                closingTimestamp,
                result.fills()
                        .getFirst()
                        .timestampMs()
        );

        assertEquals(
                OrderStatus.FILLED,
                result.parentOrder().status()
        );
    }

    @Test
    void rejectsMisalignedEligibleMarketPrice() {
        ParentOrder parentOrder =
                parent(100);

        assertThrows(
                IllegalArgumentException.class,
                () -> tokyoSimulator().run(
                        parentOrder,
                        replay(
                                event(
                                        time(10, 0),
                                        0L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_499.6,
                                        2_500.0,
                                        1_000L,
                                        1_000L
                                )
                        ),
                        fixedQuantityAlgorithm(100)
                )
        );
    }

    @Test
    void rejectsNonBoardLotParentBeforeExecution() {
        ParentOrder parentOrder =
                parent(150);

        assertThrows(
                IllegalArgumentException.class,
                () -> tokyoSimulator().run(
                        parentOrder,
                        replay(
                                event(
                                        time(10, 0),
                                        0L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_499.5,
                                        2_500.0,
                                        1_000L,
                                        1_000L
                                )
                        ),
                        fixedQuantityAlgorithm(100)
                )
        );

        assertEquals(
                OrderStatus.NEW,
                parentOrder.status()
        );
    }

    @Test
    void routedTokyoExecutionUsesOnlyFullLots() {
        SimulationResult result =
                ExecutionSimulator.routedTokyo(
                        riskManager(),
                        fillModel(),
                        LatencyProfile.zero(),
                        SmartOrderRouter.unconstrained(),
                        TokyoEquityRules.topix500()
                ).run(
                        parent(300),
                        replay(
                                event(
                                        time(10, 0),
                                        0L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_500.0,
                                        2_500.5,
                                        1_000L,
                                        250L
                                ),
                                event(
                                        time(10, 0),
                                        1L,
                                        MarketEventType.CONTINUOUS,
                                        "PTS_A",
                                        2_499.5,
                                        2_500.0,
                                        1_000L,
                                        150L
                                )
                        ),
                        fixedQuantityAlgorithm(300)
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
                        .map(RouteAllocation::venue)
                        .toList()
        );

        assertEquals(
                List.of(100, 200),
                result.routingDecisions()
                        .getFirst()
                        .allocations()
                        .stream()
                        .map(RouteAllocation::quantity)
                        .toList()
        );

        assertEquals(
                List.of(100, 200),
                result.childOrders()
                        .stream()
                        .map(ChildOrder::quantity)
                        .toList()
        );

        assertEquals(
                300,
                result.parentOrder().filledQuantity()
        );

        assertEquals(
                OrderStatus.FILLED,
                result.parentOrder().status()
        );
    }

    @Test
    void lotAwareFillRoundsLiquidityDown() {
        ChildOrder childOrder =
                activeChild(200);

        FillOutcome.Filled outcome =
                assertInstanceOf(
                        FillOutcome.Filled.class,
                        fillModel().tryFill(
                                childOrder,
                                event(
                                        time(10, 0),
                                        0L,
                                        MarketEventType.CONTINUOUS,
                                        "TSE",
                                        2_499.5,
                                        2_500.0,
                                        250L,
                                        250L
                                ),
                                "TOKYO_FILL",
                                time(10, 0),
                                100
                        )
                );

        assertEquals(
                250L,
                outcome.executableLiquidity()
        );

        assertEquals(
                200,
                outcome.fill().quantity()
        );
    }

    @Test
    void lotAwareFillRejectsOddLotChild() {
        ChildOrder childOrder =
                activeChild(150);

        assertThrows(
                IllegalArgumentException.class,
                () -> fillModel().tryFill(
                        childOrder,
                        event(
                                time(10, 0),
                                0L,
                                MarketEventType.CONTINUOUS,
                                "TSE",
                                2_499.5,
                                2_500.0,
                                1_000L,
                                1_000L
                        ),
                        "TOKYO_FILL",
                        time(10, 0),
                        100
                )
        );
    }

    private static ExecutionSimulator tokyoSimulator() {
        return ExecutionSimulator.tokyo(
                riskManager(),
                fillModel(),
                LatencyProfile.zero(),
                TokyoEquityRules.topix500()
        );
    }

    private static RiskManager riskManager() {
        return new RiskManager(
                10_000,
                100_000_000.0,
                100_000
        );
    }

    private static FillModel fillModel() {
        return new FillModel(
                1.0,
                0.0,
                0.0
        );
    }

    private static ParentOrder parent(
            int quantity
    ) {
        return new ParentOrder(
                "7203",
                Side.BUY,
                quantity,
                2_500.0
        );
    }

    private static ChildOrder activeChild(
            int quantity
    ) {
        ChildOrder childOrder =
                ChildOrder.routed(
                        "parent-tokyo",
                        "7203",
                        "TSE",
                        Side.BUY,
                        quantity,
                        time(10, 0),
                        "Tokyo fill test"
                );

        childOrder.acknowledge(
                time(10, 0)
        );

        return childOrder;
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
            MarketEventType type,
            String venue,
            double bid,
            double ask,
            long volume,
            long queueDepth
    ) {
        return new MarketEvent(
                timestampMs,
                sourceSequence,
                type,
                "7203",
                venue,
                bid,
                ask,
                ask,
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
                return "TOKYO_FIXED";
            }

            @Override
            public ExecutionDecision onEvent(
                    ParentOrder parentOrder,
                    MarketEvent event,
                    ReplayProgress progress
            ) {
                return new ExecutionDecision(
                        quantity,
                        "fixed Tokyo quantity"
                );
            }
        };
    }

    private static long time(
            int hour,
            int minute
    ) {
        return TokyoSessionSchedule.timeMs(
                hour,
                minute
        );
    }
}

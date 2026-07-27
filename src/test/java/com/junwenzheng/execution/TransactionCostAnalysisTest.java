package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.metrics.tca.TransactionCostAnalysis;
import com.junwenzheng.execution.metrics.tca.TransactionCostBreakdown;
import com.junwenzheng.execution.metrics.tca.VenueCostContribution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransactionCostAnalysisTest {

    private static final double EPSILON =
            1.0e-9;

    @Test
    void decomposesBuyImplementationShortfall() {
        ParentOrder parent =
                parent(
                        Side.BUY,
                        300,
                        100.0
                );

        parent.markWorking();
        parent.applyFill(100);
        parent.applyFill(100);
        parent.cancel();

        SimulationResult result =
                result(
                        parent,
                        replay(
                                event(
                                        1_000L,
                                        100.0,
                                        101.0,
                                        100.5,
                                        100L
                                ),
                                event(
                                        2_000L,
                                        101.0,
                                        102.0,
                                        101.5,
                                        100L
                                ),
                                event(
                                        3_000L,
                                        102.5,
                                        103.5,
                                        103.0,
                                        0L
                                )
                        ),
                        List.of(
                                fill(
                                        "child-1",
                                        "TSE",
                                        Side.BUY,
                                        100,
                                        101.0,
                                        100.5,
                                        20.0,
                                        30.0,
                                        1_000L
                                ),
                                fill(
                                        "child-2",
                                        "PTS_A",
                                        Side.BUY,
                                        100,
                                        102.0,
                                        101.0,
                                        20.0,
                                        80.0,
                                        2_000L
                                )
                        )
                );

        TransactionCostBreakdown tca =
                TransactionCostAnalysis.from(result);

        assertEquals(300, tca.parentQuantity());
        assertEquals(200, tca.filledQuantity());
        assertEquals(100, tca.unfilledQuantity());

        assertEquals(
                101.5,
                tca.averageFillPrice(),
                EPSILON
        );

        assertEquals(
                103.0,
                tca.terminalBenchmarkPrice(),
                EPSILON
        );

        assertEquals(
                20_300.0,
                tca.executedNotional(),
                EPSILON
        );

        assertEquals(
                30_000.0,
                tca.arrivalNotional(),
                EPSILON
        );

        assertEquals(
                150.0,
                tca.delayCost(),
                EPSILON
        );

        assertEquals(
                150.0,
                tca.executionCost(),
                EPSILON
        );

        assertEquals(
                40.3,
                tca.spreadCost(),
                EPSILON
        );

        assertEquals(
                110.95,
                tca.impactCost(),
                EPSILON
        );

        assertEquals(
                -1.25,
                tca.residualExecutionCost(),
                EPSILON
        );

        assertEquals(
                300.0,
                tca.filledImplementationShortfall(),
                EPSILON
        );

        assertEquals(
                300.0,
                tca.opportunityCost(),
                EPSILON
        );

        assertEquals(
                600.0,
                tca.totalImplementationShortfall(),
                EPSILON
        );

        assertEquals(
                200.0,
                tca.totalImplementationShortfallBps(),
                EPSILON
        );

        assertEquals(
                2.0 / 3.0,
                tca.fillRate(),
                EPSILON
        );
    }

    @Test
    void attributesFilledShortfallByVenue() {
        ParentOrder parent =
                parent(
                        Side.BUY,
                        200,
                        100.0
                );

        parent.markWorking();
        parent.applyFill(100);
        parent.applyFill(100);

        TransactionCostBreakdown tca =
                TransactionCostAnalysis.from(
                        result(
                                parent,
                                replay(
                                        event(
                                                1_000L,
                                                100.0,
                                                101.0,
                                                100.5,
                                                100L
                                        ),
                                        event(
                                                2_000L,
                                                101.0,
                                                102.0,
                                                101.5,
                                                100L
                                        )
                                ),
                                List.of(
                                        fill(
                                                "child-1",
                                                "TSE",
                                                Side.BUY,
                                                100,
                                                101.0,
                                                100.5,
                                                20.0,
                                                30.0,
                                                1_000L
                                        ),
                                        fill(
                                                "child-2",
                                                "PTS_A",
                                                Side.BUY,
                                                100,
                                                102.0,
                                                101.0,
                                                20.0,
                                                80.0,
                                                2_000L
                                        )
                                )
                        )
                );

        assertEquals(
                List.of("PTS_A", "TSE"),
                tca.venueContributions()
                        .stream()
                        .map(
                                VenueCostContribution::venue
                        )
                        .toList()
        );

        VenueCostContribution pts =
                tca.venueContributions()
                        .getFirst();

        VenueCostContribution tse =
                tca.venueContributions()
                        .getLast();

        assertEquals(100, pts.filledQuantity());
        assertEquals(10_200.0, pts.executedNotional(), EPSILON);
        assertEquals(200.0, pts.implementationShortfall(), EPSILON);
        assertEquals(200.0, pts.implementationShortfallBps(), EPSILON);

        assertEquals(100, tse.filledQuantity());
        assertEquals(10_100.0, tse.executedNotional(), EPSILON);
        assertEquals(100.0, tse.implementationShortfall(), EPSILON);
        assertEquals(100.0, tse.implementationShortfallBps(), EPSILON);
    }

    @Test
    void appliesSellSideSignConsistently() {
        ParentOrder parent =
                parent(
                        Side.SELL,
                        100,
                        100.0
                );

        parent.markWorking();
        parent.applyFill(100);

        TransactionCostBreakdown tca =
                TransactionCostAnalysis.from(
                        result(
                                parent,
                                replay(
                                        event(
                                                1_000L,
                                                98.0,
                                                100.0,
                                                99.0,
                                                100L
                                        )
                                ),
                                List.of(
                                        fill(
                                                "child-sell",
                                                "TSE",
                                                Side.SELL,
                                                100,
                                                98.5,
                                                99.0,
                                                20.0,
                                                30.0,
                                                1_000L
                                        )
                                )
                        )
                );

        assertEquals(
                100.0,
                tca.delayCost(),
                EPSILON
        );

        assertEquals(
                50.0,
                tca.executionCost(),
                EPSILON
        );

        assertEquals(
                150.0,
                tca.totalImplementationShortfall(),
                EPSILON
        );

        assertEquals(
                150.0,
                tca.totalImplementationShortfallBps(),
                EPSILON
        );
    }

    @Test
    void valuesCompletelyUnfilledOrderAtTerminalMidpoint() {
        ParentOrder parent =
                parent(
                        Side.BUY,
                        100,
                        100.0
                );

        parent.markWorking();
        parent.cancel();

        TransactionCostBreakdown tca =
                TransactionCostAnalysis.from(
                        result(
                                parent,
                                replay(
                                        event(
                                                1_000L,
                                                99.5,
                                                100.5,
                                                100.0,
                                                100L
                                        ),
                                        event(
                                                2_000L,
                                                101.5,
                                                102.5,
                                                102.0,
                                                100L
                                        )
                                ),
                                List.of()
                        )
                );

        assertEquals(0, tca.filledQuantity());
        assertEquals(100, tca.unfilledQuantity());
        assertEquals(0.0, tca.averageFillPrice(), EPSILON);
        assertEquals(0.0, tca.executedNotional(), EPSILON);
        assertEquals(0.0, tca.delayCost(), EPSILON);
        assertEquals(0.0, tca.executionCost(), EPSILON);
        assertEquals(200.0, tca.opportunityCost(), EPSILON);
        assertEquals(200.0, tca.totalImplementationShortfall(), EPSILON);
        assertEquals(200.0, tca.totalImplementationShortfallBps(), EPSILON);
        assertTrue(tca.venueContributions().isEmpty());
    }

    @Test
    void computesVwapSlippageFromReplay() {
        ParentOrder parent =
                parent(
                        Side.BUY,
                        100,
                        100.0
                );

        parent.markWorking();
        parent.applyFill(100);

        TransactionCostBreakdown tca =
                TransactionCostAnalysis.from(
                        result(
                                parent,
                                replay(
                                        event(
                                                1_000L,
                                                99.5,
                                                100.5,
                                                100.0,
                                                100L
                                        ),
                                        event(
                                                2_000L,
                                                101.5,
                                                102.5,
                                                102.0,
                                                100L
                                        )
                                ),
                                List.of(
                                        fill(
                                                "child-1",
                                                "TSE",
                                                Side.BUY,
                                                100,
                                                102.0,
                                                101.5,
                                                20.0,
                                                30.0,
                                                2_000L
                                        )
                                )
                        )
                );

        assertEquals(
                101.0,
                tca.marketVwap(),
                EPSILON
        );

        assertEquals(
                (102.0 - 101.0)
                        / 101.0
                        * 10_000.0,
                tca.vwapSlippageBps(),
                EPSILON
        );
    }

    @Test
    void rejectsFillQuantityThatDisagreesWithParent() {
        ParentOrder parent =
                parent(
                        Side.BUY,
                        100,
                        100.0
                );

        parent.markWorking();
        parent.applyFill(50);
        parent.cancel();

        SimulationResult result =
                result(
                        parent,
                        replay(
                                event(
                                        1_000L,
                                        99.5,
                                        100.5,
                                        100.0,
                                        100L
                                )
                        ),
                        List.of(
                                fill(
                                        "child-1",
                                        "TSE",
                                        Side.BUY,
                                        40,
                                        100.5,
                                        100.0,
                                        20.0,
                                        30.0,
                                        1_000L
                                )
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> TransactionCostAnalysis.from(
                        result
                )
        );
    }

    @Test
    void rejectsNullResult() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TransactionCostAnalysis.from(
                        null
                )
        );
    }

    private static ParentOrder parent(
            Side side,
            int quantity,
            double arrivalPrice
    ) {
        return new ParentOrder(
                "JPXDEMO",
                side,
                quantity,
                arrivalPrice
        );
    }

    private static SimulationResult result(
            ParentOrder parent,
            MarketDataReplay replay,
            List<Fill> fills
    ) {
        return new SimulationResult(
                "TCA_TEST",
                parent,
                replay,
                List.of(),
                fills,
                List.of(),
                List.of(),
                List.of()
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
            double bid,
            double ask,
            double last,
            long volume
    ) {
        return new MarketEvent(
                timestampMs,
                "JPXDEMO",
                bid,
                ask,
                last,
                volume
        );
    }

    private static Fill fill(
            String childOrderId,
            String venue,
            Side side,
            int quantity,
            double price,
            double referenceMidPrice,
            double spreadCostBps,
            double impactCostBps,
            long timestampMs
    ) {
        return new Fill(
                childOrderId,
                "parent-tca",
                "JPXDEMO",
                venue,
                side,
                quantity,
                price,
                referenceMidPrice,
                spreadCostBps,
                impactCostBps,
                timestampMs,
                "TCA_TEST",
                "TCA fixture"
        );
    }
}

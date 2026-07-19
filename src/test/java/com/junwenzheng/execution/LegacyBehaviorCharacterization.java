package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.algo.TwapAlgorithm;
import com.junwenzheng.execution.algo.VwapAlgorithm;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;

public final class LegacyBehaviorCharacterization {
    private LegacyBehaviorCharacterization() {
    }

    public static void runAll() {
        zeroVolumeEventStillProducesOneShareFill();
        twapSliceSettingActsAsMaximumCap();
        vwapDecisionDependsOnFutureReplayVolume();
        riskAcceptsNonPositiveReferencePrices();

        System.out.println(
                "Legacy behavior characterization passed"
        );
    }

    private static void zeroVolumeEventStillProducesOneShareFill() {
        MarketEvent event = new MarketEvent(
                1L,
                "JPXDEMO",
                100.0,
                100.2,
                100.1,
                0L
        );

        ChildOrder order = new ChildOrder(
                "parent-1",
                "JPXDEMO",
                Side.BUY,
                25,
                1L,
                "zero-volume characterization"
        );

        Fill fill = new FillModel(
                0.12,
                1.6
        ).tryFill(
                order,
                event,
                "CHARACTERIZATION"
        );

        assertEquals(
                1,
                fill.quantity(),
                "legacy fill model forces one share on zero volume"
        );
    }

    private static void twapSliceSettingActsAsMaximumCap() {
        ParentOrder parent = new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                1_000,
                100.2
        );

        MarketEvent event = standardEvent();

        ExecutionDecision decision = new TwapAlgorithm(
                100
        ).onEvent(
                parent,
                event,
                new ReplayProgress(
                        9,
                        10,
                        1_000,
                        1_000
                )
        );

        assertEquals(
                100,
                decision.childQuantity(),
                "legacy TWAP setting caps a 1000-share deficit at 100"
        );
    }

    private static void vwapDecisionDependsOnFutureReplayVolume() {
        ParentOrder parent = new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                1_000,
                100.2
        );

        VwapAlgorithm algorithm = new VwapAlgorithm(
                1_000
        );

        ExecutionDecision smallerFutureTotal = algorithm.onEvent(
                parent,
                standardEvent(),
                new ReplayProgress(
                        0,
                        10,
                        100,
                        1_000
                )
        );

        ExecutionDecision largerFutureTotal = algorithm.onEvent(
                parent,
                standardEvent(),
                new ReplayProgress(
                        0,
                        10,
                        100,
                        2_000
                )
        );

        assertEquals(
                100,
                smallerFutureTotal.childQuantity(),
                "legacy VWAP uses full replay total volume"
        );

        assertEquals(
                50,
                largerFutureTotal.childQuantity(),
                "changing future replay volume changes the live decision"
        );
    }

    private static void riskAcceptsNonPositiveReferencePrices() {
        RiskManager riskManager = new RiskManager(
                1_000,
                100_000.0
        );

        ChildOrder order = new ChildOrder(
                "parent-2",
                "JPXDEMO",
                Side.BUY,
                100,
                1L,
                "risk characterization"
        );

        assertTrue(
                riskManager.isAllowed(order, 0.0),
                "legacy risk accepts a zero reference price"
        );

        assertTrue(
                riskManager.isAllowed(order, -100.0),
                "legacy risk accepts a negative reference price"
        );
    }

    private static MarketEvent standardEvent() {
        return new MarketEvent(
                1L,
                "JPXDEMO",
                100.0,
                100.2,
                100.1,
                1_000L
        );
    }

    private static void assertEquals(
            int expected,
            int actual,
            String message
    ) {
        if (expected != actual) {
            throw new AssertionError(
                    message
                            + ": expected="
                            + expected
                            + ", actual="
                            + actual
            );
        }
    }

    private static void assertTrue(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyBehaviorCharacterizationTest {

    @Test
    void zeroVolumeEventStillProducesOneShareFill() {
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
                "Legacy fill model forces one share "
                        + "on zero volume"
        );
    }

    @Test
    void twapSliceSettingActsAsMaximumCap() {
        ParentOrder parent = new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                1_000,
                100.2
        );

        ExecutionDecision decision = new TwapAlgorithm(
                100
        ).onEvent(
                parent,
                standardEvent(),
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
                "Legacy TWAP caps a 1000-share "
                        + "deficit at 100"
        );
    }

    @Test
    void vwapDecisionDependsOnFutureReplayVolume() {
        ParentOrder parent = new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                1_000,
                100.2
        );

        VwapAlgorithm algorithm = new VwapAlgorithm(
                1_000
        );

        ExecutionDecision smallerFutureTotal =
                algorithm.onEvent(
                        parent,
                        standardEvent(),
                        new ReplayProgress(
                                0,
                                10,
                                100,
                                1_000
                        )
                );

        ExecutionDecision largerFutureTotal =
                algorithm.onEvent(
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
                smallerFutureTotal.childQuantity()
        );

        assertEquals(
                50,
                largerFutureTotal.childQuantity(),
                "Changing future replay volume "
                        + "changes the live decision"
        );
    }

    @Test
    void riskAcceptsNonPositiveReferencePrices() {
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
                "Legacy risk accepts a zero price"
        );

        assertTrue(
                riskManager.isAllowed(order, -100.0),
                "Legacy risk accepts a negative price"
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
}

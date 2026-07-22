package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.FillOutcome;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class FillPricingTest {

    @Test
    void buyFillSeparatesSpreadAndImpactCosts() {
        MarketEvent event = event();

        Fill fill = fill(
                Side.BUY,
                event
        );

        double mid = event.mid();

        double expectedSpread =
                (
                        event.ask() - mid
                ) / mid * 10_000.0;

        double expectedImpact = 1.6;

        double expectedPrice =
                mid * (
                        1.0
                                + (
                                expectedSpread
                                        + expectedImpact
                        ) / 10_000.0
                );

        assertEquals(mid, fill.referenceMidPrice());
        assertEquals(
                expectedSpread,
                fill.spreadCostBps(),
                1.0e-9
        );
        assertEquals(
                expectedImpact,
                fill.impactCostBps(),
                1.0e-9
        );
        assertEquals(
                expectedSpread + expectedImpact,
                fill.totalCostBps(),
                1.0e-9
        );
        assertEquals(
                expectedPrice,
                fill.price(),
                1.0e-9
        );
    }

    @Test
    void sellFillAppliesCostsBelowTheMidpoint() {
        MarketEvent event = event();

        Fill fill = fill(
                Side.SELL,
                event
        );

        double expectedPrice =
                event.mid() * (
                        1.0
                                - fill.totalCostBps()
                                / 10_000.0
                );

        assertEquals(
                expectedPrice,
                fill.price(),
                1.0e-9
        );
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FillModel(
                        Double.NaN,
                        0.0,
                        1.6
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FillModel(
                        0.10,
                        1.01,
                        1.6
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FillModel(
                        0.10,
                        0.0,
                        -1.0
                )
        );
    }

    private static Fill fill(
            Side side,
            MarketEvent event
    ) {
        ChildOrder child =
                new ChildOrder(
                        "child-pricing",
                        "parent-pricing",
                        "JPXDEMO",
                        side,
                        500,
                        1_000L,
                        "pricing test"
                );

        child.acknowledge(1_000L);

        FillOutcome outcome =
                new FillModel(
                        0.10,
                        0.0,
                        1.6
                ).tryFill(
                        child,
                        event,
                        "TEST"
                );

        return ((FillOutcome.Filled) outcome).fill();
    }

    private static MarketEvent event() {
        return new MarketEvent(
                1_000L,
                "JPXDEMO",
                100.0,
                100.2,
                100.1,
                1_000L
        );
    }
}

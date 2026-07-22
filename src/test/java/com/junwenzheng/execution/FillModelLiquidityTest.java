package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.FillOutcome;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FillModelLiquidityTest {

    @Test
    void zeroVolumeReturnsNoFill() {
        FillOutcome outcome =
                model(0.12, 0.0).tryFill(
                        activeChild(25),
                        event("JPXDEMO", 0L),
                        "TEST"
                );

        FillOutcome.NoFill noFill =
                assertInstanceOf(
                        FillOutcome.NoFill.class,
                        outcome
                );

        assertEquals(0L, noFill.participationCap());
        assertEquals(0L, noFill.queueAheadQuantity());
        assertEquals(0L, noFill.executableLiquidity());

        assertTrue(
                noFill.reason().contains("volume is zero")
        );
    }

    @Test
    void roundedParticipationCapReturnsNoFill() {
        FillOutcome outcome =
                model(0.12, 0.0).tryFill(
                        activeChild(25),
                        event("JPXDEMO", 1L),
                        "TEST"
                );

        FillOutcome.NoFill noFill =
                assertInstanceOf(
                        FillOutcome.NoFill.class,
                        outcome
                );

        assertEquals(0L, noFill.participationCap());

        assertTrue(
                noFill.reason().contains(
                        "rounded to zero"
                )
        );
    }

    @Test
    void queueAheadReducesExecutableLiquidity() {
        FillOutcome outcome =
                model(0.50, 0.40).tryFill(
                        activeChild(100),
                        event("JPXDEMO", 100L),
                        "TEST"
                );

        FillOutcome.Filled filled =
                assertInstanceOf(
                        FillOutcome.Filled.class,
                        outcome
                );

        assertEquals(50L, filled.participationCap());
        assertEquals(20L, filled.queueAheadQuantity());
        assertEquals(30L, filled.executableLiquidity());
        assertEquals(30, filled.fill().quantity());
    }

    @Test
    void fullQueueReturnsNoFill() {
        FillOutcome outcome =
                model(0.50, 1.0).tryFill(
                        activeChild(100),
                        event("JPXDEMO", 100L),
                        "TEST"
                );

        FillOutcome.NoFill noFill =
                assertInstanceOf(
                        FillOutcome.NoFill.class,
                        outcome
                );

        assertEquals(50L, noFill.participationCap());
        assertEquals(50L, noFill.queueAheadQuantity());
        assertEquals(0L, noFill.executableLiquidity());

        assertTrue(
                noFill.reason().contains(
                        "queue consumed"
                )
        );
    }

    @Test
    void fillIsCappedByChildRemainingQuantity() {
        ChildOrder child = activeChild(25);

        child.applyFill(10, 1_000L);

        FillOutcome outcome =
                model(1.0, 0.0).tryFill(
                        child,
                        event("JPXDEMO", 100L),
                        "TEST"
                );

        Fill fill =
                assertInstanceOf(
                        FillOutcome.Filled.class,
                        outcome
                ).fill();

        assertEquals(15, child.remainingQuantity());
        assertEquals(15, fill.quantity());
    }

    @Test
    void inactiveChildCannotBeFilled() {
        ChildOrder child =
                new ChildOrder(
                        "child-new",
                        "parent-1",
                        "JPXDEMO",
                        Side.BUY,
                        100,
                        1_000L,
                        "inactive child"
                );

        assertThrows(
                IllegalStateException.class,
                () -> model(0.50, 0.0).tryFill(
                        child,
                        event("JPXDEMO", 100L),
                        "TEST"
                )
        );
    }

    @Test
    void symbolMismatchIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> model(0.50, 0.0).tryFill(
                        activeChild(100),
                        event("OTHER", 100L),
                        "TEST"
                )
        );
    }

    private static FillModel model(
            double participation,
            double queueAhead
    ) {
        return new FillModel(
                participation,
                queueAhead,
                1.6
        );
    }

    private static ChildOrder activeChild(
            int quantity
    ) {
        ChildOrder child =
                new ChildOrder(
                        "child-1",
                        "parent-1",
                        "JPXDEMO",
                        Side.BUY,
                        quantity,
                        1_000L,
                        "liquidity test"
                );

        child.acknowledge(1_000L);
        return child;
    }

    private static MarketEvent event(
            String symbol,
            long volume
    ) {
        return new MarketEvent(
                1_000L,
                symbol,
                100.0,
                100.2,
                100.1,
                volume
        );
    }
}

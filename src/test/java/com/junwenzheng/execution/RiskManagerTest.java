package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.RiskDecision;
import com.junwenzheng.execution.engine.RiskDecisionReason;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RiskManagerTest {

    @Test
    void constructorRejectsInvalidLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RiskManager(
                        0,
                        1_000.0,
                        100
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new RiskManager(
                        100,
                        0.0,
                        100
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new RiskManager(
                        100,
                        Double.NaN,
                        100
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new RiskManager(
                        100,
                        1_000.0,
                        0
                )
        );
    }

    @Test
    void rejectsInvalidReferencePrices() {
        RiskManager manager =
                manager(
                        1_000,
                        1_000_000.0,
                        10_000
                );

        for (
                double price : new double[]{
                        0.0,
                        -1.0,
                        Double.NaN,
                        Double.POSITIVE_INFINITY
                }
        ) {
            RiskDecision decision =
                    manager.evaluate(
                            child(
                                    Side.BUY,
                                    10
                            ),
                            price,
                            0
                    );

            assertFalse(decision.allowed());

            assertEquals(
                    RiskDecisionReason
                            .INVALID_REFERENCE_PRICE,
                    decision.reason()
            );
        }
    }

    @Test
    void rejectsChildQuantityAboveLimit() {
        RiskDecision decision =
                manager(
                        50,
                        1_000_000.0,
                        10_000
                ).evaluate(
                        child(
                                Side.BUY,
                                51
                        ),
                        100.0,
                        0
                );

        assertFalse(decision.allowed());

        assertEquals(
                RiskDecisionReason.MAX_CHILD_QUANTITY,
                decision.reason()
        );
    }

    @Test
    void rejectsChildNotionalAboveLimit() {
        RiskDecision decision =
                manager(
                        1_000,
                        5_000.0,
                        10_000
                ).evaluate(
                        child(
                                Side.BUY,
                                51
                        ),
                        100.0,
                        0
                );

        assertFalse(decision.allowed());

        assertEquals(
                RiskDecisionReason.MAX_CHILD_NOTIONAL,
                decision.reason()
        );

        assertEquals(
                5_100.0,
                decision.childNotional()
        );
    }

    @Test
    void rejectsBuyAndSellPositionBreaches() {
        RiskManager manager =
                manager(
                        1_000,
                        1_000_000.0,
                        100
                );

        RiskDecision buy =
                manager.evaluate(
                        child(
                                Side.BUY,
                                30
                        ),
                        100.0,
                        80
                );

        assertFalse(buy.allowed());

        assertEquals(
                RiskDecisionReason.MAX_ABSOLUTE_POSITION,
                buy.reason()
        );

        assertEquals(110L, buy.projectedPosition());

        RiskDecision sell =
                manager.evaluate(
                        child(
                                Side.SELL,
                                30
                        ),
                        100.0,
                        -80
                );

        assertFalse(sell.allowed());

        assertEquals(
                RiskDecisionReason.MAX_ABSOLUTE_POSITION,
                sell.reason()
        );

        assertEquals(-110L, sell.projectedPosition());
    }

    @Test
    void allowsRiskReducingOrder() {
        RiskDecision decision =
                manager(
                        1_000,
                        1_000_000.0,
                        100
                ).evaluate(
                        child(
                                Side.SELL,
                                60
                        ),
                        100.0,
                        120
                );

        assertTrue(decision.allowed());

        assertEquals(
                RiskDecisionReason.ALLOWED,
                decision.reason()
        );

        assertEquals(60L, decision.projectedPosition());
    }

    @Test
    void evaluatesRemainingQuantityAfterPartialFill() {
        ChildOrder child =
                child(
                        Side.BUY,
                        100
                );

        child.acknowledge(1_000L);
        child.applyFill(
                90,
                1_001L
        );

        RiskDecision decision =
                manager(
                        20,
                        10_000.0,
                        100
                ).evaluate(
                        child,
                        100.0,
                        0
                );

        assertTrue(decision.allowed());
        assertEquals(10, decision.evaluatedQuantity());
        assertEquals(10L, decision.projectedPosition());
    }

    @Test
    void rejectsOverflowedNotional() {
        RiskDecision decision =
                manager(
                        1_000,
                        Double.MAX_VALUE,
                        10_000
                ).evaluate(
                        child(
                                Side.BUY,
                                2
                        ),
                        Double.MAX_VALUE,
                        0
                );

        assertFalse(decision.allowed());

        assertEquals(
                RiskDecisionReason.MAX_CHILD_NOTIONAL,
                decision.reason()
        );
    }

    private static RiskManager manager(
            int maxChildQuantity,
            double maxNotional,
            int maxPosition
    ) {
        return new RiskManager(
                maxChildQuantity,
                maxNotional,
                maxPosition
        );
    }

    private static ChildOrder child(
            Side side,
            int quantity
    ) {
        return new ChildOrder(
                "child-risk",
                "parent-risk",
                "JPXDEMO",
                side,
                quantity,
                1_000L,
                "risk test"
        );
    }
}

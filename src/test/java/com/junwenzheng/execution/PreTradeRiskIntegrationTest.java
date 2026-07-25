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
import com.junwenzheng.execution.order.ChildOrderStatus;
import com.junwenzheng.execution.order.OrderStatus;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PreTradeRiskIntegrationTest {

    @Test
    void accumulatedPositionRejectsNextChild() {
        SimulationResult result =
                simulator(
                        new RiskManager(
                                100,
                                1_000_000.0,
                                100
                        )
                ).run(
                        parent(120),
                        twoEventReplay(),
                        fixedQuantityAlgorithm(60)
                );

        assertEquals(2, result.childOrders().size());
        assertEquals(1, result.fills().size());
        assertEquals(1, result.rejectedChildren());
        assertEquals(2, result.riskDecisions().size());

        assertEquals(
                ChildOrderStatus.FILLED,
                result.childOrders()
                        .get(0)
                        .status()
        );

        assertEquals(
                ChildOrderStatus.REJECTED,
                result.childOrders()
                        .get(1)
                        .status()
        );

        assertEquals(
                RiskDecisionReason.ALLOWED,
                result.riskDecisions()
                        .get(0)
                        .reason()
        );

        assertEquals(
                RiskDecisionReason.MAX_ABSOLUTE_POSITION,
                result.riskDecisions()
                        .get(1)
                        .reason()
        );

        assertEquals(
                60,
                result.riskDecisions()
                        .get(1)
                        .currentPosition()
        );

        assertEquals(
                120L,
                result.riskDecisions()
                        .get(1)
                        .projectedPosition()
        );

        assertEquals(60, result.parentOrder().filledQuantity());
        assertEquals(OrderStatus.CANCELLED, result.parentOrder().status());
    }

    @Test
    void notionalRejectionReasonIsRetained() {
        SimulationResult result =
                simulator(
                        new RiskManager(
                                100,
                                500.0,
                                1_000
                        )
                ).run(
                        parent(10),
                        oneEventReplay(),
                        fixedQuantityAlgorithm(10)
                );

        assertEquals(0, result.fills().size());
        assertEquals(1, result.rejectedChildren());

        assertEquals(
                RiskDecisionReason.MAX_CHILD_NOTIONAL,
                result.riskDecisions()
                        .getFirst()
                        .reason()
        );
    }

    @Test
    void riskDecisionMatchesGeneratedChild() {
        SimulationResult result =
                simulator(
                        new RiskManager(
                                100,
                                1_000_000.0,
                                1_000
                        )
                ).run(
                        parent(10),
                        oneEventReplay(),
                        fixedQuantityAlgorithm(10)
                );

        assertEquals(
                result.childOrders()
                        .getFirst()
                        .childOrderId(),
                result.riskDecisions()
                        .getFirst()
                        .childOrderId()
        );
    }

    @Test
    void retainedRiskDecisionsAreImmutable() {
        SimulationResult result =
                simulator(
                        new RiskManager(
                                100,
                                1_000_000.0,
                                1_000
                        )
                ).run(
                        parent(10),
                        oneEventReplay(),
                        fixedQuantityAlgorithm(10)
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.riskDecisions().clear()
        );
    }

    private static ExecutionSimulator simulator(
            RiskManager riskManager
    ) {
        return new ExecutionSimulator(
                riskManager,
                new FillModel(
                        1.0,
                        0.0,
                        0.0
                ),
                LatencyProfile.zero()
        );
    }

    private static ParentOrder parent(
            int quantity
    ) {
        return new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                quantity,
                100.2
        );
    }

    private static MarketDataReplay oneEventReplay() {
        return MarketDataReplay.of(
                List.of(
                        event(
                                1_000L,
                                60L
                        )
                )
        );
    }

    private static MarketDataReplay twoEventReplay() {
        return MarketDataReplay.of(
                List.of(
                        event(
                                1_000L,
                                60L
                        ),
                        event(
                                2_000L,
                                60L
                        )
                )
        );
    }

    private static MarketEvent event(
            long timestampMs,
            long volume
    ) {
        return new MarketEvent(
                timestampMs,
                "JPXDEMO",
                100.0,
                100.2,
                100.1,
                volume
        );
    }

    private static ExecutionAlgorithm fixedQuantityAlgorithm(
            int quantity
    ) {
        return new ExecutionAlgorithm() {
            @Override
            public String name() {
                return "FIXED_RISK_TEST";
            }

            @Override
            public ExecutionDecision onEvent(
                    ParentOrder parentOrder,
                    MarketEvent event,
                    ReplayProgress progress
            ) {
                return new ExecutionDecision(
                        quantity,
                        "fixed risk quantity"
                );
            }
        };
    }
}

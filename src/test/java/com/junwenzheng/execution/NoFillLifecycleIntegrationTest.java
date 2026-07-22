package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
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

final class NoFillLifecycleIntegrationTest {

    @Test
    void noFillCancelsChildAndIncompleteParent() {
        ParentOrder parent =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        10,
                        100.2
                );

        SimulationResult result =
                new ExecutionSimulator(
                        new RiskManager(
                                100,
                                1_000_000.0
                        ),
                        new FillModel(
                                0.12,
                                1.6
                        )
                ).run(
                        parent,
                        zeroVolumeReplay(),
                        fixedAlgorithm()
                );

        assertEquals(1, result.childOrders().size());
        assertEquals(0, result.fills().size());

        assertEquals(
                ChildOrderStatus.CANCELLED,
                result.childOrders()
                        .getFirst()
                        .status()
        );

        assertEquals(
                OrderStatus.CANCELLED,
                parent.status()
        );

        assertEquals(0, parent.filledQuantity());
        assertEquals(10, parent.remainingQuantity());
    }

    private static MarketDataReplay zeroVolumeReplay() {
        return MarketDataReplay.of(
                List.of(
                        new MarketEvent(
                                1_000L,
                                "JPXDEMO",
                                100.0,
                                100.2,
                                100.1,
                                0L
                        )
                )
        );
    }

    private static ExecutionAlgorithm fixedAlgorithm() {
        return new ExecutionAlgorithm() {
            @Override
            public String name() {
                return "FIXED";
            }

            @Override
            public ExecutionDecision onEvent(
                    ParentOrder parentOrder,
                    MarketEvent event,
                    ReplayProgress progress
            ) {
                return new ExecutionDecision(
                        10,
                        "fixed quantity"
                );
            }
        };
    }
}

package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ChildOrderStatus;
import com.junwenzheng.execution.order.OrderStatus;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ExecutionLifecycleIntegrationTest {

    @Test
    void partiallyFilledChildIsCancelledAfterEvent() {
        ParentOrder parent = parentOrder(100);

        SimulationResult result =
                simulator(1_000).run(
                        parent,
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        assertEquals(1, result.childOrders().size());
        assertEquals(1, result.fills().size());

        ChildOrder child =
                result.childOrders().getFirst();

        assertEquals(
                ChildOrderStatus.CANCELLED,
                child.status()
        );
        assertEquals(10, child.filledQuantity());
        assertEquals(40, child.remainingQuantity());

        assertEquals(
                OrderStatus.CANCELLED,
                parent.status()
        );
        assertEquals(10, parent.filledQuantity());
        assertEquals(90, parent.remainingQuantity());
    }

    @Test
    void fullyFilledChildCompletesParent() {
        ParentOrder parent = parentOrder(10);

        SimulationResult result =
                simulator(1_000).run(
                        parent,
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        ChildOrder child =
                result.childOrders().getFirst();

        assertEquals(
                ChildOrderStatus.FILLED,
                child.status()
        );
        assertEquals(
                OrderStatus.FILLED,
                parent.status()
        );
        assertEquals(10, child.filledQuantity());
        assertEquals(10, parent.filledQuantity());
    }

    @Test
    void riskFailureRejectsAndRetainsChild() {
        ParentOrder parent = parentOrder(100);

        SimulationResult result =
                simulator(5).run(
                        parent,
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        assertEquals(1, result.childOrders().size());
        assertEquals(0, result.fills().size());
        assertEquals(1, result.rejectedChildren());

        assertEquals(
                ChildOrderStatus.REJECTED,
                result.childOrders()
                        .getFirst()
                        .status()
        );

        assertEquals(
                OrderStatus.CANCELLED,
                parent.status()
        );
        assertEquals(0, parent.filledQuantity());
    }

    @Test
    void resultChildAndFillListsAreImmutable() {
        SimulationResult result =
                simulator(1_000).run(
                        parentOrder(10),
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.childOrders().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.fills().clear()
        );
    }

    private static ExecutionSimulator simulator(
            int maxChildQuantity
    ) {
        return new ExecutionSimulator(
                new RiskManager(
                        maxChildQuantity,
                        1_000_000.0
                ),
                new FillModel(
                        0.10,
                        1.6
                )
        );
    }

    private static ParentOrder parentOrder(
            int quantity
    ) {
        return new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                quantity,
                100.2
        );
    }

    private static MarketDataReplay replay(
            long volume
    ) {
        return MarketDataReplay.of(
                List.of(
                        new MarketEvent(
                                1_000L,
                                "JPXDEMO",
                                100.0,
                                100.2,
                                100.1,
                                volume
                        )
                )
        );
    }
}

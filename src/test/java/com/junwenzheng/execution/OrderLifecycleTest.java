package com.junwenzheng.execution;

import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ChildOrderStatus;
import com.junwenzheng.execution.order.OrderStatus;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OrderLifecycleTest {

    @Test
    void parentTransitionsThroughPartialAndFullFill() {
        ParentOrder parent =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        1_000,
                        100.0
                );

        assertEquals(OrderStatus.NEW, parent.status());

        parent.markWorking();
        assertEquals(
                OrderStatus.WORKING,
                parent.status()
        );

        parent.applyFill(400);

        assertEquals(
                OrderStatus.PARTIALLY_FILLED,
                parent.status()
        );
        assertEquals(400, parent.filledQuantity());
        assertEquals(600, parent.remainingQuantity());

        parent.applyFill(600);

        assertEquals(
                OrderStatus.FILLED,
                parent.status()
        );
        assertEquals(0, parent.remainingQuantity());
        assertTrue(parent.isTerminal());
    }

    @Test
    void parentRejectsInvalidAndExcessFills() {
        ParentOrder parent =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        100,
                        100.0
                );

        assertThrows(
                IllegalStateException.class,
                () -> parent.applyFill(10)
        );

        parent.markWorking();

        assertThrows(
                IllegalArgumentException.class,
                () -> parent.applyFill(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parent.applyFill(101)
        );

        assertEquals(100, parent.remainingQuantity());
    }

    @Test
    void cancelledParentIsTerminal() {
        ParentOrder parent =
                new ParentOrder(
                        "JPXDEMO",
                        Side.SELL,
                        500,
                        100.0
                );

        parent.markWorking();
        parent.applyFill(100);
        parent.cancel();

        assertEquals(
                OrderStatus.CANCELLED,
                parent.status()
        );
        assertEquals(100, parent.filledQuantity());
        assertEquals(400, parent.remainingQuantity());
        assertTrue(parent.isTerminal());

        assertThrows(
                IllegalStateException.class,
                () -> parent.applyFill(100)
        );
    }

    @Test
    void rejectedParentCannotBecomeWorking() {
        ParentOrder parent =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        100,
                        100.0
                );

        parent.reject();

        assertEquals(
                OrderStatus.REJECTED,
                parent.status()
        );

        assertThrows(
                IllegalStateException.class,
                parent::markWorking
        );
    }

    @Test
    void childTransitionsThroughAcknowledgementAndFills() {
        ChildOrder child = childOrder(1_000L);

        assertEquals(
                ChildOrderStatus.NEW,
                child.status()
        );

        child.acknowledge(1_001L);

        assertEquals(
                ChildOrderStatus.ACKNOWLEDGED,
                child.status()
        );

        child.applyFill(40, 1_002L);

        assertEquals(
                ChildOrderStatus.PARTIALLY_FILLED,
                child.status()
        );
        assertEquals(40, child.filledQuantity());
        assertEquals(60, child.remainingQuantity());

        child.applyFill(60, 1_003L);

        assertEquals(
                ChildOrderStatus.FILLED,
                child.status()
        );
        assertEquals(0, child.remainingQuantity());
        assertTrue(child.isTerminal());
    }

    @Test
    void childCanBeRejectedBeforeAcknowledgement() {
        ChildOrder child = childOrder(1_000L);

        child.reject(1_001L);

        assertEquals(
                ChildOrderStatus.REJECTED,
                child.status()
        );
        assertTrue(child.isTerminal());

        assertThrows(
                IllegalStateException.class,
                () -> child.acknowledge(1_002L)
        );
    }

    @Test
    void partiallyFilledChildCanBeCancelled() {
        ChildOrder child = childOrder(1_000L);

        child.acknowledge(1_001L);
        child.applyFill(25, 1_002L);
        child.cancel(1_003L);

        assertEquals(
                ChildOrderStatus.CANCELLED,
                child.status()
        );
        assertEquals(25, child.filledQuantity());
        assertEquals(75, child.remainingQuantity());

        assertThrows(
                IllegalStateException.class,
                () -> child.applyFill(10, 1_004L)
        );
    }

    @Test
    void childRejectsIllegalTransitionsAndTimestamps() {
        ChildOrder child = childOrder(1_000L);

        assertThrows(
                IllegalStateException.class,
                () -> child.applyFill(10, 1_001L)
        );

        assertThrows(
                IllegalStateException.class,
                () -> child.cancel(1_001L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> child.acknowledge(999L)
        );

        child.acknowledge(1_000L);

        assertThrows(
                IllegalArgumentException.class,
                () -> child.applyFill(101, 1_001L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> child.applyFill(10, 999L)
        );
    }

    private static ChildOrder childOrder(
            long timestampMs
    ) {
        return new ChildOrder(
                "child-1",
                "parent-1",
                "JPXDEMO",
                Side.BUY,
                100,
                timestampMs,
                "lifecycle test"
        );
    }
}

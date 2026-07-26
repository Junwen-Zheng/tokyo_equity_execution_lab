package com.junwenzheng.execution;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.rules.TokyoEquityRules;
import com.junwenzheng.execution.rules.TokyoSessionSchedule;
import com.junwenzheng.execution.rules.TokyoTickSizeTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TokyoEquityRulesTest {

    @Test
    void standardRulesUseHundredShareBoardLot() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        assertEquals(100, rules.boardLotSize());
        assertTrue(rules.isBoardLot(100));
        assertTrue(rules.isBoardLot(1_000));
        assertFalse(rules.isBoardLot(50));
        assertFalse(rules.isBoardLot(0));
    }

    @Test
    void validatesParentBoardLotAndArrivalTick() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        rules.validateParentOrder(
                new ParentOrder(
                        "7203",
                        Side.BUY,
                        1_000,
                        2_500.0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateParentOrder(
                        new ParentOrder(
                                "7203",
                                Side.BUY,
                                150,
                                2_500.0
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateParentOrder(
                        new ParentOrder(
                                "7203",
                                Side.BUY,
                                1_000,
                                2_500.1
                        )
                )
        );
    }

    @Test
    void validatesChildBoardLot() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        rules.validateChildOrder(
                child(200)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateChildOrder(
                        child(150)
                )
        );
    }

    @Test
    void acceptsExecutableContinuousEvent() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        rules.validateMarketEvent(
                event(
                        time(10, 0),
                        MarketEventType.CONTINUOUS,
                        2_499.5,
                        2_500.0,
                        2_500.0
                )
        );
    }

    @Test
    void rejectsEventDuringLunchOrPreClose() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateMarketEvent(
                        event(
                                time(12, 0),
                                MarketEventType.CONTINUOUS,
                                2_499.5,
                                2_500.0,
                                2_500.0
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateMarketEvent(
                        event(
                                time(15, 25),
                                MarketEventType.CONTINUOUS,
                                2_499.5,
                                2_500.0,
                                2_500.0
                        )
                )
        );
    }

    @Test
    void acceptsMatchingOpeningAndClosingAuctions() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        rules.validateMarketEvent(
                event(
                        time(9, 0),
                        MarketEventType.OPENING_AUCTION,
                        2_499.5,
                        2_500.0,
                        2_500.0
                )
        );

        rules.validateMarketEvent(
                event(
                        time(15, 30),
                        MarketEventType.CLOSING_AUCTION,
                        2_499.5,
                        2_500.0,
                        2_500.0
                )
        );
    }

    @Test
    void rejectsMisalignedMarketPrices() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateMarketEvent(
                        event(
                                time(10, 0),
                                MarketEventType.CONTINUOUS,
                                2_499.6,
                                2_500.0,
                                2_500.0
                        )
                )
        );
    }

    @Test
    void supportsCustomBoardLotAndOtherIssueTable() {
        TokyoEquityRules rules =
                new TokyoEquityRules(
                        new TokyoSessionSchedule(),
                        TokyoTickSizeTable.OTHER_ISSUE,
                        10
                );

        assertEquals(10, rules.boardLotSize());
        assertTrue(rules.isBoardLot(30));
        assertEquals(5.0, rules.tickSize(3_005.0));

        rules.validateParentOrder(
                new ParentOrder(
                        "TEST",
                        Side.BUY,
                        30,
                        3_005.0
                )
        );
    }

    @Test
    void rejectsInvalidConfigurationAndNullInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TokyoEquityRules(
                        new TokyoSessionSchedule(),
                        TokyoTickSizeTable.TOPIX_500,
                        0
                )
        );

        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateParentOrder(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateChildOrder(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rules.validateMarketEvent(null)
        );
    }

    private static ChildOrder child(
            int quantity
    ) {
        return new ChildOrder(
                "parent-1",
                "7203",
                Side.BUY,
                quantity,
                time(10, 0),
                "Tokyo board-lot test"
        );
    }

    private static MarketEvent event(
            long timestampMs,
            MarketEventType type,
            double bid,
            double ask,
            double last
    ) {
        return new MarketEvent(
                timestampMs,
                0L,
                type,
                "7203",
                "TSE",
                bid,
                ask,
                last,
                10_000L,
                5_000L
        );
    }

    private static long time(
            int hour,
            int minute
    ) {
        return TokyoSessionSchedule.timeMs(
                hour,
                minute
        );
    }
}

package com.junwenzheng.execution;

import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.rules.TokyoSessionPhase;
import com.junwenzheng.execution.rules.TokyoSessionSchedule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TokyoSessionScheduleTest {

    private final TokyoSessionSchedule schedule =
            new TokyoSessionSchedule();

    @Test
    void classifiesMorningSessionBoundaries() {
        assertEquals(
                TokyoSessionPhase.CLOSED,
                schedule.phaseAt(time(8, 59))
        );

        assertEquals(
                TokyoSessionPhase.MORNING_OPENING_AUCTION,
                schedule.phaseAt(time(9, 0))
        );

        assertEquals(
                TokyoSessionPhase.MORNING_CONTINUOUS,
                schedule.phaseAt(time(9, 1))
        );

        assertEquals(
                TokyoSessionPhase.MORNING_CLOSING_AUCTION,
                schedule.phaseAt(time(11, 30))
        );
    }

    @Test
    void classifiesLunchAndAfternoonBoundaries() {
        assertEquals(
                TokyoSessionPhase.LUNCH_BREAK,
                schedule.phaseAt(time(12, 0))
        );

        assertEquals(
                TokyoSessionPhase.AFTERNOON_OPENING_AUCTION,
                schedule.phaseAt(time(12, 30))
        );

        assertEquals(
                TokyoSessionPhase.AFTERNOON_CONTINUOUS,
                schedule.phaseAt(time(14, 0))
        );
    }

    @Test
    void classifiesPreCloseAndAfternoonClose() {
        assertEquals(
                TokyoSessionPhase.PRE_CLOSE,
                schedule.phaseAt(time(15, 25))
        );

        assertEquals(
                TokyoSessionPhase.PRE_CLOSE,
                schedule.phaseAt(time(15, 29))
        );

        assertEquals(
                TokyoSessionPhase.AFTERNOON_CLOSING_AUCTION,
                schedule.phaseAt(time(15, 30))
        );

        assertEquals(
                TokyoSessionPhase.CLOSED,
                schedule.phaseAt(time(15, 31))
        );
    }

    @Test
    void permitsOnlyMatchingContinuousEvents() {
        assertTrue(
                schedule.allowsExecution(
                        time(10, 0),
                        MarketEventType.CONTINUOUS
                )
        );

        assertTrue(
                schedule.allowsExecution(
                        time(14, 0),
                        MarketEventType.CONTINUOUS
                )
        );

        assertFalse(
                schedule.allowsExecution(
                        time(12, 0),
                        MarketEventType.CONTINUOUS
                )
        );

        assertFalse(
                schedule.allowsExecution(
                        time(15, 25),
                        MarketEventType.CONTINUOUS
                )
        );
    }

    @Test
    void permitsOnlyMatchingAuctionEvents() {
        assertTrue(
                schedule.allowsExecution(
                        time(9, 0),
                        MarketEventType.OPENING_AUCTION
                )
        );

        assertTrue(
                schedule.allowsExecution(
                        time(12, 30),
                        MarketEventType.OPENING_AUCTION
                )
        );

        assertTrue(
                schedule.allowsExecution(
                        time(11, 30),
                        MarketEventType.CLOSING_AUCTION
                )
        );

        assertTrue(
                schedule.allowsExecution(
                        time(15, 30),
                        MarketEventType.CLOSING_AUCTION
                )
        );

        assertFalse(
                schedule.allowsExecution(
                        time(10, 0),
                        MarketEventType.OPENING_AUCTION
                )
        );
    }

    @Test
    void rejectsInvalidTimeInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> schedule.phaseAt(-1L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> schedule.phaseAt(
                        TokyoSessionSchedule.DAY_END_MS
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> TokyoSessionSchedule.timeMs(
                        24,
                        1
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> schedule.allowsExecution(
                        time(10, 0),
                        null
                )
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

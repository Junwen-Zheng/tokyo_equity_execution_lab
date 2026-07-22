package com.junwenzheng.execution;

import com.junwenzheng.execution.market.DeterministicEventClock;
import com.junwenzheng.execution.market.MarketEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeterministicEventClockTest {

    @Test
    void clockStartsUninitialized() {
        DeterministicEventClock clock =
                new DeterministicEventClock();

        assertFalse(clock.initialized());

        assertThrows(
                IllegalStateException.class,
                clock::currentTimeMs
        );
    }

    @Test
    void clockAdvancesUsingEventTime() {
        DeterministicEventClock clock =
                new DeterministicEventClock();

        assertEquals(
                1_000L,
                clock.advanceTo(event(1_000L))
        );

        assertEquals(
                2_000L,
                clock.advanceTo(event(2_000L))
        );

        assertTrue(clock.initialized());
        assertEquals(2_000L, clock.currentTimeMs());
    }

    @Test
    void equalTimestampsAreAllowed() {
        DeterministicEventClock clock =
                new DeterministicEventClock();

        clock.advanceTo(event(1_000L));

        assertEquals(
                1_000L,
                clock.advanceTo(event(1_000L))
        );
    }

    @Test
    void backwardTimeIsRejected() {
        DeterministicEventClock clock =
                new DeterministicEventClock();

        clock.advanceTo(event(2_000L));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> clock.advanceTo(
                                event(1_000L)
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("cannot move backwards")
        );

        assertEquals(2_000L, clock.currentTimeMs());
    }

    private static MarketEvent event(
            long timestampMs
    ) {
        return new MarketEvent(
                timestampMs,
                "JPXDEMO",
                100.0,
                100.2,
                100.1,
                500L
        );
    }
}

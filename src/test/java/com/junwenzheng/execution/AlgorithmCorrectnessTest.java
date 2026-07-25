package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.OnlineVwapAlgorithm;
import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.algo.TwapAlgorithm;
import com.junwenzheng.execution.algo.VwapAlgorithm;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AlgorithmCorrectnessTest {

    @Test
    void twapSliceSettingActsAsMinimum() {
        ExecutionDecision decision =
                new TwapAlgorithm(
                        250
                ).onEvent(
                        parent(
                                1_000,
                                0
                        ),
                        event(),
                        progress(
                                0,
                                10,
                                100L,
                                1_000L
                        )
                );

        assertEquals(
                250,
                decision.childQuantity()
        );
    }

    @Test
    void twapDoesNotCapScheduleDeficit() {
        ExecutionDecision decision =
                new TwapAlgorithm(
                        100
                ).onEvent(
                        parent(
                                1_000,
                                0
                        ),
                        event(),
                        progress(
                                9,
                                10,
                                1_000L,
                                1_000L
                        )
                );

        assertEquals(
                1_000,
                decision.childQuantity()
        );
    }

    @Test
    void twapDoesNotTradeWhenScheduleTargetIsMet() {
        ExecutionDecision decision =
                new TwapAlgorithm(
                        100
                ).onEvent(
                        parent(
                                1_000,
                                500
                        ),
                        event(),
                        progress(
                                4,
                                10,
                                500L,
                                1_000L
                        )
                );

        assertFalse(decision.shouldTrade());
    }

    @Test
    void oracleVwapDependsOnFullReplayVolume() {
        ParentOrder smallerTotalParent =
                parent(
                        1_000,
                        0
                );

        ParentOrder largerTotalParent =
                parent(
                        1_000,
                        0
                );

        VwapAlgorithm algorithm =
                new VwapAlgorithm(
                        1_000
                );

        ExecutionDecision smallerTotal =
                algorithm.onEvent(
                        smallerTotalParent,
                        event(),
                        progress(
                                0,
                                10,
                                100L,
                                1_000L
                        )
                );

        ExecutionDecision largerTotal =
                algorithm.onEvent(
                        largerTotalParent,
                        event(),
                        progress(
                                0,
                                10,
                                100L,
                                2_000L
                        )
                );

        assertEquals(
                100,
                smallerTotal.childQuantity()
        );

        assertEquals(
                50,
                largerTotal.childQuantity()
        );
    }

    @Test
    void onlineVwapDoesNotReadFullReplayVolume() {
        OnlineVwapAlgorithm algorithm =
                new OnlineVwapAlgorithm(
                        1_000,
                        1_000L
                );

        ExecutionDecision smallerReplay =
                algorithm.onEvent(
                        parent(
                                1_000,
                                0
                        ),
                        event(),
                        progress(
                                0,
                                10,
                                100L,
                                1_000L
                        )
                );

        ExecutionDecision largerReplay =
                algorithm.onEvent(
                        parent(
                                1_000,
                                0
                        ),
                        event(),
                        progress(
                                0,
                                10,
                                100L,
                                2_000L
                        )
                );

        assertEquals(
                100,
                smallerReplay.childQuantity()
        );

        assertEquals(
                smallerReplay,
                largerReplay
        );
    }

    @Test
    void onlineVwapHonoursMaximumSlice() {
        ExecutionDecision decision =
                new OnlineVwapAlgorithm(
                        100,
                        1_000L
                ).onEvent(
                        parent(
                                1_000,
                                0
                        ),
                        event(),
                        progress(
                                8,
                                10,
                                900L,
                                1_000L
                        )
                );

        assertEquals(
                100,
                decision.childQuantity()
        );
    }

    @Test
    void povCatchesUpToCumulativeParticipationTarget() {
        ExecutionDecision decision =
                new PovAlgorithm(
                        0.10,
                        1_000
                ).onEvent(
                        parent(
                                1_000,
                                50
                        ),
                        event(),
                        progress(
                                1,
                                5,
                                2_000L,
                                5_000L
                        )
                );

        assertEquals(
                150,
                decision.childQuantity()
        );
    }

    @Test
    void povDoesNotTradeWhenParticipationTargetIsMet() {
        ExecutionDecision decision =
                new PovAlgorithm(
                        0.10,
                        1_000
                ).onEvent(
                        parent(
                                1_000,
                                100
                        ),
                        event(),
                        progress(
                                0,
                                5,
                                1_000L,
                                5_000L
                        )
                );

        assertFalse(decision.shouldTrade());
    }

    @Test
    void algorithmConstructorsRejectInvalidSettings() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TwapAlgorithm(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new VwapAlgorithm(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new OnlineVwapAlgorithm(
                        0,
                        1_000L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new OnlineVwapAlgorithm(
                        100,
                        0L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PovAlgorithm(
                        Double.NaN,
                        100
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PovAlgorithm(
                        0.10,
                        0
                )
        );
    }

    @Test
    void replayProgressRejectsInvalidState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplayProgress(
                        0,
                        0,
                        0L,
                        0L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplayProgress(
                        -1,
                        10,
                        0L,
                        0L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplayProgress(
                        10,
                        10,
                        0L,
                        0L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplayProgress(
                        0,
                        10,
                        101L,
                        100L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> progress(
                        0,
                        10,
                        100L,
                        1_000L
                ).observedVolumeFraction(0L)
        );
    }

    @Test
    void executionDecisionRejectsInvalidState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExecutionDecision(
                        -1,
                        "invalid"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ExecutionDecision(
                        1,
                        " "
                )
        );
    }

    @Test
    void strategyNamesDistinguishOnlineAndOracleVwap() {
        assertEquals(
                "TWAP",
                new TwapAlgorithm(100).name()
        );

        assertEquals(
                "VWAP_ORACLE",
                new VwapAlgorithm(100).name()
        );

        assertEquals(
                "VWAP_ONLINE",
                new OnlineVwapAlgorithm(
                        100,
                        1_000L
                ).name()
        );

        assertEquals(
                "POV",
                new PovAlgorithm(
                        0.10,
                        100
                ).name()
        );
    }

    private static ParentOrder parent(
            int quantity,
            int filledQuantity
    ) {
        ParentOrder parent =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        quantity,
                        100.2
                );

        if (filledQuantity > 0) {
            parent.markWorking();
            parent.applyFill(filledQuantity);
        }

        return parent;
    }

    private static ReplayProgress progress(
            int eventIndex,
            int eventCount,
            long cumulativeVolume,
            long totalVolume
    ) {
        return new ReplayProgress(
                eventIndex,
                eventCount,
                cumulativeVolume,
                totalVolume
        );
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

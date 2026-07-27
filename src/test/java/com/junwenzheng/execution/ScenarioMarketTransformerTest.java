package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.scenario.ScenarioMarketTransformer;
import com.junwenzheng.execution.scenario.StressScenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ScenarioMarketTransformerTest {

    private static final double EPSILON =
            1.0e-9;

    @Test
    void baselinePreservesReplay() {
        MarketDataReplay replay =
                replay(
                        event(
                                0L,
                                0L,
                                "PRIMARY",
                                99.0,
                                101.0,
                                100.0,
                                1_000L,
                                500L
                        ),
                        event(
                                1_000L,
                                1L,
                                "PRIMARY",
                                100.0,
                                102.0,
                                101.0,
                                900L,
                                450L
                        )
                );

        MarketDataReplay transformed =
                ScenarioMarketTransformer.apply(
                        replay,
                        StressScenario.baseline()
                );

        assertEquals(
                replay.events(),
                transformed.events()
        );
    }

    @Test
    void widensSpreadAroundUnchangedMidpoint() {
        MarketEvent transformed =
                ScenarioMarketTransformer.apply(
                        replay(
                                event(
                                        0L,
                                        0L,
                                        "PRIMARY",
                                        99.0,
                                        101.0,
                                        100.0,
                                        1_000L,
                                        500L
                                )
                        ),
                        StressScenario.wideSpread(
                                2.0
                        )
                ).events().getFirst();

        assertEquals(
                100.0,
                transformed.mid(),
                EPSILON
        );

        assertEquals(
                98.0,
                transformed.bid(),
                EPSILON
        );

        assertEquals(
                102.0,
                transformed.ask(),
                EPSILON
        );
    }

    @Test
    void reducesVolumeAndQueueDepth() {
        StressScenario scenario =
                new StressScenario(
                        "THIN_BOOK",
                        1.0,
                        0.25,
                        1.0,
                        0.40,
                        0.5,
                        0.0,
                        java.util.Set.of(),
                        0L
                );

        MarketEvent transformed =
                ScenarioMarketTransformer.apply(
                        replay(
                                event(
                                        0L,
                                        0L,
                                        "PRIMARY",
                                        99.0,
                                        101.0,
                                        100.0,
                                        1_000L,
                                        500L
                                )
                        ),
                        scenario
                ).events().getFirst();

        assertEquals(
                250L,
                transformed.volume()
        );

        assertEquals(
                200L,
                transformed.queueDepth()
        );
    }

    @Test
    void amplifiesMidpointReturns() {
        MarketDataReplay transformed =
                ScenarioMarketTransformer.apply(
                        replay(
                                event(
                                        0L,
                                        0L,
                                        "PRIMARY",
                                        99.5,
                                        100.5,
                                        100.0,
                                        1_000L,
                                        500L
                                ),
                                event(
                                        1_000L,
                                        1L,
                                        "PRIMARY",
                                        100.5,
                                        101.5,
                                        101.0,
                                        1_000L,
                                        500L
                                )
                        ),
                        StressScenario.highVolatility(
                                3.0
                        )
                );

        assertEquals(
                100.0,
                transformed.events()
                        .getFirst()
                        .mid(),
                EPSILON
        );

        assertEquals(
                103.0,
                transformed.events()
                        .getLast()
                        .mid(),
                EPSILON
        );
    }

    @Test
    void appliesPersistentPriceGap() {
        MarketDataReplay transformed =
                ScenarioMarketTransformer.apply(
                        replay(
                                event(
                                        0L,
                                        0L,
                                        "PRIMARY",
                                        99.5,
                                        100.5,
                                        100.0,
                                        1_000L,
                                        500L
                                ),
                                event(
                                        1_000L,
                                        1L,
                                        "PRIMARY",
                                        100.5,
                                        101.5,
                                        101.0,
                                        1_000L,
                                        500L
                                ),
                                event(
                                        2_000L,
                                        2L,
                                        "PRIMARY",
                                        101.5,
                                        102.5,
                                        102.0,
                                        1_000L,
                                        500L
                                )
                        ),
                        StressScenario.priceGap(
                                0.5,
                                100.0
                        )
                );

        assertEquals(
                100.0,
                transformed.events()
                        .get(0)
                        .mid(),
                EPSILON
        );

        assertEquals(
                102.01,
                transformed.events()
                        .get(1)
                        .mid(),
                EPSILON
        );

        assertEquals(
                103.02,
                transformed.events()
                        .get(2)
                        .mid(),
                EPSILON
        );
    }

    @Test
    void appliesGapToAllVenuesAtThresholdTimestamp() {
        MarketDataReplay transformed =
                ScenarioMarketTransformer.apply(
                        replay(
                                event(
                                        0L,
                                        0L,
                                        "TSE",
                                        99.5,
                                        100.5,
                                        100.0,
                                        1_000L,
                                        500L
                                ),
                                event(
                                        0L,
                                        1L,
                                        "PTS",
                                        99.4,
                                        100.4,
                                        99.9,
                                        1_000L,
                                        500L
                                ),
                                event(
                                        1_000L,
                                        2L,
                                        "TSE",
                                        100.5,
                                        101.5,
                                        101.0,
                                        1_000L,
                                        500L
                                ),
                                event(
                                        1_000L,
                                        3L,
                                        "PTS",
                                        100.4,
                                        101.4,
                                        100.9,
                                        1_000L,
                                        500L
                                )
                        ),
                        StressScenario.priceGap(
                                0.67,
                                100.0
                        )
                );

        assertEquals(
                102.01,
                transformed.events()
                        .get(2)
                        .mid(),
                EPSILON
        );

        assertEquals(
                101.909,
                transformed.events()
                        .get(3)
                        .mid(),
                EPSILON
        );
    }

    @Test
    void removesUnavailableVenue() {
        MarketDataReplay transformed =
                ScenarioMarketTransformer.apply(
                        replay(
                                event(
                                        0L,
                                        0L,
                                        "TSE",
                                        99.5,
                                        100.5,
                                        100.0,
                                        1_000L,
                                        500L
                                ),
                                event(
                                        0L,
                                        1L,
                                        "PTS",
                                        99.4,
                                        100.4,
                                        99.9,
                                        1_000L,
                                        500L
                                )
                        ),
                        StressScenario.venueOutage(
                                "PTS"
                        )
                );

        assertEquals(
                1,
                transformed.events().size()
        );

        assertEquals(
                "TSE",
                transformed.events()
                        .getFirst()
                        .venue()
        );
    }

    @Test
    void rejectsScenarioThatRemovesAllEvents() {
        MarketDataReplay replay =
                replay(
                        event(
                                0L,
                                0L,
                                "TSE",
                                99.5,
                                100.5,
                                100.0,
                                1_000L,
                                500L
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> ScenarioMarketTransformer.apply(
                        replay,
                        StressScenario.venueOutage(
                                "TSE"
                        )
                )
        );
    }

    @Test
    void addsLatencyToEveryPipelineStage() {
        LatencyProfile stressed =
                StressScenario.adverseLatency(
                        10L
                ).applyTo(
                        new LatencyProfile(
                                1L,
                                2L,
                                3L,
                                4L,
                                5L
                        )
                );

        assertEquals(
                new LatencyProfile(
                        11L,
                        12L,
                        13L,
                        14L,
                        15L
                ),
                stressed
        );
    }

    private static MarketDataReplay replay(
            MarketEvent... events
    ) {
        return MarketDataReplay.of(
                List.of(events)
        );
    }

    private static MarketEvent event(
            long timestampMs,
            long sourceSequence,
            String venue,
            double bid,
            double ask,
            double last,
            long volume,
            long queueDepth
    ) {
        return new MarketEvent(
                timestampMs,
                sourceSequence,
                MarketEventType.CONTINUOUS,
                "JPXDEMO",
                venue,
                bid,
                ask,
                last,
                volume,
                queueDepth
        );
    }
}

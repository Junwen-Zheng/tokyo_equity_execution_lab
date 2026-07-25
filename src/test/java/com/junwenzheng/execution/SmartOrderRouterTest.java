package com.junwenzheng.execution;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.routing.RouteAllocation;
import com.junwenzheng.execution.routing.RoutingDecision;
import com.junwenzheng.execution.routing.SmartOrderRouter;
import com.junwenzheng.execution.routing.VenueConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmartOrderRouterTest {

    @Test
    void buyRoutesToLowestEffectivePrice() {
        RoutingDecision decision =
                router().route(
                        Side.BUY,
                        100,
                        List.of(
                                event(
                                        "TSE",
                                        100.00,
                                        100.20,
                                        500,
                                        500
                                ),
                                event(
                                        "PTS_A",
                                        100.00,
                                        100.10,
                                        500,
                                        500
                                )
                        )
                );

        assertEquals(
                "PTS_A",
                decision.allocations()
                        .getFirst()
                        .venue()
        );
    }

    @Test
    void sellRoutesToHighestEffectivePrice() {
        RoutingDecision decision =
                router().route(
                        Side.SELL,
                        100,
                        List.of(
                                event(
                                        "TSE",
                                        100.10,
                                        100.20,
                                        500,
                                        500
                                ),
                                event(
                                        "PTS_A",
                                        100.00,
                                        100.10,
                                        500,
                                        500
                                )
                        )
                );

        assertEquals(
                "TSE",
                decision.allocations()
                        .getFirst()
                        .venue()
        );
    }

    @Test
    void feesCanReverseRawPricePreference() {
        SmartOrderRouter router =
                new SmartOrderRouter(
                        List.of(
                                configuration(
                                        "TSE",
                                        3.0,
                                        1.0,
                                        0.0
                                ),
                                configuration(
                                        "PTS_A",
                                        0.0,
                                        1.0,
                                        0.0
                                )
                        )
                );

        RoutingDecision decision =
                router.route(
                        Side.BUY,
                        100,
                        List.of(
                                event(
                                        "TSE",
                                        99.90,
                                        100.00,
                                        500,
                                        500
                                ),
                                event(
                                        "PTS_A",
                                        99.95,
                                        100.01,
                                        500,
                                        500
                                )
                        )
                );

        assertEquals(
                "PTS_A",
                decision.allocations()
                        .getFirst()
                        .venue()
        );
    }

    @Test
    void routingSplitsAcrossVenueCapacity() {
        RoutingDecision decision =
                router().route(
                        Side.BUY,
                        150,
                        List.of(
                                event(
                                        "TSE",
                                        99.90,
                                        100.00,
                                        500,
                                        60
                                ),
                                event(
                                        "PTS_A",
                                        99.90,
                                        100.10,
                                        500,
                                        100
                                )
                        )
                );

        assertEquals(2, decision.allocations().size());

        assertEquals(
                List.of(60, 90),
                decision.allocations().stream()
                        .map(RouteAllocation::quantity)
                        .toList()
        );

        assertEquals(150, decision.routedQuantity());
        assertTrue(decision.fullyRouted());
    }

    @Test
    void queueDepthCapsRoutedQuantity() {
        RoutingDecision decision =
                router().route(
                        Side.BUY,
                        100,
                        List.of(
                                event(
                                        "TSE",
                                        99.90,
                                        100.00,
                                        1_000,
                                        25
                                )
                        )
                );

        assertEquals(25, decision.routedQuantity());
        assertEquals(75, decision.unallocatedQuantity());
        assertFalse(decision.fullyRouted());
    }

    @Test
    void venueParticipationLimitCapsCapacity() {
        SmartOrderRouter router =
                new SmartOrderRouter(
                        List.of(
                                configuration(
                                        "TSE",
                                        0.0,
                                        0.10,
                                        0.0
                                )
                        )
                );

        RoutingDecision decision =
                router.route(
                        Side.BUY,
                        200,
                        List.of(
                                event(
                                        "TSE",
                                        99.90,
                                        100.00,
                                        1_000,
                                        500
                                )
                        )
                );

        assertEquals(100, decision.routedQuantity());
        assertEquals(100, decision.unallocatedQuantity());
    }

    @Test
    void adverseSelectionPenaltyAffectsPriority() {
        SmartOrderRouter router =
                new SmartOrderRouter(
                        List.of(
                                configuration(
                                        "TSE",
                                        0.0,
                                        1.0,
                                        5.0
                                ),
                                configuration(
                                        "PTS_A",
                                        0.0,
                                        1.0,
                                        0.0
                                )
                        )
                );

        RoutingDecision decision =
                router.route(
                        Side.BUY,
                        100,
                        List.of(
                                event(
                                        "TSE",
                                        99.90,
                                        100.00,
                                        500,
                                        500
                                ),
                                event(
                                        "PTS_A",
                                        99.90,
                                        100.02,
                                        500,
                                        500
                                )
                        )
                );

        assertEquals(
                "PTS_A",
                decision.allocations()
                        .getFirst()
                        .venue()
        );
    }

    @Test
    void tiesUseVenueNameDeterministically() {
        RoutingDecision decision =
                router().route(
                        Side.BUY,
                        150,
                        List.of(
                                event(
                                        "VENUE_B",
                                        99.90,
                                        100.00,
                                        500,
                                        100
                                ),
                                event(
                                        "VENUE_A",
                                        99.90,
                                        100.00,
                                        500,
                                        100
                                )
                        )
                );

        assertEquals(
                List.of("VENUE_A", "VENUE_B"),
                decision.allocations().stream()
                        .map(RouteAllocation::venue)
                        .toList()
        );
    }

    @Test
    void rejectsInvalidOrMixedSnapshots() {
        SmartOrderRouter router = router();

        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(
                        Side.BUY,
                        0,
                        List.of(
                                event(
                                        "TSE",
                                        99.90,
                                        100.00,
                                        500,
                                        100
                                )
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(
                        Side.BUY,
                        100,
                        List.of(
                                event(
                                        1_000L,
                                        "JPXDEMO",
                                        "TSE"
                                ),
                                event(
                                        2_000L,
                                        "JPXDEMO",
                                        "PTS_A"
                                )
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(
                        Side.BUY,
                        100,
                        List.of(
                                event(
                                        1_000L,
                                        "AAA",
                                        "TSE"
                                ),
                                event(
                                        1_000L,
                                        "BBB",
                                        "PTS_A"
                                )
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(
                        Side.BUY,
                        100,
                        List.of(
                                event(
                                        1_000L,
                                        "AAA",
                                        "TSE"
                                ),
                                event(
                                        1_000L,
                                        "AAA",
                                        "TSE"
                                )
                        )
                )
        );
    }

    @Test
    void rejectsDuplicateConfigurations() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SmartOrderRouter(
                        List.of(
                                configuration(
                                        "TSE",
                                        0.0,
                                        1.0,
                                        0.0
                                ),
                                configuration(
                                        "TSE",
                                        1.0,
                                        0.5,
                                        1.0
                                )
                        )
                )
        );
    }

    private static SmartOrderRouter router() {
        return SmartOrderRouter.unconstrained();
    }

    private static VenueConfiguration configuration(
            String venue,
            double feeBps,
            double maxParticipation,
            double penaltyBps
    ) {
        return new VenueConfiguration(
                venue,
                feeBps,
                maxParticipation,
                penaltyBps
        );
    }

    private static MarketEvent event(
            String venue,
            double bid,
            double ask,
            long volume,
            long queueDepth
    ) {
        return new MarketEvent(
                1_000L,
                "JPXDEMO",
                venue,
                bid,
                ask,
                (bid + ask) / 2.0,
                volume,
                queueDepth
        );
    }

    private static MarketEvent event(
            long timestampMs,
            String symbol,
            String venue
    ) {
        return new MarketEvent(
                timestampMs,
                symbol,
                venue,
                99.90,
                100.00,
                99.95,
                500L,
                100L
        );
    }
}

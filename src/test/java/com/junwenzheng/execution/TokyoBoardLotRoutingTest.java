package com.junwenzheng.execution;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.routing.RouteAllocation;
import com.junwenzheng.execution.routing.RoutingDecision;
import com.junwenzheng.execution.routing.SmartOrderRouter;
import com.junwenzheng.execution.rules.TokyoEquityRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TokyoBoardLotRoutingTest {

    @Test
    void normalizesDesiredQuantityToBoardLots() {
        TokyoEquityRules rules =
                TokyoEquityRules.topix500();

        assertEquals(
                200,
                rules.normalizeBoardLotQuantity(
                        250,
                        1_000
                )
        );

        assertEquals(
                100,
                rules.normalizeBoardLotQuantity(
                        500,
                        150
                )
        );

        assertEquals(
                0,
                rules.normalizeBoardLotQuantity(
                        99,
                        1_000
                )
        );
    }

    @Test
    void routerExposesOnlyFullBoardLotCapacity() {
        RoutingDecision decision =
                SmartOrderRouter
                        .unconstrained()
                        .route(
                                Side.BUY,
                                300,
                                List.of(
                                        event(
                                                "TSE",
                                                100.00,
                                                150
                                        ),
                                        event(
                                                "PTS_A",
                                                100.10,
                                                250
                                        )
                                ),
                                100
                        );

        assertEquals(
                List.of(100, 200),
                decision.allocations()
                        .stream()
                        .map(
                                RouteAllocation::quantity
                        )
                        .toList()
        );

        assertEquals(300, decision.routedQuantity());
        assertEquals(0, decision.unallocatedQuantity());
    }

    @Test
    void routerRejectsNonLotRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SmartOrderRouter
                        .unconstrained()
                        .route(
                                Side.BUY,
                                250,
                                List.of(
                                        event(
                                                "TSE",
                                                100.00,
                                                500
                                        )
                                ),
                                100
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SmartOrderRouter
                        .unconstrained()
                        .route(
                                Side.BUY,
                                200,
                                List.of(
                                        event(
                                                "TSE",
                                                100.00,
                                                500
                                        )
                                ),
                                0
                        )
        );
    }

    private static MarketEvent event(
            String venue,
            double ask,
            long queueDepth
    ) {
        return new MarketEvent(
                1_000L,
                "7203",
                venue,
                ask - 0.5,
                ask,
                ask,
                1_000L,
                queueDepth
        );
    }
}

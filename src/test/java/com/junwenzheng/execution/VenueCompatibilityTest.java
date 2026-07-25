package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.FillOutcome;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class VenueCompatibilityTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void legacyRowsDefaultToPrimaryVenue()
            throws Exception {
        MarketEvent event =
                MarketDataReplay.fromCsv(
                        writeCsv(
                                "timestamp_ms,symbol,bid,ask,last,volume",
                                "1000,JPXDEMO,100.0,100.2,100.1,500"
                        )
                ).events().getFirst();

        assertEquals(
                MarketEvent.DEFAULT_VENUE,
                event.venue()
        );

        assertEquals(
                500L,
                event.queueDepth()
        );
    }

    @Test
    void eightColumnRowsParseVenueAndQueueDepth()
            throws Exception {
        MarketEvent event =
                MarketDataReplay.fromCsv(
                        writeCsv(
                                "timestamp_ms,symbol,venue,bid,ask,last,volume,queue_depth",
                                "1000,JPXDEMO,PTS_A,100.0,100.2,100.1,500,125"
                        )
                ).events().getFirst();

        assertEquals("PTS_A", event.venue());
        assertEquals(125L, event.queueDepth());

        assertEquals(
                MarketEventType.CONTINUOUS,
                event.type()
        );
    }

    @Test
    void nineColumnRowsParseExplicitEventType()
            throws Exception {
        MarketEvent event =
                MarketDataReplay.fromCsv(
                        writeCsv(
                                "timestamp_ms,symbol,venue,bid,ask,last,volume,queue_depth,event_type",
                                "1000,JPXDEMO,TSE,100.0,100.2,100.1,500,250,OPENING_AUCTION"
                        )
                ).events().getFirst();

        assertEquals("TSE", event.venue());

        assertEquals(
                MarketEventType.OPENING_AUCTION,
                event.type()
        );
    }

    @Test
    void replayCanFilterByVenue()
            throws Exception {
        MarketDataReplay replay =
                MarketDataReplay.fromCsv(
                        writeCsv(
                                "timestamp_ms,symbol,venue,bid,ask,last,volume,queue_depth",
                                "1000,JPXDEMO,TSE,100.0,100.2,100.1,500,250",
                                "1000,JPXDEMO,PTS_A,99.9,100.1,100.0,300,150"
                        )
                );

        MarketDataReplay filtered =
                replay.forVenue("PTS_A");

        assertEquals(1, filtered.events().size());

        assertEquals(
                "PTS_A",
                filtered.events()
                        .getFirst()
                        .venue()
        );
    }

    @Test
    void childCompatibilityConstructorUsesPrimaryVenue() {
        ChildOrder child =
                new ChildOrder(
                        "parent-1",
                        "JPXDEMO",
                        Side.BUY,
                        100,
                        1_000L,
                        "legacy child"
                );

        assertEquals(
                MarketEvent.DEFAULT_VENUE,
                child.venue()
        );
    }

    @Test
    void routedChildRetainsDestinationVenue() {
        ChildOrder child =
                ChildOrder.routed(
                        "parent-1",
                        "JPXDEMO",
                        "PTS_A",
                        Side.BUY,
                        100,
                        1_000L,
                        "smart-order route"
                );

        assertEquals("PTS_A", child.venue());
    }

    @Test
    void fillModelPropagatesVenueToFill() {
        ChildOrder child =
                ChildOrder.routed(
                        "parent-1",
                        "JPXDEMO",
                        "PTS_A",
                        Side.BUY,
                        100,
                        1_000L,
                        "smart-order route"
                );

        child.acknowledge(1_000L);

        FillOutcome outcome =
                new FillModel(
                        1.0,
                        0.0,
                        0.0
                ).tryFill(
                        child,
                        event("PTS_A"),
                        "SOR",
                        1_000L
                );

        Fill fill =
                ((FillOutcome.Filled) outcome)
                        .fill();

        assertEquals("PTS_A", fill.venue());
    }

    @Test
    void fillModelRejectsVenueMismatch() {
        ChildOrder child =
                ChildOrder.routed(
                        "parent-1",
                        "JPXDEMO",
                        "PTS_A",
                        Side.BUY,
                        100,
                        1_000L,
                        "smart-order route"
                );

        child.acknowledge(1_000L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new FillModel(
                        1.0,
                        0.0,
                        0.0
                ).tryFill(
                        child,
                        event("PTS_B"),
                        "SOR",
                        1_000L
                )
        );
    }

    @Test
    void marketEventRejectsInvalidVenueAndQueueDepth() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketEvent(
                        1_000L,
                        "JPXDEMO",
                        " ",
                        100.0,
                        100.2,
                        100.1,
                        500L,
                        100L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketEvent(
                        1_000L,
                        "JPXDEMO",
                        "TSE",
                        100.0,
                        100.2,
                        100.1,
                        500L,
                        -1L
                )
        );
    }

    private Path writeCsv(
            String... rows
    ) throws Exception {
        Path path =
                temporaryDirectory.resolve(
                        "venue_market_data.csv"
                );

        Files.writeString(
                path,
                String.join(
                        System.lineSeparator(),
                        rows
                ) + System.lineSeparator()
        );

        return path;
    }

    private static MarketEvent event(
            String venue
    ) {
        return new MarketEvent(
                1_000L,
                "JPXDEMO",
                venue,
                100.0,
                100.2,
                100.1,
                1_000L,
                500L
        );
    }
}

package com.junwenzheng.execution;

import com.junwenzheng.execution.market.MarketDataParseException;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarketDataReplayTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void sixColumnRowsDefaultToContinuousEvents()
            throws Exception {
        Path csv = writeCsv(
                "timestamp_ms,symbol,bid,ask,last,volume",
                "1000,JPXDEMO,100.0,100.2,100.1,500",
                "2000,JPXDEMO,100.1,100.3,100.2,600"
        );

        MarketDataReplay replay =
                MarketDataReplay.fromCsv(csv);

        assertEquals(2, replay.events().size());
        assertEquals(
                MarketEventType.CONTINUOUS,
                replay.events().getFirst().type()
        );
        assertEquals(
                0L,
                replay.events().getFirst().sourceSequence()
        );
        assertEquals(
                1L,
                replay.events().getLast().sourceSequence()
        );
    }

    @Test
    void sevenColumnRowsParseExplicitEventTypes()
            throws Exception {
        Path csv = writeCsv(
                "timestamp_ms,symbol,bid,ask,last,volume,event_type",
                "1000,JPXDEMO,100.0,100.2,100.1,500,OPENING_AUCTION",
                "2000,JPXDEMO,100.1,100.3,100.2,600,CLOSING_AUCTION"
        );

        MarketDataReplay replay =
                MarketDataReplay.fromCsv(csv);

        assertEquals(
                MarketEventType.OPENING_AUCTION,
                replay.events().getFirst().type()
        );
        assertEquals(
                MarketEventType.CLOSING_AUCTION,
                replay.events().getLast().type()
        );
    }

    @Test
    void equalTimestampsPreserveSourceOrdering()
            throws Exception {
        Path csv = writeCsv(
                "timestamp_ms,symbol,bid,ask,last,volume",
                "2000,LATE,100.0,100.2,100.1,500",
                "1000,FIRST,100.0,100.2,100.1,500",
                "1000,SECOND,100.0,100.2,100.1,500"
        );

        MarketDataReplay replay =
                MarketDataReplay.fromCsv(csv);

        assertEquals(
                List.of("FIRST", "SECOND", "LATE"),
                replay.events().stream()
                        .map(MarketEvent::symbol)
                        .toList()
        );

        assertEquals(
                List.of(1L, 2L, 0L),
                replay.events().stream()
                        .map(MarketEvent::sourceSequence)
                        .toList()
        );
    }

    @Test
    void malformedRowsReportSourceLine()
            throws Exception {
        Path csv = writeCsv(
                "timestamp_ms,symbol,bid,ask,last,volume",
                "1000,JPXDEMO,100.0,100.2,100.1,500",
                "2000,JPXDEMO,100.1"
        );

        MarketDataParseException exception =
                assertThrows(
                        MarketDataParseException.class,
                        () -> MarketDataReplay.fromCsv(csv)
                );

        assertEquals(csv, exception.path());
        assertEquals(3, exception.lineNumber());
        assertTrue(
                exception.getMessage().contains(
                        "expected 6 or 7 columns"
                )
        );
    }

    @Test
    void crossedQuotesReportSourceLine()
            throws Exception {
        Path csv = writeCsv(
                "timestamp_ms,symbol,bid,ask,last,volume",
                "1000,JPXDEMO,101.0,100.0,100.5,500"
        );

        MarketDataParseException exception =
                assertThrows(
                        MarketDataParseException.class,
                        () -> MarketDataReplay.fromCsv(csv)
                );

        assertEquals(2, exception.lineNumber());
        assertTrue(
                exception.getMessage().contains(
                        "ask cannot be below bid"
                )
        );
    }

    @Test
    void nonFinitePricesReportSourceLine()
            throws Exception {
        Path csv = writeCsv(
                "timestamp_ms,symbol,bid,ask,last,volume",
                "1000,JPXDEMO,NaN,100.2,100.1,500"
        );

        MarketDataParseException exception =
                assertThrows(
                        MarketDataParseException.class,
                        () -> MarketDataReplay.fromCsv(csv)
                );

        assertEquals(2, exception.lineNumber());
        assertTrue(
                exception.getMessage().contains(
                        "prices must be finite"
                )
        );
    }

    @Test
    void replaySupportsComposableFiltering() {
        MarketDataReplay replay =
                MarketDataReplay.of(
                        List.of(
                                event(
                                        1_000,
                                        0,
                                        MarketEventType.OPENING_AUCTION,
                                        "AAA"
                                ),
                                event(
                                        2_000,
                                        1,
                                        MarketEventType.CONTINUOUS,
                                        "AAA"
                                ),
                                event(
                                        3_000,
                                        2,
                                        MarketEventType.CONTINUOUS,
                                        "BBB"
                                ),
                                event(
                                        4_000,
                                        3,
                                        MarketEventType.CLOSING_AUCTION,
                                        "AAA"
                                )
                        )
                );

        MarketDataReplay filtered =
                replay.forSymbol("AAA")
                        .during(1_500, 3_500)
                        .ofType(
                                MarketEventType.CONTINUOUS
                        );

        assertEquals(1, filtered.events().size());
        assertEquals(
                2_000L,
                filtered.events()
                        .getFirst()
                        .timestampMs()
        );

        assertEquals(4, replay.events().size());
    }

    @Test
    void replayEventListsAreImmutable() {
        MarketDataReplay replay =
                MarketDataReplay.of(
                        List.of(
                                event(
                                        1_000,
                                        0,
                                        MarketEventType.CONTINUOUS,
                                        "AAA"
                                )
                        )
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> replay.events().add(
                        event(
                                2_000,
                                1,
                                MarketEventType.CONTINUOUS,
                                "AAA"
                        )
                )
        );
    }

    private Path writeCsv(
            String... lines
    ) throws Exception {
        Path path = temporaryDirectory.resolve(
                "market_data.csv"
        );

        Files.writeString(
                path,
                String.join(
                        System.lineSeparator(),
                        lines
                ) + System.lineSeparator()
        );

        return path;
    }

    private static MarketEvent event(
            long timestampMs,
            long sourceSequence,
            MarketEventType type,
            String symbol
    ) {
        return new MarketEvent(
                timestampMs,
                sourceSequence,
                type,
                symbol,
                100.0,
                100.2,
                100.1,
                500L
        );
    }
}

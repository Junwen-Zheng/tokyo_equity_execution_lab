package com.junwenzheng.execution.market;

import com.junwenzheng.execution.util.Csv;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class MarketDataReplay
        implements Iterable<MarketEvent> {

    private static final Comparator<MarketEvent>
            EVENT_ORDER =
            Comparator.comparingLong(
                    MarketEvent::timestampMs
            ).thenComparingLong(
                    MarketEvent::sourceSequence
            );

    private final List<MarketEvent> events;

    private MarketDataReplay(
            List<MarketEvent> events
    ) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException(
                    "market replay cannot be empty"
            );
        }

        List<MarketEvent> ordered =
                new ArrayList<>(events);

        ordered.sort(EVENT_ORDER);
        this.events = List.copyOf(ordered);
    }

    public static MarketDataReplay of(
            List<MarketEvent> events
    ) {
        if (events == null) {
            throw new IllegalArgumentException(
                    "events are required"
            );
        }

        return new MarketDataReplay(events);
    }

    public static MarketDataReplay fromCsv(
            Path path
    ) throws IOException {
        List<MarketEvent> events =
                new ArrayList<>();

        long sourceSequence = 0L;

        for (
                Csv.Row row :
                Csv.readNumberedRows(path)
        ) {
            try {
                events.add(
                        parseRow(
                                row,
                                sourceSequence
                        )
                );
            } catch (RuntimeException exception) {
                throw new MarketDataParseException(
                        path,
                        row.lineNumber(),
                        exception.getMessage(),
                        exception
                );
            }

            sourceSequence++;
        }

        return new MarketDataReplay(events);
    }

    private static MarketEvent parseRow(
            Csv.Row row,
            long sourceSequence
    ) {
        List<String> values = row.values();

        if (
                values.size() != 6
                        && values.size() != 7
                        && values.size() != 8
                        && values.size() != 9
        ) {
            throw new IllegalArgumentException(
                    "expected 6 or 7 columns "
                            + "for legacy rows, or 8 or 9 "
                            + "columns for venue rows, got "
                            + values.size()
            );
        }

        if (values.size() <= 7) {
            MarketEventType type =
                    values.size() == 7
                            ? parseType(values.get(6))
                            : MarketEventType.CONTINUOUS;

            long volume =
                    Long.parseLong(
                            values.get(5).trim()
                    );

            return new MarketEvent(
                    Long.parseLong(
                            values.get(0).trim()
                    ),
                    sourceSequence,
                    type,
                    values.get(1).trim(),
                    MarketEvent.DEFAULT_VENUE,
                    Double.parseDouble(
                            values.get(2).trim()
                    ),
                    Double.parseDouble(
                            values.get(3).trim()
                    ),
                    Double.parseDouble(
                            values.get(4).trim()
                    ),
                    volume,
                    volume
            );
        }

        MarketEventType type =
                values.size() == 9
                        ? parseType(values.get(8))
                        : MarketEventType.CONTINUOUS;

        return new MarketEvent(
                Long.parseLong(
                        values.get(0).trim()
                ),
                sourceSequence,
                type,
                values.get(1).trim(),
                values.get(2).trim(),
                Double.parseDouble(
                        values.get(3).trim()
                ),
                Double.parseDouble(
                        values.get(4).trim()
                ),
                Double.parseDouble(
                        values.get(5).trim()
                ),
                Long.parseLong(
                        values.get(6).trim()
                ),
                Long.parseLong(
                        values.get(7).trim()
                )
        );
    }

    private static MarketEventType parseType(
            String rawType
    ) {
        String normalized =
                rawType.trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "event type is required"
            );
        }

        try {
            return MarketEventType.valueOf(
                    normalized
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "unsupported event type: "
                            + rawType.trim(),
                    exception
            );
        }
    }

    public MarketDataReplay forSymbol(
            String symbol
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "symbol is required"
            );
        }

        String normalized = symbol.trim();

        return filtered(
                event -> event.symbol()
                        .equals(normalized),
                "no events for symbol "
                        + normalized
        );
    }

    public MarketDataReplay forVenue(
            String venue
    ) {
        if (venue == null || venue.isBlank()) {
            throw new IllegalArgumentException(
                    "venue is required"
            );
        }

        String normalized = venue.trim();

        return filtered(
                event -> event.venue()
                        .equals(normalized),
                "no events for venue "
                        + normalized
        );
    }

    public MarketDataReplay during(
            long startInclusiveMs,
            long endExclusiveMs
    ) {
        if (
                startInclusiveMs < 0
                        || endExclusiveMs
                        <= startInclusiveMs
        ) {
            throw new IllegalArgumentException(
                    "invalid replay window"
            );
        }

        return filtered(
                event ->
                        event.timestampMs()
                                >= startInclusiveMs
                                && event.timestampMs()
                                < endExclusiveMs,
                "no events in replay window"
        );
    }

    public MarketDataReplay ofType(
            MarketEventType type
    ) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "type is required"
            );
        }

        return filtered(
                event -> event.type() == type,
                "no events of type " + type
        );
    }

    private MarketDataReplay filtered(
            Predicate<MarketEvent> predicate,
            String emptyMessage
    ) {
        List<MarketEvent> filtered =
                events.stream()
                        .filter(predicate)
                        .toList();

        if (filtered.isEmpty()) {
            throw new IllegalArgumentException(
                    emptyMessage
            );
        }

        return new MarketDataReplay(filtered);
    }

    public List<MarketEvent> events() {
        return events;
    }

    public long totalVolume() {
        return events.stream()
                .mapToLong(MarketEvent::volume)
                .sum();
    }

    public double vwap() {
        double notional = 0.0;
        long volume = 0L;

        for (MarketEvent event : events) {
            notional +=
                    event.last()
                            * event.volume();

            volume += event.volume();
        }

        return volume == 0L
                ? events.getLast().mid()
                : notional / volume;
    }

    @Override
    public Iterator<MarketEvent> iterator() {
        return events.iterator();
    }
}

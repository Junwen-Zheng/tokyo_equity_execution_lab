package com.junwenzheng.execution.market;

import com.junwenzheng.execution.util.Csv;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class MarketDataReplay implements Iterable<MarketEvent> {
    private final List<MarketEvent> events;

    private MarketDataReplay(List<MarketEvent> events) {
        if (events.isEmpty()) throw new IllegalArgumentException("market replay cannot be empty");
        this.events = List.copyOf(events);
    }

    public static MarketDataReplay fromCsv(Path path) throws IOException {
        List<MarketEvent> events = new ArrayList<>();
        for (String[] row : Csv.readRows(path)) {
            if (row.length != 6) {
                throw new IllegalArgumentException("expected 6 columns, got " + row.length);
            }
            events.add(new MarketEvent(
                    Long.parseLong(row[0].trim()),
                    row[1].trim(),
                    Double.parseDouble(row[2].trim()),
                    Double.parseDouble(row[3].trim()),
                    Double.parseDouble(row[4].trim()),
                    Long.parseLong(row[5].trim())
            ));
        }
        events.sort(Comparator.comparingLong(MarketEvent::timestampMs));
        return new MarketDataReplay(events);
    }

    public List<MarketEvent> events() {
        return events;
    }

    public long totalVolume() {
        return events.stream().mapToLong(MarketEvent::volume).sum();
    }

    public double vwap() {
        double notional = 0.0;
        long volume = 0;
        for (MarketEvent event : events) {
            notional += event.last() * event.volume();
            volume += event.volume();
        }
        return volume == 0 ? events.getLast().mid() : notional / volume;
    }

    @Override
    public Iterator<MarketEvent> iterator() {
        return events.iterator();
    }
}

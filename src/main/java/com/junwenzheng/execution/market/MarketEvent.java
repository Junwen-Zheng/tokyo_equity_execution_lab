package com.junwenzheng.execution.market;

public record MarketEvent(
        long timestampMs,
        String symbol,
        double bid,
        double ask,
        double last,
        long volume
) {
    public MarketEvent {
        if (timestampMs < 0) throw new IllegalArgumentException("timestampMs must be non-negative");
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        if (bid <= 0 || ask <= 0 || last <= 0) throw new IllegalArgumentException("prices must be positive");
        if (ask < bid) throw new IllegalArgumentException("ask cannot be below bid");
        if (volume < 0) throw new IllegalArgumentException("volume must be non-negative");
    }

    public double mid() {
        return (bid + ask) / 2.0;
    }

    public double spreadBps() {
        return ((ask - bid) / mid()) * 10_000.0;
    }
}

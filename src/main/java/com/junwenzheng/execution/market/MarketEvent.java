package com.junwenzheng.execution.market;

public record MarketEvent(
        long timestampMs,
        long sourceSequence,
        MarketEventType type,
        String symbol,
        double bid,
        double ask,
        double last,
        long volume
) {
    public MarketEvent {
        if (timestampMs < 0) {
            throw new IllegalArgumentException(
                    "timestampMs must be non-negative"
            );
        }

        if (sourceSequence < 0) {
            throw new IllegalArgumentException(
                    "sourceSequence must be non-negative"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "type is required"
            );
        }

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "symbol is required"
            );
        }

        symbol = symbol.trim();

        if (
                !Double.isFinite(bid)
                        || !Double.isFinite(ask)
                        || !Double.isFinite(last)
        ) {
            throw new IllegalArgumentException(
                    "prices must be finite"
            );
        }

        if (bid <= 0.0 || ask <= 0.0 || last <= 0.0) {
            throw new IllegalArgumentException(
                    "prices must be positive"
            );
        }

        if (ask < bid) {
            throw new IllegalArgumentException(
                    "ask cannot be below bid"
            );
        }

        if (volume < 0) {
            throw new IllegalArgumentException(
                    "volume must be non-negative"
            );
        }
    }

    public MarketEvent(
            long timestampMs,
            String symbol,
            double bid,
            double ask,
            double last,
            long volume
    ) {
        this(
                timestampMs,
                0L,
                MarketEventType.CONTINUOUS,
                symbol,
                bid,
                ask,
                last,
                volume
        );
    }

    public double mid() {
        return (bid + ask) / 2.0;
    }

    public double spreadBps() {
        return (
                (ask - bid)
                        / mid()
        ) * 10_000.0;
    }
}

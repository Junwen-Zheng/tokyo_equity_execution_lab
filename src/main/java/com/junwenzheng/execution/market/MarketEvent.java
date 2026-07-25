package com.junwenzheng.execution.market;

public record MarketEvent(
        long timestampMs,
        long sourceSequence,
        MarketEventType type,
        String symbol,
        String venue,
        double bid,
        double ask,
        double last,
        long volume,
        long queueDepth
) {
    public static final String DEFAULT_VENUE =
            "PRIMARY";

    public MarketEvent {
        if (timestampMs < 0L) {
            throw new IllegalArgumentException(
                    "timestampMs must be non-negative"
            );
        }

        if (sourceSequence < 0L) {
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

        if (venue == null || venue.isBlank()) {
            throw new IllegalArgumentException(
                    "venue is required"
            );
        }

        symbol = symbol.trim();
        venue = venue.trim();

        if (
                !Double.isFinite(bid)
                        || !Double.isFinite(ask)
                        || !Double.isFinite(last)
        ) {
            throw new IllegalArgumentException(
                    "prices must be finite"
            );
        }

        if (
                bid <= 0.0
                        || ask <= 0.0
                        || last <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "prices must be positive"
            );
        }

        if (ask < bid) {
            throw new IllegalArgumentException(
                    "ask cannot be below bid"
            );
        }

        if (volume < 0L) {
            throw new IllegalArgumentException(
                    "volume must be non-negative"
            );
        }

        if (queueDepth < 0L) {
            throw new IllegalArgumentException(
                    "queueDepth must be non-negative"
            );
        }
    }

    public MarketEvent(
            long timestampMs,
            long sourceSequence,
            MarketEventType type,
            String symbol,
            double bid,
            double ask,
            double last,
            long volume
    ) {
        this(
                timestampMs,
                sourceSequence,
                type,
                symbol,
                DEFAULT_VENUE,
                bid,
                ask,
                last,
                volume,
                volume
        );
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
                DEFAULT_VENUE,
                bid,
                ask,
                last,
                volume,
                volume
        );
    }

    public MarketEvent(
            long timestampMs,
            String symbol,
            String venue,
            double bid,
            double ask,
            double last,
            long volume,
            long queueDepth
    ) {
        this(
                timestampMs,
                0L,
                MarketEventType.CONTINUOUS,
                symbol,
                venue,
                bid,
                ask,
                last,
                volume,
                queueDepth
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

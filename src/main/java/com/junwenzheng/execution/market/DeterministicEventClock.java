package com.junwenzheng.execution.market;

public final class DeterministicEventClock {
    private boolean initialized;
    private long currentTimeMs;

    public long advanceTo(MarketEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "event is required"
            );
        }

        long nextTimeMs = event.timestampMs();

        if (
                initialized
                        && nextTimeMs < currentTimeMs
        ) {
            throw new IllegalStateException(
                    "event time cannot move backwards"
            );
        }

        initialized = true;
        currentTimeMs = nextTimeMs;

        return currentTimeMs;
    }

    public boolean initialized() {
        return initialized;
    }

    public long currentTimeMs() {
        if (!initialized) {
            throw new IllegalStateException(
                    "clock has not been initialized"
            );
        }

        return currentTimeMs;
    }
}

package com.junwenzheng.execution.algo;

public record ReplayProgress(
        int eventIndex,
        int eventCount,
        long cumulativeVolume,
        long totalVolume
) {
    public ReplayProgress {
        if (eventCount <= 0) {
            throw new IllegalArgumentException(
                    "eventCount must be positive"
            );
        }

        if (
                eventIndex < 0
                        || eventIndex >= eventCount
        ) {
            throw new IllegalArgumentException(
                    "eventIndex must be within replay bounds"
            );
        }

        if (cumulativeVolume < 0L) {
            throw new IllegalArgumentException(
                    "cumulativeVolume must be non-negative"
            );
        }

        if (totalVolume < 0L) {
            throw new IllegalArgumentException(
                    "totalVolume must be non-negative"
            );
        }

        if (cumulativeVolume > totalVolume) {
            throw new IllegalArgumentException(
                    "cumulativeVolume cannot exceed totalVolume"
            );
        }
    }

    public int eventsProcessed() {
        return eventIndex + 1;
    }

    public double progressFraction() {
        return (double) eventsProcessed()
                / eventCount;
    }

    public double oracleVolumeFraction() {
        if (totalVolume == 0L) {
            return progressFraction();
        }

        return Math.min(
                1.0,
                (double) cumulativeVolume
                        / totalVolume
        );
    }

    public double observedVolumeFraction(
            long forecastTotalVolume
    ) {
        if (forecastTotalVolume <= 0L) {
            throw new IllegalArgumentException(
                    "forecastTotalVolume must be positive"
            );
        }

        return Math.min(
                1.0,
                (double) cumulativeVolume
                        / forecastTotalVolume
        );
    }

    public double volumeFraction() {
        return oracleVolumeFraction();
    }
}

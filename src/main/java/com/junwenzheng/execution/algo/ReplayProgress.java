package com.junwenzheng.execution.algo;

public record ReplayProgress(
        int eventIndex,
        int eventCount,
        long cumulativeVolume,
        long totalVolume
) {
    public double progressFraction() {
        if (eventCount <= 1) return 1.0;
        return (double) (eventIndex + 1) / eventCount;
    }

    public double volumeFraction() {
        if (totalVolume == 0) return progressFraction();
        return Math.min(1.0, (double) cumulativeVolume / totalVolume);
    }
}

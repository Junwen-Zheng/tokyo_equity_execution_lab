package com.junwenzheng.execution.metrics;

import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;

import java.util.List;

public record MicrostructureDiagnostics(
        int eventCount,
        double averageSpreadBps,
        double maxSpreadBps,
        double averageEventVolume,
        double midpointVolatilityBps
) {
    public static MicrostructureDiagnostics from(MarketDataReplay replay) {
        List<MarketEvent> events = replay.events();
        double spreadSum = 0.0;
        double maxSpread = 0.0;
        double volumeSum = 0.0;
        double squaredMidpointReturnSum = 0.0;
        int returnCount = 0;
        double previousMid = events.getFirst().mid();
        for (MarketEvent event : events) {
            double spread = event.spreadBps();
            spreadSum += spread;
            maxSpread = Math.max(maxSpread, spread);
            volumeSum += event.volume();
            double mid = event.mid();
            if (previousMid > 0.0 && mid > 0.0 && mid != previousMid) {
                double retBps = Math.log(mid / previousMid) * 10_000.0;
                squaredMidpointReturnSum += retBps * retBps;
                returnCount++;
            }
            previousMid = mid;
        }
        double volatility = returnCount == 0 ? 0.0 : Math.sqrt(squaredMidpointReturnSum / returnCount);
        return new MicrostructureDiagnostics(
                events.size(),
                spreadSum / events.size(),
                maxSpread,
                volumeSum / events.size(),
                volatility
        );
    }

    public String toMarkdown() {
        return String.format(java.util.Locale.US,
                "| Events | Avg Spread (bps) | Max Spread (bps) | Avg Event Volume | Midpoint Volatility (bps) |%n" +
                "|---:|---:|---:|---:|---:|%n" +
                "| %d | %.2f | %.2f | %.2f | %.2f |%n",
                eventCount, averageSpreadBps, maxSpreadBps, averageEventVolume, midpointVolatilityBps);
    }
}

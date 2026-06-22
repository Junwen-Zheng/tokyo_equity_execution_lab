package com.junwenzheng.execution.metrics;

import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.Side;

public record ExecutionMetrics(
        String strategy,
        int parentQuantity,
        int filledQuantity,
        double fillRate,
        double averagePrice,
        double marketVwap,
        double arrivalPrice,
        double slippageVsArrivalBps,
        double slippageVsVwapBps,
        int rejectedChildren
) {
    public static ExecutionMetrics from(SimulationResult result) {
        int filled = result.fills().stream().mapToInt(Fill::quantity).sum();
        double notional = result.fills().stream().mapToDouble(Fill::notional).sum();
        double avgPrice = filled == 0 ? 0.0 : notional / filled;
        double marketVwap = result.replay().vwap();
        double arrival = result.parentOrder().arrivalPrice();
        int sideSign = result.parentOrder().side() == Side.BUY ? 1 : -1;
        double arrivalSlip = filled == 0 ? 0.0 : sideSign * ((avgPrice - arrival) / arrival) * 10_000.0;
        double vwapSlip = filled == 0 ? 0.0 : sideSign * ((avgPrice - marketVwap) / marketVwap) * 10_000.0;
        return new ExecutionMetrics(
                result.strategyName(),
                result.parentOrder().quantity(),
                filled,
                (double) filled / result.parentOrder().quantity(),
                avgPrice,
                marketVwap,
                arrival,
                arrivalSlip,
                vwapSlip,
                result.rejectedChildren()
        );
    }

    public String toCsvRow() {
        return String.format(java.util.Locale.US, "%s,%d,%d,%.4f,%.6f,%.6f,%.6f,%.4f,%.4f,%d",
                strategy, parentQuantity, filledQuantity, fillRate, averagePrice, marketVwap,
                arrivalPrice, slippageVsArrivalBps, slippageVsVwapBps, rejectedChildren);
    }
}

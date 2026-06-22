package com.junwenzheng.execution.metrics;

import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.Side;

import java.util.List;

public final class ImplementationShortfall {
    private ImplementationShortfall() {}

    public static double averageFillPrice(List<Fill> fills) {
        int filledQuantity = fills.stream().mapToInt(Fill::quantity).sum();
        if (filledQuantity == 0) return 0.0;
        double notional = fills.stream().mapToDouble(Fill::notional).sum();
        return notional / filledQuantity;
    }

    public static double bps(List<Fill> fills, Side side, double arrivalPrice) {
        if (arrivalPrice <= 0.0) throw new IllegalArgumentException("arrivalPrice must be positive");
        double avgPrice = averageFillPrice(fills);
        if (avgPrice == 0.0) return 0.0;
        int sideSign = side == Side.BUY ? 1 : -1;
        return sideSign * ((avgPrice - arrivalPrice) / arrivalPrice) * 10_000.0;
    }
}

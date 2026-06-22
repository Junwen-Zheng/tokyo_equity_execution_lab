package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.order.Fill;

import java.util.HashMap;
import java.util.Map;

public final class PositionTracker {
    private final Map<String, Integer> positions = new HashMap<>();

    public void apply(Fill fill) {
        int signedQuantity = fill.side().sign() * fill.quantity();
        positions.merge(fill.symbol(), signedQuantity, Integer::sum);
    }

    public int position(String symbol) {
        return positions.getOrDefault(symbol, 0);
    }
}

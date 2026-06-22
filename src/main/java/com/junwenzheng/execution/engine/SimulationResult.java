package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;

import java.util.List;

public record SimulationResult(
        String strategyName,
        ParentOrder parentOrder,
        MarketDataReplay replay,
        List<Fill> fills,
        int rejectedChildren
) {}

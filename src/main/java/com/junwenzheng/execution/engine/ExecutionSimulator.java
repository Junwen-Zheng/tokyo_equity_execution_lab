package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.ExecutionDecision;
import com.junwenzheng.execution.algo.ReplayProgress;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;

import java.util.ArrayList;
import java.util.List;

public final class ExecutionSimulator {
    private final RiskManager riskManager;
    private final FillModel fillModel;

    public ExecutionSimulator(RiskManager riskManager, FillModel fillModel) {
        this.riskManager = riskManager;
        this.fillModel = fillModel;
    }

    public SimulationResult run(ParentOrder parentOrder, MarketDataReplay replay, ExecutionAlgorithm algorithm) {
        parentOrder.markWorking();
        List<Fill> fills = new ArrayList<>();
        PositionTracker positions = new PositionTracker();
        long cumulativeVolume = 0;
        int rejectedChildren = 0;
        List<MarketEvent> events = replay.events();
        for (int i = 0; i < events.size() && parentOrder.remainingQuantity() > 0; i++) {
            MarketEvent event = events.get(i);
            cumulativeVolume += event.volume();
            ReplayProgress progress = new ReplayProgress(i, events.size(), cumulativeVolume, replay.totalVolume());
            ExecutionDecision decision = algorithm.onEvent(parentOrder, event, progress);
            if (!decision.shouldTrade()) continue;
            ChildOrder childOrder = new ChildOrder(
                    parentOrder.orderId(),
                    parentOrder.symbol(),
                    parentOrder.side(),
                    Math.min(decision.childQuantity(), parentOrder.remainingQuantity()),
                    event.timestampMs(),
                    decision.reason()
            );
            if (!riskManager.isAllowed(childOrder, event.mid())) {
                rejectedChildren++;
                continue;
            }
            Fill fill = fillModel.tryFill(childOrder, event, algorithm.name());
            fills.add(fill);
            parentOrder.applyFill(fill.quantity());
            positions.apply(fill);
        }
        return new SimulationResult(algorithm.name(), parentOrder, replay, List.copyOf(fills), rejectedChildren);
    }
}

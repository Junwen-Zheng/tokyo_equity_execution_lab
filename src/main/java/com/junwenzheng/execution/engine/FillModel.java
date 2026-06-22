package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.Side;

public final class FillModel {
    private final double maxParticipation;
    private final double impactBpsPerTenPctParticipation;

    public FillModel(double maxParticipation, double impactBpsPerTenPctParticipation) {
        if (maxParticipation <= 0 || maxParticipation > 1) throw new IllegalArgumentException("maxParticipation must be in (0, 1]");
        this.maxParticipation = maxParticipation;
        this.impactBpsPerTenPctParticipation = impactBpsPerTenPctParticipation;
    }

    public Fill tryFill(ChildOrder childOrder, MarketEvent event, String strategyName) {
        int maxFill = Math.max(1, (int) Math.floor(event.volume() * maxParticipation));
        int filledQty = Math.min(childOrder.quantity(), maxFill);
        double eventParticipation = event.volume() == 0 ? 0.0 : (double) filledQty / event.volume();
        double impactBps = (eventParticipation / 0.10) * impactBpsPerTenPctParticipation;
        double impactMultiplier = impactBps / 10_000.0;
        double price = childOrder.side() == Side.BUY
                ? event.ask() * (1.0 + impactMultiplier)
                : event.bid() * (1.0 - impactMultiplier);
        return new Fill(
                childOrder.parentOrderId(),
                childOrder.symbol(),
                childOrder.side(),
                filledQty,
                price,
                event.timestampMs(),
                strategyName,
                childOrder.reason()
        );
    }
}

package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ChildOrderStatus;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.Side;

public final class FillModel {
    private final double maxParticipation;
    private final double queueAheadFraction;
    private final double impactBpsPerTenPctParticipation;

    public FillModel(
            double maxParticipation,
            double impactBpsPerTenPctParticipation
    ) {
        this(
                maxParticipation,
                0.0,
                impactBpsPerTenPctParticipation
        );
    }

    public FillModel(
            double maxParticipation,
            double queueAheadFraction,
            double impactBpsPerTenPctParticipation
    ) {
        if (
                !Double.isFinite(maxParticipation)
                        || maxParticipation <= 0.0
                        || maxParticipation > 1.0
        ) {
            throw new IllegalArgumentException(
                    "maxParticipation must be in (0, 1]"
            );
        }

        if (
                !Double.isFinite(queueAheadFraction)
                        || queueAheadFraction < 0.0
                        || queueAheadFraction > 1.0
        ) {
            throw new IllegalArgumentException(
                    "queueAheadFraction must be in [0, 1]"
            );
        }

        if (
                !Double.isFinite(
                        impactBpsPerTenPctParticipation
                )
                        || impactBpsPerTenPctParticipation
                        < 0.0
        ) {
            throw new IllegalArgumentException(
                    "impact coefficient must be finite "
                            + "and non-negative"
            );
        }

        this.maxParticipation = maxParticipation;
        this.queueAheadFraction = queueAheadFraction;
        this.impactBpsPerTenPctParticipation =
                impactBpsPerTenPctParticipation;
    }

    public FillOutcome tryFill(
            ChildOrder childOrder,
            MarketEvent event,
            String strategyName
    ) {
        validateInputs(
                childOrder,
                event,
                strategyName
        );

        long participationCap =
                calculateParticipationCap(
                        event.volume()
                );

        long queueAheadQuantity =
                (long) Math.floor(
                        participationCap
                                * queueAheadFraction
                );

        long executableLiquidity =
                participationCap
                        - queueAheadQuantity;

        if (event.volume() == 0L) {
            return new FillOutcome.NoFill(
                    "event volume is zero",
                    0L,
                    0L,
                    0L
            );
        }

        if (participationCap == 0L) {
            return new FillOutcome.NoFill(
                    "participation cap rounded to zero",
                    0L,
                    0L,
                    0L
            );
        }

        if (executableLiquidity == 0L) {
            return new FillOutcome.NoFill(
                    "queue consumed executable liquidity",
                    participationCap,
                    queueAheadQuantity,
                    0L
            );
        }

        int filledQuantity =
                Math.toIntExact(
                        Math.min(
                                childOrder
                                        .remainingQuantity(),
                                executableLiquidity
                        )
                );

        double referenceMidPrice =
                event.mid();

        double spreadCostBps =
                spreadCostBps(
                        childOrder.side(),
                        event,
                        referenceMidPrice
                );

        double eventParticipation =
                (double) filledQuantity
                        / event.volume();

        double impactCostBps =
                (
                        eventParticipation
                                / 0.10
                ) * impactBpsPerTenPctParticipation;

        double totalCostMultiplier =
                (
                        spreadCostBps
                                + impactCostBps
                ) / 10_000.0;

        double price =
                referenceMidPrice
                        * (
                        1.0
                                + childOrder
                                .side()
                                .sign()
                                * totalCostMultiplier
                );

        if (
                !Double.isFinite(price)
                        || price <= 0.0
        ) {
            throw new IllegalStateException(
                    "fill model produced an invalid price"
            );
        }

        Fill fill = new Fill(
                childOrder.childOrderId(),
                childOrder.parentOrderId(),
                childOrder.symbol(),
                childOrder.side(),
                filledQuantity,
                price,
                referenceMidPrice,
                spreadCostBps,
                impactCostBps,
                event.timestampMs(),
                strategyName,
                childOrder.reason()
        );

        return new FillOutcome.Filled(
                fill,
                participationCap,
                queueAheadQuantity,
                executableLiquidity
        );
    }

    private long calculateParticipationCap(
            long eventVolume
    ) {
        return (long) Math.floor(
                eventVolume
                        * maxParticipation
        );
    }

    private static double spreadCostBps(
            Side side,
            MarketEvent event,
            double mid
    ) {
        double spreadCost =
                side == Side.BUY
                        ? event.ask() - mid
                        : mid - event.bid();

        return (
                spreadCost
                        / mid
        ) * 10_000.0;
    }

    private static void validateInputs(
            ChildOrder childOrder,
            MarketEvent event,
            String strategyName
    ) {
        if (childOrder == null) {
            throw new IllegalArgumentException(
                    "childOrder is required"
            );
        }

        if (event == null) {
            throw new IllegalArgumentException(
                    "event is required"
            );
        }

        if (
                strategyName == null
                        || strategyName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "strategyName is required"
            );
        }

        if (
                childOrder.status()
                        != ChildOrderStatus.ACKNOWLEDGED
                        && childOrder.status()
                        != ChildOrderStatus.PARTIALLY_FILLED
        ) {
            throw new IllegalStateException(
                    "child must be active before fill attempt"
            );
        }

        if (
                !childOrder.symbol()
                        .equals(event.symbol())
        ) {
            throw new IllegalArgumentException(
                    "child and event symbols do not match"
            );
        }

        if (
                event.timestampMs()
                        < childOrder.lastUpdateTimestampMs()
        ) {
            throw new IllegalArgumentException(
                    "event timestamp precedes child lifecycle"
            );
        }
    }
}

package com.junwenzheng.execution.metrics.tca;

import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.order.Fill;
import com.junwenzheng.execution.order.ParentOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TransactionCostAnalysis {
    private TransactionCostAnalysis() {}

    public static TransactionCostBreakdown from(
            SimulationResult result
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "result is required"
            );
        }

        ParentOrder parent =
                result.parentOrder();

        List<Fill> fills =
                result.fills();

        int sideSign =
                parent.side().sign();

        int filledQuantity =
                fills.stream()
                        .mapToInt(Fill::quantity)
                        .sum();

        if (
                filledQuantity
                        != parent.filledQuantity()
        ) {
            throw new IllegalArgumentException(
                    "fill quantity does not match "
                            + "parent filled quantity"
            );
        }

        double arrivalPrice =
                parent.arrivalPrice();

        double executedNotional =
                fills.stream()
                        .mapToDouble(Fill::notional)
                        .sum();

        double averageFillPrice =
                filledQuantity == 0
                        ? 0.0
                        : executedNotional
                        / filledQuantity;

        double delayCost = 0.0;
        double executionCost = 0.0;
        double spreadCost = 0.0;
        double impactCost = 0.0;

        Map<String, VenueAccumulator>
                venueAccumulators =
                new LinkedHashMap<>();

        for (Fill fill : fills) {
            double quantity =
                    fill.quantity();

            delayCost +=
                    sideSign
                            * (
                            fill.referenceMidPrice()
                                    - arrivalPrice
                    )
                            * quantity;

            executionCost +=
                    sideSign
                            * (
                            fill.price()
                                    - fill.referenceMidPrice()
                    )
                            * quantity;

            spreadCost +=
                    fill.referenceMidPrice()
                            * fill.spreadCostBps()
                            / 10_000.0
                            * quantity;

            impactCost +=
                    fill.referenceMidPrice()
                            * fill.impactCostBps()
                            / 10_000.0
                            * quantity;

            VenueAccumulator accumulator =
                    venueAccumulators
                            .computeIfAbsent(
                                    fill.venue(),
                                    ignored ->
                                            new VenueAccumulator()
                            );

            accumulator.quantity +=
                    fill.quantity();

            accumulator.notional +=
                    fill.notional();

            accumulator.shortfall +=
                    sideSign
                            * (
                            fill.price()
                                    - arrivalPrice
                    )
                            * quantity;
        }

        double filledImplementationShortfall =
                delayCost + executionCost;

        double terminalBenchmarkPrice =
                result.replay()
                        .events()
                        .getLast()
                        .mid();

        int unfilledQuantity =
                parent.quantity()
                        - filledQuantity;

        double opportunityCost =
                sideSign
                        * (
                        terminalBenchmarkPrice
                                - arrivalPrice
                )
                        * unfilledQuantity;

        double totalImplementationShortfall =
                filledImplementationShortfall
                        + opportunityCost;

        double arrivalNotional =
                parent.quantity()
                        * arrivalPrice;

        double totalImplementationShortfallBps =
                totalImplementationShortfall
                        / arrivalNotional
                        * 10_000.0;

        double marketVwap =
                result.replay().vwap();

        double vwapSlippageBps =
                filledQuantity == 0
                        ? 0.0
                        : sideSign
                        * (
                        averageFillPrice
                                - marketVwap
                )
                        / marketVwap
                        * 10_000.0;

        double residualExecutionCost =
                executionCost
                        - spreadCost
                        - impactCost;

        List<VenueCostContribution>
                venueContributions =
                new ArrayList<>();

        for (
                Map.Entry<String, VenueAccumulator>
                        entry :
                venueAccumulators.entrySet()
        ) {
            VenueAccumulator accumulator =
                    entry.getValue();

            double venueArrivalNotional =
                    accumulator.quantity
                            * arrivalPrice;

            double venueBps =
                    accumulator.shortfall
                            / venueArrivalNotional
                            * 10_000.0;

            venueContributions.add(
                    new VenueCostContribution(
                            entry.getKey(),
                            accumulator.quantity,
                            accumulator.notional,
                            accumulator.shortfall,
                            venueBps
                    )
            );
        }

        venueContributions.sort(
                Comparator.comparing(
                        VenueCostContribution::venue
                )
        );

        return new TransactionCostBreakdown(
                result.strategyName(),
                parent.quantity(),
                filledQuantity,
                unfilledQuantity,
                arrivalPrice,
                averageFillPrice,
                terminalBenchmarkPrice,
                executedNotional,
                arrivalNotional,
                delayCost,
                executionCost,
                spreadCost,
                impactCost,
                residualExecutionCost,
                filledImplementationShortfall,
                opportunityCost,
                totalImplementationShortfall,
                totalImplementationShortfallBps,
                marketVwap,
                vwapSlippageBps,
                venueContributions
        );
    }

    private static final class VenueAccumulator {
        private int quantity;
        private double notional;
        private double shortfall;
    }
}

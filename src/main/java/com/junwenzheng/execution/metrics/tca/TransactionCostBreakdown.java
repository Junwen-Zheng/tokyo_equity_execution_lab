package com.junwenzheng.execution.metrics.tca;

import java.util.List;

public record TransactionCostBreakdown(
        String strategy,
        int parentQuantity,
        int filledQuantity,
        int unfilledQuantity,
        double arrivalPrice,
        double averageFillPrice,
        double terminalBenchmarkPrice,
        double executedNotional,
        double arrivalNotional,
        double delayCost,
        double executionCost,
        double spreadCost,
        double impactCost,
        double residualExecutionCost,
        double filledImplementationShortfall,
        double opportunityCost,
        double totalImplementationShortfall,
        double totalImplementationShortfallBps,
        double marketVwap,
        double vwapSlippageBps,
        List<VenueCostContribution> venueContributions
) {
    private static final double RECONCILIATION_TOLERANCE =
            1.0e-7;

    public TransactionCostBreakdown {
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException(
                    "strategy is required"
            );
        }

        if (parentQuantity <= 0) {
            throw new IllegalArgumentException(
                    "parentQuantity must be positive"
            );
        }

        if (
                filledQuantity < 0
                        || filledQuantity > parentQuantity
        ) {
            throw new IllegalArgumentException(
                    "filledQuantity is outside "
                            + "parent bounds"
            );
        }

        if (
                unfilledQuantity
                        != parentQuantity
                        - filledQuantity
        ) {
            throw new IllegalArgumentException(
                    "unfilledQuantity does not reconcile"
            );
        }

        validatePositiveFinite(
                arrivalPrice,
                "arrivalPrice"
        );

        validatePositiveFinite(
                terminalBenchmarkPrice,
                "terminalBenchmarkPrice"
        );

        validateNonNegativeFinite(
                averageFillPrice,
                "averageFillPrice"
        );

        validateNonNegativeFinite(
                executedNotional,
                "executedNotional"
        );

        validatePositiveFinite(
                arrivalNotional,
                "arrivalNotional"
        );

        validateFinite(delayCost, "delayCost");
        validateFinite(executionCost, "executionCost");
        validateFinite(spreadCost, "spreadCost");
        validateFinite(impactCost, "impactCost");

        validateFinite(
                residualExecutionCost,
                "residualExecutionCost"
        );

        validateFinite(
                filledImplementationShortfall,
                "filledImplementationShortfall"
        );

        validateFinite(
                opportunityCost,
                "opportunityCost"
        );

        validateFinite(
                totalImplementationShortfall,
                "totalImplementationShortfall"
        );

        validateFinite(
                totalImplementationShortfallBps,
                "totalImplementationShortfallBps"
        );

        validatePositiveFinite(
                marketVwap,
                "marketVwap"
        );

        validateFinite(
                vwapSlippageBps,
                "vwapSlippageBps"
        );

        if (venueContributions == null) {
            throw new IllegalArgumentException(
                    "venueContributions are required"
            );
        }

        strategy = strategy.trim();
        venueContributions =
                List.copyOf(venueContributions);

        requireClose(
                delayCost + executionCost,
                filledImplementationShortfall,
                "delay plus execution cost"
        );

        requireClose(
                spreadCost
                        + impactCost
                        + residualExecutionCost,
                executionCost,
                "execution-cost decomposition"
        );

        requireClose(
                filledImplementationShortfall
                        + opportunityCost,
                totalImplementationShortfall,
                "total implementation shortfall"
        );

        int venueQuantity =
                venueContributions.stream()
                        .mapToInt(
                                VenueCostContribution::
                                        filledQuantity
                        )
                        .sum();

        if (venueQuantity != filledQuantity) {
            throw new IllegalArgumentException(
                    "venue quantities do not reconcile"
            );
        }

        double venueShortfall =
                venueContributions.stream()
                        .mapToDouble(
                                VenueCostContribution::
                                        implementationShortfall
                        )
                        .sum();

        requireClose(
                venueShortfall,
                filledImplementationShortfall,
                "venue shortfall"
        );
    }

    public double fillRate() {
        return (double) filledQuantity
                / parentQuantity;
    }

    private static void requireClose(
            double actual,
            double expected,
            String label
    ) {
        double scale =
                Math.max(
                        1.0,
                        Math.max(
                                Math.abs(actual),
                                Math.abs(expected)
                        )
                );

        if (
                Math.abs(actual - expected)
                        > RECONCILIATION_TOLERANCE
                        * scale
        ) {
            throw new IllegalArgumentException(
                    label + " does not reconcile"
            );
        }
    }

    private static void validatePositiveFinite(
            double value,
            String name
    ) {
        if (
                !Double.isFinite(value)
                        || value <= 0.0
        ) {
            throw new IllegalArgumentException(
                    name + " must be finite and positive"
            );
        }
    }

    private static void validateNonNegativeFinite(
            double value,
            String name
    ) {
        if (
                !Double.isFinite(value)
                        || value < 0.0
        ) {
            throw new IllegalArgumentException(
                    name
                            + " must be finite "
                            + "and non-negative"
            );
        }
    }

    private static void validateFinite(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    name + " must be finite"
            );
        }
    }
}

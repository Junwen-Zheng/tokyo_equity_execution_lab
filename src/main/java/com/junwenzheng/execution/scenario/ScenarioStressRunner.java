package com.junwenzheng.execution.scenario;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.metrics.MicrostructureDiagnostics;
import com.junwenzheng.execution.metrics.tca.TransactionCostAnalysis;
import com.junwenzheng.execution.metrics.tca.TransactionCostBreakdown;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ScenarioStressRunner {
    private ScenarioStressRunner() {}

    public static List<ScenarioRunResult> run(
            MarketDataReplay baselineReplay,
            List<StressScenario> scenarios,
            List<ExecutionAlgorithm> algorithms,
            String symbol,
            Side side,
            int parentQuantity,
            double arrivalPrice,
            LatencyProfile baselineLatency,
            Function<
                    LatencyProfile,
                    ExecutionSimulator
                    > simulatorFactory
    ) {
        validateInputs(
                baselineReplay,
                scenarios,
                algorithms,
                symbol,
                side,
                parentQuantity,
                arrivalPrice,
                baselineLatency,
                simulatorFactory
        );

        String normalizedSymbol =
                symbol.trim();

        List<ScenarioRunResult> results =
                new ArrayList<>();

        for (StressScenario scenario : scenarios) {
            MarketDataReplay stressedReplay =
                    ScenarioMarketTransformer.apply(
                            baselineReplay,
                            scenario
                    );

            LatencyProfile stressedLatency =
                    scenario.applyTo(
                            baselineLatency
                    );

            ExecutionSimulator simulator =
                    simulatorFactory.apply(
                            stressedLatency
                    );

            if (simulator == null) {
                throw new IllegalArgumentException(
                        "simulatorFactory returned null"
                );
            }

            MicrostructureDiagnostics diagnostics =
                    MicrostructureDiagnostics.from(
                            stressedReplay
                    );

            for (
                    ExecutionAlgorithm algorithm :
                    algorithms
            ) {
                ParentOrder parentOrder =
                        new ParentOrder(
                                normalizedSymbol,
                                side,
                                parentQuantity,
                                arrivalPrice
                        );

                SimulationResult simulationResult =
                        simulator.run(
                                parentOrder,
                                stressedReplay,
                                algorithm
                        );

                TransactionCostBreakdown
                        transactionCosts =
                        TransactionCostAnalysis.from(
                                simulationResult
                        );

                results.add(
                        new ScenarioRunResult(
                                scenario,
                                simulationResult,
                                transactionCosts,
                                diagnostics
                        )
                );
            }
        }

        return List.copyOf(results);
    }

    private static void validateInputs(
            MarketDataReplay baselineReplay,
            List<StressScenario> scenarios,
            List<ExecutionAlgorithm> algorithms,
            String symbol,
            Side side,
            int parentQuantity,
            double arrivalPrice,
            LatencyProfile baselineLatency,
            Function<
                    LatencyProfile,
                    ExecutionSimulator
                    > simulatorFactory
    ) {
        if (baselineReplay == null) {
            throw new IllegalArgumentException(
                    "baselineReplay is required"
            );
        }

        if (
                scenarios == null
                        || scenarios.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "scenarios are required"
            );
        }

        if (
                scenarios.stream()
                        .anyMatch(
                                scenario ->
                                        scenario == null
                        )
        ) {
            throw new IllegalArgumentException(
                    "scenarios cannot contain null"
            );
        }

        if (
                algorithms == null
                        || algorithms.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "algorithms are required"
            );
        }

        if (
                algorithms.stream()
                        .anyMatch(
                                algorithm ->
                                        algorithm == null
                        )
        ) {
            throw new IllegalArgumentException(
                    "algorithms cannot contain null"
            );
        }

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "symbol is required"
            );
        }

        if (side == null) {
            throw new IllegalArgumentException(
                    "side is required"
            );
        }

        if (parentQuantity <= 0) {
            throw new IllegalArgumentException(
                    "parentQuantity must be positive"
            );
        }

        if (
                !Double.isFinite(arrivalPrice)
                        || arrivalPrice <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "arrivalPrice must be "
                            + "finite and positive"
            );
        }

        if (baselineLatency == null) {
            throw new IllegalArgumentException(
                    "baselineLatency is required"
            );
        }

        if (simulatorFactory == null) {
            throw new IllegalArgumentException(
                    "simulatorFactory is required"
            );
        }
    }
}

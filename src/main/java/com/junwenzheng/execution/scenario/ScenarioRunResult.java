package com.junwenzheng.execution.scenario;

import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.metrics.MicrostructureDiagnostics;
import com.junwenzheng.execution.metrics.tca.TransactionCostBreakdown;

public record ScenarioRunResult(
        StressScenario scenario,
        SimulationResult simulationResult,
        TransactionCostBreakdown transactionCosts,
        MicrostructureDiagnostics diagnostics
) {
    public ScenarioRunResult {
        if (scenario == null) {
            throw new IllegalArgumentException(
                    "scenario is required"
            );
        }

        if (simulationResult == null) {
            throw new IllegalArgumentException(
                    "simulationResult is required"
            );
        }

        if (transactionCosts == null) {
            throw new IllegalArgumentException(
                    "transactionCosts are required"
            );
        }

        if (diagnostics == null) {
            throw new IllegalArgumentException(
                    "diagnostics are required"
            );
        }

        if (
                !simulationResult.strategyName()
                        .equals(
                                transactionCosts.strategy()
                        )
        ) {
            throw new IllegalArgumentException(
                    "strategy names do not match"
            );
        }

        if (
                simulationResult.parentOrder()
                        .quantity()
                        != transactionCosts
                        .parentQuantity()
        ) {
            throw new IllegalArgumentException(
                    "parent quantities do not match"
            );
        }

        if (
                simulationResult.replay()
                        .events()
                        .size()
                        != diagnostics.eventCount()
        ) {
            throw new IllegalArgumentException(
                    "diagnostic event count "
                            + "does not match replay"
            );
        }
    }

    public String scenarioName() {
        return scenario.name();
    }

    public String strategy() {
        return simulationResult.strategyName();
    }

    public double fillRate() {
        return transactionCosts.fillRate();
    }

    public double totalShortfallBps() {
        return transactionCosts
                .totalImplementationShortfallBps();
    }

    public long totalMarketVolume() {
        return simulationResult
                .replay()
                .totalVolume();
    }
}

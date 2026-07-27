package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.algo.TwapAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.routing.SmartOrderRouter;
import com.junwenzheng.execution.scenario.ScenarioRunResult;
import com.junwenzheng.execution.scenario.ScenarioStressRunner;
import com.junwenzheng.execution.scenario.StressScenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScenarioStressRunnerTest {

    private static final double EPSILON =
            1.0e-9;

    @Test
    void runsEveryAlgorithmAcrossEveryScenario() {
        List<ScenarioRunResult> results =
                ScenarioStressRunner.run(
                        replay(),
                        List.of(
                                StressScenario.baseline(),
                                StressScenario
                                        .thinLiquidity(0.25)
                        ),
                        algorithms(),
                        "JPXDEMO",
                        Side.BUY,
                        600,
                        100.5,
                        LatencyProfile.zero(),
                        ScenarioStressRunnerTest::
                                simulator
                );

        assertEquals(
                4,
                results.size()
        );

        assertEquals(
                List.of(
                        "BASELINE",
                        "BASELINE",
                        "THIN_LIQUIDITY",
                        "THIN_LIQUIDITY"
                ),
                results.stream()
                        .map(
                                ScenarioRunResult::
                                        scenarioName
                        )
                        .toList()
        );

        assertEquals(
                List.of(
                        "TWAP",
                        "POV",
                        "TWAP",
                        "POV"
                ),
                results.stream()
                        .map(
                                ScenarioRunResult::
                                        strategy
                        )
                        .toList()
        );
    }

    @Test
    void preservesCommonArrivalBenchmark() {
        double arrivalPrice = 100.5;

        List<ScenarioRunResult> results =
                ScenarioStressRunner.run(
                        replay(),
                        List.of(
                                StressScenario.baseline(),
                                StressScenario
                                        .wideSpread(4.0)
                        ),
                        List.of(
                                new TwapAlgorithm(300)
                        ),
                        "JPXDEMO",
                        Side.BUY,
                        600,
                        arrivalPrice,
                        LatencyProfile.zero(),
                        ScenarioStressRunnerTest::
                                simulator
                );

        for (ScenarioRunResult result : results) {
            assertEquals(
                    arrivalPrice,
                    result.transactionCosts()
                            .arrivalPrice(),
                    EPSILON
            );
        }
    }

    @Test
    void recordsStressedMarketDiagnostics() {
        List<ScenarioRunResult> results =
                ScenarioStressRunner.run(
                        replay(),
                        List.of(
                                StressScenario.baseline(),
                                StressScenario
                                        .thinLiquidity(0.25)
                        ),
                        List.of(
                                new PovAlgorithm(
                                        0.50,
                                        600
                                )
                        ),
                        "JPXDEMO",
                        Side.BUY,
                        600,
                        100.5,
                        LatencyProfile.zero(),
                        ScenarioStressRunnerTest::
                                simulator
                );

        ScenarioRunResult baseline =
                results.getFirst();

        ScenarioRunResult thin =
                results.getLast();

        assertEquals(
                3_000L,
                baseline.totalMarketVolume()
        );

        assertEquals(
                750L,
                thin.totalMarketVolume()
        );

        assertEquals(
                baseline.diagnostics()
                        .eventCount(),
                thin.diagnostics()
                        .eventCount()
        );
    }

    @Test
    void createsIndependentParentOrders() {
        List<ScenarioRunResult> results =
                ScenarioStressRunner.run(
                        replay(),
                        List.of(
                                StressScenario.baseline()
                        ),
                        algorithms(),
                        "JPXDEMO",
                        Side.BUY,
                        600,
                        100.5,
                        LatencyProfile.zero(),
                        ScenarioStressRunnerTest::
                                simulator
                );

        assertEquals(
                2,
                results.size()
        );

        assertTrue(
                !results.getFirst()
                        .simulationResult()
                        .parentOrder()
                        .orderId()
                        .equals(
                                results.getLast()
                                        .simulationResult()
                                        .parentOrder()
                                        .orderId()
                        )
        );
    }

    @Test
    void queueDepletionReducesRoutedFillRate() {
        List<ScenarioRunResult> results =
                ScenarioStressRunner.run(
                        replay(),
                        List.of(
                                StressScenario.baseline(),
                                StressScenario
                                        .queueDepletion(0.05)
                        ),
                        List.of(
                                new PovAlgorithm(
                                        0.50,
                                        600
                                )
                        ),
                        "JPXDEMO",
                        Side.BUY,
                        600,
                        100.5,
                        LatencyProfile.zero(),
                        ScenarioStressRunnerTest::
                                routedSimulator
                );

        assertEquals(
                1.0,
                results.getFirst()
                        .fillRate(),
                EPSILON
        );

        assertEquals(
                0.25,
                results.getLast()
                        .fillRate(),
                EPSILON
        );
    }

    @Test
    void rejectsInvalidInputsAndNullSimulator() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ScenarioStressRunner.run(
                        replay(),
                        List.of(),
                        algorithms(),
                        "JPXDEMO",
                        Side.BUY,
                        600,
                        100.5,
                        LatencyProfile.zero(),
                        ScenarioStressRunnerTest::
                                simulator
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ScenarioStressRunner.run(
                        replay(),
                        List.of(
                                StressScenario.baseline()
                        ),
                        algorithms(),
                        "JPXDEMO",
                        Side.BUY,
                        600,
                        100.5,
                        LatencyProfile.zero(),
                        latency -> null
                )
        );
    }

    private static List<ExecutionAlgorithm>
    algorithms() {
        return List.of(
                new TwapAlgorithm(300),
                new PovAlgorithm(
                        0.50,
                        600
                )
        );
    }

    private static ExecutionSimulator simulator(
            LatencyProfile latencyProfile
    ) {
        return new ExecutionSimulator(
                new RiskManager(
                        1_000,
                        1_000_000.0
                ),
                new FillModel(
                        1.0,
                        0.0
                ),
                latencyProfile
        );
    }

    private static ExecutionSimulator
    routedSimulator(
            LatencyProfile latencyProfile
    ) {
        return ExecutionSimulator.routed(
                new RiskManager(
                        1_000,
                        1_000_000.0
                ),
                new FillModel(
                        1.0,
                        0.0
                ),
                latencyProfile,
                SmartOrderRouter.unconstrained()
        );
    }

    private static MarketDataReplay replay() {
        return MarketDataReplay.of(
                List.of(
                        new MarketEvent(
                                0L,
                                "JPXDEMO",
                                100.0,
                                101.0,
                                100.5,
                                1_000L
                        ),
                        new MarketEvent(
                                1_000L,
                                "JPXDEMO",
                                100.5,
                                101.5,
                                101.0,
                                1_000L
                        ),
                        new MarketEvent(
                                2_000L,
                                "JPXDEMO",
                                101.0,
                                102.0,
                                101.5,
                                1_000L
                        )
                )
        );
    }
}

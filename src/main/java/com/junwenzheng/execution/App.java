package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.OnlineVwapAlgorithm;
import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.algo.TwapAlgorithm;
import com.junwenzheng.execution.algo.VwapAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.SyntheticMarketDataGenerator;
import com.junwenzheng.execution.metrics.ExecutionMetrics;
import com.junwenzheng.execution.metrics.LatencyBenchmark;
import com.junwenzheng.execution.metrics.ReportWriter;
import com.junwenzheng.execution.metrics.tca.TransactionCostBreakdown;
import com.junwenzheng.execution.metrics.tca.TransactionCostReportWriter;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.routing.SmartOrderRouter;
import com.junwenzheng.execution.scenario.ScenarioRunResult;
import com.junwenzheng.execution.scenario.ScenarioStressReportWriter;
import com.junwenzheng.execution.scenario.ScenarioStressRunner;
import com.junwenzheng.execution.scenario.StressScenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class App {
    public static void main(String[] args) throws Exception {
        Path dataPath = Path.of(args.length > 0 ? args[0] : "data/sample_market_data.csv");
        if (Files.notExists(dataPath)) {
            SyntheticMarketDataGenerator.writeSample(dataPath, "JPXDEMO", 480, 42L);
        }

        MarketDataReplay replay = MarketDataReplay.fromCsv(dataPath);
        double arrivalPrice = replay.events().getFirst().ask();
        int parentQty = 20_000;

        long onlineVolumeForecast =
                Math.max(
                        1L,
                        replay.events()
                                .getFirst()
                                .volume()
                                * (long) replay.events().size()
                );

        List<ExecutionAlgorithm> algorithms = List.of(
                new TwapAlgorithm(750),
                new VwapAlgorithm(1_200),
                new OnlineVwapAlgorithm(
                        1_200,
                        onlineVolumeForecast
                ),
                new PovAlgorithm(0.08, 1_000)
        );

        LatencyProfile baselineLatency =
                LatencyProfile
                        .deterministicBaseline();

        ExecutionSimulator simulator =
                createSimulator(
                        baselineLatency
                );

        List<SimulationResult> results = new ArrayList<>();
        for (ExecutionAlgorithm algorithm : algorithms) {
            ParentOrder order = new ParentOrder("JPXDEMO", Side.BUY, parentQty, arrivalPrice);
            results.add(simulator.run(order, replay, algorithm));
        }

        List<ExecutionMetrics> metrics = ReportWriter.metrics(results);
        ReportWriter.writeCsv(Path.of("reports/execution_summary.csv"), metrics);
        ReportWriter.writeMarkdown(Path.of("reports/execution_report.md"), metrics);
        ReportWriter.writeMicrostructureDiagnostics(Path.of("reports/microstructure_diagnostics.md"), replay);

        List<TransactionCostBreakdown> transactionCosts =
                TransactionCostReportWriter.analyse(
                        results
                );

        TransactionCostReportWriter.writeSummaryCsv(
                Path.of(
                        "reports",
                        "transaction_cost_summary.csv"
                ),
                transactionCosts
        );

        TransactionCostReportWriter.writeVenueCsv(
                Path.of(
                        "reports",
                        "venue_cost_attribution.csv"
                ),
                transactionCosts
        );

        TransactionCostReportWriter.writeMarkdown(
                Path.of(
                        "reports",
                        "transaction_cost_report.md"
                ),
                transactionCosts
        );

        List<ScenarioRunResult> stressResults =
                ScenarioStressRunner.run(
                        replay,
                        StressScenario.standardSuite(),
                        algorithms,
                        "JPXDEMO",
                        Side.BUY,
                        parentQty,
                        arrivalPrice,
                        baselineLatency,
                        App::createStressSimulator
                );

        ScenarioStressReportWriter.writeCsv(
                Path.of(
                        "reports",
                        "stress_scenario_summary.csv"
                ),
                stressResults
        );

        ScenarioStressReportWriter.writeMarkdown(
                Path.of(
                        "reports",
                        "stress_scenario_report.md"
                ),
                stressResults
        );

        LatencyBenchmark.run(replay, 1_000);

        System.out.println("Execution report written to reports/execution_report.md");
        System.out.println("TCA report written to reports/transaction_cost_report.md");
        System.out.println("Stress report written to reports/stress_scenario_report.md");
        for (ExecutionMetrics metric : metrics) {
            System.out.printf(java.util.Locale.US, "%s fillRate=%.2f%% arrivalSlip=%.2fbps vwapSlip=%.2fbps%n",
                    metric.strategy(), metric.fillRate() * 100.0, metric.slippageVsArrivalBps(), metric.slippageVsVwapBps());
        }

        for (
                TransactionCostBreakdown breakdown :
                transactionCosts
        ) {
            System.out.printf(
                    java.util.Locale.US,
                    "%s totalIS=%.2fbps delay=%.2f execution=%.2f opportunity=%.2f%n",
                    breakdown.strategy(),
                    breakdown.totalImplementationShortfallBps(),
                    breakdown.delayCost(),
                    breakdown.executionCost(),
                    breakdown.opportunityCost()
            );
        }
    }

    private static ExecutionSimulator createSimulator(
            LatencyProfile latencyProfile
    ) {
        return new ExecutionSimulator(
                new RiskManager(
                        2_000,
                        250_000.0
                ),
                new FillModel(
                        0.12,
                        1.6
                ),
                latencyProfile
        );
    }

    private static ExecutionSimulator
    createStressSimulator(
            LatencyProfile latencyProfile
    ) {
        return ExecutionSimulator.routed(
                new RiskManager(
                        2_000,
                        250_000.0
                ),
                new FillModel(
                        0.12,
                        1.6
                ),
                latencyProfile,
                SmartOrderRouter.unconstrained()
        );
    }
}

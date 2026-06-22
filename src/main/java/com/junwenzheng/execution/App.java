package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.ExecutionAlgorithm;
import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.algo.TwapAlgorithm;
import com.junwenzheng.execution.algo.VwapAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.SyntheticMarketDataGenerator;
import com.junwenzheng.execution.metrics.ExecutionMetrics;
import com.junwenzheng.execution.metrics.LatencyBenchmark;
import com.junwenzheng.execution.metrics.ReportWriter;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;

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

        List<ExecutionAlgorithm> algorithms = List.of(
                new TwapAlgorithm(750),
                new VwapAlgorithm(1_200),
                new PovAlgorithm(0.08, 1_000)
        );

        ExecutionSimulator simulator = new ExecutionSimulator(
                new RiskManager(2_000, 250_000.0),
                new FillModel(0.12, 1.6)
        );

        List<SimulationResult> results = new ArrayList<>();
        for (ExecutionAlgorithm algorithm : algorithms) {
            ParentOrder order = new ParentOrder("JPXDEMO", Side.BUY, parentQty, arrivalPrice);
            results.add(simulator.run(order, replay, algorithm));
        }

        List<ExecutionMetrics> metrics = ReportWriter.metrics(results);
        ReportWriter.writeCsv(Path.of("reports/execution_summary.csv"), metrics);
        ReportWriter.writeMarkdown(Path.of("reports/execution_report.md"), metrics);
        LatencyBenchmark.run(replay, 1_000);

        System.out.println("Execution report written to reports/execution_report.md");
        for (ExecutionMetrics metric : metrics) {
            System.out.printf(java.util.Locale.US, "%s fillRate=%.2f%% arrivalSlip=%.2fbps vwapSlip=%.2fbps%n",
                    metric.strategy(), metric.fillRate() * 100.0, metric.slippageVsArrivalBps(), metric.slippageVsVwapBps());
        }
    }
}

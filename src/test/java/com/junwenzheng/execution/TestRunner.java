package com.junwenzheng.execution;

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
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;

import java.nio.file.Path;

public final class TestRunner {
    public static void main(String[] args) throws Exception {
        Path sample = Path.of("data/test_market_data.csv");
        SyntheticMarketDataGenerator.writeSample(sample, "JPXDEMO", 120, 7L);
        MarketDataReplay replay = MarketDataReplay.fromCsv(sample);
        assertTrue(replay.events().size() == 120, "replay should load all generated rows");
        assertTrue(replay.vwap() > 0.0, "vwap should be positive");

        ExecutionSimulator simulator = new ExecutionSimulator(new RiskManager(2_000, 250_000.0), new FillModel(0.12, 1.6));
        SimulationResult result = simulator.run(new ParentOrder("JPXDEMO", Side.BUY, 5_000, replay.events().getFirst().ask()), replay, new PovAlgorithm(0.10, 800));
        ExecutionMetrics metrics = ExecutionMetrics.from(result);
        assertTrue(metrics.filledQuantity() > 0, "POV should generate fills");
        assertTrue(metrics.fillRate() > 0 && metrics.fillRate() <= 1.0, "fill rate should be bounded");
        assertTrue(metrics.averagePrice() >= replay.events().getFirst().bid(), "average price should be plausible");

        SimulationResult twap = simulator.run(new ParentOrder("JPXDEMO", Side.BUY, 5_000, replay.events().getFirst().ask()), replay, new TwapAlgorithm(500));
        SimulationResult vwap = simulator.run(new ParentOrder("JPXDEMO", Side.BUY, 5_000, replay.events().getFirst().ask()), replay, new VwapAlgorithm(800));
        assertTrue(!twap.fills().isEmpty(), "TWAP should fill at least once");
        assertTrue(!vwap.fills().isEmpty(), "VWAP should fill at least once");

        String benchmark = LatencyBenchmark.run(replay, 100);
        assertTrue(benchmark.contains("eventsPerSecond"), "benchmark should report throughput");
        System.out.println("All tests passed");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

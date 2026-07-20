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
import com.junwenzheng.execution.metrics.ImplementationShortfall;
import com.junwenzheng.execution.metrics.LatencyBenchmark;
import com.junwenzheng.execution.metrics.MicrostructureDiagnostics;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BaselineExecutionTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void replayLoadsGeneratedRowsAndDiagnostics()
            throws Exception {
        MarketDataReplay replay = sampleReplay();

        assertEquals(
                120,
                replay.events().size()
        );

        assertTrue(
                replay.vwap() > 0.0
        );

        MicrostructureDiagnostics diagnostics =
                MicrostructureDiagnostics.from(
                        replay
                );

        assertTrue(
                diagnostics.averageSpreadBps() > 0.0
        );

        assertTrue(
                diagnostics.averageEventVolume() > 0.0
        );
    }

    @Test
    void povProducesPlausibleExecutionMetrics()
            throws Exception {
        MarketDataReplay replay = sampleReplay();

        ExecutionSimulator simulator =
                simulator();

        SimulationResult result = simulator.run(
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        5_000,
                        replay.events().getFirst().ask()
                ),
                replay,
                new PovAlgorithm(
                        0.10,
                        800
                )
        );

        ExecutionMetrics metrics =
                ExecutionMetrics.from(result);

        assertTrue(
                metrics.filledQuantity() > 0
        );

        assertTrue(
                metrics.fillRate() > 0.0
                        && metrics.fillRate() <= 1.0
        );

        assertTrue(
                metrics.averagePrice()
                        >= replay.events()
                        .getFirst()
                        .bid()
        );

        double shortfall =
                ImplementationShortfall.bps(
                        result.fills(),
                        Side.BUY,
                        replay.events()
                                .getFirst()
                                .ask()
                );

        assertTrue(
                Double.isFinite(shortfall)
        );
    }

    @Test
    void twapAndVwapProduceFills()
            throws Exception {
        MarketDataReplay replay = sampleReplay();

        SimulationResult twap = simulator().run(
                parentOrder(replay),
                replay,
                new TwapAlgorithm(500)
        );

        SimulationResult vwap = simulator().run(
                parentOrder(replay),
                replay,
                new VwapAlgorithm(800)
        );

        assertFalse(twap.fills().isEmpty());
        assertFalse(vwap.fills().isEmpty());
    }

    @Test
    void benchmarkCanWriteOutsideRepository()
            throws Exception {
        MarketDataReplay replay = sampleReplay();

        String report = LatencyBenchmark.measure(
                replay,
                10
        );

        assertTrue(
                report.contains("eventsPerSecond")
        );

        Path destination =
                temporaryDirectory.resolve(
                        "reports/latency_benchmark.txt"
                );

        LatencyBenchmark.writeReport(
                report,
                destination
        );

        assertEquals(
                report,
                Files.readString(destination)
        );
    }

    private MarketDataReplay sampleReplay()
            throws Exception {
        Path sample = temporaryDirectory.resolve(
                "test_market_data.csv"
        );

        SyntheticMarketDataGenerator.writeSample(
                sample,
                "JPXDEMO",
                120,
                7L
        );

        return MarketDataReplay.fromCsv(sample);
    }

    private static ExecutionSimulator simulator() {
        return new ExecutionSimulator(
                new RiskManager(
                        2_000,
                        250_000.0
                ),
                new FillModel(
                        0.12,
                        1.6
                )
        );
    }

    private static ParentOrder parentOrder(
            MarketDataReplay replay
    ) {
        return new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                5_000,
                replay.events().getFirst().ask()
        );
    }
}

package com.junwenzheng.execution.metrics;

import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class LatencyBenchmark {
    private LatencyBenchmark() {
    }

    public static String measure(
            MarketDataReplay replay,
            int iterations
    ) {
        if (iterations <= 0) {
            throw new IllegalArgumentException(
                    "iterations must be positive"
            );
        }

        ExecutionSimulator simulator =
                new ExecutionSimulator(
                        new RiskManager(
                                2_000,
                                250_000.0
                        ),
                        new FillModel(
                                0.12,
                                1.6
                        )
                );

        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            ParentOrder order = new ParentOrder(
                    "JPXDEMO",
                    Side.BUY,
                    20_000,
                    replay.events().getFirst().ask()
            );

            simulator.run(
                    order,
                    replay,
                    new PovAlgorithm(
                            0.08,
                            1_000
                    )
            );
        }

        long elapsedNs = System.nanoTime() - start;

        double perRunMs =
                elapsedNs
                        / 1_000_000.0
                        / iterations;

        double eventsPerSecond =
                (
                        (double) replay.events().size()
                                * iterations
                )
                        / (
                        elapsedNs
                                / 1_000_000_000.0
                );

        return String.format(
                Locale.US,
                "iterations=%d%n"
                        + "eventsPerRun=%d%n"
                        + "meanRunMs=%.4f%n"
                        + "eventsPerSecond=%.2f%n",
                iterations,
                replay.events().size(),
                perRunMs,
                eventsPerSecond
        );
    }

    public static void writeReport(
            String report,
            Path destination
    ) throws IOException {
        Path parent = destination.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                destination,
                report
        );
    }

    public static String run(
            MarketDataReplay replay,
            int iterations
    ) throws IOException {
        String report = measure(
                replay,
                iterations
        );

        writeReport(
                report,
                Path.of(
                        "reports",
                        "latency_benchmark.txt"
                )
        );

        return report;
    }
}

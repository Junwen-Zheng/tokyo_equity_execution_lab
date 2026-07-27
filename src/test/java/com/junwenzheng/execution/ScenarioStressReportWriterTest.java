package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.metrics.MicrostructureDiagnostics;
import com.junwenzheng.execution.metrics.tca.TransactionCostBreakdown;
import com.junwenzheng.execution.metrics.tca.VenueCostContribution;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.scenario.ScenarioRunResult;
import com.junwenzheng.execution.scenario.ScenarioStressReportWriter;
import com.junwenzheng.execution.scenario.StressScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScenarioStressReportWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesBaselineRelativeCsv()
            throws IOException {
        Path destination =
                temporaryDirectory.resolve(
                        "stress.csv"
                );

        ScenarioStressReportWriter.writeCsv(
                destination,
                results()
        );

        String output =
                Files.readString(destination);

        assertTrue(
                output.contains(
                        "scenario,strategy,fillRate"
                )
        );

        assertTrue(
                output.contains(
                        "BASELINE,TWAP,1.000000,0.000000,"
                                + "100.000000,0.000000"
                )
        );

        assertTrue(
                output.contains(
                        "THIN_LIQUIDITY,TWAP,0.500000,"
                                + "-0.500000,180.000000,80.000000"
                )
        );
    }

    @Test
    void writesMarkdownWithDefinitions()
            throws IOException {
        Path destination =
                temporaryDirectory.resolve(
                        "stress.md"
                );

        ScenarioStressReportWriter.writeMarkdown(
                destination,
                results()
        );

        String output =
                Files.readString(destination);

        assertTrue(
                output.contains(
                        "# Deterministic execution stress report"
                )
        );

        assertTrue(
                output.contains(
                        "| THIN_LIQUIDITY | TWAP | 50.00%"
                )
        );

        assertTrue(
                output.contains(
                        "## Scenario definitions"
                )
        );

        assertTrue(
                output.contains(
                        "| THIN_LIQUIDITY | 1.00 | 0.50 |"
                )
        );
    }

    @Test
    void rejectsMissingBaseline() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ScenarioStressReportWriter
                        .writeCsv(
                                temporaryDirectory
                                        .resolve("stress.csv"),
                                List.of(
                                        run(
                                                StressScenario
                                                        .thinLiquidity(
                                                                0.50
                                                        ),
                                                0.50,
                                                180.0
                                        )
                                )
                        )
        );
    }

    @Test
    void rejectsDuplicateBaseline() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ScenarioStressReportWriter
                        .writeMarkdown(
                                temporaryDirectory
                                        .resolve("stress.md"),
                                List.of(
                                        run(
                                                StressScenario
                                                        .baseline(),
                                                1.0,
                                                100.0
                                        ),
                                        run(
                                                StressScenario
                                                        .baseline(),
                                                1.0,
                                                100.0
                                        )
                                )
                        )
        );
    }

    private static List<ScenarioRunResult>
    results() {
        return List.of(
                run(
                        StressScenario.baseline(),
                        1.0,
                        100.0
                ),
                run(
                        StressScenario
                                .thinLiquidity(0.50),
                        0.50,
                        180.0
                )
        );
    }

    private static ScenarioRunResult run(
            StressScenario scenario,
            double fillRate,
            double shortfallBps
    ) {
        int parentQuantity = 100;
        int filledQuantity =
                (int) Math.round(
                        parentQuantity * fillRate
                );

        ParentOrder parentOrder =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        parentQuantity,
                        100.0
                );

        parentOrder.markWorking();

        if (filledQuantity > 0) {
            parentOrder.applyFill(
                    filledQuantity
            );
        }

        if (!parentOrder.isTerminal()) {
            parentOrder.cancel();
        }

        MarketDataReplay replay =
                MarketDataReplay.of(
                        List.of(
                                new MarketEvent(
                                        0L,
                                        "JPXDEMO",
                                        99.5,
                                        100.5,
                                        100.0,
                                        1_000L
                                )
                        )
                );

        SimulationResult simulationResult =
                new SimulationResult(
                        "TWAP",
                        parentOrder,
                        replay,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                );

        double totalShortfall =
                shortfallBps;

        TransactionCostBreakdown costs =
                new TransactionCostBreakdown(
                        "TWAP",
                        parentQuantity,
                        filledQuantity,
                        parentQuantity
                                - filledQuantity,
                        100.0,
                        filledQuantity == 0
                                ? 0.0
                                : 100.0,
                        100.0,
                        filledQuantity
                                * 100.0,
                        10_000.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        totalShortfall,
                        totalShortfall,
                        shortfallBps,
                        100.0,
                        0.0,
                        filledQuantity == 0
                                ? List.of()
                                : List.of(
                                new VenueCostContribution(
                                        "PRIMARY",
                                        filledQuantity,
                                        filledQuantity
                                                * 100.0,
                                        0.0,
                                        0.0
                                )
                        )
                );

        return new ScenarioRunResult(
                scenario,
                simulationResult,
                costs,
                MicrostructureDiagnostics.from(
                        replay
                )
        );
    }
}

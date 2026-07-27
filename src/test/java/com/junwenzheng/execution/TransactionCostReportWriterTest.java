package com.junwenzheng.execution;

import com.junwenzheng.execution.metrics.tca.TransactionCostBreakdown;
import com.junwenzheng.execution.metrics.tca.TransactionCostReportWriter;
import com.junwenzheng.execution.metrics.tca.VenueCostContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransactionCostReportWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesSummaryCsv() throws IOException {
        Path destination =
                temporaryDirectory.resolve(
                        "summary.csv"
                );

        TransactionCostReportWriter
                .writeSummaryCsv(
                        destination,
                        List.of(breakdown())
                );

        String output =
                Files.readString(destination);

        assertTrue(
                output.startsWith(
                        "strategy,parentQuantity,"
                )
        );

        assertTrue(
                output.contains(
                        "TEST,100,100,0,1.000000"
                )
        );

        assertTrue(
                output.contains(
                        ",150.000000,150.000000"
                )
        );
    }

    @Test
    void writesVenueCsv() throws IOException {
        Path destination =
                temporaryDirectory.resolve(
                        "venues.csv"
                );

        TransactionCostReportWriter
                .writeVenueCsv(
                        destination,
                        List.of(breakdown())
                );

        String output =
                Files.readString(destination);

        assertTrue(
                output.contains(
                        "strategy,venue,filledQuantity"
                )
        );

        assertTrue(
                output.contains(
                        "TEST,TSE,100,10100.000000,"
                                + "150.000000,150.000000"
                )
        );
    }

    @Test
    void writesReconciledMarkdown()
            throws IOException {
        Path destination =
                temporaryDirectory.resolve(
                        "report.md"
                );

        TransactionCostReportWriter
                .writeMarkdown(
                        destination,
                        List.of(breakdown())
                );

        String output =
                Files.readString(destination);

        assertTrue(
                output.contains(
                        "# Transaction cost analysis"
                )
        );

        assertTrue(
                output.contains(
                        "| TEST | 100.00% |"
                )
        );

        assertTrue(
                output.contains(
                        "Execution-cost decomposition"
                )
        );

        assertTrue(
                output.contains(
                        "Venue attribution"
                )
        );

        assertTrue(
                output.contains(
                        "delay cost plus execution cost"
                )
        );
    }

    @Test
    void rejectsInvalidWriterInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TransactionCostReportWriter
                        .writeSummaryCsv(
                                null,
                                List.of()
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> TransactionCostReportWriter
                        .writeMarkdown(
                                temporaryDirectory
                                        .resolve("report.md"),
                                null
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> TransactionCostReportWriter
                        .writeVenueCsv(
                                temporaryDirectory
                                        .resolve("venues.csv"),
                                java.util.Arrays.asList(
                                        breakdown(),
                                        null
                                )
                        )
        );
    }

    private static TransactionCostBreakdown
    breakdown() {
        return new TransactionCostBreakdown(
                "TEST",
                100,
                100,
                0,
                100.0,
                101.0,
                102.0,
                10_100.0,
                10_000.0,
                100.0,
                50.0,
                20.0,
                25.0,
                5.0,
                150.0,
                0.0,
                150.0,
                150.0,
                101.0,
                0.0,
                List.of(
                        new VenueCostContribution(
                                "TSE",
                                100,
                                10_100.0,
                                150.0,
                                150.0
                        )
                )
        );
    }
}

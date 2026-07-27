package com.junwenzheng.execution.metrics.tca;

import com.junwenzheng.execution.engine.SimulationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TransactionCostReportWriter {
    private TransactionCostReportWriter() {}

    public static List<TransactionCostBreakdown> analyse(
            List<SimulationResult> results
    ) {
        if (results == null) {
            throw new IllegalArgumentException(
                    "results are required"
            );
        }

        return results.stream()
                .map(TransactionCostAnalysis::from)
                .sorted(
                        Comparator.comparingDouble(
                                TransactionCostBreakdown::
                                        totalImplementationShortfallBps
                        )
                )
                .toList();
    }

    public static void writeSummaryCsv(
            Path path,
            List<TransactionCostBreakdown> breakdowns
    ) throws IOException {
        validateInputs(path, breakdowns);
        createParentDirectories(path);

        StringBuilder output =
                new StringBuilder(
                        "strategy,parentQuantity,"
                                + "filledQuantity,"
                                + "unfilledQuantity,"
                                + "fillRate,"
                                + "arrivalPrice,"
                                + "averageFillPrice,"
                                + "terminalBenchmarkPrice,"
                                + "marketVwap,"
                                + "vwapSlippageBps,"
                                + "delayCost,"
                                + "executionCost,"
                                + "spreadCost,"
                                + "impactCost,"
                                + "residualExecutionCost,"
                                + "filledImplementationShortfall,"
                                + "opportunityCost,"
                                + "totalImplementationShortfall,"
                                + "totalImplementationShortfallBps\n"
                );

        for (
                TransactionCostBreakdown breakdown :
                breakdowns
        ) {
            output.append(
                    String.format(
                            Locale.US,
                            "%s,%d,%d,%d,%.6f,"
                                    + "%.6f,%.6f,%.6f,"
                                    + "%.6f,%.6f,"
                                    + "%.6f,%.6f,%.6f,"
                                    + "%.6f,%.6f,%.6f,"
                                    + "%.6f,%.6f,%.6f%n",
                            csv(breakdown.strategy()),
                            breakdown.parentQuantity(),
                            breakdown.filledQuantity(),
                            breakdown.unfilledQuantity(),
                            breakdown.fillRate(),
                            breakdown.arrivalPrice(),
                            breakdown.averageFillPrice(),
                            breakdown.terminalBenchmarkPrice(),
                            breakdown.marketVwap(),
                            breakdown.vwapSlippageBps(),
                            breakdown.delayCost(),
                            breakdown.executionCost(),
                            breakdown.spreadCost(),
                            breakdown.impactCost(),
                            breakdown.residualExecutionCost(),
                            breakdown
                                    .filledImplementationShortfall(),
                            breakdown.opportunityCost(),
                            breakdown
                                    .totalImplementationShortfall(),
                            breakdown
                                    .totalImplementationShortfallBps()
                    )
            );
        }

        Files.writeString(
                path,
                output.toString()
        );
    }

    public static void writeVenueCsv(
            Path path,
            List<TransactionCostBreakdown> breakdowns
    ) throws IOException {
        validateInputs(path, breakdowns);
        createParentDirectories(path);

        StringBuilder output =
                new StringBuilder(
                        "strategy,venue,filledQuantity,"
                                + "executedNotional,"
                                + "implementationShortfall,"
                                + "implementationShortfallBps\n"
                );

        for (
                TransactionCostBreakdown breakdown :
                breakdowns
        ) {
            for (
                    VenueCostContribution venue :
                    breakdown.venueContributions()
            ) {
                output.append(
                        String.format(
                                Locale.US,
                                "%s,%s,%d,%.6f,%.6f,%.6f%n",
                                csv(breakdown.strategy()),
                                csv(venue.venue()),
                                venue.filledQuantity(),
                                venue.executedNotional(),
                                venue.implementationShortfall(),
                                venue.implementationShortfallBps()
                        )
                );
            }
        }

        Files.writeString(
                path,
                output.toString()
        );
    }

    public static void writeMarkdown(
            Path path,
            List<TransactionCostBreakdown> breakdowns
    ) throws IOException {
        validateInputs(path, breakdowns);
        createParentDirectories(path);

        StringBuilder output =
                new StringBuilder();

        output.append(
                "# Transaction cost analysis\n\n"
        );

        output.append(
                "The decomposition uses the parent arrival "
                        + "price, each fill's contemporaneous "
                        + "reference midpoint, and the terminal "
                        + "replay midpoint. Positive cost is "
                        + "adverse for the parent order.\n\n"
        );

        output.append(
                "| Strategy | Fill Rate | Delay Cost | "
                        + "Execution Cost | Opportunity Cost | "
                        + "Total Shortfall | Total IS (bps) | "
                        + "VWAP Slip (bps) |\n"
        );

        output.append(
                "|---|---:|---:|---:|---:|---:|---:|---:|\n"
        );

        for (
                TransactionCostBreakdown breakdown :
                breakdowns
        ) {
            output.append(
                    String.format(
                            Locale.US,
                            "| %s | %.2f%% | %.2f | %.2f | "
                                    + "%.2f | %.2f | %.2f | %.2f |%n",
                            breakdown.strategy(),
                            breakdown.fillRate() * 100.0,
                            breakdown.delayCost(),
                            breakdown.executionCost(),
                            breakdown.opportunityCost(),
                            breakdown
                                    .totalImplementationShortfall(),
                            breakdown
                                    .totalImplementationShortfallBps(),
                            breakdown.vwapSlippageBps()
                    )
            );
        }

        output.append(
                "\n## Execution-cost decomposition\n\n"
        );

        output.append(
                "| Strategy | Spread Cost | Impact Cost | "
                        + "Residual Cost | Execution Cost |\n"
        );

        output.append(
                "|---|---:|---:|---:|---:|\n"
        );

        for (
                TransactionCostBreakdown breakdown :
                breakdowns
        ) {
            output.append(
                    String.format(
                            Locale.US,
                            "| %s | %.2f | %.2f | %.2f | %.2f |%n",
                            breakdown.strategy(),
                            breakdown.spreadCost(),
                            breakdown.impactCost(),
                            breakdown.residualExecutionCost(),
                            breakdown.executionCost()
                    )
            );
        }

        output.append(
                "\n## Venue attribution\n\n"
        );

        output.append(
                "| Strategy | Venue | Filled Quantity | "
                        + "Executed Notional | "
                        + "Shortfall | Shortfall (bps) |\n"
        );

        output.append(
                "|---|---|---:|---:|---:|---:|\n"
        );

        for (
                TransactionCostBreakdown breakdown :
                breakdowns
        ) {
            for (
                    VenueCostContribution venue :
                    breakdown.venueContributions()
            ) {
                output.append(
                        String.format(
                                Locale.US,
                                "| %s | %s | %d | %.2f | %.2f | %.2f |%n",
                                breakdown.strategy(),
                                venue.venue(),
                                venue.filledQuantity(),
                                venue.executedNotional(),
                                venue.implementationShortfall(),
                                venue.implementationShortfallBps()
                        )
                );
            }
        }

        output.append(
                "\n## Reconciliation\n\n"
        );

        output.append(
                "For every strategy:\n\n"
                        + "- delay cost plus execution cost "
                        + "equals filled implementation shortfall\n"
                        + "- spread cost plus impact cost plus "
                        + "residual cost equals execution cost\n"
                        + "- filled shortfall plus opportunity cost "
                        + "equals total implementation shortfall\n"
                        + "- venue shortfall contributions sum to "
                        + "filled implementation shortfall\n"
        );

        Files.writeString(
                path,
                output.toString()
        );
    }

    private static void validateInputs(
            Path path,
            List<TransactionCostBreakdown> breakdowns
    ) {
        if (path == null) {
            throw new IllegalArgumentException(
                    "path is required"
            );
        }

        if (breakdowns == null) {
            throw new IllegalArgumentException(
                    "breakdowns are required"
            );
        }

        if (
                breakdowns.stream()
                        .anyMatch(
                                breakdown ->
                                        breakdown == null
                        )
        ) {
            throw new IllegalArgumentException(
                    "breakdowns cannot contain null"
            );
        }
    }

    private static void createParentDirectories(
            Path path
    ) throws IOException {
        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String csv(
            String value
    ) {
        if (
                value.indexOf(',') < 0
                        && value.indexOf('"') < 0
                        && value.indexOf('\n') < 0
                        && value.indexOf('\r') < 0
        ) {
            return value;
        }

        return "\""
                + value.replace(
                        "\"",
                        "\"\""
                )
                + "\"";
    }
}

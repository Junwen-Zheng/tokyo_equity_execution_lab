package com.junwenzheng.execution.scenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ScenarioStressReportWriter {
    private ScenarioStressReportWriter() {}

    public static void writeCsv(
            Path path,
            List<ScenarioRunResult> results
    ) throws IOException {
        validateInputs(path, results);
        createParentDirectories(path);

        Map<String, ScenarioRunResult> baselines =
                baselinesByStrategy(results);

        StringBuilder output =
                new StringBuilder(
                        "scenario,strategy,"
                                + "fillRate,"
                                + "fillRateDelta,"
                                + "totalShortfallBps,"
                                + "shortfallDeltaBps,"
                                + "averageSpreadBps,"
                                + "midpointVolatilityBps,"
                                + "totalMarketVolume,"
                                + "eventCount,"
                                + "additionalLatencyMs\n"
                );

        for (
                ScenarioRunResult result :
                ordered(results)
        ) {
            ScenarioRunResult baseline =
                    baselines.get(
                            result.strategy()
                    );

            output.append(
                    String.format(
                            Locale.US,
                            "%s,%s,%.6f,%.6f,"
                                    + "%.6f,%.6f,"
                                    + "%.6f,%.6f,"
                                    + "%d,%d,%d%n",
                            csv(result.scenarioName()),
                            csv(result.strategy()),
                            result.fillRate(),
                            result.fillRate()
                                    - baseline.fillRate(),
                            result.totalShortfallBps(),
                            result.totalShortfallBps()
                                    - baseline
                                    .totalShortfallBps(),
                            result.diagnostics()
                                    .averageSpreadBps(),
                            result.diagnostics()
                                    .midpointVolatilityBps(),
                            result.totalMarketVolume(),
                            result.diagnostics()
                                    .eventCount(),
                            result.scenario()
                                    .additionalLatencyMs()
                    )
            );
        }

        Files.writeString(
                path,
                output.toString()
        );
    }

    public static void writeMarkdown(
            Path path,
            List<ScenarioRunResult> results
    ) throws IOException {
        validateInputs(path, results);
        createParentDirectories(path);

        Map<String, ScenarioRunResult> baselines =
                baselinesByStrategy(results);

        StringBuilder output =
                new StringBuilder();

        output.append(
                "# Deterministic execution stress report\n\n"
        );

        output.append(
                "Each strategy is rerun from a fresh parent "
                        + "order using a common arrival benchmark. "
                        + "Deltas are measured against that "
                        + "strategy's baseline run. Positive "
                        + "shortfall delta is adverse.\n\n"
        );

        output.append(
                "| Scenario | Strategy | Fill Rate | "
                        + "Fill Δ | Total IS (bps) | IS Δ (bps) | "
                        + "Avg Spread (bps) | Market Volume | "
                        + "Added Latency (ms) |\n"
        );

        output.append(
                "|---|---|---:|---:|---:|---:|---:|---:|---:|\n"
        );

        for (
                ScenarioRunResult result :
                ordered(results)
        ) {
            ScenarioRunResult baseline =
                    baselines.get(
                            result.strategy()
                    );

            output.append(
                    String.format(
                            Locale.US,
                            "| %s | %s | %.2f%% | %+.2f%% | "
                                    + "%.2f | %+.2f | %.2f | "
                                    + "%d | %d |%n",
                            result.scenarioName(),
                            result.strategy(),
                            result.fillRate() * 100.0,
                            (
                                    result.fillRate()
                                            - baseline.fillRate()
                            ) * 100.0,
                            result.totalShortfallBps(),
                            result.totalShortfallBps()
                                    - baseline
                                    .totalShortfallBps(),
                            result.diagnostics()
                                    .averageSpreadBps(),
                            result.totalMarketVolume(),
                            result.scenario()
                                    .additionalLatencyMs()
                    )
            );
        }

        output.append(
                "\n## Scenario definitions\n\n"
        );

        output.append(
                "| Scenario | Spread × | Liquidity × | "
                        + "Volatility × | Queue × | Gap (bps) | "
                        + "Unavailable Venues | Added Latency (ms) |\n"
        );

        output.append(
                "|---|---:|---:|---:|---:|---:|---|---:|\n"
        );

        for (
                StressScenario scenario :
                distinctScenarios(results)
        ) {
            String unavailableVenues =
                    scenario.unavailableVenues()
                            .isEmpty()
                            ? "—"
                            : String.join(
                                    ";",
                                    scenario
                                            .unavailableVenues()
                                            .stream()
                                            .sorted()
                                            .toList()
                            );

            output.append(
                    String.format(
                            Locale.US,
                            "| %s | %.2f | %.2f | %.2f | "
                                    + "%.2f | %.2f | %s | %d |%n",
                            scenario.name(),
                            scenario.spreadMultiplier(),
                            scenario.liquidityMultiplier(),
                            scenario.volatilityMultiplier(),
                            scenario.queueDepthMultiplier(),
                            scenario.gapBps(),
                            unavailableVenues,
                            scenario.additionalLatencyMs()
                    )
            );
        }

        output.append(
                "\n## Interpretation\n\n"
        );

        output.append(
                "The framework is deterministic sensitivity "
                        + "analysis. It does not assign probabilities "
                        + "to scenarios and does not claim that the "
                        + "transformations reproduce a live exchange "
                        + "order book.\n"
        );

        Files.writeString(
                path,
                output.toString()
        );
    }

    private static Map<String, ScenarioRunResult>
    baselinesByStrategy(
            List<ScenarioRunResult> results
    ) {
        Map<String, ScenarioRunResult> baselines =
                new HashMap<>();

        for (ScenarioRunResult result : results) {
            if (
                    !result.scenarioName()
                            .equals("BASELINE")
            ) {
                continue;
            }

            ScenarioRunResult previous =
                    baselines.put(
                            result.strategy(),
                            result
                    );

            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate baseline for strategy "
                                + result.strategy()
                );
            }
        }

        for (ScenarioRunResult result : results) {
            if (
                    !baselines.containsKey(
                            result.strategy()
                    )
            ) {
                throw new IllegalArgumentException(
                        "missing baseline for strategy "
                                + result.strategy()
                );
            }
        }

        return Map.copyOf(baselines);
    }

    private static List<ScenarioRunResult> ordered(
            List<ScenarioRunResult> results
    ) {
        return results.stream()
                .sorted(
                        Comparator.comparingInt(
                                (
                                        ScenarioRunResult
                                                result
                                ) ->
                                        result.scenarioName()
                                                .equals(
                                                        "BASELINE"
                                                )
                                                ? 0
                                                : 1
                        ).thenComparing(
                                ScenarioRunResult::
                                        scenarioName
                        ).thenComparing(
                                ScenarioRunResult::
                                        strategy
                        )
                )
                .toList();
    }

    private static List<StressScenario>
    distinctScenarios(
            List<ScenarioRunResult> results
    ) {
        return results.stream()
                .map(
                        ScenarioRunResult::scenario
                )
                .distinct()
                .sorted(
                        Comparator.comparing(
                                StressScenario::name
                        )
                )
                .toList();
    }

    private static void validateInputs(
            Path path,
            List<ScenarioRunResult> results
    ) {
        if (path == null) {
            throw new IllegalArgumentException(
                    "path is required"
            );
        }

        if (
                results == null
                        || results.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "results are required"
            );
        }

        if (
                results.stream()
                        .anyMatch(
                                result -> result == null
                        )
        ) {
            throw new IllegalArgumentException(
                    "results cannot contain null"
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

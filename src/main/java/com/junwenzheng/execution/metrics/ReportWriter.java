package com.junwenzheng.execution.metrics;

import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class ReportWriter {
    private ReportWriter() {}

    public static List<ExecutionMetrics> metrics(List<SimulationResult> results) {
        return results.stream()
                .map(ExecutionMetrics::from)
                .sorted(Comparator.comparingDouble(ExecutionMetrics::slippageVsArrivalBps))
                .toList();
    }

    public static void writeCsv(Path path, List<ExecutionMetrics> metrics) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder sb = new StringBuilder("strategy,parentQuantity,filledQuantity,fillRate,averagePrice,marketVwap,arrivalPrice,slippageVsArrivalBps,slippageVsVwapBps,rejectedChildren\n");
        for (ExecutionMetrics metric : metrics) {
            sb.append(metric.toCsvRow()).append('\n');
        }
        Files.writeString(path, sb.toString());
    }

    public static void writeMicrostructureDiagnostics(Path path, MarketDataReplay replay) throws IOException {
        Files.createDirectories(path.getParent());
        MicrostructureDiagnostics diagnostics = MicrostructureDiagnostics.from(replay);
        StringBuilder sb = new StringBuilder();
        sb.append("# Market microstructure diagnostics\n\n");
        sb.append("These diagnostics summarize the replay tape used by all execution strategies. They are intended to make data assumptions visible before comparing algorithm performance.\n\n");
        sb.append(diagnostics.toMarkdown()).append('\n');
        sb.append("\n## Notes\n\n");
        sb.append("Average spread and midpoint volatility are simple proxies for trading difficulty. A richer production analysis would add venue-level liquidity, queue position, auction periods, and fee/rebate schedules.\n");
        Files.writeString(path, sb.toString());
    }

    public static void writeMarkdown(Path path, List<ExecutionMetrics> metrics) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("# Execution quality report\n\n");
        sb.append("This report compares execution strategies under the same parent order and replayed market tape. Lower slippage is better for a BUY order.\n\n");
        sb.append("| Strategy | Fill Rate | Avg Price | Market VWAP | Arrival Slip (bps) | VWAP Slip (bps) | Rejected Children |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|\n");
        for (ExecutionMetrics m : metrics) {
            sb.append(String.format(java.util.Locale.US,
                    "| %s | %.2f%% | %.4f | %.4f | %.2f | %.2f | %d |%n",
                    m.strategy(), m.fillRate() * 100.0, m.averagePrice(), m.marketVwap(),
                    m.slippageVsArrivalBps(), m.slippageVsVwapBps(), m.rejectedChildren()));
        }
        sb.append("\n## Interpretation\n\n");
        sb.append("The result should not be interpreted as a profitable trading signal. It is an execution-quality comparison under a deterministic replay and fill model. The useful engineering evidence is the explicit order lifecycle, reproducible market replay, strategy abstraction, risk gate, fill constraints, and evaluation output.\n");
        Files.writeString(path, sb.toString());
    }
}

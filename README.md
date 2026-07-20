# Tokyo Equity Execution Lab

A Java 21 trading-systems case study for equities execution technology roles.

This project is not an alpha-prediction toy. It focuses on the engineering layer that sits near a real algorithmic execution stack: market-data replay, parent/child order lifecycle, execution-algo scheduling, fill simulation, position/risk tracking, execution-quality metrics, and reproducible reports.

The sample data in `data/sample_market_data.csv` is synthetic so the repository runs offline and remains reproducible. The parser expects a simple trade/quote replay format, so the same engine can be pointed at external public market data after normalisation.

## Why this project exists

The target role is a Java algo/execution technology role, not a pure quant researcher role. The project therefore demonstrates:

- Server-side Java design for a market-data replay and execution simulation engine
- Order and fill state machines with realistic failure modes and risk checks
- TWAP, VWAP, and POV execution strategies
- Execution-quality metrics such as fill rate, implementation shortfall, VWAP slippage, participation, and turnover
- Latency/throughput microbenchmarks and reproducible command-line runs
- Research logs showing iteration, assumptions, limitations, and what did not work

## Quick start

```bash
./scripts/run_tests.sh
./scripts/run_demo.sh
```

Outputs are written to `reports/`:

- `execution_summary.csv`
- `execution_report.md`
- `microstructure_diagnostics.md`
- `latency_benchmark.txt`

## Repository structure

```text
src/main/java/com/junwenzheng/execution
  algo/       TWAP, VWAP, POV execution strategies
  engine/     Fill model, risk manager, simulator, positions
  market/     Market data record, replay, synthetic generator
  metrics/    Execution quality metrics and report writer
  order/      Parent/child order, fill, side, status
  util/       CSV and formatting helpers
src/test/java/com/junwenzheng/execution
  TestRunner.java
scripts/
  run_demo.sh, run_tests.sh, compile.sh
docs/
  research_log.md, design_notes.md, market_microstructure_notes.md, strategy_comparison.md
```

## Design principles

1. **Do not fake trading expertise.** The project explains assumptions and limitations rather than claiming production-market realism.
2. **Show engineering depth.** The goal is reliable Java architecture, explicit state transitions, reproducible evaluation, and testability.
3. **Evaluate execution quality, not PnL.** The algorithms are judged on slippage, fill behaviour, implementation shortfall, VWAP deviation, and data assumptions, not on future price prediction.
4. **Keep the repo inspectable.** No heavyweight framework is required; the project builds with `javac` and runs with Java 21.

## Current limitations

- Synthetic market-data sample is used for reproducibility. Real external trade/quote data should be normalised into the same schema before serious evaluation.
- The fill model approximates liquidity constraints using reported event volume and configurable participation limits. It does not model queue position or exchange matching rules.
- SOR is represented as a design extension in `docs/design_notes.md`; the implemented strategies focus on single-venue TWAP/VWAP/POV behaviour.


## Build and test

The v2 development path uses Java 21, Maven, and JUnit 5.

Run the complete test suite with:

```bash
mvn -B test
```

or:

```bash
bash scripts/run_tests.sh
```

Compile and run the demo with:

```bash
bash scripts/run_demo.sh
```

GitHub Actions runs `mvn -B verify` on every push to `main` and on pull requests.

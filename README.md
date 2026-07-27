# Tokyo Equity Execution Lab

A Java 21 trading-systems case study for equities execution technology roles.

This project is not an alpha-prediction toy. It focuses on the engineering layer that sits near a real algorithmic execution stack: market-data replay, parent/child order lifecycle, execution-algo scheduling, fill simulation, position/risk tracking, execution-quality metrics, and reproducible reports.

The sample data in `data/sample_market_data.csv` is synthetic so the repository runs offline and remains reproducible. The parser expects a simple trade/quote replay format, so the same engine can be pointed at external public market data after normalisation.

## Why this project exists

The target role is a Java algo/execution technology role, not a pure quant researcher role. The project therefore demonstrates:

- Server-side Java design for a market-data replay and execution simulation engine
- Order and fill state machines with realistic failure modes and risk checks
- TWAP, online VWAP, oracle VWAP, and POV execution strategies
- Deterministic multi-venue smart order routing with venue costs and liquidity constraints
- Opt-in Tokyo sessions, auctions, tick-size tables, and board-lot enforcement
- Execution-quality metrics such as fill rate, implementation shortfall, VWAP slippage, participation, and turnover
- Deterministic latency modelling and reproducible command-line runs
- Research logs showing iteration, assumptions, limitations, and implementation defects

## Quick start

```bash
./scripts/run_tests.sh
./scripts/run_demo.sh
```

Outputs are written to `reports/`:

- `execution_summary.csv`
- `execution_report.md`
- `microstructure_diagnostics.md`
- `transaction_cost_summary.csv`
- `venue_cost_attribution.csv`
- `transaction_cost_report.md`
- `latency_benchmark.txt`

## Repository structure

```text
src/main/java/com/junwenzheng/execution
  algo/       TWAP, online/oracle VWAP, and POV scheduling
  engine/     Simulator, fill model, latency, risk, positions
  market/     Typed events, deterministic replay, synthetic data
  metrics/    Execution metrics, TCA, diagnostics, and reports
  order/      Parent/child lifecycle, fills, side, status
  routing/    Venue configuration and smart order routing
  rules/      Tokyo sessions, tick sizes, and board lots
  util/       CSV and formatting helpers
src/test/java/com/junwenzheng/execution
  JUnit unit, compatibility, and integration tests
scripts/
  run_demo.sh, run_tests.sh, compile.sh
docs/
  design notes, microstructure notes, strategy comparison, daily research logs
```

## Design principles

1. **Do not fake trading expertise.** The project explains assumptions and limitations rather than claiming production-market realism.
2. **Show engineering depth.** The goal is reliable Java architecture, explicit state transitions, reproducible evaluation, and testability.
3. **Evaluate execution quality, not PnL.** The algorithms are judged on slippage, fill behaviour, implementation shortfall, VWAP deviation, and data assumptions, not on future price prediction.
4. **Keep the repo inspectable.** The project uses Java 21, Maven, and JUnit 5 while retaining small command-line scripts and explicit domain objects.

## Current limitations

- Synthetic market data is used for reproducibility. External trade and quote data must be normalised before serious evaluation.
- The deterministic fill model approximates liquidity using event volume, displayed queue depth, participation limits, and configurable slippage. It is not an exchange matching engine.
- Smart order routing uses snapshot liquidity and configured costs; it does not model message races, live order amendments, or order-book priority.
- Transaction cost analysis uses replay midpoints and deterministic fill-model components. It is an attribution framework, not an estimate of realised broker or exchange accounting.
- Tokyo rules cover core session, auction, tick-size, and board-lot constraints but not the complete JPX rulebook.
- The included demo continues to use relative timestamps and the generic simulator. Tokyo rules are enabled explicitly through the Tokyo simulator factories.


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

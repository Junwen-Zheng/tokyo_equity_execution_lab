# Deterministic scenario and stress framework

## Objective

Add a repeatable stress-testing layer for comparing execution algorithms under
controlled changes to spreads, liquidity, volatility, queue depth, market
gaps, venue availability, and latency.

## Architecture

The implementation separates three responsibilities:

1. `StressScenario` defines immutable stress parameters.
2. `ScenarioMarketTransformer` creates a new stressed replay.
3. `ScenarioStressRunner` executes every algorithm under every scenario and
   calculates transaction-cost attribution.

`ScenarioStressReportWriter` produces baseline-relative CSV and Markdown
reports.

## Scenario transformations

### Spread widening

Bid and ask are widened symmetrically around the stressed midpoint. The
midpoint therefore remains unchanged when spread is the only active stress.

### Liquidity reduction

Event volume is multiplied by a scenario factor and rounded down. This affects
participation-based algorithms, fill capacity, and modeled market impact.

### Queue-depth reduction

Displayed queue depth is scaled independently from volume. Routed execution
uses the smaller of queue depth and venue participation capacity, so queue
depletion can change allocations, execution timing, and shortfall.

### Volatility amplification

Midpoint returns are amplified independently for each symbol and venue. This
preserves deterministic sequencing while increasing the magnitude of market
movement.

### Price gap

A persistent multiplicative gap begins at a deterministic timestamp derived
from the configured replay fraction. Simultaneous venue events cross the gap
together.

### Venue outage

Configured venues are removed from the replay. A scenario that removes every
event is rejected rather than producing a misleading empty simulation.

### Adverse latency

A fixed amount is added to every latency-pipeline stage. The current simulator
records those later lifecycle timestamps but does not reprice against later
market data.

### Combined severe stress

The combined scenario applies:

- 3× spread
- 25% baseline volume
- 2.5× midpoint volatility
- 2% baseline queue depth
- 150-basis-point persistent price gap
- 25 milliseconds additional latency per pipeline stage

## Baseline-relative reporting

Every strategy has its own baseline result. Stress reports calculate:

    fill-rate delta =
        stressed fill rate - baseline fill rate

    shortfall delta =
        stressed total implementation shortfall bps
        - baseline total implementation shortfall bps

A positive shortfall delta is adverse.

## Deterministic sample findings

The sample demonstrates several distinct failure modes:

- spread widening increases execution cost without reducing fill rate
- high volatility materially increases implementation shortfall
- thin liquidity especially harms POV and online VWAP completion
- queue depletion changes routed execution behaviour
- the persistent price gap penalises algorithms that continue executing later
- combined stress produces substantial opportunity cost and fill degradation
- latency-only stress does not alter economics under the current snapshot model

These results are sensitivity analysis, not scenario probabilities or
predictions of live exchange behaviour.

## Verification

Added:

- 9 market-transformation tests
- 6 stress-runner tests
- 4 stress-report tests

Current suite:

- 157 tests passed
- 0 failures
- 0 errors
- 0 skipped

## Limitations

The framework does not model:

- stochastic scenario likelihood
- dynamic order-book reconstruction
- queue position or cancellation races
- delayed repricing against later snapshots
- exchange halts or volatility interruptions
- correlated multi-asset shocks
- broker fees, taxes, or realised exchange invoices

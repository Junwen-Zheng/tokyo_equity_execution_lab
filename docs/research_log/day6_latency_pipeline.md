# Day 6 — Deterministic Latency Pipeline

## Objective

Separate market-event time from simulated decision, risk, acknowledgement,
fill, rejection, and cancellation timing.

## Latency profile

`LatencyProfile` defines deterministic stage delays for:

- decision processing
- risk checking
- acknowledgement
- fill processing
- cancellation

All delays are non-negative and validated at construction.

The baseline profile uses fixed delays so repeated simulations produce the
same lifecycle timestamps.

A zero-latency profile is also available for controlled tests.

## Deterministic pipeline

`DeterministicLatencyPipeline` advances timestamps using checked arithmetic.

It rejects:

- negative base timestamps
- negative configured delays
- timestamp overflow

The pipeline contains no wall-clock calls, random sampling, or mutable global
state.

## Lifecycle stages

Each generated child order records latency events for the stages it reaches:

- `MARKET_EVENT`
- `DECISION`
- `RISK_CHECK`
- `ACKNOWLEDGEMENT`
- `REJECTION`
- `FILL`
- `CANCELLATION`

Risk-rejected children stop after the rejection stage.

Filled children record acknowledgement and fill stages.

Partially filled and no-fill children record cancellation after the fill
attempt delay.

## Fill timestamps

`FillModel` now accepts an explicit simulated fill timestamp.

The model validates that the timestamp does not precede:

- the market event
- the child order's latest lifecycle update

The original three-argument fill method remains available and defaults to the
market-event timestamp for direct model tests.

## Simulation results

`SimulationResult` now retains an immutable list of latency events.

This provides an observable event timeline without changing execution metric
calculations.

## Wall-clock benchmarking

`LatencyBenchmark` continues to use `System.nanoTime()` only to measure actual
runtime performance.

Wall-clock benchmark measurements remain separate from simulated lifecycle
latency.

## Verification

The suite contains 50 tests covering:

- existing execution behaviour
- market replay ordering
- order lifecycle transitions
- liquidity-aware fills
- deterministic latency-stage arithmetic
- zero-latency profiles
- timestamp overflow handling
- filled-child stage ordering
- partial-fill cancellation timing
- risk rejection timing
- immutable latency-event results

## Day 6 conclusion

Execution lifecycle timing is now explicit and reproducible. Market time,
simulated processing time, and wall-clock benchmark time are separate
concepts.

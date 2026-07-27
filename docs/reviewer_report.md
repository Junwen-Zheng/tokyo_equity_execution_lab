# Independent reviewer report

## Review objective

Evaluate whether version 2.0.0 is suitable for publication as a Java execution
technology case study.

The review focuses on software architecture, deterministic behaviour,
testability, execution-domain modelling, reproducibility, and honesty about
limitations. It does not certify the project as production trading software.

## Release verdict

Version 2.0.0 is suitable for publication as an inspectable engineering case
study.

The repository demonstrates substantially more than algorithm pseudocode. It
contains explicit domain state, validated market events, deterministic replay,
parent and child order lifecycles, risk decisions, latency stages, routed
execution, market rules, transaction-cost attribution, stress testing, and
repeatable performance measurements.

The project consistently distinguishes model behaviour from live-market
claims. That distinction is essential to the release verdict.

## Reviewed areas

### Build and verification

- Java 21 compiler target
- Maven build lifecycle
- JUnit 5 unit and integration tests
- GitHub Actions verification
- generated-repository-state checks
- separate JMH benchmark module

### Market-data replay

- typed event categories
- deterministic ordering by timestamp and source sequence
- immutable replay storage
- CSV validation and line-specific parse failures
- filtering by symbol, venue, type, and replay window

### Order lifecycle

- parent-order working and terminal states
- child-order acknowledgement, partial fill, fill, rejection, and cancellation
- monotonic lifecycle timestamps
- residual parent and child cancellation
- no-fill lifecycle handling

### Execution modelling

- TWAP scheduling
- participation-of-volume scheduling
- implementable online VWAP scheduling
- explicitly labelled oracle VWAP benchmark
- spread and participation impact
- displayed queue and executable-liquidity constraints
- lot-size enforcement

### Risk and latency

- child-size and notional controls
- position-aware risk evaluation
- deterministic decision, risk, acknowledgement, fill, and cancellation timing
- immutable latency-event output

### Routing and Tokyo rules

- deterministic effective-price ranking
- venue fees and adverse-selection penalties
- participation and queue-depth capacity
- stable venue and source-sequence tie-breaking
- Tokyo trading sessions and auctions
- tick-size validation
- board-lot validation

### Evaluation

- fill rate and execution-price metrics
- arrival-price and VWAP comparison
- implementation-shortfall decomposition
- venue-level cost attribution
- deterministic stress scenarios
- baseline-relative stress reporting

### Performance methodology

- OpenJDK JMH harness
- isolated benchmark module
- replay, routing, single-venue, and routed execution paths
- warm-up, measurement, and fork controls
- optional allocation and garbage-collection profiling
- no numeric performance gate on shared CI hardware

## Strengths

1. Determinism is treated as an architectural requirement rather than an
   incidental property.
2. Domain objects validate their own invariants instead of relying solely on
   calling code.
3. Offline oracle information is explicitly separated from implementable
   online behaviour.
4. Execution quality is evaluated without converting synthetic output into
   unsupported profitability claims.
5. Routing, fill capacity, risk, latency, and market rules are integrated
   rather than presented as disconnected demonstrations.
6. Stress results are compared against strategy-specific baselines.
7. Performance claims are separated from smoke-test results and environment-
   specific measurements.
8. Known model limitations are documented throughout the repository.

## Material limitations

The release does not model:

- exchange-grade order-book reconstruction
- queue position and cancellation races
- live network transport or exchange gateways
- asynchronous multi-threaded strategy execution
- market-data packet loss and recovery
- production persistence and audit infrastructure
- complete JPX rules
- stochastic scenario probabilities
- delayed repricing against future market snapshots
- real broker fees, taxes, or exchange invoices
- calibrated market impact from proprietary execution data

These limitations do not invalidate the case study, but they prevent the
repository from being described as a production trading platform.

## Reviewer conclusion

The release demonstrates credible Java execution-system engineering while
remaining explicit about abstraction boundaries.

The appropriate public description is:

> A deterministic Java 21 execution-system case study covering market replay,
> order lifecycle, algorithms, risk, routing, Tokyo market rules, transaction
> cost analysis, stress testing, and reproducible JMH benchmarks.

Descriptions implying live trading readiness, exchange matching fidelity, or
validated production latency would not be supported by the evidence.

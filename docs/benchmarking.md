# Performance benchmarking

## Purpose

Runtime benchmarking is separated from simulated execution latency.

- simulated latency models deterministic lifecycle timestamps
- JMH measures wall-clock JVM performance
- neither measurement predicts exchange round-trip latency

The benchmark harness is a separate Maven project under `benchmarks/`. It
depends on the main execution-lab artifact and produces an executable shaded
JAR.

## Benchmarked paths

### Replay VWAP

`replayVwap` scans a 480-event immutable replay and calculates volume-weighted
average price.

This isolates replay aggregation without order lifecycle or routing work.

### Four-venue routing

`routeFourVenueSnapshot` routes a 2,000-share BUY order across four venues with
different:

- prices
- fees
- adverse-selection penalties
- participation limits
- queue depths

The benchmark includes candidate construction, side-aware effective-price
ranking, deterministic tie-breaking, and allocation.

### Single-venue execution

`singleVenuePov` executes a fresh 20,000-share parent order over 480 market
events with:

- POV scheduling
- pre-trade risk
- child-order lifecycle
- fill generation
- position updates
- deterministic latency events

A new parent order is created for every benchmark invocation.

### Routed execution

`routedPov` executes the same parent quantity over 120 four-venue snapshots,
480 events in total.

It includes:

- consolidated snapshot construction
- scheduling
- smart order routing
- venue-specific child orders
- risk checks
- fills
- lifecycle and routing result construction

## Commands

Build and run the configured benchmark suite:

    bash scripts/run_benchmarks.sh

Run a short harness smoke test:

    bash scripts/run_benchmark_smoke.sh

Run with JMH's garbage-collection profiler:

    bash scripts/run_benchmark_profile.sh

Run selected benchmarks:

    bash scripts/run_benchmarks.sh '.*routedPov.*'

Override benchmark settings through standard JMH arguments:

    bash scripts/run_benchmarks.sh \
        -wi 5 \
        -i 8 \
        -f 3 \
        -w 1s \
        -r 1s

Write machine-readable JSON:

    bash scripts/run_benchmarks.sh \
        -rf json \
        -rff benchmark-results.json

Generated benchmark results are intentionally not committed. Runtime figures
depend on the JVM, operating system, processor, power state, thermal state, and
background workload.

## Default methodology

The benchmark class uses:

- average-time mode
- microseconds per operation
- one benchmark thread
- three warm-up iterations
- five measurement iterations
- two isolated JVM forks
- fixed 512 MiB initial and maximum heap

JMH consumes benchmark return values, preventing trivial dead-code
elimination.

## Interpretation rules

Performance figures should only be compared when the following remain
consistent:

- source revision
- benchmark parameters
- JVM distribution and version
- JMH version
- processor and operating system
- power and thermal conditions
- profiler configuration

A smoke run with one fork and one short iteration only verifies that the
harness works. It is not statistically adequate for performance conclusions.

Allocation results from `-prof gc` are useful for locating object-heavy paths,
but they should be interpreted alongside execution time and domain
correctness. Lower allocation is not automatically better if it complicates
the model or changes execution semantics.

## Current limitations

The benchmark fixtures are deterministic synthetic workloads. They do not
represent:

- production market-data decoding
- network transport
- exchange gateways
- persistence
- concurrent strategy execution
- multi-symbol contention
- live garbage-collector tuning
- end-to-end trading-system latency

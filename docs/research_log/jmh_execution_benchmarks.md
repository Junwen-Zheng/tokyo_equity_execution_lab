# JMH execution benchmarks

## Objective

Replace informal wall-clock timing as the primary performance methodology with
a reproducible OpenJDK JMH harness.

The existing lightweight `LatencyBenchmark` remains useful for demonstration
output, but JMH is now the appropriate tool for comparative JVM measurements.

## Module structure

The benchmark harness lives in a separate Maven project:

    benchmarks/

It depends on the installed main project artifact and packages:

    benchmarks/target/benchmarks.jar

The main application therefore remains free of JMH runtime dependencies.

## Benchmark coverage

Four execution paths are covered:

1. replay VWAP aggregation
2. four-venue smart-order routing
3. complete single-venue POV simulation
4. complete routed POV simulation

The complete simulation benchmarks construct a fresh parent order per
invocation so terminal order state is never reused.

Immutable replays, simulators, routing configuration, and stateless algorithm
configuration are prepared once per trial.

## Harness controls

The default benchmark configuration uses:

- average-time mode
- microsecond output
- three warm-up iterations
- five measurement iterations
- two forks
- one thread
- fixed 512 MiB heap

A short smoke script overrides these settings to one warm-up iteration, one
measurement iteration, and one fork. Its purpose is build and runtime
validation only.

## Profiling

The profiling script enables JMH's GC profiler:

    bash scripts/run_benchmark_profile.sh

This reports allocation rate, allocation per operation, and garbage-collection
activity where supported.

## CI integration

Continuous integration runs the short JMH smoke suite after the main Maven
verification. This checks:

- application artifact installation
- benchmark-module compilation
- annotation processing
- shaded executable JAR creation
- benchmark discovery
- successful execution of every benchmark method

CI does not enforce numeric performance thresholds because shared hosted
runners do not provide sufficiently stable hardware for reliable regression
gates.

## Initial smoke checkpoint

The first local smoke run discovered and executed all four benchmark methods.
The root regression suite remained unchanged:

- 157 tests passed
- 0 failures
- 0 errors
- 0 skipped

The resulting timings are deliberately not treated as durable benchmark
claims because the smoke configuration provides insufficient warm-up,
measurement duration, and fork count.

## Limitations

The harness currently benchmarks single-threaded deterministic workloads. It
does not yet test:

- concurrent parent orders
- multiple symbols
- contention between strategies
- CSV parsing
- file-system report generation
- network or exchange-gateway latency
- production order-book structures

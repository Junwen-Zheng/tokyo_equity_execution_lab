# Day 2 — Maven, JUnit 5, and CI

## Objective

Replace the custom shell-only test infrastructure with a standard Java build
and automated verification pipeline.

## Build migration

The repository now uses Maven with:

- Java 21 compilation
- JUnit 5
- Maven Compiler Plugin
- Maven Surefire Plugin
- deterministic Maven test entry points

The existing shell scripts remain as convenient wrappers around Maven.

## Test migration

The original custom `TestRunner` was replaced by JUnit test classes.

The migrated suite covers:

- generated market-data replay
- replay VWAP and microstructure diagnostics
- POV execution metrics
- implementation shortfall
- TWAP and VWAP execution
- latency benchmark measurement and report writing
- the four Day 1 legacy-behaviour characterizations

The suite currently contains eight tests.

## Benchmark isolation

Latency measurement and report persistence are now separate operations.

Tests write benchmark output only to JUnit temporary directories and no longer
modify the tracked `reports/latency_benchmark.txt` file.

The legacy `LatencyBenchmark.run` method remains available for the demo and
continues to write the standard report.

## Continuous integration

GitHub Actions now:

1. checks out the repository
2. installs Eclipse Temurin Java 21
3. caches Maven dependencies
4. runs `mvn -B verify`
5. confirms that verification does not modify tracked files

## Day 2 conclusion

The repository now has a standard Java build, isolated JUnit tests, and
automated CI verification.

Production execution behaviour remains unchanged. Behavioural corrections
begin on Day 3.

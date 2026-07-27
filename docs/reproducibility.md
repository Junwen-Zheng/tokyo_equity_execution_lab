# Reproducibility guide

## Supported environment

The release is designed for:

- Java 21
- Maven 3.9 or later
- Bash
- Git
- an offline-capable local checkout after dependencies are cached

The reference development environment uses Eclipse Temurin Java 21.

## Clean verification

From the repository root:

    bash scripts/verify_release.sh

The release verifier performs:

1. Maven verification of the main project
2. all 157 JUnit tests
3. benchmark-module compilation and packaging
4. execution of the four short JMH smoke benchmarks
5. deterministic demonstration execution
6. existence checks for generated reports
7. cleanup of generated report changes
8. whitespace and repository-report checks

## Individual commands

Run the automated tests:

    mvn -B verify

Run the deterministic demonstration:

    bash scripts/run_demo.sh

Run the short JMH harness validation:

    bash scripts/run_benchmark_smoke.sh

Run the configured JMH suite:

    bash scripts/run_benchmarks.sh

Run with allocation profiling:

    bash scripts/run_benchmark_profile.sh

## Generated reports

The demonstration writes:

- reports/execution_summary.csv
- reports/execution_report.md
- reports/microstructure_diagnostics.md
- reports/transaction_cost_summary.csv
- reports/venue_cost_attribution.csv
- reports/transaction_cost_report.md
- reports/stress_scenario_summary.csv
- reports/stress_scenario_report.md
- reports/latency_benchmark.txt

Some reports are tracked examples and some are generated locally. The release
verification script restores the repository's tracked report state after
checking the outputs.

## Determinism boundary

The following are deterministic for a fixed source revision and Java runtime:

- event ordering
- synthetic fixtures
- algorithm decisions
- routing tie-breaking
- lifecycle timestamps
- fills
- transaction-cost output
- stress transformations
- CSV and Markdown report ordering

JMH wall-clock measurements are not deterministic. They depend on processor,
operating system, JVM, background load, thermal state, and benchmark settings.

## Source integrity

Before creating a release tag:

    git status --short
    git diff --check
    git rev-parse HEAD

The working tree must be clean and the release commit must match the commit
referenced by the annotated tag.

## Interpretation

Successful reproduction establishes that the repository builds and produces
the documented deterministic outputs. It does not establish that the model
matches live exchange behaviour or production execution performance.

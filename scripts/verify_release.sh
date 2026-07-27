#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.."
  pwd
)"

cd "$ROOT_DIR"

cleanup_reports() {
  git restore -- reports

  rm -f \
    reports/microstructure_diagnostics.md \
    reports/transaction_cost_report.md \
    reports/transaction_cost_summary.csv \
    reports/venue_cost_attribution.csv \
    reports/stress_scenario_report.md \
    reports/stress_scenario_summary.csv
}

trap cleanup_reports EXIT

python3 - <<'PY'
from pathlib import Path

paths = [
    Path("pom.xml"),
    Path("benchmarks/pom.xml"),
]

for path in paths:
    text = path.read_text()

    if "2.0.0-SNAPSHOT" in text:
        raise SystemExit(
            f"{path} still contains a snapshot version"
        )

    if "<version>2.0.0</version>" not in text:
        raise SystemExit(
            f"{path} does not contain version 2.0.0"
        )
PY

mvn -B verify
bash scripts/run_benchmark_smoke.sh
bash scripts/run_demo.sh

test -s reports/execution_report.md
test -s reports/execution_summary.csv
test -s reports/transaction_cost_report.md
test -s reports/transaction_cost_summary.csv
test -s reports/venue_cost_attribution.csv
test -s reports/stress_scenario_report.md
test -s reports/stress_scenario_summary.csv
test -s reports/latency_benchmark.txt

cleanup_reports
trap - EXIT

git diff --exit-code -- reports
git diff --check

#!/usr/bin/env bash
set -euo pipefail

"$(dirname "$0")/run_benchmarks.sh" \
  -prof gc \
  "$@"

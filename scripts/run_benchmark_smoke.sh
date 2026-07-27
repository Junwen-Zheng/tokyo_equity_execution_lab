#!/usr/bin/env bash
set -euo pipefail

"$(dirname "$0")/run_benchmarks.sh" \
  -wi 1 \
  -i 1 \
  -f 1 \
  -w 200ms \
  -r 200ms \
  -foe true

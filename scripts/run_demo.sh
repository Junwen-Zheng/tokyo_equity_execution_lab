#!/usr/bin/env bash
set -euo pipefail

./scripts/compile.sh
java -cp target/classes \
  com.junwenzheng.execution.App

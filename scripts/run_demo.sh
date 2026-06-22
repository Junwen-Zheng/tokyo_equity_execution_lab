#!/usr/bin/env bash
set -euo pipefail
./scripts/compile.sh
java -cp build/classes com.junwenzheng.execution.App

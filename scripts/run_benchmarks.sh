#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.."
  pwd
)"

cd "$ROOT_DIR"

mvn -B \
  -Dmaven.test.skip=true \
  install

mvn -B \
  -f benchmarks/pom.xml \
  clean \
  package

java -jar \
  benchmarks/target/benchmarks.jar \
  "$@"

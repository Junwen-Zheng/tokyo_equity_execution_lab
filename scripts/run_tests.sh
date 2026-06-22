#!/usr/bin/env bash
set -euo pipefail
./scripts/compile.sh
find src/test/java -name '*.java' > build/test-sources.txt
javac --release 21 -cp build/classes -d build/classes @build/test-sources.txt
java -cp build/classes com.junwenzheng.execution.TestRunner

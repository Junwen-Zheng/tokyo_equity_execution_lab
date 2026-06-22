#!/usr/bin/env bash
set -euo pipefail
rm -rf build/classes
mkdir -p build/classes
find src/main/java -name '*.java' > build/sources.txt
javac --release 21 -d build/classes @build/sources.txt

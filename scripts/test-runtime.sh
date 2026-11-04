#!/usr/bin/env bash
# Each lifecycle test owns a fresh world; never reuses or deletes an existing save.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p work
run_dir=$(mktemp -d work/runtime-test.XXXXXX)
printf 'Runtime test world and logs: %s\n' "$run_dir"
exec scripts/gradle-cloud.sh runGameTestServer -PkingdomTestRunDir="$run_dir" --console=plain "$@"

#!/usr/bin/env bash
# Runs the Minecraft-independent rules; it does not replace ./gradlew build.
set -euo pipefail
cd "$(dirname "$0")/.."
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
cache_dir="${KINGDOM_TEST_CACHE:-.cache/domain-runner}"
mkdir -p "$cache_dir" artifacts/domain-tests
runner="$cache_dir/junit-platform-console-standalone-1.11.4.jar"
runner_hash=d8ba605f8f223d94b6a5a82debd641da9d5755cd8d68178afba45e3146d574f7b70dcc9d08864ca0abc0379dba055a26e0cdbef6499b862fcb4e54f1448d6bb8
if [[ ! -f "$runner" ]]; then
  curl --fail --location --show-error --silent --proto '=https' \
    https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar \
    --output "$runner"
fi
printf '%s  %s\n' "$runner_hash" "$runner" | sha512sum --check --status
classes_dir=$(mktemp -d "$cache_dir/classes.XXXXXX")
trap 'rm -rf "$classes_dir"' EXIT
mapfile -t sources < <(find src/main/java/com/mohymods/kingdomsmp/kingdom src/test/java/com/mohymods/kingdomsmp/kingdom -name '*.java' -type f | sort)
"$javac_bin" --release 21 -cp "$runner" -d "$classes_dir" "${sources[@]}" \
  src/main/java/com/mohymods/kingdomsmp/client/OverviewRequestTracker.java \
  src/main/java/com/mohymods/kingdomsmp/client/ui/RoyalLayout.java \
  src/test/java/com/mohymods/kingdomsmp/client/OverviewRequestTrackerTest.java \
  src/test/java/com/mohymods/kingdomsmp/client/ui/RoyalLayoutTest.java
"$java_bin" -jar "$runner" execute --class-path "$classes_dir" --scan-class-path \
  --fail-if-no-tests --details summary --reports-dir artifacts/domain-tests

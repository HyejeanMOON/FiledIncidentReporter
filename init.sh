#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required to initialize the project." >&2
  exit 1
fi
if ! command -v java >/dev/null 2>&1; then
  echo "Java is required to initialize the project." >&2
  exit 1
fi

mkdir -p incident-photos
mvn -Dkotlin.compiler.daemon=false -DskipTests package

# Starting Spring without the web server runs Flyway and creates the H2 database.
java -jar target/filed-incident-reporter-server-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none

echo "Initialization complete. Run ./start.sh to start the server."

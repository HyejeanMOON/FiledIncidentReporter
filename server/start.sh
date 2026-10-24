#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

if ! command -v java >/dev/null 2>&1; then
  echo "Java is required to start the server." >&2
  exit 1
fi

jar=target/filed-incident-reporter-server-0.0.1-SNAPSHOT.jar
if [[ ! -f "$jar" || ! -f incidents-db.mv.db ]]; then
  echo "Project is not initialized. Run ./init.sh first." >&2
  exit 1
fi

exec java -jar "$jar" "$@"

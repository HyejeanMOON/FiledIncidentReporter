#!/usr/bin/env bash
# Starts the incident server and the admin dashboard together, setting each one up on first run.
# Pass --init to rebuild the server and reinstall the dashboard's dependencies. Ctrl+C stops both.
# SERVER_PORT and ADMIN_PORT override the default ports 8080 and 3000.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

SERVER_PORT=${SERVER_PORT:-8080}
ADMIN_PORT=${ADMIN_PORT:-3000}
SERVER_JAR=server/target/filed-incident-reporter-server-0.0.1-SNAPSHOT.jar
LOG_DIR=logs

reinit=false
case "${1:-}" in
  "") ;;
  --init) reinit=true ;;
  *) echo "Usage: ./start.sh [--init]" >&2; exit 2 ;;
esac

fail() {
  echo "Error: $*" >&2
  exit 1
}

require() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required. $2"
}

require java "Install JDK 17 or later."
require node "Install Node.js 20.9 or later."
require npm "Install Node.js 20.9 or later."
require curl "Install curl."

java_major=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1)
[[ "${java_major:-0}" -ge 17 ]] || fail "JDK 17 or later is required; found Java ${java_major:-unknown}."
node -e 'const [major, minor] = process.versions.node.split(".").map(Number);
  process.exit(major > 20 || (major === 20 && minor >= 9) ? 0 : 1)' ||
  fail "Node.js 20.9 or later is required; found $(node -v)."

for port in "$SERVER_PORT" "$ADMIN_PORT"; do
  if lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    fail "Port $port is already in use. Stop the process using it and try again."
  fi
done

if $reinit || [[ ! -f "$SERVER_JAR" || ! -f server/incidents-db.mv.db ]]; then
  echo "==> Building the server (first run downloads Maven and dependencies)..."
  server/init.sh
fi

if $reinit || [[ ! -d admin/node_modules ]]; then
  echo "==> Installing the admin dashboard's dependencies..."
  (cd admin && npm ci)
fi

mkdir -p "$LOG_DIR"
server_pid=""
admin_pid=""

stop() {
  trap - EXIT INT TERM
  echo
  echo "==> Stopping..."
  for pid in $admin_pid $server_pid; do
    pkill -TERM -P "$pid" 2>/dev/null || true
    kill -TERM "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap stop EXIT
trap 'exit 130' INT TERM

echo "==> Starting the server on port $SERVER_PORT..."
server/start.sh --server.port="$SERVER_PORT" >"$LOG_DIR/server.log" 2>&1 &
server_pid=$!

echo "==> Starting the admin dashboard on port $ADMIN_PORT..."
(cd admin && INCIDENT_API_URL="http://localhost:$SERVER_PORT" \
  exec ./node_modules/.bin/next dev --port "$ADMIN_PORT") >"$LOG_DIR/admin.log" 2>&1 &
admin_pid=$!

# Waits until the URL answers, or fails with the end of the log if the process exits first.
wait_for() {
  local name=$1 pid=$2 url=$3 log=$4
  for _ in $(seq 1 180); do
    curl -s -o /dev/null "$url" && return 0
    if ! kill -0 "$pid" 2>/dev/null; then
      tail -n 40 "$log" >&2
      fail "The $name stopped while starting. Full log: $log"
    fi
    sleep 1
  done
  fail "The $name did not start within 3 minutes. Log: $log"
}

wait_for server "$server_pid" "http://localhost:$SERVER_PORT/incidents?size=1" "$LOG_DIR/server.log"
wait_for "admin dashboard" "$admin_pid" "http://localhost:$ADMIN_PORT" "$LOG_DIR/admin.log"

cat <<EOF

  Incident API       http://localhost:$SERVER_PORT/incidents
  Admin dashboard    http://localhost:$ADMIN_PORT
  Android app        Run the "app" configuration in Android Studio on an emulator,
                     or run ./gradlew installDebug with an emulator open.
                     The emulator reaches the API at http://10.0.2.2:$SERVER_PORT/.

  Logs are in $LOG_DIR/server.log and $LOG_DIR/admin.log. Press Ctrl+C to stop.

EOF

while kill -0 "$server_pid" 2>/dev/null && kill -0 "$admin_pid" 2>/dev/null; do
  sleep 1
done
fail "A process stopped unexpectedly. See $LOG_DIR/server.log and $LOG_DIR/admin.log."

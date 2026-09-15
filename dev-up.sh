#!/usr/bin/env bash
set -uo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$REPO/.dev-run"
mkdir -p "$RUN_DIR"

port_up() {
  local port="$1"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${port}" 2>/dev/null)
  [ "$code" != "000" ]
}

start_service() {
  local name="$1" port="$2" dir="$3" cmd="$4"
  if port_up "$port"; then
    echo "  $name already up on :$port — skipping"
    return
  fi
  ( cd "$dir" && nohup bash -c "$cmd" > "$RUN_DIR/$name.log" 2>&1 & )
  echo "  $name launching on :$port (log: $RUN_DIR/$name.log)"
}

wait_for() {
  local name="$1" port="$2"
  for _ in $(seq 1 30); do
    if port_up "$port"; then
      echo "  $name: up (:$port)"
      return 0
    fi
    sleep 3
  done
  echo "  $name: NOT up after 90s — check $RUN_DIR/$name.log"
  return 1
}

cmd_down() {
  # mvnw/npm wrappers fork detached children (the real java/node process
  # ends up outside the launcher's process group), so killing by pattern or
  # process group unreliably misses them. Killing whatever actually holds
  # the port is the one thing that always reaches the real process.
  local name port
  for entry in "landlord-backend:8080" "barivara-backend:8081" "landlord-frontend:4200" "barivara-frontend:4201"; do
    name="${entry%%:*}"; port="${entry##*:}"
    if fuser -k -TERM "${port}/tcp" >/dev/null 2>&1; then
      echo "stopped $name (:$port)"
    else
      echo "$name: not running (:$port)"
    fi
  done
  echo "Postgres left running (docker) — stop it yourself with: docker compose -f \"$REPO/docker-compose.yml\" stop"
}

if [ "${1:-}" = "down" ]; then
  cmd_down
  exit 0
fi

echo "== Postgres =="
docker compose -f "$REPO/docker-compose.yml" up -d

echo "== Launching services =="
start_service landlord-backend  8080 "$REPO/landlord-backend"                "./mvnw spring-boot:run"
start_service barivara-backend  8081 "$REPO/barivara-backend"                "./mvnw spring-boot:run"
start_service landlord-frontend 4200 "$REPO/LandLord-Angular-Project-R70"    "npx ng serve --port 4200 --host 0.0.0.0"
start_service barivara-frontend 4201 "$REPO/BariVara-Angular-Project-R70"    "npx ng serve --port 4201 --host 0.0.0.0"

echo "== Waiting for services =="
ok=1
wait_for landlord-backend  8080 || ok=0
wait_for barivara-backend  8081 || ok=0
wait_for landlord-frontend 4200 || ok=0
wait_for barivara-frontend 4201 || ok=0

echo
if [ "$ok" = "1" ]; then
  echo "All up:"
else
  echo "Some services did not come up in time — check logs above. Still up:"
fi
cat <<EOF

  LandLord frontend   http://localhost:4200   (landlord / Landlord@12345)
  BariVara frontend   http://localhost:4201   (landlord-linked / Landlord@12345)
  landlord-backend    http://localhost:8080
  barivara-backend    http://localhost:8081

Stop everything (except Postgres) with:  $REPO/dev-up.sh down
EOF

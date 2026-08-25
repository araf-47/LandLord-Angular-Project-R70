#!/usr/bin/env bash
# Boots the packaged jar for the Playwright suite.
#
# MAIL_SINK_ENABLED=true swaps the default logging sender for FileMailSink, which
# writes each mail (OTP included) as JSON under MAIL_SINK_DIR. That is the only
# way a black-box test can learn an OTP - the service stores it BCrypt-hashed and
# never returns it. Local/CI only.
set -euo pipefail

cd "$(dirname "$0")"

# The runnable jar specifically. `mvn package` produces only the thin jar by
# default (springboot.repackage.skip=true), which has no Main-Class and no bundled
# dependencies - `java -jar` on it fails with a confusing error, so require the
# classified one explicitly.
#
# Resolved with a nullglob array rather than `ls | head`: under `set -euo pipefail`
# a non-matching `ls` fails the pipeline and aborts the script before the guard
# below can print anything useful.
shopt -s nullglob
JARS=(../target/auth-*-exec.jar)
shopt -u nullglob

if (( ${#JARS[@]} == 0 )); then
  echo "No executable jar in ../target." >&2
  echo "Build it with:  mvn -f ../pom.xml package -DskipTests -Dspringboot.repackage.skip=false" >&2
  exit 1
fi
JAR="${JARS[0]}"

MAIL_SINK_DIR="${MAIL_SINK_DIR:-$(pwd)/.mail-sink}"
rm -rf "$MAIL_SINK_DIR"
mkdir -p "$MAIL_SINK_DIR"

exec "${JAVA_CMD:-java}" -jar "$JAR" \
  --server.port="${AUTH_PORT:-18080}" \
  --spring.datasource.url="${DB_URL:-jdbc:postgresql://localhost:5462/auth_apitest}" \
  --spring.datasource.username="${DB_USERNAME:-auth}" \
  --spring.datasource.password="${DB_PASSWORD:-auth}" \
  --credentials.default.username="${ADMIN_USERNAME:-admin}" \
  --credentials.default.password="${ADMIN_PASSWORD:-Admin@12345}" \
  --credentials.default.email=admin@apitest.local \
  --credentials.default.phone=+8801700000000 \
  --security.account-lockout.max-attempts=5 \
  --mail.sink.enabled=true \
  --mail.sink.dir="$MAIL_SINK_DIR" \
  --logging.level.root=WARN \
  --logging.level.com.idb=INFO

#!/usr/bin/env bash
# Second instance, with per-IP blocking switched ON.
#
# IpBlockingServiceImpl, IpBlockingFilter and IpBlockController are all
# @ConditionalOnProperty on auth.ip.block.enabled, so those three endpoints do not
# exist in the default instance's bean graph at all - they need their own service
# to be reachable. It also gets its own database, because blocking is stateful and
# would otherwise leak into the main suite.
#
# ddl-auto=create rebuilds that schema on every boot: a block persisted by a
# previous run would otherwise still be active at startup, and the readiness probe
# would be the first thing it refused.
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

exec "${JAVA_CMD:-java}" -jar "$JAR" \
  --server.port="${AUTH_IPBLOCK_PORT:-18081}" \
  --spring.datasource.url="${DB_IPBLOCK_URL:-jdbc:postgresql://localhost:5462/auth_apitest_ipblock}" \
  --spring.datasource.username="${DB_USERNAME:-auth}" \
  --spring.datasource.password="${DB_PASSWORD:-auth}" \
  --credentials.default.username="${ADMIN_USERNAME:-admin}" \
  --credentials.default.password="${ADMIN_PASSWORD:-Admin@12345}" \
  --credentials.default.email=admin@apitest.local \
  --credentials.default.phone=+8801700000000 \
  --spring.jpa.hibernate.ddl-auto=create \
  --auth.ip.block.enabled=true \
  --auth.ip.block.max.unauthenticated.attempts=3 \
  --auth.ip.block.max.invalid.jwt.attempts=3 \
  --auth.ip.block.max.failed.attempts=50 \
  --auth.ip.block.max.invalid.otp.attempts=50 \
  --auth.ip.block.block.duration.hours=24 \
  --logging.level.root=WARN \
  --logging.level.com.idb=WARN

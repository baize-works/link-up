#!/usr/bin/env bash
# Runs one local, batch Link-Up job in the foreground.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LINK_UP_HOME="${LINK_UP_HOME:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
CONF_DIR="${LINK_UP_CONF_DIR:-${LINK_UP_HOME}/config}"
LOG_DIR="${LINK_UP_LOG_DIR:-${LINK_UP_HOME}/logs}"
LOGFILE="${LOGFILE:-${LOG_DIR}/link-up.log}"
JOB_LOG_DIR="${LINK_UP_JOB_LOG_DIR:-${LOG_DIR}/jobs}"
JAVA_BIN="${JAVA_HOME:+${JAVA_HOME}/bin/}java"

if ! command -v "${JAVA_BIN}" >/dev/null 2>&1; then
  echo "Java 8 or later is required; set JAVA_HOME or add java to PATH." >&2
  exit 1
fi

mkdir -p "$(dirname "${LOGFILE}")" "${JOB_LOG_DIR}"

if [[ $# -eq 0 ]]; then
  set -- --config "${CONF_DIR}/link-up.yaml"
fi

JAVA_OPTS=(
  -Xms256m
  -Xmx1024m
  -XX:+ExitOnOutOfMemoryError
  -Dfile.encoding=UTF-8
  -Dlog4j.configurationFile="${CONF_DIR}/log4j2.xml"
  -Dlink.up.log.dir="${LOG_DIR}"
  -Dlink.up.log.file="${LOGFILE}"
  -Dlink.up.job.log.dir="${JOB_LOG_DIR}"
)

if [[ -n "${LINK_UP_JAVA_OPTS:-}" ]]; then
  # shellcheck disable=SC2206
  JAVA_OPTS+=( ${LINK_UP_JAVA_OPTS} )
fi

exec "${JAVA_BIN}" \
  "${JAVA_OPTS[@]}" \
  -cp "${LINK_UP_HOME}/lib/*" \
  com.link.up.launcher.LocalSyncLauncher \
  "$@"

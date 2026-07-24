#!/usr/bin/env bash
# Starts Baize Flux Server in the foreground.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BAIZE_FLUX_HOME="${BAIZE_FLUX_HOME:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
CONF_DIR="${BAIZE_FLUX_CONF_DIR:-${BAIZE_FLUX_HOME}/config}"
LOG_DIR="${BAIZE_FLUX_LOG_DIR:-${BAIZE_FLUX_HOME}/logs}"
LOGFILE="${LOGFILE:-${LOG_DIR}/baize-flux-server.log}"
JOB_LOG_DIR="${BAIZE_FLUX_JOB_LOG_DIR:-${LOG_DIR}/jobs}"
JAVA_BIN="${JAVA_HOME:+${JAVA_HOME}/bin/}java"

if ! command -v "${JAVA_BIN}" >/dev/null 2>&1; then
  echo "Java 8 or later is required; set JAVA_HOME or add java to PATH." >&2
  exit 1
fi

mkdir -p "$(dirname "${LOGFILE}")" "${JOB_LOG_DIR}"

JAVA_OPTS=(
  -Xms256m
  -Xmx1024m
  -XX:+ExitOnOutOfMemoryError
  -Dfile.encoding=UTF-8
  -Dlog4j.configurationFile="${CONF_DIR}/log4j2.xml"
  -Dbaize.flux.log.dir="${LOG_DIR}"
  -Dbaize.flux.log.file="${LOGFILE}"
  -Dbaize.flux.job.log.dir="${JOB_LOG_DIR}"
)

if [[ -n "${BAIZE_FLUX_JAVA_OPTS:-}" ]]; then
  # shellcheck disable=SC2206
  JAVA_OPTS+=( ${BAIZE_FLUX_JAVA_OPTS} )
fi

exec "${JAVA_BIN}" \
  "${JAVA_OPTS[@]}" \
  -cp "${BAIZE_FLUX_HOME}/lib/*" \
  com.baize.flux.server.FluxServer \
  "$@"

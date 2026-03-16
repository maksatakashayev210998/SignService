#!/usr/bin/env bash
set -euo pipefail

TUMAR_TGZ_PATH="${TUMAR_TGZ_PATH:-/install/TumarCSP.tgz}"
TUMAR_INSTALL_MARKER="${TUMAR_INSTALL_MARKER:-/opt/tumar/.installed}"

if [[ -f "$TUMAR_TGZ_PATH" ]] && [[ ! -f "$TUMAR_INSTALL_MARKER" ]]; then
  echo "Tumar CSP archive found. Installing from $TUMAR_TGZ_PATH"
  mkdir -p /tmp/tumar
  tar -xzf "$TUMAR_TGZ_PATH" -C /tmp/tumar

  # Common distribution layout: TumarCSP5.2/setup_csp.sh
  if [[ -f /tmp/tumar/TumarCSP5.2/setup_csp.sh ]]; then
    chmod +x /tmp/tumar/TumarCSP5.2/setup_csp.sh
    (cd /tmp/tumar/TumarCSP5.2 && ./setup_csp.sh install)
  else
    echo "ERROR: setup_csp.sh not found in extracted archive"
    find /tmp/tumar -maxdepth 3 -type f -name "setup_csp.sh" -print || true
    exit 1
  fi

  mkdir -p "$(dirname "$TUMAR_INSTALL_MARKER")"
  touch "$TUMAR_INSTALL_MARKER"
  rm -rf /tmp/tumar
else
  echo "Skipping Tumar CSP install (archive missing or already installed)."
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar


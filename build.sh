#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "Building JAR..."
mvn -q -DskipTests package
echo "Building Docker image..."
docker compose build
echo "Done. Run: docker compose up -d"

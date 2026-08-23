#!/usr/bin/env bash
set -euo pipefail

for img in accounts-service cash-service transfer-service notification-service gateway-service; do
  tag="$img:0.0.3-SNAPSHOT"
  echo "==> Loading $tag"
  if ! docker image inspect "$tag" >/dev/null 2>&1; then
    echo "ERROR: local image $tag not found" >&2
    exit 1
  fi
  docker save "$tag" | minikube image load -
  echo "    done: $tag"
done
echo "All images loaded."

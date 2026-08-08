#!/usr/bin/env bash
# Использовать для доступа к gateway(localhost:9091) и PostgreSQL(localhost:5432) на localhost через kubectl port-forward.
# Убрать port-forward: pkill -f "port-forward svc/"
set -euo pipefail

kubectl port-forward -n bank svc/postgres 5432:5432 &
kubectl port-forward -n bank svc/gateway-service 9091:9091 &

echo "Port-forwards started (postgres 5432, gateway 9091)."

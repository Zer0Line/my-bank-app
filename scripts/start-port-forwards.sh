#!/usr/bin/env bash
# Использовать для доступа к gateway(localhost:9091), PostgreSQL(localhost:5432) и Kafka UI(localhost:9092) на localhost через kubectl port-forward.
# Убрать port-forward: pkill -f "port-forward svc/"
set -euo pipefail

kubectl port-forward -n bank svc/postgres 5432:5432 &
kubectl port-forward -n bank svc/gateway-service 9091:9091 &
kubectl port-forward -n bank svc/bank-kafka-ui 9092:8080 &

echo "Port-forwards started (postgres 5432, gateway 9091, kafka-ui localhost:9092)."

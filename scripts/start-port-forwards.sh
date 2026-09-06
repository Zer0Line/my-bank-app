#!/usr/bin/env bash
# Использовать для доступа к PostgreSQL(localhost:5432), Kafka UI(localhost:9092) и Ingress(localhost:8081) на localhost через kubectl port-forward.
# Убрать port-forward: pkill -f "port-forward svc/"
set -euo pipefail

kubectl port-forward -n bank svc/postgres 5432:5432 &
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8081:80 &
kubectl port-forward -n bank svc/bank-kafka-ui 9092:8080 &
kubectl port-forward -n bank svc/zipkin 9411:9411 &
kubectl port-forward -n bank svc/prometheus 19090:9090 &
kubectl port-forward -n bank svc/grafana 13000:3000 &
kubectl port-forward -n bank svc/logstash 5000:5000 &
kubectl port-forward -n bank svc/elasticsearch 9200:9200 &
kubectl port-forward -n bank svc/kibana 15601:5601 &
kubectl port-forward -n bank svc/keycloak 18082:8082

echo "Port-forwards started (postgres 5432, ingress 8080, kafka-ui localhost:9092, elasticsearch 9200, kibana 15601)."

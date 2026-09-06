# RUNBOOK: Запуск стенда my-bank-app

Инструкция по сборке образов, развертыванию через minikube и Helm и запуску frontend-service на хосте.

> Описание сервисов и архитектуры — см. `README.md`, детали Helm-чартов — `helm/README.md`.

## Сборка и тесты

```bash
./gradlew build
./gradlew test
```

## Сборка Docker-образов

```bash
# удалить старые образы
docker rmi accounts-service:0.0.3-SNAPSHOT \
  cash-service:0.0.3-SNAPSHOT transfer-service:0.0.3-SNAPSHOT \
  notification-service:0.0.3-SNAPSHOT frontend-service:0.0.3-SNAPSHOT

./gradlew dockerBuildImages       # собрать всё
```

## Загрузка образов в minikube

Через скрипт `scripts/add_images_to_minikube.sh`
или
```bash
for img in accounts-service cash-service transfer-service notification-service; do
  docker save $img:0.0.3-SNAPSHOT | minikube image load -
done
```

## Установка через Helm

> Таймаут увеличен (`--timeout 15m`): Helm ждёт post-install hook-джобы
> `elasticsearch-init` и `bank-kafka-topics-init`, которые дожидаются готовности
> Elasticsearch/Kafka. На дефолтных 5 минутах чистый стенд может не успеть подняться
> (Helm отдаёт `failed post-install: timed out waiting for the condition`).

### Первая установка (с нуля)
При первой установке желательно поднимать сначала postgresql и keycloak.

```bash
minikube addons enable ingress

helm dependency build helm/bank

# 1. PostgreSQL — база должна быть готова до запуска остальных сервисов
helm install bank helm/bank -n bank --create-namespace --timeout 5m \
  --set accounts-service.enabled=false \
  --set cash-service.enabled=false \
  --set transfer-service.enabled=false \
  --set notification-service.enabled=false \
  --set keycloak.enabled=false

# 2. Keycloak — зависит от PostgreSQL (KC_DB_URL_HOST)
helm install bank helm/bank -n bank --timeout 5m \
  --set accounts-service.enabled=false \
  --set cash-service.enabled=false \
  --set transfer-service.enabled=false \
  --set notification-service.enabled=false

# 3. Umbrella chart — поднимает оставшиеся сервисы и инфраструктуру
helm upgrade --install bank helm/bank -n bank --timeout 15m
```

### Повторная установка / обновление

После правок в чартах:

```bash
helm dependency update helm/bank
helm dependency build helm/bank
helm upgrade --install bank helm/bank -n bank --timeout 15m
```

Секреты БД по умолчанию: `bankuser` / `bankpass` / БД `bankdb` (`helm/postgres/values.yaml`).

Секреты OAuth2-клиентов Keycloak задаются в `helm/<service>/values.yaml` → `secrets.serviceClientSecret`.

## Frontend на хосте

```bash
docker compose up -d frontend-service
```

Frontend работает на `http://localhost:9090`.

> Если `scripts/start-port-forwards.sh` падает, поднимать форварды вручную через
> `setsid nohup kubectl -n bank port-forward svc/<svc> <port>:<port> &`.

## Доступ

- **Frontend (UI):** http://localhost:9090 — логин `bankuser` / `bankuser`
- **Keycloak (admin):** http://localhost:8081/admin/ — `admin` / `admin`
- **API (Ingress):** http://localhost:8081/api/accounts, /api/cash, /api/transfers
- **Prometheus (UI):** http://localhost:8081/prometheus/graph или http://localhost:19090/ (port-forward)
- **PostgreSQL:** `localhost:5432` (порт-форвард)
- **Zipkin (UI):** http://localhost:9411/ — через port-forward `svc/zipkin 9411:9411`
- **Elasticsearch (API):** http://localhost:9200/ — через port-forward `svc/elasticsearch 9200:9200`
- **Kibana (UI):** http://localhost:8081/kibana/ (Ingress) или http://localhost:15601/ (port-forward)

## Обновление после изменения кода

```bash
docker rmi <service>:0.0.3-SNAPSHOT
./gradlew :<service>:dockerBuildImage
docker save <service>:0.0.3-SNAPSHOT | minikube image load -
kubectl -n bank rollout restart deploy <service>
kubectl -n bank rollout status deploy <service> --timeout=240s
```

## Удаление

```bash
docker compose down
helm uninstall bank -n bank
kubectl delete pvc -n bank data-postgres-0   # если нужно стереть данные
```
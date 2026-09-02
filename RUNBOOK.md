# RUNBOOK: Запуск стенда my-bank-app

Инструкция по сборке образов, развертыванию через minikube и Helm.

## Как устроено

| Сервис                                          | Где работает | Как запустить  |
|-------------------------------------------------|---|----------------|
| PostgreSQL 16                                   | minikube (StatefulSet + PVC) | Helm           |
| accounts, cash, transfer, notification          | minikube | Helm           |
| Keycloak 24                                     | minikube (StatefulSet) | Helm           |
| Ingress (nginx)                                 | minikube | minikube addon |
| frontend-service                                | хост-машина | docker-compose |

Все backend-сервисы и Keycloak работают в minikube. Маршрутизация — через Ingress.

## OAuth2 и клиенты Keycloak

Каждый бэкенд-сервис, которому нужно ходить в другой сервис по HTTP, имеет **собственный** client в realm `bank-realm` и собственный секрет (client-credentials flow):

| Сервис (clientId)     | Секрет (Helm)                                      | Назначение |
|-----------------------|----------------------------------------------------|------------|
| `bank-ui`             | (пользовательский логин `bankuser` / `bankuser`)   | фронт; роли `USER`, `TRANSFER_WRITE` |
| `cash-service`        | `helm/cash-service/values.yaml` → `secrets.serviceClientSecret`   | вызывает `accounts-service` (`/api/internal/**`, роль `ACCOUNTS_WRITE`) |
| `transfer-service`    | `helm/transfer-service/values.yaml` → `secrets.serviceClientSecret` | вызывает `accounts-service` (`/api/internal/**`, роль `ACCOUNTS_WRITE`) |
| `accounts-service`    | — (не имеет client-секрета, только resource server) | принимает токены `aud=accounts-service` |
| `notification-service`| — (Kafka-only, токены не используются)             | слушает Kafka, HTTP не принимает |

- В `keycloak/bank-realm.json` у всех клиентов `fullScopeAllowed=false`, а каждому клиенту выданы **только минимально необходимые** realm-роли через scope mappings.
- Каждый resource server проверяет **issuer + audience** токена (см. `app.security.oauth2.resourceserver.jwt.*` в соответствующих configmap).

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

```bash
for img in accounts-service cash-service transfer-service notification-service; do
  docker save $img:0.0.3-SNAPSHOT | minikube image load -
done
```

## Установка через Helm

```bash
minikube addons enable ingress
helm dependency build helm/bank
helm install bank helm/bank -n bank --create-namespace
```

Только PostgreSQL (для работы с БД без backend):

```bash
helm install bank helm/bank -n bank --create-namespace \
  --set accounts-service.enabled=false \
  --set cash-service.enabled=false \
  --set transfer-service.enabled=false \
  --set notification-service.enabled=false \
  --set keycloak.enabled=false
```

Повторное применение после правок в чартах:

```bash
helm dependency build helm/bank
helm upgrade bank helm/bank -n bank
```

Секреты БД по умолчанию: `bankuser` / `bankpass` / БД `bankdb` (`helm/postgres/values.yaml`).

Секреты OAuth2-клиентов Keycloak задаются в `helm/<service>/values.yaml` → `secrets.serviceClientSecret` (см. раздел «OAuth2 и клиенты Keycloak»).

## Frontend на хосте

```bash
docker compose up -d frontend-service
```

Frontend работает на `http://localhost:9090`.

> Если `scripts/start-port-forwards.sh` падает, поднимать форварды вручную через `setsid nohup kubectl -n bank port-forward svc/<svc> <port>:<port> &`.

## Доступ

- **Frontend (UI):** http://localhost:9090 — логин `bankuser` / `bankuser`
- **Keycloak (admin):** http://localhost:8081/admin/ — `admin` / `admin`
- **API (Ingress):** http://localhost:8081/api/accounts, /api/cash, /api/transfers
- **Prometheus (UI):** http://localhost:8081/prometheus/graph или http://localhost:19090/ (port-forward)
- **PostgreSQL:** `localhost:5432` (порт-форвард)
- **Zipkin (UI):** http://localhost:9411/ — через port-forward `svc/zipkin 9411:9411`

## Zipkin

Zipkin разворачивается внутри minikube чартом `openzipkin/zipkin` (image `openzipkin/zipkin:3.5`)
как подчарт `helm/bank` (`helm/bank/values.yaml` → `zipkin.*`). Service `zipkin` (ClusterIP, порт 9411)
доступен backend-сервисам в кластере по адресу `http://zipkin:9411`.

```bash
kubectl port-forward -n bank svc/zipkin 9411:9411
# Zipkin UI: http://localhost:9411/
```

Порт-форвард также поднимается скриптом `scripts/start-port-forwards.sh`.

### Поставка трейсов (Micrometer Tracing / Brave)

Каждый микросервис и frontend-service используют `micrometer-tracing-bridge-brave` +
`zipkin-reporter-brave` и отправляют трейсы в Zipkin:

- **accounts, cash, transfer, notification** — `management.zipkin.tracing.endpoint: http://zipkin:9411/api/v2/spans`
  (in-cluster Service), sampling probability = 1.0. Трассируются входящие/исходящие HTTP-запросы,
  обращения в БД (JPA) и Apache Kafka (продюсер/консьюмер).
- **frontend-service** (хост, docker-compose) — `management.zipkin.tracing.endpoint: http://localhost:9411/api/v2/spans`

## Prometheus

Prometheus разворачивается внутри minikube локальным чартом `helm/prometheus`
(image `prom/prometheus`) как подчарт `helm/bank` (`helm/bank/values.yaml` → `prometheus.*`).
Service `prometheus` (ClusterIP, порт 9090) доступен backend-сервисам в кластере по адресу `http://prometheus:9090`.

Конфигурация scrape-задач — в `helm/prometheus/values.yaml` → `config` (монтируется в ConfigMap `prometheus-config`).
TSDB хранится в PVC `prometheus-data` (по умолчанию `local-path`).

Доступ на localhost:

```bash
# через Ingress (nginx rewrite /prometheus -> /)
# http://localhost:8081/prometheus/

# либо через port-forward (порт 19090, т.к. 9090 на хосте занят frontend)
kubectl port-forward -n bank svc/prometheus 19090:9090
# http://localhost:19090/
```

Port-forward также поднимается скриптом `scripts/start-port-forwards.sh`.

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

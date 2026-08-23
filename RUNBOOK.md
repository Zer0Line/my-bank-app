# RUNBOOK: Запуск продуктинвного стенда my-bank-app

Инструкция по сборке образов, развертыванию backend+PostgreSQL через minikube и Helm

## Как устроено

| Сервис                                          | Где работает | Как запустить  |
|-------------------------------------------------|---|----------------|
| PostgreSQL 16                                   | minikube (StatefulSet + PVC) | Helm           |
| gateway, accounts, cash, transfer, notification | minikube | Helm           |
| Keycloak 24                                     | хост-машина | docker-compose |
| frontend-service                                | хост-машина | docker-compose |

Keycloak отдаёт токены с issuer `http://localhost:8082` (доступен из браузера и frontend на хосте), а backend-поды валидируют JWT по подписи через `jwk-set-uri` → Service `keycloak` (адрес хоста задаётся в `helm/bank/values.yaml` → `externalKeycloak.host`).

## OAuth2 и клиенты Keycloak

Каждый бэкенд-сервис, которому нужно ходить в другой сервис по HTTP, имеет **собственный** client в realm `bank-realm` и собственный секрет (client-credentials flow):

| Сервис (clientId)     | Секрет (Helm)                                      | Назначение |
|-----------------------|----------------------------------------------------|------------|
| `bank-ui`             | (пользовательский логин `bankuser` / `bankuser`)   | фронт; роли `USER`, `TRANSFER_WRITE` |
| `cash-service`        | `helm/cash-service/values.yaml` → `secrets.serviceClientSecret`   | вызывает `accounts-service` (`/api/internal/**`, роль `ACCOUNTS_WRITE`) |
| `transfer-service`    | `helm/transfer-service/values.yaml` → `secrets.serviceClientSecret` | вызывает `accounts-service` (`/api/internal/**`, роль `ACCOUNTS_WRITE`) |
| `accounts-service`    | — (не имеет client-секрета, только resource server) | принимает токены `aud=accounts-service` |
| `notification-service`| — (Kafka-only, токены не используются)             | слушает Kafka, HTTP не принимает |

- В `keycloak/bank-realm.json` у всех клиентов `fullScopeAllowed=false`, а каждому клиенту выданы **только минимально необходимые** realm-роли через scope mappings: `cash-service`/`transfer-service` → `ACCOUNTS_WRITE`, `bank-ui` → `USER`,`TRANSFER_WRITE`. Итоговый токен сервиса содержит ровно одну роль и `aud` равный целевому сервису (например, токен `cash-service` → `aud=accounts-service`, `roles=[ACCOUNTS_WRITE]`).
- Каждый resource server (`accounts`, `cash`, `transfer`) проверяет **issuer + audience** токена (см. `app.security.oauth2.resourceserver.jwt.*` в соответствующих configmap). Чужой токен (другой `aud`/issuer) отклоняется.
- **Секреты клиентов в `helm/<service>/values.yaml` должны совпадать** с секретом соответствующего client в Keycloak. При смене секрета — меняйте и там, и в админке Keycloak, иначе сервис не сможет получить токен.
- Gateway не логирует JWT: фильтр `RequestLogging` удалён, `JwtTokenRelay` логирует только длину токена (на DEBUG).

## Сборка и тесты

```bash
./gradlew build
./gradlew test
```

## Сборка Docker-образов

```bash
# удалить старые образы 
docker rmi gateway-service:0.0.3-SNAPSHOT accounts-service:0.0.3-SNAPSHOT \
  cash-service:0.0.3-SNAPSHOT transfer-service:0.0.3-SNAPSHOT \
  notification-service:0.0.3-SNAPSHOT frontend-service:0.0.3-SNAPSHOT

./gradlew dockerBuildImages       # собрать всё
```

Образы собираются из корневого `Dockerfile` (`eclipse-temurin:21-jre`), внутрь кладётся `bootJar` сервиса, порт прокидывается `--build-arg APP_PORT`.

## Загрузка образов в minikube

`minikube` (драйвер `docker`) использует собственный docker-демон внутри ноды, поэтому образы передаются через `minikube image load`:

### Все сразу. Можно запустить через /scripts/add_to_minikube.sh
```bash
for img in accounts-service cash-service transfer-service notification-service gateway-service; do
  docker save $img:0.0.3-SNAPSHOT | minikube image load -
done
```

## Установка через Helm (backend + PostgreSQL)

```bash
helm dependency build helm/bank          # упаковать сабчарты в helm/bank/charts/*.tgz
helm install bank helm/bank -n bank --create-namespace
```

Только PostgreSQL (для работы с БД без backend):

```bash
helm install bank helm/bank -n bank --create-namespace \
  --set gateway-service.enabled=false \
  --set accounts-service.enabled=false \
  --set cash-service.enabled=false \
  --set transfer-service.enabled=false \
  --set notification-service.enabled=false
```

Повторное применение после правок в чартах:

```bash
helm dependency build helm/bank   # если менялись helm/<service>/*
helm upgrade bank helm/bank -n bank
```

Секреты БД по умолчанию: `bankuser` / `bankpass` / БД `bankdb` (`helm/postgres/values.yaml`).

Секреты OAuth2-клиентов Keycloak задаются в `helm/<service>/values.yaml` → `secrets.serviceClientSecret` (см. раздел «OAuth2 и клиенты Keycloak»). В проекте, для удобства, они хранятся в виде открытого значения в values-файлах.

## Port-forward и docker-compose (Keycloak + frontend)

```bash
bash scripts/start-port-forwards.sh   # gateway → localhost:9091, postgres → localhost:5432
docker compose up -d
```

- docker-compose использует host-сеть: Keycloak на `localhost:8082`, frontend на `localhost:9090`.
- Keycloak при первом старте импортирует `keycloak/bank-realm.json` (при повторном запуске с тем же datadir импорт пропускается — правки вносить в админке **и синхронизировать с `keycloak/bank-realm.json`**, иначе при следующем re-import стенд откатится к старой конфигурации клиентов/ролей).
- Если `scripts/start-port-forwards.sh` падает при обращении к shell, поднимать форварды вручную через `setsid nohup kubectl -n bank port-forward svc/<svc> <port>:<port> &`.
- Сначала port-forward PostgreSQL на `localhost:5432`, затем `docker compose up -d` — иначе Keycloak не подключается к БД.

> Port-forward обрывается при пересоздании подов — после рестарта gateway заново `bash scripts/start-port-forwards.sh`.

## Доступ

- **Frontend (UI):** http://localhost:9090 — логин `bankuser` / `bankuser`
- **Keycloak (admin):** http://localhost:8082 — `admin` / `admin`
- **Gateway:** http://localhost:9091 (порт-форвард)
- **Accounts (напрямую):** http://localhost:9092 (порт-форвард)
- **PostgreSQL:** `localhost:5432` (порт-форвард)

## Обновление после изменения кода

```bash
docker rmi <service>:0.0.3-SNAPSHOT
./gradlew :<service>:dockerBuildImage
docker save <service>:0.0.3-SNAPSHOT | minikube image load -
kubectl -n bank rollout restart deploy <service>
kubectl -n bank rollout status deploy <service> --timeout=240s
bash scripts/start-port-forwards.sh   # повторно, если поды пересоздавались
```

## Удаление

```bash
docker compose down
helm uninstall bank -n bank
kubectl delete pvc -n bank data-postgres-0   # только если нужно стереть данные
pkill -f "port-forward svc/"                 # снять порт-форварды
```
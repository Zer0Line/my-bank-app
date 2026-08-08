# Helm-чарты my-bank-app

umbrella чарт `helm/bank`

| Сабчарт | Что разворачивает                                                                            |
|---|----------------------------------------------------------------------------------------------|
| `postgres` | PostgreSQL 16 как **StatefulSet** (volumeClaimTemplates → PVC, headless + ClusterIP сервисы) |
| `gateway-service` | API Gateway (Spring Cloud Gateway + OAuth2 JWT)                                              |
| `accounts-service`, `cash-service`, `transfer-service`, `notification-service` | (Deployment + Service + ConfigMap + Secret)                                                  |

**Keycloak и frontend-service** — работают
через `docker-compose.yml` (см. корневой README). Для доступа из подов Keycloak подключён
через Service/Endpoints `keycloak` → хост (`helm/bank/templates/external-keycloak.yaml`,
адрес хоста настраивается в `helm/bank/values.yaml` → `externalKeycloak.host`).

Конфигурация микросервисов:
- **ConfigMap** `<service>-config` — `application.yaml` с обычными настройками (порт, JPA, resilience4j, маршруты gateway);
- **Secret** `<service>-secrets` — секреты OAuth2 (client-secret'ы OAuth2). Пароли БД лежат в Secret `postgres-credentials`.

Service discovery через **Kubernetes Services** (ClusterIP, DNS-имена):
- Gateway и Feign-клиенты ходят напрямую по `http://<service>:<port>` (без Eureka).

Валидация JWT в backend выполняется **по подписи** (`jwk-set-uri: http://keycloak:8082/.../certs`),
: issuer в Keycloak — `http://localhost:8082` (`KC_HOSTNAME_URL`), этот адрес недоступен из подов.

## Структура

```
helm/
├── bank/
│   ├── charts/
│   └── templates/
│       ├── external-keycloak.yaml  # Service/Endpoints `keycloak` → хост (docker-compose)
│       └── NOTES.txt
├── postgres/                    # StatefulSet + Secret + Service
└── <service>-service/           # Deployment + Service + ConfigMap (+ Secret) для каждого микросервиса
```

## Развертывание

Образы микросервисов (`<service>:0.0.3-SNAPSHOT`) собираются локально и загружаются
в minikube (драйвер `docker` использует собственный docker-демон внутри ноды, поэтому передаём через скрипт):

```bash
./gradlew dockerBuildImages
for img in accounts-service cash-service transfer-service notification-service gateway-service; do
  docker save $img:0.0.3-SNAPSHOT | minikube image load -
done
kubectl -n bank rollout restart deploy gateway-service accounts-service \
  cash-service transfer-service notification-service
```

Сборка и установка через Helm:

```bash
helm dependency build helm/bank
helm install bank helm/bank -n bank --create-namespace
```

## Запуск PostgreSQL (без backend-сервисов)

```bash
helm install bank helm/bank -n bank --create-namespace \
  --set gateway-service.enabled=false \
  --set accounts-service.enabled=false \
  --set cash-service.enabled=false \
  --set transfer-service.enabled=false \
  --set notification-service.enabled=false
```

## Переопределение значений

Секреты БД по умолчанию `bankuser` / `bankpass` / `bankdb` берутся из
`helm/postgres/values.yaml` (Secret `postgres-credentials`). Задать свои:

```bash
helm install bank helm/bank -n bank --create-namespace \
  --set postgres.auth.username=myuser \
  --set postgres.auth.password=mypass \
  --set postgres.persistence.storageClass=standard \
  --set postgres.persistence.size=2Gi
```

## Удаление

```bash
helm uninstall bank -n bank
```

> Данные PostgreSQL хранятся в PVC (по умолчанию `local-path`). Для полной
> очистка - удалить PVC: `kubectl delete pvc -n bank data-postgres-0`.

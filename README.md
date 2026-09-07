# my-bank-app

Training project. Spring Boot микросервисное приложение банка.

> Запуск стенда (сборка образов, Helm, frontend) — см. `RUNBOOK.md`;
> детальное описание Helm-чартов — `helm/README.md`.

## Поднимаемые сервисы

| Сервис | Порт | Где работает | Назначение |
|---|---|---|---|
| **frontend-service** | 9090 | хост (docker-compose) | UI на Thymeleaf, OAuth2 Login |
| **accounts-service** | 9092 | minikube (Helm) | Счета и балансы; операции (снятие, пополнение, перевод) |
| **cash-service** | 8084 | minikube (Helm) | Кассовые операции |
| **transfer-service** | 8083 | minikube (Helm) | Переводы между счетами |
| **notification-service** | 8085 | minikube (Helm) | Уведомления об операциях (Kafka-only) |
| **PostgreSQL 16** | 5432 | minikube (Helm) | БД (счета, уведомления, Keycloak) |
| **Keycloak 24.0** | 8082 | minikube (Helm) | OIDC IdP, realm `bank-realm` |
| **Kafka (KRaft)** | 9092 | minikube (Helm) | Обмен событиями между сервисами |
| **Zipkin** | 9411 | minikube (Helm) | Распределённый трейсинг |
| **Prometheus** | 9090 | minikube (Helm) | Сбор метрик |
| **Grafana** | 3000 | minikube (Helm) | Дашборды метрик |
| **ELK** | 5000 / 9200 / 5601 | minikube (Helm) | Логи: Logstash, Elasticsearch, Kibana |

Пользовательские доступы:

- **Frontend (UI):** http://localhost:9090 — логин `bankuser` / `bankuser`
- **Keycloak (admin):** http://localhost:8081/admin/ — `admin` / `admin`

## Архитектура

- В minikube (umbrella-chart `helm/bank`) работают: PostgreSQL, Kafka, Keycloak, backend-сервисы
  (accounts, cash, transfer, notification) и стек наблюдения (Zipkin, Prometheus, Grafana, ELK).
- Frontend-service запускается через docker-compose на хосте.
- Маршрутизация HTTP-запросов — через **Ingress** (nginx):
  `/api/accounts` → accounts-service, `/api/cash` → cash-service, `/api/transfers` → transfer-service,
  `/realms`, `/admin` → Keycloak.
- Сервисы общаются напрямую через Kubernetes Services (ClusterIP / DNS-имена).
- Конфигурация микросервисов хранится в Kubernetes-объектах **ConfigMap** и **Secret**
  в Helm-чартах (`helm/<service>/templates/configmap.yaml` и `secret.yaml`).

## Безопасность

- Аутентификация — **Keycloak 24.0** (в minikube), realm `bank-realm` импортируется автоматически
  при первом старте (данные realm хранятся в PostgreSQL).
- Backend-сервисы — resource server: проверяют **issuer + audience** JWT.
- Сервис-to-сервис — OAuth2 `client_credentials` (у cash/transfer свой клиент и секрет).
- Тестовый пользователь для входа: **`bankuser` / `bankuser`** (роли `USER`, `TRANSFER_WRITE`).
- Данные счетов других пользователей можно добавить в `accounts-service/src/main/resources/data.sql`.

## Конфигурация

- PostgreSQL, Keycloak, Kafka и backend-микросервисы: Helm-чарты в `helm/`.
- Frontend-service: `docker-compose.yml` (конфиг — `docker/frontend/application.yaml`).
- Конфиги микросервисов: ConfigMap `<service>-config` + Secret `<service>-secrets`.
- Секреты БД: Secret `postgres-credentials`.
- Ingress для API и Keycloak: `helm/bank/templates/ingress.yaml`.
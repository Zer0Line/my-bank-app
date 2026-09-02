# my-bank-app

Training project. Spring Boot микросервисное приложение банка.

| Сервис | Порт | Назначение |
|---|---|---|
| **accounts-service** | 9092 | Управление счетами |
| **cash-service** | 8084 | Кассовые операции |
| **transfer-service** | 8083 | Переводы |
| **notification-service** | 8085 | Уведомления |
| **frontend-service** | 9090 | UI (Thymeleaf + OAuth2 Login) |

Для аутентификации используется **Keycloak 24.0** (в minikube).
Для хранения данных — **PostgreSQL 16**.

**Архитектура:**
- В minikube (Helm) работают: PostgreSQL, Keycloak, backend-сервисы (accounts, cash, transfer, notification).
- Frontend-service запускается через docker-compose на хосте.
- Маршрутизация HTTP-запросов — через **Ingress** (nginx), без API Gateway.
- Сервисы общаются напрямую через Kubernetes Services (ClusterIP / DNS-имена).
- Конфигурация микросервисов хранится в Kubernetes-объектах **ConfigMap** и **Secret**
  в Helm-чартах (`helm/<service>/templates/configmap.yaml` и `secret.yaml`).

---

### Сборка проекта

```bash
./gradlew build
```

### Запуск тестов

```bash
./gradlew test
```

### Сборка Docker-образов

```bash
./gradlew :accounts-service:dockerBuildImage
./gradlew :cash-service:dockerBuildImage
./gradlew :transfer-service:dockerBuildImage
./gradlew :notification-service:dockerBuildImage
./gradlew :frontend-service:dockerBuildImage
```

Собрать все образы сразу:

```bash
./gradlew dockerBuildImages
```

> Задача `dockerBuildImage` **пропускает** сборку, если образ с таким тегом уже
> существует. Для пересборки из актуального исходного кода сначала нужно удалить старые образы:
> `docker rmi <service>:0.0.3-SNAPSHOT`.

### Запуск через minikube и Helm

#### 1. Сборка и загрузка Docker-образов

```bash
./gradlew dockerBuildImages
for img in accounts-service cash-service transfer-service notification-service; do
  docker save $img:0.0.3-SNAPSHOT | minikube image load -
done
```

#### 2. Включение Ingress

```bash
minikube addons enable ingress
```

#### 3. Установка Helm-чартов

```bash
helm dependency build helm/bank
helm install bank helm/bank -n bank --create-namespace
```

#### 4. Запуск frontend на хосте

```bash
docker compose up -d frontend-service
```

Frontend поднимается на `http://localhost:9090`.

#### 5. Доступ к сервисам

- **Frontend (UI):** http://localhost:9090
- **Keycloak (admin):** http://localhost:8081/admin/ (admin / admin)
- **API (через Ingress):** http://localhost:8081/api/accounts, /api/cash, /api/transfers
- **Prometheus (UI):** http://localhost:8081/prometheus/ или http://localhost:19090/ (port-forward)
- **PostgreSQL:** `localhost:5432` (через `kubectl port-forward`)

Keycloak доступен через Ingress по пути `/admin/` и `/realms/`.

Prometheus разворачивается локальным чартом `helm/prometheus` (image `prom/prometheus`) как подчарт `helm/bank`. Доступен на localhost:
- через Ingress: `http://localhost:8081/prometheus/` (nginx rewrite `/prometheus` → `/`);
- через port-forward: `kubectl port-forward -n bank svc/prometheus 19090:9090` → `http://localhost:19090/`.

> Realm `bank-realm` импортируется автоматически при первом старте Keycloak.
> При повторном запуске импорт пропускается (данные хранятся в БД).

### Удаление

```bash
docker compose down
helm uninstall bank -n bank
```

> Данные PostgreSQL хранятся в PVC (по умолчанию `local-path`).

### Авторизация

В проекте создан тестовый пользователь для входа:

- **Логин:** `bankuser`
- **Пароль:** `bankuser`

Данные счетов других пользователей можно добавить в `accounts-service/src/main/resources/data.sql`.

### Конфигурация

- PostgreSQL, Keycloak и backend-микросервисы: Helm-чарты в `helm/`
- Frontend-service: `docker-compose.yml` (конфиг — `docker/frontend/application.yaml`)
- Конфиги микросервисов: ConfigMap `<service>-config` + Secret `<service>-secrets`
- Секреты БД: Secret `postgres-credentials`
- Ingress для API и Keycloak: `helm/bank/templates/ingress.yaml`
- Валидация JWT в backend: `jwk-set-uri: http://keycloak:8082/realms/bank-realm/protocol/openid-connect/certs`

### Экспорт realm Keycloak

После изменения настроек realm (пользователи, клиенты, роли) можно сделать экспорт:

```bash
kubectl -n bank exec -it keycloak-0 -- /opt/keycloak/bin/kc.sh export \
  --realm bank-realm --dir /tmp/ --users realm_file
kubectl -n bank cp keycloak-0:/tmp/bank-realm-realm.json keycloak/bank-realm.json
```

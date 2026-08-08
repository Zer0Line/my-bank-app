# my-bank-app

Training project. Spring Boot микросервисное приложение банка.

| Сервис | Порт | Назначение |
|---|---|---|
| **gateway-service** | 9091 | API Gateway (Spring Cloud Gateway) с OAuth2 JWT |
| **accounts-service** | 9092 | Управление счетами |
| **cash-service** | 8084 | Кассовые операции |
| **transfer-service** | 8083 | Переводы |
| **notification-service** | 8085 | Уведомления |
| **frontend-service** | 9090 | UI (Thymeleaf + OAuth2 Login) |

Для аутентификации используется **Keycloak 24.0**.
Для хранения данных — **PostgreSQL 16**.

**Архитектура:**
- Для развертываня используется minikube и Helm. В кластере k8s разворачиваются:
PostgreSQL и backend-сервисы (gateway, accounts, cash, transfer, notification).
- Через docker-compose** запускаются: **keycloak** и **frontend-service**.
- Для доступа по сети через localhost(без привязки к IP) к сервисам : frontend `:9090`, Keycloak `:8082`,
gateway `:9091` и PostgreSQL `:5432`нужно использовать **port-forward**. Для этого запустить скрипт
  scripts/start-port-forwards.sh  
- В конфигах приложений нет IP — используются DNS-имена: `keycloak` и `postgres`
(in-cluster Services) для подов и `localhost` для контейнеров на хосте.
- Backend валидируют JWT **по подписи** (`jwk-set-uri` через Service `keycloak`), т.к. issuer
(`KC_HOSTNAME_URL=http://localhost:8082`) недоступен из подов.
- Конфигурация микросервисов хранится в Kubernetes-объектах **ConfigMap** и **Secret**
в Helm-чартах (`helm/<service>/templates/configmap.yaml` и `secret.yaml`).

- Сервисы общаются напрямую через Kubernetes Services (ClusterIP / DNS-имена).

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

Образы собираются через Dockerfile для каждого сервиса:

```bash
./gradlew :gateway-service:dockerBuildImage
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

#### 1. Сборка Docker-образов

```bash
# Удаляем существующие, если остались от предыдущих запусков
docker rmi gateway-service:0.0.3-SNAPSHOT accounts-service:0.0.3-SNAPSHOT \
  cash-service:0.0.3-SNAPSHOT transfer-service:0.0.3-SNAPSHOT \
  notification-service:0.0.3-SNAPSHOT frontend-service:0.0.3-SNAPSHOT
./gradlew dockerBuildImages
```

Если minikube с драйвером `docker` использует собственный docker-демон внутри ноды, то
`minikube image load <tag>` не находит образы в хостовом демоне. Загружаем следующим образом:

```bash
for img in accounts-service cash-service transfer-service notification-service gateway-service; do
  docker save $img:0.0.3-SNAPSHOT | minikube image load -
done
```

После загрузки новых образов рестартуем поды:

```bash
kubectl -n bank rollout restart deploy gateway-service accounts-service \
  cash-service transfer-service notification-service
```

#### 2. Установка Helm-чартов

```bash
helm dependency build helm/bank
helm install bank helm/bank -n bank --create-namespace
```

#### 3. Запуск Keycloak и frontend на хосте

Сначала настроим port-forward для связи с k8s, затем запустим frontend и keycloak

```bash
bash scripts/start-port-forwards.sh   # opens gateway (9091) and PostgreSQL (5432) on localhost
docker compose up -d
```

- Keycloak поднимается на `http://localhost:8082` и при первом запуске
  импортирует realm `bank-realm` из `keycloak/bank-realm.json`.
- Frontend поднимается на `http://localhost:9090`.

> Если realm уже есть в БД (например, с прошлого запуска), импорт пропускается.
> Тогда правки в `bank-realm.json` нужно применить вручную в админке Keycloak
> (или удалить схему Keycloak из PostgreSQL).

#### 4. Доступ к сервисам

- **Frontend (UI):** http://localhost:9090
- **Keycloak (admin):** http://localhost:8082 (admin / admin)
- **Gateway:** http://localhost:9091 (через `kubectl port-forward`)
- **PostgreSQL:** `localhost:5432` (через `kubectl port-forward`)

> Порт-форварды (`kubectl port-forward`) обрываются при пересоздании подов — после
> рестарта gateway нужно заново сделать port-forward: `bash scripts/start-port-forwards.sh`.

> Есть привязка к IP — адрес хоста, как его видят поды
> (`minikube ssh "ip route" | grep default`, по умолчанию `192.168.49.1`),
> в `helm/bank/values.yaml` → `externalKeycloak.host` (Service/Endpoints `keycloak`).

Realm `bank-realm` должен содержать клиент **bank-ui** и пользователя **bankuser**.

### Удаление

```bash
docker compose down
helm uninstall bank -n bank
```

> Данные PostgreSQL хранятся в PVC (по умолчанию `local-path`).

### Авторизация

В проекте создан тестовый пользователь для входа, в БД есть 2 пользователя, которым можно переводить средства. Результат можно смотреть в БД accounts и результаты операций в notifications-service:

- **Логин:** `bankuser`
- **Пароль:** `bankuser`

Данные счетов других пользователей можно добавить в `accounts-service/src/main/resources/data.sql`.

### Конфигурация

- PostgreSQL и backend-микросервисы: Helm-чарты в `helm/`
- Keycloak и frontend-service: `docker-compose.yml` (конфиг фронтенда — `docker/frontend/application.yaml`)
- Конфиги микросервисов: ConfigMap `<service>-config` + Secret `<service>-secrets`
- Секреты БД: Secret `postgres-credentials`
- Keycloak для сервисов Kubernetes: Service/Endpoints `keycloak` → хост (см. `helm/bank/templates/external-keycloak.yaml`)
- Валидация JWT в backend: `jwk-set-uri: http://keycloak:8082/realms/bank-realm/protocol/openid-connect/certs`
  (issuer у Keycloak — `http://localhost:8082`, поэтому токены проверяются по подписи)
- Порт-форварды для доступа с хоста: `scripts/start-port-forwards.sh`
- Docker-образ: `Dockerfile`

### Экспорт realm Keycloak

После изменения настроек realm (пользователи, клиенты, роли) можно сделать экспорт конфигурации:

```bash
docker exec my-bank-app-keycloak-1 /opt/keycloak/bin/kc.sh export \
  --realm bank-realm --dir /tmp/ --users realm_file
docker cp my-bank-app-keycloak-1:/tmp/bank-realm-realm.json keycloak/bank-realm.json
```

# my-bank-app

Training project. Spring Boot микросервисное приложение банка.

| Сервис | Порт | Назначение |
|---|---|---|
| **configuration-service** | 8888 | Config-сервер (Spring Cloud Config) |
| **eureka-service** | 8761 | Service Discovery (Eureka) |
| **gateway-service** | 9091 | API Gateway (Spring Cloud Gateway) с OAuth2 JWT |
| **accounts-service** | 9092 | Управление счетами |
| **cash-service** | 8084 | Кассовые операции |
| **transfer-service** | 8083 | Переводы |
| **notification-service** | 8085 | Уведомления |
| **frontend-service** | 9090 | UI (Thymeleaf + OAuth2 Login) |

Для аутентификации используется **Keycloak 24.0**.
Для хранения данных — **PostgreSQL 16**.

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
./gradlew :configuration-service:dockerBuildImage
./gradlew :eureka-service:dockerBuildImage
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

### Запуск

#### 1. Инфраструктура (PostgreSQL + Keycloak)

```bash
./gradlew dockerComposeInfraUp
```

При первом запуске Keycloak автоматически импортирует realm `bank-realm` из файла `keycloak/bank-realm.json`.

#### 2. Настройка Keycloak

Зайти в админку http://localhost:8082, (admin / admin).
Realm `bank-realm` уже должен быть импортирован — в нём создан клиент **bank-ui** и пользователь **bankuser**.

> При сбросе томов PostgreSQL (`docker compose -f docker-compose-db_auth.yaml down -v`) Keycloak пересоздаёт realm из файла импорта — пароли не должны потеряться.

#### 3. Микросервисы проекта

```bash
./gradlew dockerComposeAppsUp
```

### Можно попробовать запустить всё сразу

```bash
./gradlew dockerBuildImages dockerComposeUp
```

### Остановка

```bash
# Остановить микросервисы
./gradlew dockerComposeAppsDown

# Остановить инфраструктуру
./gradlew dockerComposeInfraDown

# Остановить всё
./gradlew dockerComposeDown

# Остановить всё и удалить тома PostgreSQL
./gradlew dockerComposeDownVolumes
```

### Запуск только контейнеров (без пересборки)

```bash
# Инфраструктура
docker compose -f docker-compose-db_auth.yaml up -d

# Микросервисы
docker compose -f docker-compose.yaml up -d
```

Остановка:

```bash
docker compose -f docker-compose.yaml down
docker compose -f docker-compose-db_auth.yaml down
```

### Авторизация

В проекте создан тестовый пользователь для входа, в БД есть 2 пользователя, которым можно переводить средства. Результат можно смотреть в БД accounts и результаты операций в notifications-service:

- **Логин:** `bankuser`
- **Пароль:** `bankuser`

Данные счетов других пользователей можно добавить в `accounts-service/src/main/resources/data.sql`.

### Конфигурация контейнеров

- Инфраструктура: `docker-compose-db_auth.yaml`
- Микросервисы: `docker-compose.yml`
- Docker-образ: `Dockerfile`

### Экспорт realm Keycloak

После изменения настроек realm (пользователи, клиенты, роли) можно сделать экспорт конфигурации:

```bash
docker exec bank-keycloak /opt/keycloak/bin/kc.sh export \
  --realm bank-realm --dir /tmp/ --users realm_file
docker cp bank-keycloak:/tmp/bank-realm-realm.json keycloak/bank-realm.json
```

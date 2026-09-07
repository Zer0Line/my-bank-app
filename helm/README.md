# Helm-чарты my-bank-app

Umbrella-chart `helm/bank` объединяет подчарты, которые разворачиваются в minikube:

- **Инфраструктура:** `postgres`, `kafka`, `keycloak`;
- **Бизнес-сервисы:** `accounts-service`, `cash-service`, `transfer-service`, `notification-service`;
- **Мониторинг и логи:** `zipkin`, `prometheus`, `grafana`, `elk`.

Каталог `helm/`:

```
helm/
├── bank/                 # umbrella-chart (пробрасывает значения в подчарты)
├── accounts-service/     # бизнес-сервис по работе с счетом
├── cash-service/         # бизнес-сервис пополнения/снятия
├── transfer-service/     # бизнес-сервис по работе с переводами
├── notification-service/ # бизнес-сервис «уведомления», пишет операции в БД
├── postgres/             # PostgreSQL 16 (StatefulSet)
├── kafka/                # Apache Kafka (KRaft) + Kafka UI
├── keycloak/             # Keycloak 24 (StatefulSet + импорт realm)
├── elk/                  # Logstash + Elasticsearch + Kibana
└── prometheus/           # Prometheus
```

## Бизнес-сервисы

Бизнес-сервисы собраны из образов `*-service:0.0.*-SNAPSHOT`; каждый разворачивается как
`Deployment` + `Service` (ClusterIP). Конфигурация монтируется из ConfigMap `<service>-config`
(файл `application.yaml`), секреты — из Secret `<service>-secrets`. Все сервисы отправляют
трейсы в Zipkin, метрики в Prometheus, логи в Logstash.

### accounts-service (порт 9092)

### cash-service (порт 8084)

### transfer-service (порт 8083)

### notification-service (порт 8085)

### PostgreSQL 16

`StatefulSet` + `Service postgres` (ClusterIP, порт 5432), PVC 1Gi.

- БД `bankdb`, пользователь `bankuser` / `bankpass` (`helm/postgres/values.yaml`).
- Учётка монтируется в сервисы через Secret `postgres-credentials`
  (`SPRING_DATASOURCE_USERNAME/PASSWORD`).
- Используется: accounts-service, notification-service, Keycloak.

### Keycloak 24

`StatefulSet` + `Service keycloak` (ClusterIP, порт 8082).

- Realm `bank-realm` импортируется при первом старте (`start-dev --import-realm`, конфигмап
  `keycloak-realm` ← `keycloak/bank-realm.json`); при повторном запуске импорт пропускается.
- Админ: `admin` / `admin` (`helm/keycloak/values.yaml` → `auth.*`).
- `KC_HOSTNAME_URL=http://localhost:18082` — issuer всех JWT. Backend-сервисы валидируют issuer
  по этому же URL; подпись проверяется через JWKS внутрикластерного Service `keycloak`.
- OAuth2-клиенты (`fullScopeAllowed=false`, роли выдаются через scope mappings):

| clientId | Flow | Роли в токене | Назначение |
|---|---|---|---|
| `bank-ui` | authorization_code (public) | `USER`, `TRANSFER_WRITE` | UI фронтенда |
| `cash-service` | client_credentials | `ACCOUNTS_WRITE` | вызов internal-endpoints accounts-service |
| `transfer-service` | client_credentials | `ACCOUNTS_WRITE` | вызов internal-endpoints accounts-service |
| `accounts-service` | — | — | только resource server (audience токенов) |

Каждый resource server проверяет issuer + audience (см. `app.security.oauth2.resourceserver.jwt.*`
в configmap'ах). Секреты service-клиентов задаются в `helm/<service>/values.yaml`
→ `secrets.serviceClientSecret`.

Экспорт realm после изменения настроек (пользователи, клиенты, роли):

```bash
kubectl -n bank exec -it keycloak-0 -- /opt/keycloak/bin/kc.sh export \
  --realm bank-realm --dir /tmp/ --users realm_file
kubectl -n bank cp keycloak-0:/tmp/bank-realm-realm.json keycloak/bank-realm.json
```

### Kafka (KRaft)

Apache Kafka в режиме KRaft (без ZooKeeper): 1 нода-**контроллер** + 1 **брокер**
(image `kafkace/kafka:v3.7.1-63ba8d2`). Брокер — `StatefulSet` + `Service bank-kafka-broker`
(ClusterIP, порт **9092** — bootstrap-адрес для сервисов). В чарт также входит **Kafka UI**
(`provectuslabs/kafka-ui`).

Топики:

| Топик | Продюсер | Консьюмер |
|---|---|---|
| `account-operations` | accounts-service (outbox) | notification-service |
| `cash-request` | cash-service | notification-service |
| `transfer-request` | transfer-service | notification-service |

### Zipkin

Чарт `openzipkin/zipkin` (image 3.5), `Service zipkin` (ClusterIP, порт 9411).
Все сервисы (accounts, cash, transfer, notification, frontend) используют
`micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` и отправляют трейсы:

- backend — `management.zipkin.tracing.endpoint: http://zipkin:9411/api/v2/spans`
  (in-cluster), sampling probability = 1.0. Трассируются входящие/исходящие HTTP-запросы,
  обращения в БД (JPA) и Kafka (продюсер/консьюмер);
- frontend (хост) — `http://localhost:9411/api/v2/spans`.

## Мониторинг и логи

### Prometheus

Prometheus разворачивается внутри minikube локальным чартом `helm/prometheus`
(image `prom/prometheus`) как подчарт `helm/bank` (`helm/bank/values.yaml` → `prometheus.*`).
`Service prometheus` (ClusterIP, порт 9090) доступен backend-сервисам в кластере
по адресу `http://prometheus:9090`.

Конфигурация scrape-задач — в `helm/prometheus/values.yaml` → `config` (монтируется в ConfigMap
`prometheus-config`). TSDB хранится в PVC `prometheus-data` (по умолчанию `local-path`).

Доступ наружу — через Ingress: `http://localhost:8081/prometheus/`
(nginx rewrite `/prometheus` → `/`).

### Grafana

Grafana разворачивается внутри minikube чартом `grafana/grafana` (image `grafana/grafana`).
`Service grafana` (ClusterIP, порт 3000) доступен backend-сервисам в кластере
по адресу `http://grafana:3000`.

- Источник данных Prometheus настроен in-cluster: `http://prometheus:9090`
  (`helm/bank/values.yaml` → `grafana.datasources.datasources.yaml`). Datasource имеет явный
  `uid: prometheus` — на него ссылаются правила алертов и переменная `DS_PROMETHEUS` дашбордов.
- Встроенные дашборды монтируются в `/var/lib/grafana/dashboards/default`
  (`helm/bank/values.yaml` → `grafana.dashboards`): `http-metrics` (RPS, 4xx, 5xx, персентили),
  `business-metrics` (неуспешные снятия/переводы, ошибки сохранения уведомлений) и `alerts`
  (метрики алертов + список активных алертов).
- **Алерты (Grafana Alerting)** провижены через `grafana.alerting.rules.yaml`
  (папка `Alerts`), метрики Spring Boot из `/actuator/prometheus`:
  - `cpu-high` — `process_cpu_usage > 0.8` (80%) в течение 5m;
  - `memory-high` — JVM heap: `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes` > 0.8 (80%) в течение 5m;
  - `http-4xx-high` — более 3 ответов 4xx за 5 минут (`increase(...{status=~"4.."}[5m]) > 3`);
  - `http-5xx-high` — более 3 ответов 5xx за 5 минут (`severity: critical`).
- Логин/пароль по умолчанию: `admin` / `admin`.

Доступ наружу — через Ingress: `http://localhost:8081/grafana/`
(`nginx serve_from_sub_path`, префикс обрабатывает сам Grafana).

## ELK (Logstash + Elasticsearch + Kibana)

Стек сбора/хранения/визуализации логов разворачивается внутри minikube локальным
чартом `helm/elk` как подчарт `helm/bank` (`helm/bank/values.yaml` → `elk.*`).
Состоит из трёх компонентов — **Logstash** (сбор), **Elasticsearch** (хранение + поиск),
**Kibana** (UI).

```
микросервисы (5 шт, logback-spring.xml)
        │  JSON-lines по TCP
        ▼
   logstash:5000  (Deployment/Service logstash, input tcp+json_lines)
        │  output: elasticsearch → index bank-logs-%{+YYYY.MM.dd}
        ▼
   elasticsearch:9200  (StatefulSet/Service elasticsearch, HTTP 9200 / transport 9300)
        │  шаблон bank-logs-template (шарды/реплики + маппинги) — hook-job elasticsearch-init
        ▼  ▲
     индексы bank-logs-YYYY.MM.DD  (PVC 5Gi)  │
        │                                     │ поиск
        ▼                                     │
   kibana:5601  (Deployment/Service kibana, HTTP 5601, basePath /kibana)
        │  Ingress http://localhost:8081/kibana/
```

### Logstash 9.0.0

- `Service/Deployment logstash` (ClusterIP): **TCP input — 5000**, HTTP API — **9600**
  (используется для readiness/liveness probe по `GET /`).
- Все 5 сервисов (accounts, cash, transfer, notification, frontend) шлют логи в едином
  JSON-формате (Microservice Chassis) через `logstash:5000` / `localhost:5000` (frontend).
- Какой input/output — задаётся в `helm/elk/values.yaml` → `logstash.pipelineConfig`
  (монтируется в ConfigMap `logstash-config` → `01-bank-logs.conf`).
  Input: `tcp { port => 5000, codec => json_lines }`.
  Output: `elasticsearch { hosts => ["http://elasticsearch:9200"],
  index => "bank-logs-%{+YYYY.MM.dd}", manage_template => false }` —
  шаблоном Elasticsearch управляет hook-job, поэтому в Logstash выключен `manage_template`.
- Пайплайн объявлен в `logstash.pipelines` (memory queue).

Полезные команды (запуск pipeline, ошибки подключения к ES):

```bash
kubectl -n bank logs deploy/logstash -f
```

### Elasticsearch OSS 7.10.2

- `Service/StatefulSet elasticsearch` (ClusterIP): **HTTP — 9200** (в него пишет Logstash),
  transport — **9300**.
- Образ `docker.elastic.co/elasticsearch/elasticsearch-oss:7.10.2` — **OSS-дистрибутив без
  X-Pack** (лицензионно свободный).
- Платные/лицензируемые конфигурации X-Pack **отключены** в `elasticsearch.yml`
  (`helm/elk/values.yaml` → `elasticsearch.config`):
    - `xpack.security.enabled: false`
    - `xpack.monitoring.enabled: false`
    - `xpack.ml.enabled: false`
    - `xpack.watcher.enabled: false`
- Одиночный узел: `discovery.type: single-node` (режим разработки — снимает bootstrap-checks,
  напр. `vm.max_map_count`), `cluster.name: bank-logs`.
- Данные хранятся в PVC `data-elasticsearch-0` (по умолчанию **5Gi**, storageClass по умолчанию).
- JVM heap: `-Xms512m -Xmx512m` (`elasticsearch.heapSize`).

### Индексы, шаблоны, маппинги

Логи пишутся в **ежедневные** индексы `bank-logs-YYYY.MM.DD` (ротация по дням), имя задаётся в
output Logstash: `index => "bank-logs-%{+YYYY.MM.dd}"`.

Индексный шаблон и маппинги задаются в `helm/elk/values.yaml` → `elasticsearch.indexTemplate`
(JSON-тело шаблона — в `indexTemplate.body`) и применяются hook-job'ом **`elasticsearch-init`**
(Helm post-install/post-upgrade, идемпотентно):

- `PUT _template/bank-logs-template` — настройки индекса + схема полей;
- предварительное создание текущего дневного индекса.

Шаблон использует **legacy `_template`** (ES 7.x), матчит `index_patterns = ["bank-logs-*"]`.
Поля (соответствуют единому JSON-логу):

| Поле           | Тип      | Описание                                        |
|----------------|----------|-------------------------------------------------|
| `@timestamp`   | date     | время события (UTC)                             |
| `message`      | text + keyword | текст лога (keyword-подполе для агрегаций) |
| `logger_name`  | keyword  | класс-логгер (сокращён до 36)                   |
| `thread_name`  | keyword  | поток                                           |
| `level`        | keyword  | уровень (DEBUG/INFO/WARN/ERROR)                 |
| `service_name` | keyword  | имя сервиса (spring.application.name)           |
| `trace_id`     | keyword  | trace (Micrometer Tracing)                      |
| `span_id`      | keyword  | span (Micrometer Tracing)                       |
| `stack_trace`  | text     | стек ошибки                                     |
| `host`/`port`  | keyword/long | источник TCP                                |
| `mdc`          | object (dynamic) | прочий MDC-контекст                     |
| `@version`     | keyword  | версия схемы                                    |

`"dynamic": false` — поля вне схемы не индексируются (передаются только `_source`).

**Выбор шардов и реплик** (single-node): **1 первичный шард на индекс и 0 реплик** — на одном
узле реплика не размещается (кластер уходил бы в `yellow`), а объём логов невелик и разбит по
дням.

### Kibana 7.10.2

Визуальный UI для поиска и просмотра логов из Elasticsearch. Развёртывается как подчарт
`helm/elk` (`helm/elk/values.yaml` → `kibana.*`). Версия 7.10.2 совпадает с
Elasticsearch OSS 7.10.2.

- `Service/Deployment kibana` (ClusterIP, **порт 5601**).
- `elasticsearch.hosts: ["http://elasticsearch:9200"]` — в `kibana.yml`
  (`helm/elk/values.yaml` → `kibana.config`).
- Kibana доступна **по подпути `/kibana`** через Ingress (`http://localhost:8081/kibana/`) —
  `server.basePath=/kibana` + поддержка basePath в самом Kibana (без nginx-rewrite).

**Быстрый старт (discover):**

```bash
# открыть http://localhost:8081/kibana/
# Management → Stack Management → Index Patterns
# Create index pattern: bank-logs-*
# Выбрать @timestamp как Time Filter field
```

**Index pattern:** `bank-logs-*` — покрывает все дневные индексы.

Логи Kibana:

```bash
kubectl -n bank logs deploy/kibana -f
```

### Troubleshooting

- **Logstash пишет `Connection refused` к ES** — ES ещё стартует; Logstash ретраит в фоне.
  Убедиться, что под `elasticsearch-0` в `Running/Ready`:
  `kubectl -n bank get pod -l app.kubernetes.io/name=elasticsearch`.
- **Индекс `bank-logs-*` не создаётся** — отработал ли hook-job (`kubectl -n bank get job
  elasticsearch-init`, логи `kubectl -n bank logs job/elasticsearch-init`). Job удаляется
  helm'ом после успеха (hook-delete-policy: before-hook-creation), при `upgrade` пересоздаётся.
- **Логи не доходят** — проверить цепочку: под Logstash в `Running/Ready`; документы
  появляются в ES (в Kibana по Ingress `/kibana/` в Discover должны быть видны записи
  с index pattern `bank-logs-*`).
- **Маппинги не применились к уже созданному индексу** — шаблон применяется к *новым* индексам.
  Если индекс создан раньше шаблона, удалить дневной индекс (`curl -XDELETE localhost:9200/bank-logs-<дата>`,
  доступ к ES на 9200 — через port-forward, см. RUNBOOK) — Logstash пересоздаст по шаблону.
- **Kibana: "Unable to connect" / HTTP 502** — под Kibana не Ready или ES ещё не доступен.
  Проверить: `kubectl -n bank get pod -l app.kubernetes.io/name=kibana`, посмотреть логи
  (`kubectl -n bank logs deploy/kibana -f`). Kibana после старта занимает ~30–60 сек
  на полную инициализацию (находит ES, индексы).

## Ingress (nginx)

Внешняя маршрутизация — `Ingress` из `helm/bank/templates/ingress.yaml`
(`ingressClassName: nginx`). Через него к API обращается и frontend-service
(свойство `gateway.base-url` = `http://localhost:8081`).

| Путь | Backend (Service:порт) | Примечание |
|---|---|---|
| `/api/accounts` | accounts-service:9092 | — |
| `/api/cash` | cash-service:8084 | — |
| `/api/transfers` | transfer-service:8083 | — |
| `/realms`, `/admin` | keycloak:8082 | Keycloak |
| `/prometheus` | prometheus:9090 | `nginx rewrite /prometheus → /` |
| `/grafana` | grafana:3000 | `serve_from_sub_path` (префикс режет Grafana) |
| `/kibana` | kibana:5601 | `basePath=/kibana` (префикс режет Kibana) |
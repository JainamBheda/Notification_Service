# Notification Service

Production-style reusable notification microservice built with **Java 21** and **Spring Boot 3.4**.

Client products (e-commerce, banking, SaaS, etc.) authenticate with JWT, queue notifications over REST, and the service delivers them asynchronously via Kafka to EMAIL / SMS / PUSH channels.

## Architecture

```
Client Product
  → REST API (JWT, validation, idempotency, rate limit)
  → Persist QUEUED (MySQL)
  → Kafka (priority-aware topics)
  → Consumer + Template resolve
  → NotificationSender (Strategy)
      ├── Email (SMTP or logging)
      ├── SMS (Twilio-shaped or logging)
      └── Push (FCM-shaped or logging)
  → Status update + retry / DLQ
```

## Stack

| Concern | Technology |
|--------|------------|
| API | Spring Web, Validation, OpenAPI |
| Persistence | Spring Data JPA, MySQL, Flyway |
| Messaging | Apache Kafka (KRaft) |
| Cache / rate limit | Redis |
| Security | Spring Security + JWT |
| Observability | Actuator, Micrometer, MDC correlation |
| Packaging | Maven, Docker Compose |

## Quick start

```bash
docker compose up --build
```

Service: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Health: `http://localhost:8080/actuator/health`

### Demo credentials

| Field | Value |
|-------|-------|
| clientId | `demo-client` |
| clientSecret | `demo-secret` (override with `DEMO_CLIENT_SECRET`) |

### Example flow

```bash
# 1) Get token
curl -s -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"clientId\":\"demo-client\",\"clientSecret\":\"demo-secret\"}"

# 2) Queue notification
curl -s -X POST http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: abc123" \
  -d "{\"recipient\":\"user@example.com\",\"channel\":\"EMAIL\",\"templateCode\":\"WELCOME_USER\",\"priority\":\"HIGH\",\"data\":{\"name\":\"Jainam\"}}"

# 3) Check status
curl -s http://localhost:8080/api/v1/notifications/<notificationId>/status \
  -H "Authorization: Bearer <token>"
```

## Local development (without Docker app)

1. Start infra: `docker compose up mysql redis kafka`
2. Run: `mvn spring-boot:run` with profile `local` (connects to `localhost:9092` for Kafka)

Required env (defaults exist for local):

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `REDIS_HOST`, `REDIS_PORT`
- `JWT_SECRET` (min 32 chars)

## API overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/token` | No | Client credentials → JWT |
| POST | `/api/v1/notifications` | Yes | Queue notification (`202 QUEUED`) |
| GET | `/api/v1/notifications/{id}` | Yes | Details |
| GET | `/api/v1/notifications` | Yes | Paged list |
| GET | `/api/v1/notifications/{id}/status` | Yes | Status only |
| POST/GET/PUT/DELETE | `/api/v1/templates` | Yes | Template CRUD |

### Idempotency

Send `Idempotency-Key` on create. Unique per `(client_id, idempotency_key)`. Replays return the original notification.

### Rate limiting

Default **100 requests/minute/client** on `POST /api/v1/notifications` (Redis). Returns `429 RATE_LIMIT_EXCEEDED`. Configure with `RATE_LIMIT_PER_MINUTE`.

### Priority

| Priority | Kafka topic |
|----------|-------------|
| LOW, NORMAL | `notification.requested` |
| HIGH, CRITICAL | `notification.requested.priority` (higher consumer concurrency) |

### Retry / DLQ

Configurable via `notification.retry.*`:

- Exponential backoff
- After max attempts → `notification.dead-letter` and status `FAILED`

Statuses: `PENDING` → `QUEUED` → `PROCESSING` → `SENT` | `RETRYING` → `FAILED`

## Redis usage

1. **Rate limiting** – multi-instance safe counters  
2. **Template cache** – `clientId:templateCode:channel` with TTL  

## Channel providers

| Channel | `notification.providers.*` | Real integration |
|---------|----------------------------|------------------|
| email | `logging` (default) / `smtp` | Spring Mail |
| sms | `logging` / `twilio` | Twilio REST |
| push | `logging` / `fcm` | FCM legacy HTTP |

Set secrets via env: `TWILIO_*`, `FCM_SERVER_KEY`, mail host/port.

## Testing

```bash
mvn test
```

- Unit tests: template resolver, sender registry, notification service, processor/retry, Kafka publisher, JWT, rate limit, validation errors  
- Integration test (Testcontainers, skipped if Docker unavailable): auth → create → async SENT  

## Project layout

```
com.example.notification
├── controller / service / repository / entity / dto / mapper
├── kafka (producer, consumer, events)
├── channel (email, sms, push) + NotificationSender strategy
├── template
├── security
├── ratelimit
├── exception
├── config / observability / scheduler / common
```

## Production notes

- Do not commit secrets; inject via environment.
- Dual-write (DB then Kafka) is mitigated by the stuck-notification reconciliation job; a transactional outbox is the recommended hardening step.
- Swap JWT issuer to an external IdP later by replacing `JwtDecoder` / token endpoint.

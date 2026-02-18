# MagicOnCall — AI-Assisted On-Call + AI Visibility + Governance Operating System

## Vision
An AI-Assisted On-Call Operating System with AI Visibility and Governance.
Deterministic-first, LLM-second. All actions auditable and idempotent.

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.3, Gradle Kotlin DSL
- **Database**: PostgreSQL 16 + Flyway migrations (V001–V005)
- **Messaging**: Kafka (Redpanda v24.2.4)
- **Cache**: Redis 7
- **Observability**: OpenTelemetry + Micrometer + Prometheus
- **Resilience**: Resilience4j
- **Frontend**: React 18 (Vite 5) — user-facing, Vaadin — admin console
- **Testing**: Testcontainers (PostgreSQL + Kafka), REST Assured, JUnit 5, AssertJ

## Build & Run
- `make up` — start infra (Postgres, Redis, Redpanda)
- `make down` — stop infra
- `make build` — full build + tests
- `make run` — run the application (`./gradlew :app:bootRun`)
- `make test` — all tests
- `make test-integration` — integration tests only (`./gradlew :app:test`)
- `make test-unit` — unit tests only (excludes integration)
- `make ui-install` / `make ui-dev` — React frontend
- `make kafka-topics` — list Kafka topics
- `make db-reset` — drop + recreate database
- `make help` — list all targets

## Module Structure (10 Gradle modules)
```
modules/
  domain/                     — Entities, value objects, ports (interfaces), policy engine
  application/                — Use cases, service orchestration, commands, webhook ingestion
  api/                        — REST controllers, DTOs, auth/tenant filters, webhook endpoints, exception handler
  infrastructure/
    persistence/              — JPA repository adapters, outbox event publisher adapter
    messaging/                — Kafka producer config + KafkaEventPublisher
    connectors/               — External service integrations (ConnectorRegistry placeholder)
    observability/            — Metrics config (Micrometer common tags)
  workers/                    — Background jobs (OutboxPollerWorker — polls outbox → Kafka)
  eval/                       — LLM evaluation engine (EvalEngine placeholder)
  ui-react/                   — React frontend (Vite, not a Gradle module)
  ui-vaadin/                  — Vaadin admin console (placeholder)
infra/                        — docker-compose.yml (Postgres, Redis, Redpanda, Redpanda Console)
app/                          — Composition root (Spring Boot @EnableScheduling)
```

## Dependency Graph
```
domain              → (no project deps — pure model + ports)
application         → domain  (+ micrometer-core, resilience4j-retry, resilience4j-timelimiter)
api                 → application, domain
infra/persistence   → domain
infra/messaging     → domain
infra/connectors    → domain
infra/observability → (standalone)
workers             → domain, infra/persistence, infra/messaging
eval                → domain
app                 → ALL modules (composition root)
```

## Architecture Patterns

### Clean Architecture (Ports & Adapters)
- **Domain** defines interfaces (ports): `AlertRepository`, `IncidentRepository`, `EventPublisher`, `PolicyEngine`
- **Infrastructure** implements them (adapters): `JpaAlertRepository`, `JpaIncidentRepository`, `OutboxEventPublisher`, `KafkaEventPublisher`
- Direction of dependency always points inward: infra → domain, never domain → infra

### Outbox Pattern
- `EventPublisher` port in domain → `OutboxEventPublisher` adapter writes `OutboxEvent` to `outbox_events` table in the SAME transaction as the domain mutation
- `OutboxPollerWorker` (in workers module) polls unpublished events on a fixed delay (`magiconcall.outbox.poll-interval-ms`)
- Calls `KafkaEventPublisher.publish(aggregateType, eventType, key, payload)` to send to Kafka
- Marks event as published after successful send
- Kafka topic naming: `moc.{aggregate}.{event}` (e.g., `moc.alert.alert_created`)

### Policy Engine
- `PolicyEngine` interface with `evaluate(PolicyContext)` → `PolicyDecision`
- `AlertPolicyEvaluator` implements deterministic rules:
  - Duplicate externalId → DENY (dedup_rule)
  - CRITICAL severity → ALLOW with auto-escalation flag (severity_rule + auto_escalate_rule)
  - Default → ALLOW (default_allow_rule)
- PolicyDecision captures: outcome (ALLOW/DENY/ESCALATE), reason (audit trail), appliedRules

### Multi-Tenancy
- `X-Customer-Id` HTTP header → `TenantFilter` (Order 2) → `TenantContext.setTenantId()` (ThreadLocal)
- All entities have `tenant_id` column via `BaseEntity`
- Services call `TenantContext.requireTenantId()` and set on entity before save
- `DomainEvent` carries `tenantId` field → propagated to `OutboxEvent.tenantId`
- Missing header returns 400 with `MISSING_TENANT` error
- **Exception**: Webhook endpoints (`/webhooks/**`) set tenant from URL path, bypassing TenantFilter

### API Key Authentication
- `X-Api-Key` HTTP header → `ApiKeyAuthFilter` (Order 1)
- Valid keys configured via `magiconcall.security.api-keys` (comma-separated in application.yml)
- Only applied to `/api/**` paths; actuator/health/webhook endpoints are exempt
- Missing/invalid key returns 401 with `UNAUTHORIZED` error
- Dev keys: `moc-dev-key-001`, `moc-dev-key-002`
- Test key: `test-api-key`

## Implemented Features

### 1. Alert Ingestion & Management
- **POST** `/api/v1/alerts` — ingest alert (policy-evaluated, deduplicated, outbox event published)
- **GET** `/api/v1/alerts/{id}` — get alert by ID
- **GET** `/api/v1/alerts?status=OPEN` — list alerts by status
- **POST** `/api/v1/alerts/{id}/acknowledge` — transition to ACKNOWLEDGED
- **POST** `/api/v1/alerts/{id}/resolve` — transition to RESOLVED
- All endpoints require `X-Api-Key` + `X-Customer-Id` headers
- Domain events: `ALERT_CREATED` via outbox

### 2. Incident Brain (Core Incident Management)
**Entities**: Incident, IncidentEvent (timeline), Hypothesis, Evidence, Action, AuditLog

**State Machine** (enforced in `IncidentStatus` enum):
```
NEW → TRIAGING → INVESTIGATING → MITIGATING → MONITORING → RESOLVED → POSTMORTEM
                 ↘ RESOLVED     ↗ RESOLVED   ↗ RESOLVED
```
- Shortcuts: TRIAGING/INVESTIGATING/MITIGATING can jump directly to RESOLVED
- `Incident.transitionTo(newStatus)` returns previous status for audit logging
- Invalid transitions throw `IncidentStatus.InvalidTransitionException` → 409 CONFLICT

**APIs**:
- **POST** `/api/v1/incidents` — create incident (idempotent on externalId)
- **GET** `/api/v1/incidents/{id}` — get incident by ID
- **GET** `/api/v1/incidents/{id}/timeline` — get append-only timeline events
- **POST** `/api/v1/incidents/{id}/hypotheses` — add hypothesis (PROPOSED/INVESTIGATING/CONFIRMED/REJECTED)
- **POST** `/api/v1/incidents/{id}/evidence` — add evidence (LOG/METRIC/TRACE/ALERT/RUNBOOK/SCREENSHOT/OTHER)
- **POST** `/api/v1/incidents/{id}/transition?status=X` — transition state machine

**Cross-cutting**:
- Structured logging with MDC (`incidentId` context)
- Every mutation creates timeline IncidentEvent + AuditLog entries
- Domain events: `INCIDENT_CREATED` via outbox

### 3. PagerDuty Webhook Ingestion
**Endpoint**: `POST /webhooks/{tenantId}/pagerduty` (bypasses API key auth)

**Flow**: PD webhook → PagerDutyPayload DTO → PagerDutyNormalizer → NormalizedAlert → WebhookIngestionService

**Behavior**:
1. **Normalize** — PD V2 payload → source-agnostic `NormalizedAlert`
2. **Dedup** — composite key `pd:{service_id}:{incident_key}` maps to Alert `externalId`; if exists, return existing (dedup hit)
3. **Find or create Incident** — PD incident ID → `pd-incident:{pd_incident_id}` as Incident `externalId`; creates if not found
4. **Create Alert** — linked to incident via `alert.incident_id` FK
5. **Timeline** — `ALERT_CORRELATED` event added to incident timeline
6. **Emit event** — `ALERT_RECEIVED` domain event published via outbox

**Severity mapping**:
- PD urgency `high` → AlertSeverity.CRITICAL / IncidentSeverity.SEV1
- PD urgency `low` → AlertSeverity.WARNING / IncidentSeverity.SEV3

**Metrics** (Micrometer counters):
- `moc_alerts_ingested_total{source=pagerduty}` — total alerts ingested
- `moc_incidents_created_total{source=pagerduty}` — auto-created incidents
- `moc_webhook_dedup_total{outcome=hit|miss}` — dedup hit rate

**Key design decisions**:
- Webhooks bypass `/api/**` auth — `ApiKeyAuthFilter` only applies to `/api/**` paths
- Tenant set from URL path `{tenantId}`, not from `X-Customer-Id` header
- `WebhookIngestionService` works directly with repositories (not through AlertService/IncidentService) for full transaction control
- Multi-message PD webhooks processed in a loop; null incidents skipped

### 4. Tool Framework (AI Tool Execution)
**Purpose**: AI incident assistant invokes external tools (logs, metrics, deploy history, topology) during investigation. Responses stored as Evidence for audit trail.

**Architecture** (Clean Architecture — ports in domain, orchestration in application, REST in api):
- `Tool` interface (port): `String name()`, `ToolResponse execute(ToolRequest)`
- `ToolExecutionService` (orchestrator): rate limit → Resilience4j retry+timeout → persist log → metrics → store Evidence → publish event
- `ToolRegistry`: Spring injects `List<Tool>` beans → ConcurrentHashMap by name

**Available Tools** (stubs returning synthetic data):
- `LogsTool` ("logs") — log queries
- `MetricsTool` ("metrics") — metric queries
- `DeployTool` ("deploy") — deployment history
- `TopologyTool` ("topology") — service dependency maps

**APIs**:
- **POST** `/api/v1/tools/{toolName}/run` — execute tool (optional incidentId links output as Evidence)
- **GET** `/api/v1/tools` — list available tool names

**Resilience4j** (programmatic API, not annotations):
- RetryConfig: 3 attempts, 500ms wait, retries RuntimeException
- TimeLimiterConfig: 30s timeout, cancels running future
- Package-private constructor accepts custom configs for unit testing

**Rate Limiting**: `ToolRateLimiter` interface → `StubToolRateLimiter` (always allows) — ready for real implementation

**Audit Trail**:
- Every execution persisted as `ToolExecutionLog` entity (toolName, status, request/response, duration, retryCount, requestedBy)
- Successful executions with `incidentId` → `Evidence` (type=TOOL_OUTPUT) + `IncidentEvent` (EVIDENCE_ADDED) timeline entry

**Execution Statuses**: SUCCESS, FAILURE, TIMEOUT, RATE_LIMITED

**Metrics** (Micrometer counters):
- `moc_tool_executions_total{tool,status}` — total executions by tool and status
- `moc_tool_rate_limited_total{tool}` — rate-limited executions

**Key design decisions**:
- `ToolMetrics` is an interface (not concrete class) for Java 24 Mockito compatibility
- `@Autowired` on public constructor of `ToolExecutionService` (has 2 constructors — Spring needs disambiguation)
- Tool stubs are `@Component` beans in `application.tool.tools` package
- ToolNotFoundException → 404 TOOL_NOT_FOUND via GlobalExceptionHandler

## Database Schema (Flyway Migrations)

### V001 — Initial Schema
- `alerts` (id, external_id, title, description, source, severity, status, labels JSONB, policy_decision_reason, audit fields, version)
- `outbox_events` (id, aggregate_type, aggregate_id, event_type, payload JSONB, published, audit fields)

### V002 — Multi-Tenancy
- Added `tenant_id` column to `alerts` and `outbox_events`

### V003 — Incident Brain
- `incidents` (id, tenant_id, external_id, title, summary, severity, status, commander_name, slack_channel_id, tags JSONB, audit fields, version)
- `incident_events` (id, tenant_id, incident_id FK, event_type, description, metadata JSONB, audit fields)
- `hypotheses` (id, tenant_id, incident_id FK, title, description, status, confidence, source, audit fields)
- `evidence` (id, tenant_id, incident_id FK, hypothesis_id FK nullable, evidence_type, title, content, source_url, metadata JSONB, audit fields)
- `actions` (id, tenant_id, incident_id FK, title, description, status, assignee, action_type, audit fields)
- `audit_log` (id, tenant_id, incident_id FK, entity_type, entity_id, action, previous_state JSONB, new_state JSONB, audit fields)

### V004 — Alert-Incident Link
- Added `incident_id UUID REFERENCES incidents(id)` to `alerts` table
- Index: `idx_alerts_incident_id`

### V005 — Tool Execution Log
- `tool_execution_log` (id, tenant_id, tool_name, incident_id, status CHECK, request_payload JSONB, response_content, error_message, duration_ms, retry_count, requested_by, executed_at, audit fields)
- Indexes: tool_name, incident_id, tenant_id+executed_at, status
- Updated `evidence` CHECK constraint to include `TOOL_OUTPUT`

## Package Naming Convention
```
com.magiconcall.domain.alert          — Alert, AlertRepository (port), AlertSeverity, AlertStatus
com.magiconcall.domain.incident       — Incident, IncidentStatus, IncidentEvent, IncidentEventType, Hypothesis, Evidence, Action, AuditLog, + repositories (ports)
com.magiconcall.domain.common         — BaseEntity
com.magiconcall.domain.tool           — Tool (port), ToolRequest, ToolResponse, ToolExecutionStatus, ToolExecutionLog, ToolExecutionLogRepository (port)
com.magiconcall.domain.event          — DomainEvent, EventPublisher (port), AlertCreatedEvent, AlertReceivedEvent, IncidentCreatedEvent
com.magiconcall.domain.policy         — PolicyEngine, PolicyContext, PolicyDecision, AlertPolicyEvaluator
com.magiconcall.domain.tenant         — TenantContext

com.magiconcall.application.alert     — AlertService, IngestAlertCommand, AlertResult
com.magiconcall.application.incident  — IncidentService, CreateIncidentCommand, AddHypothesisCommand, AddEvidenceCommand, IncidentResult, HypothesisResult, EvidenceResult, TimelineResult
com.magiconcall.application.webhook   — WebhookIngestionService, NormalizedAlert, WebhookMetrics
com.magiconcall.application.tool      — ToolExecutionService, ToolRegistry, ToolMetrics (interface), MicrometerToolMetrics, ToolRateLimiter (interface), StubToolRateLimiter, ExecuteToolCommand, ToolExecutionResult
com.magiconcall.application.tool.tools — LogsTool, MetricsTool, DeployTool, TopologyTool

com.magiconcall.api.alert             — AlertController, AlertRequest, AlertResponse
com.magiconcall.api.incident          — IncidentController, CreateIncidentRequest, IncidentResponse, AddHypothesisRequest, HypothesisResponse, AddEvidenceRequest, EvidenceResponse, TimelineResponse
com.magiconcall.api.webhook           — PagerDutyWebhookController, PagerDutyPayload, PagerDutyNormalizer
com.magiconcall.api.tool              — ToolController, ToolRunRequest, ToolRunResponse
com.magiconcall.api.security          — ApiKeyAuthFilter
com.magiconcall.api.tenant            — TenantFilter
com.magiconcall.api                   — GlobalExceptionHandler

com.magiconcall.infrastructure.persistence.alert     — JpaAlertRepository, SpringDataAlertRepository
com.magiconcall.infrastructure.persistence.incident  — Jpa*Repository (5), SpringData*Repository (5)
com.magiconcall.infrastructure.persistence.tool       — JpaToolExecutionLogRepository, SpringDataToolExecutionLogRepository
com.magiconcall.infrastructure.persistence.outbox    — OutboxEvent, OutboxRepository, OutboxEventPublisher
com.magiconcall.infrastructure.messaging.kafka       — KafkaConfig, KafkaEventPublisher
com.magiconcall.infrastructure.connectors            — ConnectorRegistry
com.magiconcall.infrastructure.observability         — MetricsConfig
com.magiconcall.workers.outbox                       — OutboxPollerWorker
com.magiconcall.eval                                 — EvalEngine
```

## Test Suite (68 tests, all GREEN)

### Unit Tests
- `AlertPolicyEvaluatorTest` (4 tests) — dedup, severity rules, default allow
- `IncidentStatusTest` (6 tests) — valid transitions, invalid transitions, entity enforcement, force-close
- `PagerDutyNormalizerTest` (5 tests) — urgency mapping, null handling, dedup key construction
- `ToolRegistryTest` (3 tests) — find by name, not found exception, list all tools
- `ToolExecutionServiceTest` (6 tests) — retry on transient failure, all retries exhausted, timeout, rate limiting, evidence storage with/without incidentId

### Integration Tests (Testcontainers: PostgreSQL + Kafka)
- `MagicOnCallApplicationTest` (1 test) — context loads
- `AlertIntegrationTest` (8 tests) — CRUD, policy denial, acknowledge, resolve, status filter, 404
- `IncidentIntegrationTest` (8 tests) — create, idempotent, get, full state machine walk, invalid transition, add hypothesis, add evidence, 404
- `PagerDutyWebhookIntegrationTest` (7 tests) — ingest + auto-create incident, dedup, attach to existing, timeline correlation, severity mapping, empty payload 400, bypass auth
- `ToolIntegrationTest` (5 tests) — execute logs tool, execute metrics tool, unknown tool 404, execute with incidentId stores evidence + timeline, list tools

### Test counts by module
- `app/` — 30 integration tests
- `modules/application/` — 9 unit tests (6 ToolExecutionService + 3 ToolRegistry)
- `modules/domain/` — 24 unit tests (parameterized expand to 24)
- `modules/api/` — 5 unit tests

## Key Configuration (application.yml)
```yaml
magiconcall:
  security:
    api-keys: moc-dev-key-001,moc-dev-key-002   # comma-separated valid API keys
  outbox:
    poll-interval-ms: 1000                        # outbox poller interval

spring.kafka.bootstrap-servers: localhost:19092   # Redpanda Kafka API
spring.data.redis.host: localhost                 # Redis
spring.datasource.url: jdbc:postgresql://localhost:5432/magiconcall
```

## Conventions
- Java 21, Spring Boot 3.3, Gradle Kotlin DSL
- All entities extend `BaseEntity` (tenantId, createdAt, updatedAt, createdBy, version)
- All mutations produce domain events via the Outbox
- REST endpoints: `/api/v1/<resource>` (auth required), `/webhooks/{tenantId}/<source>` (no auth)
- Flyway migrations in `app/src/main/resources/db/migration/`
- Integration tests use Testcontainers (PostgreSQL + Kafka) — no Docker Compose needed
- Test class naming: `*IntegrationTest.java` for integration, `*Test.java` for unit

## Code Style
- No Lombok — use records for DTOs/commands/events, explicit getters/setters for entities
- Prefer constructor injection
- All public service methods are `@Transactional`
- Idempotency enforced via external IDs / deduplication keys
- Filters ordered: ApiKeyAuthFilter (Order 1) → TenantFilter (Order 2)
- Spring Data interfaces are package-private; public adapters implement domain ports
- Static factory methods on domain entities (e.g., `IncidentEvent.created()`, `AuditLog.statusChanged()`)
- Structured logging with SLF4J MDC for context (incidentId, alertId)

## Claude Workflow — Staff+ Founding Engineer Mode
When implementing features, always follow this sequence:
1. **PLAN** — print the implementation plan
2. **REPO TREE** — print the affected repo tree
3. **File-by-file code** — print code for each file
4. **HOW TO RUN** — finish with run/verify instructions
5. **Tests** — always include tests

Ship MVP slices pragmatically.

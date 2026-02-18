# MagicOnCall — Progress Tracker

## Phase 1: Persona & Master Prompt
**Status**: DONE

- Established Staff+ Founding Engineer persona
- Defined project vision: AI-Assisted On-Call Operating System
- Defined tech stack, architecture patterns, conventions
- Created initial `CLAUDE.md` as living context document

## Phase 2: Production-Grade Monorepo Scaffold
**Status**: DONE

- **10 Gradle modules** created under `modules/`:
  - `domain`, `application`, `api`
  - `infrastructure/persistence`, `infrastructure/messaging`, `infrastructure/connectors`, `infrastructure/observability`
  - `workers`, `eval`, `ui-react`, `ui-vaadin`
- `app/` composition root (Spring Boot)
- Dependency graph enforced (domain has zero project deps, everything points inward)
- Flyway migrations V001-V002 (alerts, outbox_events, multi-tenancy)
- Outbox pattern: domain events → outbox table → Kafka relay
- Multi-tenancy: `TenantFilter` + `TenantContext` ThreadLocal
- API key auth: `ApiKeyAuthFilter`
- Alert Ingestion feature: CRUD + policy engine + dedup
- Docker Compose infra (Postgres, Redis, Redpanda)
- Makefile with all targets
- **15 tests GREEN**

## Phase 3: Context Documentation
**Status**: DONE

- Comprehensive `CLAUDE.md` update (all patterns, APIs, packages)
- `MEMORY.md` created for cross-session auto memory

## Phase 4: Incident Brain
**Status**: DONE

- **6 new entities**: Incident, IncidentEvent (timeline), Hypothesis, Evidence, Action, AuditLog
- **State machine** in `IncidentStatus` enum:
  ```
  NEW → TRIAGING → INVESTIGATING → MITIGATING → MONITORING → RESOLVED → POSTMORTEM
                   ↘ RESOLVED     ↗ RESOLVED   ↗ RESOLVED
  ```
- Flyway V003 — 6 new tables with constraints, indexes
- Full API surface:
  - `POST /api/v1/incidents` (idempotent on externalId)
  - `GET /api/v1/incidents/{id}`
  - `GET /api/v1/incidents/{id}/timeline`
  - `POST /api/v1/incidents/{id}/hypotheses`
  - `POST /api/v1/incidents/{id}/evidence`
  - `POST /api/v1/incidents/{id}/transition?status=X`
- 5 JPA repository adapters
- Domain events: `INCIDENT_CREATED` via outbox
- Structured logging with MDC (incidentId)
- **39 tests GREEN** (+24 new)

## Phase 5: PagerDuty Webhook Ingestion
**Status**: DONE

- `POST /webhooks/{tenantId}/pagerduty` (bypasses API key auth)
- Full pipeline: PD V2 payload → `PagerDutyNormalizer` → `NormalizedAlert` → `WebhookIngestionService`
- Dedup: composite key `pd:{service_id}:{incident_key}`
- Auto-creates incidents from PD incident data
- Links alerts to incidents via `alert.incident_id` FK
- Timeline: `ALERT_CORRELATED` events
- Domain events: `ALERT_RECEIVED` via outbox
- Flyway V004 — alert-incident FK
- Micrometer metrics: `moc_alerts_ingested_total`, `moc_incidents_created_total`, `moc_webhook_dedup_total`
- **54 tests GREEN** (+15 new)

## Phase 6: Tool Framework
**Status**: DONE

- `Tool` port interface: `ToolRequest → ToolResponse`
- 4 stub tools: `LogsTool`, `MetricsTool`, `DeployTool`, `TopologyTool`
- `ToolExecutionService` orchestrator: rate limit → Resilience4j retry (3 attempts) + timeout (30s) → persist `ToolExecutionLog` → metrics → store as `Evidence` → publish event
- `ToolRegistry`: auto-discovers `Tool` beans
- `ToolRateLimiter` interface + `StubToolRateLimiter`
- `ToolMetrics` interface + `MicrometerToolMetrics`
- Flyway V005 — `tool_execution_log` table
- API: `POST /api/v1/tools/{toolName}/run`, `GET /api/v1/tools`
- **68 tests GREEN** (+14 new)

## Phase 7: Git & Push
**Status**: DONE

- Created repo at https://github.com/sujayopensource/MagicOnCall
- 154 files, 6,972 lines
- Initial commit pushed to `main`

## Phase 8: Hypothesis Engine (AI Triage Pipeline)
**Status**: DONE

- Two-stage AI triage pipeline: **EvidenceSummarizer** → **LLM JSON Reasoner**
- `LlmClient` port interface in domain (like `Tool`, `EventPublisher`)
- `MockLlmClient` default via `@ConditionalOnProperty(llm.enabled=false, matchIfMissing=true)`
- `EvidenceSummarizer`: fetches evidence, groups by type, truncates, SHA-256 hashes
- `TriageService` orchestrator: summarize → cache check → budget check → LLM → parse → persist → timeline → metrics → cache
- ConcurrentHashMap cache by `evidence_hash` — avoids redundant LLM calls
- Token budget gating (~4 chars/token heuristic)
- `TriageMetrics` interface + `MicrometerTriageMetrics`: `moc_triage_runs_total`, `moc_llm_tokens_used_total`, `moc_triage_budget_exceeded_total`
- `TokenBudgetExceededException` → 413 PAYLOAD_TOO_LARGE
- Hypothesis entity extended: `evidenceFor`, `evidenceAgainst`, `nextBestTest`, `stopCondition`, `evidenceHash`
- `IncidentEventType.TRIAGE_COMPLETED` + `IncidentEvent.triageCompleted()` factory
- Flyway V006 — 5 new columns on `hypotheses` table
- New APIs:
  - `POST /api/v1/incidents/{id}/triage` — trigger AI triage
  - `GET /api/v1/incidents/{id}/hypotheses` — list all hypotheses
- 12 new files, 9 modified files
- **83 tests GREEN** (+15 new: 4 EvidenceSummarizer, 2 MockLlmClient, 4 TriageService, 5 TriageIntegration)

## Phase 9: Incident Correlation Graph
**Status**: DONE

- **Persisted correlation graph** per incident: nodes (ALERT, METRIC_ANOMALY, LOG_CLUSTER, DEPLOY, SERVICE, DEPENDENCY) and edges (TIME_CORRELATION, DEPENDS_ON, CAUSAL_HINT, SAME_RELEASE)
- **Root cause path analysis** — DFS backward from symptom nodes to root cause nodes, scored by edge weight product with length penalty
- **Blast radius computation** — BFS forward from root cause node through outgoing edges
- **Auto-seeding** — GET graph auto-creates ALERT nodes from linked alerts (idempotent via referenceId)
- Domain: `CorrelationNode`, `CorrelationEdge` entities + `CorrelationNodeType`, `CorrelationEdgeType` enums + repository ports
- Persistence: JPA adapters for nodes and edges, `AlertRepository.findByIncidentId` added
- Flyway V007 — `correlation_nodes` + `correlation_edges` tables (CHECK constraints, no-self-loop, weight 0–1)
- `CorrelationGraphService` — full graph CRUD + DFS/BFS algorithms
- New timeline events: `GRAPH_NODE_ADDED`, `GRAPH_SEEDED`
- New APIs:
  - `GET /api/v1/incidents/{id}/graph` — full graph (auto-seeds alerts)
  - `POST /api/v1/incidents/{id}/graph/nodes` — add node (201)
  - `POST /api/v1/incidents/{id}/graph/edges` — add edge (201)
  - `GET /api/v1/incidents/{id}/root-cause-paths?maxPaths=3` — DFS analysis
  - `GET /api/v1/incidents/{id}/blast-radius/{nodeId}` — BFS analysis
- **React UI** (`@xyflow/react` + `@dagrejs/dagre`): custom node component with type badges/colors, dagre auto-layout, root cause paths panel, route `/incidents/:incidentId/graph`
- **Vaadin Admin** HTML table view at `/admin/graph/{incidentId}`
- `NodeNotFoundException` → 404 NODE_NOT_FOUND via GlobalExceptionHandler
- 30 new files, 8 modified files
- **104 tests GREEN** (+21 new: 2 CorrelationNodeType, 2 CorrelationEdgeType, 9 CorrelationGraphService, 8 CorrelationGraphIntegration)

---

## Summary

| Phase | Description | Tests | Status |
|-------|-------------|-------|--------|
| 1 | Persona & Master Prompt | — | DONE |
| 2 | Production-Grade Monorepo Scaffold | 15 | DONE |
| 3 | Context Documentation | — | DONE |
| 4 | Incident Brain | 39 | DONE |
| 5 | PagerDuty Webhook Ingestion | 54 | DONE |
| 6 | Tool Framework | 68 | DONE |
| 7 | Git & Push | — | DONE |
| 8 | Hypothesis Engine | 83 | DONE |
| 9 | Correlation Graph | 104 | DONE |

**Current state**: 104 tests, all GREEN. 7 Flyway migrations. 10 Gradle modules. 6 feature slices shipped.

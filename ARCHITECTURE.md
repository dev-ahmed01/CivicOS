# CivicOS Implementation Architecture

## Product boundary

CivicOS is a coordination and lifecycle-intelligence layer around road-impacting interventions. The flagship MVP connects cross-agency work on Bengaluru road segments and demonstrates deterministic conflict detection, human-controlled coordination, approval, execution evidence, inspection, citizen verification, and closure.

It does not replace MARCS or other statutory systems.

## Runtime architecture

```text
Browser
  ↓
React + TypeScript + Vite
  ↓
REST /api/v1
  ↓
Spring Boot application services
  ↓
Modular domain packages
  ↓
PostgreSQL + PostGIS
```

AI remains behind a provider-neutral gateway and cannot mutate authoritative workflow state.

## Backend structure

The backend is a modular monolith rooted at `com.civicos`. Modules are introduced in the master specification's implementation order:

```text
common, auth, user, agency, road, casefile, intervention,
dependency, conflict, coordination, approval, sla, escalation,
evidence, inspection, verification, workflow, notification, audit, ai, admin
```

Each implemented module owns its controller, service/application logic, domain model, repository, DTOs, mapping, and validation where applicable. Controllers do not contain business logic and JPA entities are never exposed directly.

## Authority boundaries

```text
Deterministic rules = conflict, severity, workflow, SLA, authorization
Human actors        = consequential coordination and approval decisions
AI                  = advisory classification, explanation, and recommendation
Audit               = immutable accountability record
Evidence            = provenance-preserving proof
```

## Phase 1 decisions

- Java 21 and Spring Boot 3.5.x
- Maven Wrapper for reproducible backend builds
- React 19, TypeScript, and Vite for the frontend build
- PostgreSQL 17 + PostGIS 3.5 as the only Docker Compose service
- environment variables as the configuration and secrets boundary
- AI disabled by default
- no database schema generation before Flyway is introduced in Phase 2
- no domain behavior or role-specific UI implemented ahead of its phase

## Phase 2 decisions

- Flyway is the sole production schema mechanism; Hibernate validates rather than creates the schema.
- PostGIS and `pgcrypto` are enabled by versioned migration.
- Spatial data uses EPSG:4326 with GiST indexes on road, road-segment, intervention, and observation geometry.
- The schema covers identity, agencies, roads, cases, observations, interventions, dependencies, conflicts, AI advice, coordination decisions, approvals, SLAs, escalations, evidence, inspections, verifications, notifications, and audit events.
- Database constraints enforce lifecycle vocabulary, referential integrity, valid geometry and time ranges, and required human-decision reasons.
- Audit events are append-only at the database layer.
- Domain entities and repositories were deliberately deferred to Phase 3.

## Phase 3 decisions

- The canonical domain is represented by 22 JPA entities; `Road` supports the required `RoadSegment` aggregate and Phase 2 schema.
- Entities use UUID identity, timezone-aware `Instant` timestamps, domain enums for authoritative states, JTS geometry for PostGIS, and typed JSONB mappings for AI and evidence metadata.
- `CivicCase`, `Intervention`, `Conflict`, `Approval`, `Sla`, and `Inspection` use optimistic locking where defined by the schema.
- Relationships map the database foreign keys and the role-permission, user-role, and conflict-intervention junction tables.
- Each module owns its Spring Data repository. No generic cross-domain CRUD repository exists.
- Spatial intersection, radius, and spatial-temporal candidate searches execute in PostGIS through native repository queries.
- `AuditEventRepository` deliberately exposes append and read operations without delete operations; the database remains the final immutability guard.
- Entity state does not expose generic status setters. Authoritative transition methods belong to the Phase 5 workflow implementation.

## Phase 4 decisions

- The canonical MVP role set is `CITIZEN`, `AGENCY_OFFICER`, `COORDINATOR`, `INSPECTOR`, and `ADMIN`; permissions remain explicit action-oriented records.
- Password authentication uses BCrypt. Access tokens are HMAC-SHA256 JWTs with validated issuer, issued-at, expiry, subject, and token ID claims.
- Refresh tokens are cryptographically random, stored only as SHA-256 hashes, rotated on use, revocable on logout, and protected against concurrent use.
- JWTs carry identity rather than trusted role claims. Active status, roles, and effective permissions are reloaded from PostgreSQL for every authenticated request.
- Method-level permission checks and application-level agency scope checks keep backend authorization authoritative.
- Requests receive correlation IDs, and authentication lifecycle events are written to the append-only audit store.
- Security errors use consistent non-leaking JSON responses. Token and password DTO string representations are redacted.
- Workflow authorization continues to resolve explicit database-backed permissions; administrative access is not an implicit operational override.

## Phase 5 decisions

- `CivicCase` and `Intervention` own command-oriented transition methods. There is no generic status mutation path.
- The master specification's lifecycle vocabulary and correction loop are authoritative; supporting-document-only statuses are not introduced.
- A transition transaction validates the actor, role, permission, agency scope, expected entity version, current state, separation of duties, and applicable business preconditions before mutation.
- `@Version` optimistic locking plus a caller-supplied expected version prevents duplicate authoritative transitions and returns a conflict-class domain error.
- Intervention approval requires an authoritative approval record from the current actor, no unresolved high-severity blocking conflict, and a creator distinct from the approver.
- Pass/fail workflow transitions require the corresponding human verification record. Closure rechecks that successful final verification exists.
- The entity update and immutable audit event are committed atomically. Audit state records the action, before/after status and version, actor, reason, and correlation ID.
- Stable workflow error codes distinguish invalid state, failed preconditions, concurrent modification, and separation-of-duties denial.
- Phase 5 exposes application services for later command APIs; the REST/OpenAPI surface remains in its master-specified phase.

## Phase 6 decisions

- Road and road-segment commands validate non-empty EPSG:4326 geometry and persist authoritative spatial data through PostGIS.
- Road-segment identity is immutable. Geometry changes require the separate `ROAD_GEOMETRY_UPDATE` permission, and inactive or operationally referenced records cannot be silently removed.
- Intervention creation is agency-scoped and starts in `DRAFT`. The civic case, owning agency, active road segment, planned interval, and PostGIS intersection are validated atomically.
- Direct intervention edits are restricted to `DRAFT`; submitted work must use the workflow and later material-change/re-analysis path.
- Required dependencies use the canonical `source → target` direction. Invalid current schedules create a visible `BLOCKED` dependency rather than hiding the planning relationship.
- Required dependency cycles are rejected under a PostgreSQL transaction-level advisory lock, preventing concurrent inverse edges from bypassing cycle detection.
- Dependencies are cancelled with a reason and audit event instead of being deleted.
- Migration `V11` adds optimistic versions to roads, road segments, and dependencies. Intervention optimistic locking already existed.
- Phase 6 mutations use explicit permissions, roles, agency scope, stable validation/conflict errors, and atomic immutable audit records.
- Conflict record generation remains Phase 7; Phase 6 only supplies the validated spatial, temporal, and dependency inputs.

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
evidence, inspection, verification, notification, audit, ai, admin
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
- Workflow-state and separation-of-duties authorization remain part of Phase 5, where authoritative transition rules exist.

# CivicOS

CivicOS is a municipal coordination platform for cross-agency road-cutting interventions. It connects planned work on the same physical road, detects spatial and temporal coordination risks, supports human decisions, and preserves an evidence-backed lifecycle through verification and closure.

The SIH MVP is intentionally focused on Bengaluru road-cutting coordination. It complements existing municipal systems; it is not another permission portal, complaint-only application, or AI chatbot.

## Current implementation status

Phases 1 through 4 establish the deployable project, persistence, domain, and security foundation:

- Java 21 and Spring Boot 3 modular-monolith backend scaffold
- React, TypeScript, and Vite frontend scaffold
- PostgreSQL 17 with PostGIS 3.5 through Docker Compose
- versioned Flyway migrations for the complete CivicOS relational schema
- database-enforced foreign keys, uniqueness, checks, timestamps, spatial indexes, and append-only audit events
- Docker-backed integration tests for migration, schema, spatial, and audit behavior
- 22 operational JPA domain entities plus hashed refresh-token persistence
- 23 module-owned Spring Data repositories with native PostGIS candidate queries where appropriate
- stateless JWT authentication with rotating, revocable refresh tokens
- explicit database-backed RBAC, method authorization, agency scope checks, and security audit events
- environment-based configuration with no committed credentials
- backend and frontend smoke tests

The core workflow/state machine begins in Phase 5. Conflict detection and role-specific screens remain in their later phases as defined by the master specification.

## Repository layout

```text
CivicOS/
├── backend/             Spring Boot REST application
├── frontend/            React + TypeScript application
├── docker-compose.yml   Local PostgreSQL/PostGIS
├── .env.example         Configuration contract without secrets
├── ARCHITECTURE.md      Implementation architecture
└── SETUP.md             Reproducible local setup
```

## Quick start

See [SETUP.md](SETUP.md) for prerequisites and exact commands.

```powershell
Copy-Item .env.example .env
docker compose up -d

Set-Location backend
.\mvnw.cmd test

Set-Location ..\frontend
npm.cmd install
npm.cmd run dev
```

## Canonical specifications

The final implementation contract is `New_Final_Codex_Master_Implementation_Specification.md`. The other engineering-definition documents provide domain, architecture, authorization, engine, database/API, AI, UI/UX, and deterministic demo-data details.

All Bengaluru operational records shipped with the eventual demo will be clearly labelled **DEMO / SYNTHETIC**.

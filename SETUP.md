# CivicOS Local Setup

## Prerequisites

- Java 21 or newer
- Node.js 20.19+, 22.12+, or a newer compatible release
- Docker Desktop with Docker Compose
- Git

The backend includes the Maven Wrapper, so a machine-wide Maven installation is not required.

## 1. Configure the environment

From the repository root:

```powershell
Copy-Item .env.example .env
```

Replace the placeholder database password and JWT secret before using a shared or deployed environment. The JWT value is not consumed until authentication is implemented in Phase 4.

## 2. Start PostgreSQL/PostGIS

```powershell
docker compose up -d
docker compose ps
```

Stop the service without deleting its data:

```powershell
docker compose down
```

The named database volume is intentionally preserved. Do not add automatic destructive reset behavior.

## 3. Build and test the backend

Windows PowerShell:

```powershell
Set-Location backend
.\mvnw.cmd clean verify
```

macOS/Linux:

```bash
cd backend
./mvnw clean verify
```

Run locally:

```powershell
.\mvnw.cmd spring-boot:run
```

The default backend URL is `http://localhost:8080`. Spring Security is present as required by the technology baseline; the CivicOS authentication and RBAC contract is implemented in Phase 4.

Flyway applies the versioned migrations from `backend/src/main/resources/db/migration` when the application starts. `clean verify` also starts an isolated PostGIS Testcontainers database and verifies the schema, spatial behavior, and append-only audit protection. Docker Desktop must therefore be running for backend integration tests.

Applied migrations are immutable. Add a new versioned migration for every later schema change; do not edit a migration that has already been applied to a shared database.

## 4. Build and test the frontend

```powershell
Set-Location frontend
npm.cmd install
npm.cmd run lint
npm.cmd run test
npm.cmd run build
npm.cmd run dev
```

The default frontend URL is `http://localhost:5173`.

## 5. Configuration

All deployable configuration is supplied through environment variables. The contract is documented in `.env.example`.

Important feature defaults:

```text
AI_ENABLED=false
CITIZEN_REPORTING_ENABLED=true
DEMO_MODE=false
```

The core workflow must remain operational with AI disabled.

## Phase boundary

Phase 1 supplies the repository, builds, Docker Compose service, and configuration contract. Phase 2 supplies PostgreSQL/PostGIS persistence configuration and Flyway migrations through `V9`. Phase 3 maps the domain entities and module-owned repositories, with Hibernate validating those mappings against Flyway. Authentication and RBAC begin in Phase 4.

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

Replace the placeholder database password and JWT secret before starting the backend. `JWT_SECRET` must contain at least 32 characters and must be unique per deployed environment.

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

The default backend URL is `http://localhost:8080`. CivicOS uses stateless bearer authentication with BCrypt password verification, signed access JWTs, and rotating refresh tokens stored only as SHA-256 hashes.

Authentication endpoints:

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
GET  /api/v1/users/me
```

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

## 6. Controlled synthetic demo dataset

Phase 19 provides a deterministic dataset for local judging and integration work. It is never loaded by the default `local` profile. Use a fresh, non-production database, set a demo-only password of at least 12 characters, and activate the explicit profile:

```powershell
$env:SPRING_PROFILES_ACTIVE='demo'
$env:DEMO_PASSWORD='choose-a-demo-only-password'
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The loader refuses to overlay an existing operational database unless that database already contains the CivicOS demo marker. It can then be rerun safely and idempotently. All agencies, users, roads, interventions, evidence metadata, AI outputs, notifications, and audit history are labelled **DEMO / SYNTHETIC**. The external-project descriptions are simulated source records; they do not claim a live MARCS connection.

Demo sign-in emails use the shared password supplied through `DEMO_PASSWORD`:

```text
citizen.demo@civicos.example.invalid
engineer.demo@civicos.example.invalid
coordinator.demo@civicos.example.invalid
inspector.demo@civicos.example.invalid
admin.demo@civicos.example.invalid
```

Never activate the `demo` profile or reuse its password in a production environment.

## Phase boundary

Phases 1 through 18 supply the deployable foundation, governed backend capabilities, REST/OpenAPI layer, and five role-specific workspaces. Phase 19 adds only controlled seed/demo data. Phase 20 is the next phase and owns end-to-end integration across the complete demonstration story.

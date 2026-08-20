# CivicOS — Final Codex Master Implementation Specification
## v2.0

**Purpose:** Single implementation contract for Codex.

**Project:** CivicOS  
**MVP domain:** Cross-agency road-cutting / digging coordination and lifecycle verification in Bengaluru.

---

# 1. IMPLEMENTATION MANDATE

Build a production-quality MVP of CivicOS as a modular municipal coordination platform.

The system must demonstrate:

```text
Citizen observation
→ case creation
→ intervention matching
→ cross-agency visibility
→ deterministic conflict detection
→ AI-assisted recommendation
→ human coordination decision
→ approval
→ SLA tracking
→ execution
→ evidence submission
→ field verification
→ citizen verification
→ closure
```

Do not reduce the product to a CRUD application.

The core value is:

```text
CROSS-PROJECT COORDINATION
+
CONFLICT PREVENTION
+
LIFECYCLE VERIFICATION
```

---

# 2. PRODUCT POSITIONING

CivicOS is not intended to replace existing municipal systems.

Bengaluru already has MARCS for road-cutting permissions and related workflow. Public BBMP documentation describes road-cutting request handling, verification, restoration cost, validity and completion inspection, and the public GBA/BBMP portal lists MARCS 3.0 and Road History 2.0.

Therefore:

```text
DO NOT BUILD:
"another road-cutting permission form"

BUILD:
"a coordination and lifecycle intelligence layer around
road-impacting interventions"
```

The MVP must make this distinction visible in the UI and demo.

---

# 3. TECHNOLOGY BASELINE

Use:

```text
Backend:
Java 21+
Spring Boot 3+
Spring Web
Spring Security
Spring Data JPA
Hibernate
PostgreSQL
PostGIS
Flyway

Frontend:
React
TypeScript
Vite
component-based architecture

API:
REST
OpenAPI

Infrastructure:
Docker Compose for local development

Testing:
JUnit
Spring Boot Test
Testcontainers where useful
frontend unit/E2E framework appropriate to stack

Build:
Maven
```

Do not introduce unnecessary infrastructure.

Do not add Kafka, Kubernetes, microservices or cloud orchestration unless required by a concrete implementation constraint.

The MVP should be a modular monolith.

---

# 4. ARCHITECTURE

Use:

```text
Browser
   ↓
Frontend
   ↓
REST API
   ↓
Application services
   ↓
Domain modules
   ↓
PostgreSQL/PostGIS
```

AI:

```text
Application
   ↓
AI Gateway
   ↓
Provider adapter
```

AI must never directly mutate authoritative workflow state.

---

# 5. MODULAR BACKEND

Recommended structure:

```text
com.civicos
├── common
├── auth
├── user
├── agency
├── road
├── casefile
├── intervention
├── dependency
├── conflict
├── coordination
├── approval
├── sla
├── escalation
├── evidence
├── inspection
├── verification
├── notification
├── audit
├── ai
└── admin
```

Each module should contain:

```text
controller
service
domain
repository
dto
mapper
validation
```

Avoid a giant service package.

---

# 6. CORE DOMAIN ENTITIES

Implement at minimum:

```text
User
Role
Permission
Agency
RoadSegment
CitizenObservation
Case
Intervention
Dependency
Conflict
CoordinationDecision
Approval
SLA
Escalation
Evidence
Inspection
Verification
Notification
AuditEvent
AIRun
AIRecommendation
```

---

# 7. ROAD SEGMENT

Required fields:

```text
id
externalReference
name
classification
surfaceType
length
geometry
active
createdAt
updatedAt
```

Use PostGIS geometry.

Spatial operations must be performed in the database where appropriate.

---

# 8. CITIZEN OBSERVATION

Fields:

```text
id
caseId
submittedBy
category
description
location
roadSegmentId
submittedAt
status
```

Optional:

```text
aiSuggestedCategory
aiConfidence
```

AI suggestions are advisory.

Citizen-provided information remains authoritative for the observation.

---

# 9. CASE

Fields:

```text
id
caseNumber
source
status
priority
roadSegmentId
createdAt
updatedAt
closedAt
```

A case represents the traceable problem/lifecycle context.

---

# 10. INTERVENTION

Fields:

```text
id
caseId
agencyId
type
description
roadSegmentId
geometry
plannedStart
plannedEnd
actualStart
actualEnd
status
priority
createdAt
updatedAt
```

Interventions represent actual planned/executed work.

---

# 11. DEPENDENCY

Fields:

```text
id
sourceInterventionId
targetInterventionId
type
required
status
reason
```

Examples:

```text
MUST_COMPLETE_BEFORE
MUST_VERIFY_BEFORE
RESTORATION_DEPENDS_ON
```

---

# 12. CONFLICT

Fields:

```text
id
roadSegmentId
type
severity
status
detectedAt
resolvedAt
explanation
```

Many-to-many relation:

```text
Conflict ↔ Intervention
```

Conflict detection must be deterministic.

---

# 13. COORDINATION DECISION

Fields:

```text
id
conflictId
coordinatorId
decisionType
decisionText
acceptedRecommendationId
createdAt
```

Decision types:

```text
ACCEPT
ACCEPT_WITH_MODIFICATION
REJECT
REQUEST_INFORMATION
```

---

# 14. APPROVAL

Fields:

```text
id
interventionId
actorId
status
decision
reason
createdAt
decidedAt
```

Statuses:

```text
PENDING
APPROVED
APPROVED_WITH_CONDITIONS
REJECTED
RETURNED
```

Rejection requires a reason.

---

# 15. SLA

Fields:

```text
id
targetType
targetId
slaType
startAt
deadline
status
pausedAt
completedAt
```

States:

```text
NORMAL
AT_RISK
BREACHED
PAUSED
COMPLETED
```

SLA calculations must be deterministic.

---

# 16. EVIDENCE

Fields:

```text
id
targetType
targetId
uploadedBy
type
fileReference
capturedAt
latitude
longitude
metadata
createdAt
```

Evidence provenance must be preserved.

---

# 17. INSPECTION

Fields:

```text
id
interventionId
inspectorId
status
result
startedAt
completedAt
notes
```

Results:

```text
PASSED
FAILED
CONDITIONAL
```

---

# 18. VERIFICATION

Fields:

```text
id
targetType
targetId
source
result
submittedBy
reason
createdAt
```

Sources:

```text
FIELD_INSPECTOR
CITIZEN
SYSTEM
```

---

# 19. AUDIT

Every consequential operation must generate an audit event.

Store:

```text
actor
action
entity
entityId
timestamp
before
after
reason
requestId
```

AI decisions must also be auditable.

---

# 20. LIFECYCLE

Case:

```text
OPEN
→ UNDER_REVIEW
→ IN_PROGRESS
→ PENDING_VERIFICATION
→ VERIFIED
→ CLOSED
```

Intervention:

```text
DRAFT
→ SUBMITTED
→ UNDER_REVIEW
→ COORDINATION_REQUIRED
→ APPROVED
→ SCHEDULED
→ IN_PROGRESS
→ COMPLETED_PENDING_VERIFICATION
→ VERIFIED
→ CLOSED
```

Correction:

```text
VERIFICATION_FAILED
→ CORRECTIVE_ACTION
→ IN_PROGRESS
→ COMPLETED_PENDING_VERIFICATION
```

Invalid transitions must return a clear domain error.

---

# 21. ACTOR RESPONSIBILITIES

Citizen:

```text
submit observation
upload evidence
track case
provide resolution feedback
```

Agency officer:

```text
manage assigned interventions
submit evidence
respond to coordination
request approval
```

Coordinator:

```text
review conflicts
coordinate agencies
accept/modify/reject recommendations
make coordination decisions
```

Inspector:

```text
inspect
capture evidence
pass/fail verification
```

Administrator:

```text
manage users
roles
agencies
rules
configuration
```

---

# 22. RBAC

Implement:

```text
CITIZEN
AGENCY_OFFICER
COORDINATOR
INSPECTOR
ADMIN
```

Permissions must be explicit.

Never rely only on frontend visibility.

Backend authorization is authoritative.

---

# 23. CONFLICT ENGINE

Implement a deterministic conflict engine.

Inputs:

```text
intervention
road geometry
time range
intervention type
existing interventions
dependencies
```

Detect at minimum:

```text
same-road overlap
spatial overlap
temporal overlap
unsafe sequencing
repeat-digging risk
restoration-before-excavation-completion
```

The engine must generate:

```text
conflict type
severity
affected interventions
reason
```

---

# 24. CONFLICT SEVERITY

Implement deterministic scoring/configuration:

```text
HIGH
MEDIUM
LOW
```

Do not let AI decide severity without deterministic validation.

---

# 25. RECOMMENDATION ENGINE

The recommendation pipeline:

```text
Conflict
→ collect facts
→ deterministic constraints
→ candidate sequences
→ optional AI explanation/ranking
→ recommendation
→ human decision
```

AI cannot directly approve or reschedule work.

---

# 26. AI GATEWAY

Implement:

```text
AiProvider
AiGateway
AiRequest
AiResponse
```

Provider-specific implementations must be isolated.

Tasks:

```text
classification
matching assistance
evidence analysis
conflict explanation
recommendation generation
case summarization
```

Use structured JSON outputs.

Validate all AI responses against schemas.

---

# 27. AI SAFETY

AI must:

```text
never directly modify authoritative records
never bypass RBAC
never approve work
never close a case
never change an SLA
never create an irreversible workflow transition
```

AI output is advisory.

Human/system rules remain authoritative.

---

# 28. AI FAILURE

If AI fails:

```text
timeout
invalid output
provider error
low confidence
```

the workflow continues using deterministic/manual paths.

Do not make AI a hard dependency for core lifecycle execution.

---

# 29. AI CONFIDENCE

Store:

```text
confidence
model
provider
promptVersion
schemaVersion
createdAt
```

Low-confidence output should require human review.

---

# 30. SLA ENGINE

Implement:

```text
SlaService
SlaDeadlineCalculator
SlaMonitor
EscalationService
```

The engine must:

```text
create SLA
calculate deadline
track status
mark AT_RISK
mark BREACHED
create escalation
complete SLA
```

Do not implement timers with in-memory state.

Persist SLA state.

Use scheduled jobs for monitoring.

---

# 31. APPROVAL ENGINE

Implement:

```text
ApprovalService
ApprovalPolicy
ApprovalDecision
```

The engine must verify:

```text
actor permission
current workflow state
required dependencies
conflict state
required evidence
approval conditions
```

Approval must be auditable.

---

# 32. EVIDENCE ENGINE

Implement:

```text
EvidenceService
EvidenceValidator
EvidenceMetadataService
```

Validate:

```text
file type
size
required evidence
target entity
uploader permission
```

Do not trust client-supplied metadata blindly.

---

# 33. VERIFICATION ENGINE

Verification must combine:

```text
required evidence
inspection result
workflow state
citizen feedback
```

AI may assist but cannot replace official verification.

---

# 34. NOTIFICATION ENGINE

Support:

```text
in-app notifications
```

Structure:

```text
type
recipient
title
message
target
readAt
createdAt
```

Design email/SMS/push adapters but do not make external messaging mandatory for MVP.

---

# 35. API

REST endpoints should follow:

```text
/api/v1
```

Examples:

```text
POST   /cases
GET    /cases/{id}
GET    /cases
POST   /observations
GET    /observations/{id}

GET    /roads
GET    /roads/{id}
GET    /roads/{id}/interventions

POST   /interventions
GET    /interventions/{id}
PATCH  /interventions/{id}

GET    /conflicts
GET    /conflicts/{id}
POST   /conflicts/{id}/decisions

POST   /approvals/{id}/approve
POST   /approvals/{id}/reject

GET    /slas
GET    /escalations

POST   /evidence
GET    /evidence/{id}

POST   /inspections
POST   /inspections/{id}/complete

POST   /verifications

GET    /notifications
PATCH  /notifications/{id}/read
```

Exact endpoint naming may be refined during implementation but must remain RESTful and consistent.

---

# 36. API RULES

Every endpoint must have:

```text
authentication
authorization
input validation
consistent error response
request correlation ID
audit where consequential
```

Use DTOs rather than exposing JPA entities directly.

---

# 37. ERROR MODEL

Use a consistent response:

```json
{
  "code": "WORKFLOW_INVALID_TRANSITION",
  "message": "Intervention cannot be approved from its current state.",
  "requestId": "..."
}
```

Do not leak stack traces.

---

# 38. DATABASE

Use PostgreSQL + PostGIS.

Required database concerns:

```text
foreign keys
unique constraints
indexes
check constraints
timestamps
optimistic locking where needed
spatial indexes
```

Use Flyway migrations.

Do not use automatic schema generation as the production schema mechanism.

---

# 39. IMPORTANT INDEXES

At minimum:

```text
road_segment geometry
intervention roadSegmentId
intervention plannedStart/plannedEnd
intervention status
conflict status
SLA deadline/status
notification recipient/readAt
audit entity/entityId
```

Use GiST indexes for PostGIS geometry.

---

# 40. FRONTEND

Build role-specific workspaces.

Citizen:

```text
Report
My Reports
Track
Notifications
```

Agency:

```text
Dashboard
Interventions
Approvals
Evidence
```

Coordinator:

```text
Command Center
Conflicts
Map
Coordination
SLA
```

Inspector:

```text
Inspections
Evidence
Verification
```

Admin:

```text
Users
Roles
Agencies
Configuration
Audit
```

---

# 41. UI RULE

Every operational screen should show:

```text
Current status
Current owner
Next action
Deadline
Available transitions
```

---

# 42. MAP

Use a map for:

```text
road segments
interventions
conflicts
citizen observations
work zones
```

Every map interaction must have a list/table equivalent.

Map must not be the only source of information.

---

# 43. CONFLICT UI

Display:

```text
conflict
severity
affected road
affected interventions
reason
timeline
AI recommendation
human decision
```

AI output must be visually labelled:

```text
AI-assisted recommendation
```

---

# 44. CITIZEN FLOW

Implement:

```text
Report Issue
→ Photo
→ Location
→ Category
→ Description
→ Review
→ Submit
→ Tracking
→ Resolution request
→ Citizen verification
```

Keep this flow simple.

---

# 45. INSPECTOR FLOW

Implement mobile-friendly:

```text
My Inspections
→ Open
→ Location
→ Checklist
→ Evidence
→ Pass/Fail
→ Submit
```

---

# 46. COORDINATOR COMMAND CENTER

Show:

```text
active conflicts
SLA at risk
pending approvals
verification backlog
upcoming interventions
```

Clicking metrics must open filtered operational queues.

---

# 47. DEMO DATA

Implement the deterministic synthetic scenario defined in:

```text
CivicOS Engineering Definition v1.8
Seed Data, Demo Dataset & End-to-End SIH Scenario
```

Primary road:

```text
R001
Outer Ring Road — Demo Segment
```

Primary interventions:

```text
INT-001 BESCOM
INT-002 BWSSB
INT-005 Telecom
INT-003 BBMP Restoration
```

Primary conflicts:

```text
C001
C002
C003
```

Primary case:

```text
CASE-001
```

---

# 48. DEMO STORY

The complete demo must support:

```text
Citizen report
→ matching
→ conflict detection
→ AI recommendation
→ coordinator decision
→ approvals
→ execution
→ evidence
→ inspection
→ citizen confirmation
→ closure
```

No database editing should be necessary to demonstrate this flow.

---

# 49. NEGATIVE DEMO DATA

Seed:

```text
no-conflict intervention
duplicate citizen reports
unauthorized suspected work
failed restoration
citizen disagreement
```

These exist to demonstrate workflow robustness.

---

# 50. DEMO DATA SAFETY

Clearly mark seed data as:

```text
DEMO / SYNTHETIC
```

Never imply that synthetic interventions are real government projects.

Never ship demo users with production credentials.

---

# 51. SCHEDULED JOBS

Implement jobs for:

```text
SLA monitoring
SLA escalation
notification processing
optional AI job processing
```

Jobs must be idempotent.

---

# 52. CONCURRENCY

Protect against:

```text
two officers approving simultaneously
duplicate workflow transitions
duplicate evidence processing
duplicate notifications
```

Use:

```text
optimistic locking
database constraints
idempotency keys where appropriate
```

---

# 53. SECURITY

Implement:

```text
JWT/session-based authentication appropriate to architecture
password hashing if local auth is used
RBAC
input validation
file upload validation
rate limiting for citizen endpoints where appropriate
CORS configuration
secure headers
audit logging
```

Do not store secrets in source control.

---

# 54. FILE STORAGE

Abstract storage:

```text
FileStorageService
```

Implement local storage for MVP.

Design adapter interface for:

```text
S3-compatible storage
```

Do not hard-code filesystem paths into domain logic.

---

# 55. OBSERVABILITY

Include:

```text
structured logs
request IDs
error logs
AI run logs
workflow transition logs
```

Useful metrics:

```text
conflicts detected
recommendations generated
approval duration
SLA breaches
verification failures
AI failures
```

---

# 56. CONFIGURATION

Externalize:

```text
database URL
JWT settings
AI provider
AI model
AI timeout
file storage
upload limits
SLA thresholds
feature flags
```

Use environment variables.

---

# 57. SECRETS

Never hard-code:

```text
API keys
passwords
JWT secrets
database passwords
cloud credentials
```

Provide:

```text
.env.example
```

with placeholders.

---

# 58. LOCAL DEVELOPMENT

Provide:

```text
docker-compose.yml
```

for:

```text
PostgreSQL
PostGIS
```

Frontend and backend can run locally or through Docker.

Document:

```text
Java version
Node version
Maven commands
npm commands
database startup
migration
seed
run
```

---

# 59. OPENAPI

Generate and maintain OpenAPI documentation.

Document:

```text
authentication
request schemas
response schemas
errors
pagination
filters
workflow transitions
```

---

# 60. PAGINATION

All potentially large collections must use:

```text
page
size
sort
```

or cursor-based pagination where justified.

Never return unbounded collections.

---

# 61. FILTERING

Support filters for:

```text
status
agency
road
date
priority
SLA
conflict
verification
```

---

# 62. FRONTEND SERVER STATE

Keep server state separate from local UI state.

Do not create a second authoritative copy of workflow state in the frontend.

Backend is authoritative.

---

# 63. ACCESSIBILITY

Target:

```text
WCAG 2.2 AA
```

At minimum:

```text
keyboard navigation
semantic controls
focus states
screen-reader labels
non-colour status communication
accessible forms
accessible tables
map alternatives
```

---

# 64. TESTING EXPECTATION

Although a separate testing specification is intentionally omitted, Codex must generate tests as part of implementation.

Minimum:

```text
domain unit tests
service tests
repository/integration tests
API tests
security/RBAC tests
workflow transition tests
conflict-engine tests
SLA tests
AI schema/fallback tests
frontend component tests
end-to-end happy path
end-to-end failure path
```

Testing is not optional.

---

# 65. ACCEPTANCE TEST — PRIMARY SCENARIO

Given:

```text
INT-001 BESCOM on R001
INT-002 BWSSB on R001
INT-005 Telecom on R001
INT-003 BBMP restoration on R001
```

When the interventions are scheduled with overlapping/unsafe sequencing:

Then:

```text
conflict is detected
```

And:

```text
coordinator sees affected interventions
```

And:

```text
AI recommendation can be generated
```

And:

```text
coordinator can accept/modify/reject it
```

And:

```text
approval workflow is created
```

And:

```text
SLA is tracked
```

And after execution:

```text
evidence is uploaded
inspection occurs
citizen can verify
case closes
```

---

# 66. ACCEPTANCE TEST — AI FAILURE

If AI provider is unavailable:

```text
conflict detection still works
workflow still works
approval still works
evidence still works
verification still works
```

The system must not become unusable.

---

# 67. ACCEPTANCE TEST — RBAC

Citizen must not be able to:

```text
approve intervention
resolve conflict
change SLA
verify official inspection
```

Agency officer must not be able to:

```text
change global rules
manage administrators
```

Inspector must not be able to:

```text
approve agency work unless explicitly permitted
```

Coordinator must only perform authorized coordination decisions.

---

# 68. ACCEPTANCE TEST — INVALID TRANSITION

Attempt:

```text
CLOSED → IN_PROGRESS
```

Expected:

```text
400/409 domain error
```

No database mutation.

---

# 69. ACCEPTANCE TEST — CONCURRENT APPROVAL

Two actors attempt the same approval.

Expected:

```text
only one authoritative transition succeeds
```

The other receives a conflict/state error.

---

# 70. ACCEPTANCE TEST — NO CONFLICT

For INT-004:

```text
no spatial/temporal conflict
```

Expected:

```text
NO_CONFLICT
```

No false conflict should be generated.

---

# 71. IMPLEMENTATION ORDER

Codex must implement in this order:

```text
PHASE 1
Repository + build + Docker + configuration

PHASE 2
Database + Flyway + PostGIS

PHASE 3
Domain entities + repositories

PHASE 4
Authentication + RBAC

PHASE 5
Core workflow/state machine

PHASE 6
Roads + interventions + dependencies

PHASE 7
Conflict detection engine

PHASE 8
Approval + SLA + escalation

PHASE 9
Evidence + inspection + verification

PHASE 10
Notifications + audit

PHASE 11
AI gateway + advisory capabilities

PHASE 12
REST API/OpenAPI

PHASE 13
Frontend foundation

PHASE 14
Citizen UI

PHASE 15
Agency UI

PHASE 16
Coordinator UI

PHASE 17
Inspector UI

PHASE 18
Admin UI

PHASE 19
Seed/demo data

PHASE 20
End-to-end integration

PHASE 21
Hardening and bug fixing
```

Do not jump randomly between phases.

---

# 72. CODING RULES

Use:

```text
clear names
small services
single responsibility
constructor injection
immutable DTOs where practical
transaction boundaries at application-service level
domain validation
central exception handling
structured logging
```

Avoid:

```text
god classes
static service state
business logic in controllers
business logic in React components
direct entity exposure
magic strings
hard-coded configuration
```

---

# 73. DATABASE RULES

Do not:

```text
drop production data
auto-reset databases unexpectedly
use destructive migrations
```

For local demo:

```text
reset capability may exist behind an explicit command
```

---

# 74. API VERSIONING

Start:

```text
/api/v1
```

Do not break existing v1 contracts casually.

---

# 75. DOCUMENTATION

Generate:

```text
README.md
ARCHITECTURE.md
SETUP.md
API.md
AI.md
DEMO.md
```

README must allow a new developer to start the project.

---

# 76. ENVIRONMENT FILE

Provide:

```text
.env.example
```

Example categories:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

JWT_SECRET

AI_PROVIDER
AI_MODEL
AI_API_KEY

FILE_STORAGE_PATH

CORS_ALLOWED_ORIGINS
```

No real credentials.

---

# 77. FEATURE FLAGS

Use feature flags for optional functionality:

```text
AI_ENABLED
CITIZEN_REPORTING_ENABLED
DEMO_MODE
```

Core deterministic workflow must work when:

```text
AI_ENABLED=false
```

---

# 78. DEMO MODE

When enabled:

```text
load synthetic data
show DEMO indicator
allow controlled reset
```

Do not expose demo reset functionality in production.

---

# 79. FINAL UI REQUIREMENT

The UI must demonstrate the product without requiring technical explanation.

A judge should be able to follow:

```text
Problem
→ Coordination risk
→ Detection
→ Recommendation
→ Decision
→ Execution
→ Proof
→ Verification
→ Closure
```

---

# 80. FINAL DIFFERENTIATION REQUIREMENT

The implementation must visibly demonstrate that CivicOS is not merely:

```text
complaint management
```

or:

```text
road-cutting permission management
```

or:

```text
AI chatbot
```

The flagship feature is:

```text
CROSS-AGENCY ROAD-WORK COORDINATION
```

with:

```text
spatial awareness
+
temporal awareness
+
dependency awareness
+
conflict detection
+
AI-assisted recommendation
+
human approval
+
lifecycle evidence
+
verification
```

---

# 81. DO NOT BUILD YET

Do not add:

```text
payments
ERP
procurement
contract management
full GIS platform
citizen social network
generic chatbot
native mobile applications
microservices
complex event streaming
```

unless explicitly required later.

The MVP must stay focused.

---

# 82. FUTURE SERVICES

The architecture should permit future intervention types:

```text
lake restoration
drainage
sewage
streetlights
footpaths
waste
parks
tree work
flood mitigation
```

But do not implement them as first-class workflows in the MVP unless needed for extensibility.

Road cutting/digging is the flagship domain.

---

# 83. DEFINITION OF DONE

Codex implementation is complete when:

```text
project builds
database migrates
application starts
authentication works
RBAC works
citizen can report
agency can manage intervention
conflict engine detects seeded conflict
coordinator can resolve conflict
AI recommendation works or gracefully falls back
approval works
SLA works
evidence works
inspection works
citizen verification works
case closes
audit trail exists
demo seed loads
primary end-to-end scenario works
tests pass
README setup works from clean environment
```

---

# 84. CODex EXECUTION BEHAVIOUR

Before implementing:

1. Inspect repository.
2. Identify existing files.
3. Do not overwrite working functionality unnecessarily.
4. Create/update architecture documents if needed.
5. Implement one phase at a time.
6. Run tests/build after each meaningful phase.
7. Fix compilation and test failures before continuing.
8. Keep changes incremental.
9. Do not invent external integrations.
10. Do not claim functionality that is only mocked.
11. Keep AI behind an interface.
12. Keep government data synthetic unless an actual integration is explicitly provided.

---

# 85. IF AN AMBIGUITY EXISTS

Prefer:

```text
existing engineering definitions
→ deterministic business rules
→ simplest implementation
```

Do not invent a new business process when an existing definition already answers the question.

If an ambiguity materially affects architecture or data integrity:

```text
document the assumption
choose the least risky implementation
continue
```

---

# 86. FINAL BUILD PRIORITY

Priority order:

```text
P0 — lifecycle correctness
P0 — conflict detection
P0 — RBAC
P0 — auditability
P0 — demo scenario

P1 — AI recommendation
P1 — rich map
P1 — advanced dashboards

P2 — integrations
P2 — advanced analytics
P2 — future service types
```

The system must remain valuable even if all AI features are disabled.

---

# 87. FINAL PRODUCT STATEMENT

CivicOS is:

> A municipal coordination platform that connects road-impacting interventions across agencies, detects spatial and temporal conflicts before they cause repeated disruption, assists coordinators with recommendations, and maintains an evidence-backed lifecycle through verification and closure.

That statement defines the MVP boundary.

---

# 88. FINAL INSTRUCTION TO CODEX

Build the system described in this specification.

Do not build a generic civic app.

Do not build a chatbot.

Do not build only a complaint portal.

Do not build only a permission system.

Build the **coordination engine and operational workflow around road-cutting interventions**, with citizen reporting as an input and lifecycle verification as the output.

The implementation must be modular enough to expand later, but the first complete vertical slice must be:

```text
Citizen
→ Road R001
→ Observation
→ Intervention matching
→ BESCOM + BWSSB + Telecom + BBMP
→ Conflict
→ AI recommendation
→ Coordinator
→ Approval
→ SLA
→ Execution
→ Evidence
→ Inspection
→ Citizen verification
→ Closure
```

This is the canonical CivicOS MVP.

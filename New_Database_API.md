# CivicOS — Engineering Definition
## Database Schema, PostgreSQL/PostGIS Model & API Specification

**Version:** 1.5  
**Status:** Engineering baseline  
**Scope:** SIH MVP — multi-agency road-cutting / digging coordination and lifecycle verification  
**Backend:** Java 21+ / Spring Boot  
**Database:** PostgreSQL + PostGIS  
**ORM:** Hibernate/JPA  
**Migrations:** Flyway  
**API:** REST `/api/v1`

---

# 1. Purpose

This document converts the existing:

- Domain Model
- System Architecture
- Workflow / State Machine
- Engine Architecture
- RBAC / Actor Matrix

into an implementable persistence and API contract.

The existing architecture establishes PostgreSQL + PostGIS, Hibernate/JPA, Flyway, REST, Spring Security, object storage for evidence, modular monolith architecture, and vertical-slice development. fileciteturn16file0L53-L77

The schema must support the central CivicOS requirement:

```text
Problem
→ Location
→ Intervention
→ Actors
→ Dependencies
→ Conflicts
→ Coordination
→ Approval
→ Execution
→ Evidence
→ Verification
→ Citizen validation
→ Closure
→ Audit
```

---

# 2. Database Design Principles

## DB-001 — PostgreSQL is authoritative

Operational CivicOS records live in PostgreSQL.

## DB-002 — PostGIS is authoritative for spatial relationships

Use PostGIS geometry types and spatial indexes.

## DB-003 — Evidence binaries are not stored in PostgreSQL

Store files in S3-compatible object storage.

PostgreSQL stores metadata and object references. This is already established in the project architecture. fileciteturn16file2L380-L397

## DB-004 — Audit records are append-only

Never update or delete historical audit events.

## DB-005 — State transitions are domain operations

Do not expose arbitrary status updates.

## DB-006 — Policies are data

SLA durations, approval chains, conflict thresholds and similar policies must be configurable.

## DB-007 — Foreign keys protect domain integrity

Do not rely only on application-level validation.

## DB-008 — Timestamps are timezone-aware

Use:

```sql
TIMESTAMPTZ
```

for business events.

## DB-009 — UUIDs

Use UUID primary keys for distributed-safe identifiers and opaque public identifiers.

## DB-010 — Soft deletion only where appropriate

Authoritative interventions, approvals, evidence, inspections and audit events should not be physically deleted.

---

# 3. PostgreSQL Extensions

Required:

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

Optional later:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Do not add extensions merely for future speculation.

---

# 4. Core Schema Namespaces

For the MVP, one PostgreSQL database is sufficient.

Logical grouping:

```text
identity
organisation
geospatial
case_management
intervention
coordination
workflow
approval
evidence
verification
sla
notification
audit
ai
configuration
```

These can initially remain PostgreSQL schemas only if there is a practical reason. A simpler MVP may use one schema with strong table naming/module boundaries.

Recommended MVP:

```text
public
```

with module-based table names.

---

# 5. Entity Inventory

## Identity

```text
users
roles
permissions
user_roles
role_permissions
agency_memberships
delegations
```

## Organisation

```text
organisations
organisation_types
jurisdictions
```

## Geospatial

```text
roads
road_segments
road_segment_junctions
```

## Civic lifecycle

```text
civic_cases
citizen_observations
assignments
tasks
```

## Intervention

```text
interventions
intervention_road_segments
dependencies
milestones
```

## Coordination

```text
conflicts
conflict_interventions
coordination_decisions
recommendations
```

## Approval

```text
approval_requests
approval_conditions
approval_decisions
```

## SLA

```text
sla_policies
sla_instances
sla_pause_periods
escalations
```

## Evidence

```text
evidence
evidence_versions
evidence_requirements
evidence_reviews
```

## Verification

```text
inspections
inspection_checks
verification_results
```

## Communication

```text
notifications
```

## Audit / events

```text
audit_events
domain_events
processed_events
```

## AI

```text
ai_runs
ai_recommendations
```

## Configuration

```text
intervention_types
road_classes
policy_rules
working_calendars
holidays
```

---

# 6. Users

## `users`

```text
id UUID PK
external_subject VARCHAR NULL
email VARCHAR UNIQUE
phone VARCHAR NULL
full_name VARCHAR
password_hash VARCHAR NULL
status VARCHAR
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
last_login_at TIMESTAMPTZ NULL
```

### Status

```text
ACTIVE
INVITED
SUSPENDED
DISABLED
```

Never store raw passwords.

---

# 7. Roles

## `roles`

```text
id UUID PK
code VARCHAR UNIQUE
name VARCHAR
description TEXT
system_role BOOLEAN
created_at TIMESTAMPTZ
```

Seed:

```text
CITIZEN
AGENCY_USER
ENGINEER
COORDINATOR
CONTRACTOR
INSPECTOR
APPROVER
ADMIN
SYSTEM
```

The role model is defined in the RBAC document.

---

# 8. Permissions

## `permissions`

```text
id UUID PK
code VARCHAR UNIQUE
description TEXT
```

Examples:

```text
INTERVENTION_CREATE
INTERVENTION_SUBMIT
CONFLICT_VIEW
CONFLICT_RESOLVE
APPROVAL_APPROVE
EVIDENCE_UPLOAD
VERIFICATION_PASS
ADMIN_OVERRIDE
```

---

# 9. Role Permissions

## `role_permissions`

```text
role_id UUID FK
permission_id UUID FK

PRIMARY KEY(role_id, permission_id)
```

---

# 10. User Roles

## `user_roles`

```text
user_id UUID FK
role_id UUID FK

PRIMARY KEY(user_id, role_id)
```

A user may have multiple roles.

Example:

```text
COORDINATOR
+
ENGINEER
```

---

# 11. Organisations

## `organisations`

```text
id UUID PK
code VARCHAR UNIQUE
name VARCHAR
organisation_type VARCHAR
status VARCHAR
parent_organisation_id UUID NULL FK
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

Examples:

```text
BBMP
BWSSB
BESCOM
TELECOM_OPERATOR
CONTRACTOR
OTHER_AUTHORITY
```

---

# 12. Agency Membership

## `agency_memberships`

```text
id UUID PK
user_id UUID FK
organisation_id UUID FK
jurisdiction_id UUID NULL FK
status VARCHAR
starts_at TIMESTAMPTZ
ends_at TIMESTAMPTZ NULL
```

This supports contextual authorization.

---

# 13. Jurisdictions

## `jurisdictions`

```text
id UUID PK
code VARCHAR UNIQUE
name VARCHAR
type VARCHAR
geometry GEOMETRY(MultiPolygon, 4326)
parent_id UUID NULL FK
```

Spatial index:

```sql
CREATE INDEX idx_jurisdictions_geometry
ON jurisdictions
USING GIST (geometry);
```

---

# 14. Roads

## `roads`

```text
id UUID PK
external_reference VARCHAR NULL
name VARCHAR
road_class_id UUID FK
geometry GEOMETRY(MultiLineString, 4326)
jurisdiction_id UUID NULL FK
status VARCHAR
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

---

# 15. Road Segments

## `road_segments`

```text
id UUID PK
road_id UUID FK
segment_code VARCHAR
geometry GEOMETRY(LineString, 4326)
start_point GEOMETRY(Point, 4326)
end_point GEOMETRY(Point, 4326)
length_meters NUMERIC
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

Unique:

```text
(road_id, segment_code)
```

Spatial index:

```sql
CREATE INDEX idx_road_segments_geometry
ON road_segments
USING GIST (geometry);
```

---

# 16. Civic Case

A case represents the overall tracked civic issue/intervention journey.

## `civic_cases`

```text
id UUID PK
case_number VARCHAR UNIQUE
title VARCHAR
description TEXT
category VARCHAR
priority VARCHAR
status VARCHAR
reported_by UUID NULL FK users
source VARCHAR
location GEOMETRY(Point, 4326)
jurisdiction_id UUID NULL FK
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
closed_at TIMESTAMPTZ NULL
```

### Source

```text
CITIZEN
AGENCY
SYSTEM
IMPORT
```

---

# 17. Citizen Observation

## `citizen_observations`

```text
id UUID PK
case_id UUID NULL FK
submitted_by UUID FK users
description TEXT
category VARCHAR
location GEOMETRY(Point, 4326)
observed_at TIMESTAMPTZ
status VARCHAR
matched_intervention_id UUID NULL
ai_match_confidence NUMERIC NULL
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

### Status

```text
SUBMITTED
TRIAGE
MATCHED
UNDER_REVIEW
ACTION_CREATED
RESOLVED
REJECTED
```

AI confidence is advisory.

---

# 18. Assignments

## `assignments`

```text
id UUID PK
case_id UUID NULL FK
intervention_id UUID NULL FK
task_id UUID NULL FK
assigned_to_user_id UUID NULL FK
assigned_to_org_id UUID NULL FK
assigned_by UUID FK
assignment_type VARCHAR
status VARCHAR
assigned_at TIMESTAMPTZ
due_at TIMESTAMPTZ NULL
completed_at TIMESTAMPTZ NULL
```

Exactly one relevant target should be enforced at application/domain level.

---

# 19. Tasks

## `tasks`

```text
id UUID PK
case_id UUID NULL FK
intervention_id UUID NULL FK
conflict_id UUID NULL FK
task_type VARCHAR
title VARCHAR
description TEXT
priority VARCHAR
status VARCHAR
assigned_to_user_id UUID NULL FK
assigned_to_org_id UUID NULL FK
created_by UUID NULL FK
created_at TIMESTAMPTZ
due_at TIMESTAMPTZ NULL
completed_at TIMESTAMPTZ NULL
```

### Status

```text
OPEN
ASSIGNED
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED
```

---

# 20. Intervention

## `interventions`

```text
id UUID PK
case_id UUID NULL FK
intervention_number VARCHAR UNIQUE
owning_organisation_id UUID FK
intervention_type_id UUID FK
title VARCHAR
description TEXT
geometry GEOMETRY(Geometry, 4326)
status VARCHAR
priority VARCHAR
planned_start_at TIMESTAMPTZ NULL
planned_end_at TIMESTAMPTZ NULL
actual_start_at TIMESTAMPTZ NULL
actual_end_at TIMESTAMPTZ NULL
contractor_org_id UUID NULL FK
created_by UUID FK
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
closed_at TIMESTAMPTZ NULL
```

Spatial index:

```sql
CREATE INDEX idx_interventions_geometry
ON interventions
USING GIST (geometry);
```

---

# 21. Intervention–Road Segment Relationship

## `intervention_road_segments`

```text
intervention_id UUID FK
road_segment_id UUID FK
overlap_ratio NUMERIC NULL
overlap_length_meters NUMERIC NULL

PRIMARY KEY(intervention_id, road_segment_id)
```

This makes the relationship:

```text
Intervention N ↔ M RoadSegment
```

explicit.

---

# 22. Intervention Types

## `intervention_types`

```text
id UUID PK
code VARCHAR UNIQUE
name VARCHAR
description TEXT
requires_approval BOOLEAN
requires_inspection BOOLEAN
requires_restoration_evidence BOOLEAN
active BOOLEAN
```

Examples:

```text
UTILITY_EXCAVATION
ROAD_CUTTING
ROAD_RESTORATION
RESURFACING
DRAINAGE_WORK
TELECOM_INSTALLATION
```

---

# 23. Dependencies

## `dependencies`

```text
id UUID PK
source_intervention_id UUID FK
target_intervention_id UUID FK
dependency_type VARCHAR
status VARCHAR
created_by UUID FK
created_at TIMESTAMPTZ
```

### Dependency types

```text
PRECEDES
FOLLOWS
BLOCKS
REQUIRES
SHOULD_PRECEDE
COORDINATE_WITH
```

Constraint:

```text
source_intervention_id != target_intervention_id
```

Hard dependency cycle detection remains a domain service.

---

# 24. Milestones

## `milestones`

```text
id UUID PK
intervention_id UUID FK
code VARCHAR
name VARCHAR
sequence_number INTEGER
status VARCHAR
planned_at TIMESTAMPTZ NULL
completed_at TIMESTAMPTZ NULL
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

Examples:

```text
PRE_WORK
WORK_STARTED
WORK_COMPLETED
RESTORATION_STARTED
RESTORATION_COMPLETED
FINAL_INSPECTION
```

---

# 25. Conflicts

## `conflicts`

```text
id UUID PK
conflict_number VARCHAR UNIQUE
conflict_type VARCHAR
severity VARCHAR
status VARCHAR
description TEXT
detected_at TIMESTAMPTZ
resolved_at TIMESTAMPTZ NULL
resolved_by UUID NULL FK
resolution_notes TEXT NULL
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

### Conflict types

```text
SPATIAL
TEMPORAL
SPATIAL_TEMPORAL
DEPENDENCY
RESTORATION
REPEAT_EXCAVATION
DUPLICATE
AGENCY_COORDINATION
```

### Status

```text
OPEN
UNDER_REVIEW
COORDINATION
RESOLVED
ACCEPTED_EXCEPTION
DISMISSED
```

---

# 26. Conflict Interventions

## `conflict_interventions`

```text
conflict_id UUID FK
intervention_id UUID FK
relationship_type VARCHAR

PRIMARY KEY(conflict_id, intervention_id)
```

---

# 27. Recommendations

## `recommendations`

```text
id UUID PK
conflict_id UUID NULL FK
intervention_id UUID NULL FK
recommendation_type VARCHAR
title VARCHAR
description TEXT
recommended_sequence JSONB
assumptions JSONB
expected_benefit JSONB
confidence NUMERIC NULL
generated_by VARCHAR
ai_run_id UUID NULL FK
status VARCHAR
created_at TIMESTAMPTZ
```

Important:

```text
recommendation.status != authoritative approval
```

---

# 28. Coordination Decisions

## `coordination_decisions`

```text
id UUID PK
conflict_id UUID FK
decision_type VARCHAR
decision_text TEXT
decided_by UUID FK
decided_at TIMESTAMPTZ
supporting_recommendation_id UUID NULL FK
```

Examples:

```text
ACCEPT_SEQUENCE
CHANGE_SCHEDULE
COORDINATE_WORK
ESCALATE
ACCEPT_EXCEPTION
```

---

# 29. Approval Requests

## `approval_requests`

```text
id UUID PK
intervention_id UUID FK
approval_type VARCHAR
status VARCHAR
requested_by UUID FK
requested_at TIMESTAMPTZ
due_at TIMESTAMPTZ NULL
completed_at TIMESTAMPTZ NULL
```

### Status

```text
PENDING
APPROVED
REJECTED
RETURNED
CONDITIONAL
CANCELLED
```

---

# 30. Approval Conditions

## `approval_conditions`

```text
id UUID PK
approval_request_id UUID FK
condition_code VARCHAR
description TEXT
required BOOLEAN
status VARCHAR
satisfied_at TIMESTAMPTZ NULL
satisfied_by UUID NULL FK
```

An approval cannot become final when required conditions remain unsatisfied.

---

# 31. Approval Decisions

## `approval_decisions`

```text
id UUID PK
approval_request_id UUID FK
decision VARCHAR
decided_by UUID FK
reason TEXT
decided_at TIMESTAMPTZ
```

### Decision

```text
APPROVE
REJECT
RETURN
APPROVE_WITH_CONDITIONS
```

Approval decisions are immutable.

---

# 32. SLA Policies

## `sla_policies`

```text
id UUID PK
code VARCHAR UNIQUE
name VARCHAR
sla_type VARCHAR
duration_minutes INTEGER
business_calendar_id UUID NULL
priority VARCHAR NULL
severity VARCHAR NULL
active BOOLEAN
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

---

# 33. SLA Instances

## `sla_instances`

```text
id UUID PK
policy_id UUID FK
entity_type VARCHAR
entity_id UUID
status VARCHAR
started_at TIMESTAMPTZ
due_at TIMESTAMPTZ
completed_at TIMESTAMPTZ NULL
breached_at TIMESTAMPTZ NULL
escalation_level INTEGER DEFAULT 0
```

### Status

```text
ACTIVE
PAUSED
COMPLETED
BREACHED
CANCELLED
```

---

# 34. SLA Pause Periods

## `sla_pause_periods`

```text
id UUID PK
sla_instance_id UUID FK
started_at TIMESTAMPTZ
ended_at TIMESTAMPTZ NULL
reason TEXT
created_by UUID FK
```

---

# 35. Escalations

## `escalations`

```text
id UUID PK
sla_instance_id UUID NULL FK
task_id UUID NULL FK
level INTEGER
escalated_to_user_id UUID NULL FK
escalated_to_org_id UUID NULL FK
reason TEXT
created_at TIMESTAMPTZ
resolved_at TIMESTAMPTZ NULL
```

---

# 36. Evidence

## `evidence`

```text
id UUID PK
intervention_id UUID NULL FK
case_id UUID NULL FK
milestone_id UUID NULL FK
inspection_id UUID NULL FK
observation_id UUID NULL FK
evidence_type VARCHAR
storage_key VARCHAR
original_filename VARCHAR
mime_type VARCHAR
file_size_bytes BIGINT
checksum VARCHAR
captured_at TIMESTAMPTZ NULL
captured_location GEOMETRY(Point, 4326) NULL
submitted_by UUID FK
status VARCHAR
created_at TIMESTAMPTZ
```

The actual binary stays in object storage.

---

# 37. Evidence Versions

## `evidence_versions`

```text
id UUID PK
evidence_id UUID FK
version_number INTEGER
storage_key VARCHAR
checksum VARCHAR
uploaded_by UUID FK
created_at TIMESTAMPTZ
```

No silent replacement of evidence.

---

# 38. Evidence Requirements

## `evidence_requirements`

```text
id UUID PK
intervention_type_id UUID FK
milestone_code VARCHAR
evidence_type VARCHAR
required BOOLEAN
description TEXT
```

This enables policy-driven evidence requirements.

---

# 39. Evidence Reviews

## `evidence_reviews`

```text
id UUID PK
evidence_id UUID FK
reviewed_by UUID FK
decision VARCHAR
notes TEXT
reviewed_at TIMESTAMPTZ
```

Decision:

```text
ACCEPT
REJECT
REQUEST_RESUBMISSION
```

---

# 40. Inspections

## `inspections`

```text
id UUID PK
intervention_id UUID FK
assigned_to UUID FK
inspection_type VARCHAR
scheduled_at TIMESTAMPTZ NULL
started_at TIMESTAMPTZ NULL
completed_at TIMESTAMPTZ NULL
status VARCHAR
result VARCHAR NULL
notes TEXT NULL
created_at TIMESTAMPTZ
```

---

# 41. Inspection Checks

## `inspection_checks`

```text
id UUID PK
inspection_id UUID FK
check_code VARCHAR
description TEXT
result VARCHAR
notes TEXT NULL
```

Result:

```text
PASS
FAIL
NOT_APPLICABLE
```

---

# 42. Verification Results

## `verification_results`

```text
id UUID PK
inspection_id UUID FK
status VARCHAR
confidence_score NUMERIC NULL
reviewed_by UUID FK
reviewed_at TIMESTAMPTZ
notes TEXT
```

Status:

```text
PASS
FAIL
CONDITIONAL
REQUIRES_REINSPECTION
```

---

# 43. Notifications

## `notifications`

```text
id UUID PK
recipient_user_id UUID FK
notification_type VARCHAR
title VARCHAR
message TEXT
entity_type VARCHAR NULL
entity_id UUID NULL
channel VARCHAR
status VARCHAR
created_at TIMESTAMPTZ
sent_at TIMESTAMPTZ NULL
read_at TIMESTAMPTZ NULL
```

MVP channels:

```text
IN_APP
EMAIL
```

The existing context explicitly identifies these as sufficient for MVP. fileciteturn16file3L461-L484

---

# 44. Audit Events

## `audit_events`

```text
id UUID PK
event_id UUID UNIQUE
actor_user_id UUID NULL FK
actor_type VARCHAR
organisation_id UUID NULL FK
action VARCHAR
entity_type VARCHAR
entity_id UUID
previous_state VARCHAR NULL
new_state VARCHAR NULL
reason TEXT NULL
metadata JSONB NULL
correlation_id UUID NULL
occurred_at TIMESTAMPTZ
```

Indexes:

```sql
CREATE INDEX idx_audit_entity
ON audit_events(entity_type, entity_id);

CREATE INDEX idx_audit_actor
ON audit_events(actor_user_id);

CREATE INDEX idx_audit_occurred_at
ON audit_events(occurred_at);
```

---

# 45. Domain Events

## `domain_events`

```text
id UUID PK
event_id UUID UNIQUE
event_type VARCHAR
aggregate_type VARCHAR
aggregate_id UUID
payload JSONB
occurred_at TIMESTAMPTZ
published_at TIMESTAMPTZ NULL
```

Use this for internal asynchronous processing.

---

# 46. Processed Events

## `processed_events`

```text
event_id UUID PK
consumer VARCHAR
processed_at TIMESTAMPTZ
```

This provides idempotency for consumers.

---

# 47. AI Runs

## `ai_runs`

```text
id UUID PK
provider VARCHAR
model VARCHAR
operation VARCHAR
input_reference JSONB
output JSONB
confidence NUMERIC NULL
status VARCHAR
error_message TEXT NULL
created_at TIMESTAMPTZ
completed_at TIMESTAMPTZ NULL
```

AI output is advisory and auditable.

---

# 48. AI Recommendations

## `ai_recommendations`

```text
id UUID PK
ai_run_id UUID FK
entity_type VARCHAR
entity_id UUID
recommendation_type VARCHAR
recommendation JSONB
confidence NUMERIC NULL
accepted_by UUID NULL FK
accepted_at TIMESTAMPTZ NULL
created_at TIMESTAMPTZ
```

The recommendation does not itself modify authoritative workflow.

---

# 49. Policy Rules

## `policy_rules`

```text
id UUID PK
code VARCHAR UNIQUE
rule_type VARCHAR
scope JSONB
configuration JSONB
version INTEGER
active BOOLEAN
effective_from TIMESTAMPTZ
effective_until TIMESTAMPTZ NULL
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

Use for:

```text
approval rules
SLA rules
conflict thresholds
evidence requirements
escalation rules
```

---

# 50. Working Calendars

## `working_calendars`

```text
id UUID PK
code VARCHAR UNIQUE
name VARCHAR
timezone VARCHAR
working_days JSONB
working_hours JSONB
```

---

# 51. Holidays

## `holidays`

```text
id UUID PK
calendar_id UUID FK
holiday_date DATE
name VARCHAR
```

---

# 52. Important Database Constraints

## Intervention dates

```text
planned_end_at >= planned_start_at
```

## Actual dates

```text
actual_end_at >= actual_start_at
```

## Dependency

```text
source != target
```

## Approval

Only one active final approval request of a given type should exist unless explicitly configured.

## Evidence

Checksum must exist for accepted evidence.

## Inspection

A completed inspection requires a result.

## Verification

A verification result must reference an inspection.

---

# 53. Spatial Indexes

Required:

```text
roads.geometry
road_segments.geometry
interventions.geometry
jurisdictions.geometry
evidence.captured_location
citizen_observations.location
```

Use:

```sql
USING GIST
```

---

# 54. Temporal Indexes

Useful indexes:

```text
interventions(planned_start_at, planned_end_at)
tasks(due_at, status)
sla_instances(due_at, status)
audit_events(occurred_at)
```

For conflict candidate queries:

```text
interventions(status, planned_start_at, planned_end_at)
```

combined with spatial index where appropriate.

---

# 55. Unique Constraints

Recommended:

```text
users.email
roles.code
permissions.code
organisations.code
roads.external_reference
road_segments(road_id, segment_code)
interventions.intervention_number
conflicts.conflict_number
approval requests where configured
sla_policies.code
policy_rules.code
domain_events.event_id
audit_events.event_id
```

---

# 56. API Design Principles

Base URL:

```text
/api/v1
```

The system architecture explicitly specifies REST and this base path. fileciteturn16file7L1402-L1430

Use:

```text
resource endpoints
+
command endpoints
```

for domain operations.

Do not expose arbitrary state mutation.

Bad:

```http
PATCH /interventions/123
{
  "status": "APPROVED"
}
```

Good:

```http
POST /interventions/123/approval-requests
```

then:

```http
POST /approvals/{approvalId}/decision
```

---

# 57. Authentication APIs

```http
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

Production authentication mechanism may evolve, but backend authorization must remain server-controlled.

---

# 58. User APIs

```http
GET    /api/v1/users/me
GET    /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users
PATCH  /api/v1/users/{id}
```

Admin-only operations must be protected.

---

# 59. Organisation APIs

```http
GET  /api/v1/organisations
GET  /api/v1/organisations/{id}
POST /api/v1/organisations
PATCH /api/v1/organisations/{id}
```

---

# 60. Road APIs

```http
GET /api/v1/roads
GET /api/v1/roads/{id}
GET /api/v1/roads/{id}/segments
GET /api/v1/road-segments/{id}
GET /api/v1/road-segments/{id}/interventions
```

Spatial query:

```http
GET /api/v1/roads/nearby?lat=...&lng=...&radiusMeters=...
```

---

# 61. Civic Case APIs

```http
GET  /api/v1/cases
POST /api/v1/cases
GET  /api/v1/cases/{id}
PATCH /api/v1/cases/{id}
POST /api/v1/cases/{id}/assignments
GET  /api/v1/cases/{id}/timeline
```

---

# 62. Citizen Observation APIs

```http
POST /api/v1/observations
GET  /api/v1/observations/{id}
GET  /api/v1/me/observations
POST /api/v1/observations/{id}/evidence
POST /api/v1/observations/{id}/validation
```

---

# 63. Intervention APIs

```http
GET  /api/v1/interventions
POST /api/v1/interventions
GET  /api/v1/interventions/{id}
PATCH /api/v1/interventions/{id}
```

Commands:

```http
POST /api/v1/interventions/{id}/submit
POST /api/v1/interventions/{id}/schedule
POST /api/v1/interventions/{id}/start
POST /api/v1/interventions/{id}/complete
POST /api/v1/interventions/{id}/hold
POST /api/v1/interventions/{id}/resume
POST /api/v1/interventions/{id}/cancel
POST /api/v1/interventions/{id}/reopen
```

Each command invokes domain/workflow validation.

---

# 64. Conflict APIs

```http
GET  /api/v1/conflicts
GET  /api/v1/conflicts/{id}
POST /api/v1/conflicts/{id}/acknowledge
POST /api/v1/conflicts/{id}/assign
POST /api/v1/conflicts/{id}/resolve
POST /api/v1/conflicts/{id}/escalate
POST /api/v1/conflicts/{id}/exception
```

System detection endpoint may be internal:

```http
POST /api/v1/internal/conflict-analysis/interventions/{id}
```

Do not expose dangerous internal endpoints publicly.

---

# 65. Recommendation APIs

```http
GET /api/v1/conflicts/{id}/recommendations
POST /api/v1/conflicts/{id}/recommendations/generate
GET /api/v1/recommendations/{id}
POST /api/v1/recommendations/{id}/accept
POST /api/v1/recommendations/{id}/reject
```

Accepting a recommendation does not itself equal approval of work.

---

# 66. Coordination APIs

```http
GET  /api/v1/coordination/tasks
POST /api/v1/coordination/tasks
GET  /api/v1/coordination/tasks/{id}
POST /api/v1/coordination/tasks/{id}/assign
POST /api/v1/coordination/tasks/{id}/complete
POST /api/v1/coordination/tasks/{id}/escalate
```

---

# 67. Dependency APIs

```http
GET  /api/v1/interventions/{id}/dependencies
POST /api/v1/interventions/{id}/dependencies
DELETE /api/v1/dependencies/{id}
```

Deletion should be replaced by an auditable cancellation if the dependency has affected decisions.

---

# 68. Approval APIs

```http
GET  /api/v1/interventions/{id}/approval-requests
POST /api/v1/interventions/{id}/approval-requests
GET  /api/v1/approval-requests/{id}
POST /api/v1/approval-requests/{id}/decision
```

Decision request:

```json
{
  "decision": "APPROVE",
  "reason": "All required conditions satisfied."
}
```

---

# 69. SLA APIs

```http
GET /api/v1/slas
GET /api/v1/slas/{id}
POST /api/v1/slas/{id}/pause
POST /api/v1/slas/{id}/resume
POST /api/v1/slas/{id}/escalate
```

Policy management:

```http
GET   /api/v1/admin/sla-policies
POST  /api/v1/admin/sla-policies
PATCH /api/v1/admin/sla-policies/{id}
```

---

# 70. Task APIs

```http
GET  /api/v1/tasks
GET  /api/v1/tasks/{id}
POST /api/v1/tasks/{id}/assign
POST /api/v1/tasks/{id}/start
POST /api/v1/tasks/{id}/complete
POST /api/v1/tasks/{id}/block
```

---

# 71. Evidence APIs

Preferred upload architecture:

```text
Request upload
    ↓
Receive object-storage upload URL
    ↓
Client uploads file
    ↓
Confirm upload
    ↓
Create evidence record
```

Endpoints:

```http
POST /api/v1/evidence/upload-init
POST /api/v1/evidence/{id}/confirm
GET  /api/v1/evidence/{id}
POST /api/v1/evidence/{id}/review
```

Do not stream large files through the main API unnecessarily.

---

# 72. Inspection APIs

```http
POST /api/v1/interventions/{id}/inspections
GET  /api/v1/inspections/{id}
POST /api/v1/inspections/{id}/start
POST /api/v1/inspections/{id}/complete
POST /api/v1/inspections/{id}/reinspect
```

---

# 73. Verification APIs

```http
GET  /api/v1/interventions/{id}/verification
POST /api/v1/inspections/{id}/verification
```

Verification result:

```json
{
  "status": "PASS",
  "notes": "Restoration matches approved scope."
}
```

---

# 74. Notification APIs

```http
GET  /api/v1/notifications
POST /api/v1/notifications/{id}/read
POST /api/v1/notifications/read-all
```

---

# 75. Audit APIs

```http
GET /api/v1/audit/entities/{entityType}/{entityId}
GET /api/v1/audit/events/{eventId}
```

Access must be restricted by authorization scope.

---

# 76. AI APIs

AI should be asynchronous where processing may be slow.

Example:

```http
POST /api/v1/ai/observations/{id}/classify
POST /api/v1/ai/evidence/{id}/analyse
POST /api/v1/ai/conflicts/{id}/explain
POST /api/v1/ai/conflicts/{id}/recommend
```

Response:

```json
{
  "jobId": "...",
  "status": "QUEUED"
}
```

The core workflow must continue if the AI provider fails.

The project context explicitly requires AI failure not to become a system failure. fileciteturn16file4L828-L843

---

# 77. Standard API Response

Successful single resource:

```json
{
  "data": {},
  "meta": {
    "requestId": "..."
  }
}
```

Collection:

```json
{
  "data": [],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 100
  },
  "meta": {
    "requestId": "..."
  }
}
```

---

# 78. Standard Error Response

```json
{
  "error": {
    "code": "WORKFLOW_ACTION_NOT_ALLOWED",
    "message": "The intervention cannot be approved in its current state.",
    "details": [],
    "requestId": "..."
  }
}
```

Do not expose stack traces.

---

# 79. HTTP Status Rules

```text
200 OK
201 Created
202 Accepted
204 No Content

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests

500 Internal Server Error
503 Service Unavailable
```

Use `409` for domain concurrency/conflict situations where appropriate.

---

# 80. Pagination

Default:

```text
page=0
size=20
```

Maximum:

```text
size=100
```

Never permit unbounded collection queries.

---

# 81. Filtering

Examples:

```http
GET /api/v1/interventions?status=APPROVED
GET /api/v1/interventions?agencyId=...
GET /api/v1/conflicts?severity=HIGH
GET /api/v1/tasks?status=OPEN
```

---

# 82. Geospatial API Rules

Coordinates:

```text
WGS84 / EPSG:4326
```

Input:

```text
latitude
longitude
```

Internal spatial operations use PostGIS.

Never perform production spatial conflict detection using Java floating-point distance calculations when PostGIS can perform the operation correctly.

---

# 83. Concurrency

Optimistic locking should be used on mutable aggregate roots.

JPA:

```java
@Version
private Long version;
```

Relevant entities:

```text
CivicCase
Intervention
Conflict
ApprovalRequest
Task
```

If two officers edit the same intervention:

```text
first commit succeeds
second receives conflict
```

Do not silently overwrite.

---

# 84. Transaction Boundaries

Example:

```text
approveIntervention()
    ↓
validate authorization
    ↓
validate workflow
    ↓
validate conditions
    ↓
create approval decision
    ↓
transition intervention
    ↓
write audit event
    ↓
commit
```

After commit:

```text
publish domain event
```

Notifications and AI processing should not block the authoritative transaction.

---

# 85. Outbox Pattern

For important domain events:

```text
Transaction
 ├── domain change
 ├── audit event
 └── outbox/domain event
       ↓
commit
       ↓
event processor
       ↓
notifications / analytics / AI
```

This prevents losing events after a successful database transaction.

---

# 86. REST Security

Every protected endpoint requires:

```text
authentication
+
permission
+
resource scope
+
workflow authorization
```

The frontend must never supply trusted:

```text
role
agency
permissions
```

The backend resolves them from authenticated identity and database state.

---

# 87. API Idempotency

Use idempotency keys for operations that may be retried.

Especially:

```text
create intervention
submit evidence
request approval
approve
schedule
start
complete
```

Example:

```http
Idempotency-Key: 7e2...
```

Repeated requests must not create duplicate authoritative records.

---

# 88. API Versioning

All public APIs begin with:

```text
/api/v1
```

Breaking changes require a new version.

---

# 89. Flyway Migration Structure

```text
src/main/resources/db/migration/
├── V1__extensions.sql
├── V2__identity.sql
├── V3__organisations.sql
├── V4__geospatial.sql
├── V5__cases.sql
├── V6__interventions.sql
├── V7__dependencies.sql
├── V8__conflicts.sql
├── V9__approvals.sql
├── V10__sla.sql
├── V11__evidence.sql
├── V12__verification.sql
├── V13__audit_events.sql
├── V14__ai.sql
├── V15__policies.sql
└── V16__seed_reference_data.sql
```

Do not modify an already-applied migration in development once it is part of the shared baseline.

Create a new migration.

---

# 90. JPA Rules

Use JPA for:

```text
CRUD
aggregate persistence
relationships
transactions
```

Use native SQL/PostGIS queries for:

```text
spatial intersection
buffer searches
complex conflict candidate detection
geospatial aggregation
```

Do not force complex spatial logic into inefficient object traversal.

---

# 91. Repository Boundaries

Repositories belong to their domain modules.

Example:

```text
InterventionRepository
ConflictRepository
ApprovalRequestRepository
EvidenceRepository
InspectionRepository
```

Do not create a generic:

```text
GenericRepository
```

for all domain behaviour.

---

# 92. DTO Rules

Never expose JPA entities directly as public API contracts.

Use:

```text
Request DTO
Application Command
Domain Entity
Response DTO
```

Example:

```text
CreateInterventionRequest
       ↓
CreateInterventionCommand
       ↓
Intervention
       ↓
InterventionResponse
```

---

# 93. Database-to-Domain Mapping Rule

Database structure should support the domain, not dictate it.

Example:

```text
intervention status
```

is not simply an arbitrary database string.

It represents a domain state governed by the Workflow Engine.

---

# 94. Required Database Tests

Test:

```text
foreign keys
unique constraints
spatial indexes/queries
date constraints
audit immutability
idempotency
optimistic locking
cascade behaviour
```

---

# 95. Required API Tests

For each major command test:

```text
happy path
unauthorised actor
wrong agency
wrong state
missing prerequisite
blocking conflict
SOD violation
duplicate request
concurrent request
```

---

# 96. PostGIS Conflict Query Example

Conceptually:

```sql
SELECT i2.id
FROM interventions i1
JOIN interventions i2
  ON i1.id <> i2.id
WHERE ST_Intersects(i1.geometry, i2.geometry)
  AND i1.planned_start_at <= i2.planned_end_at
  AND i2.planned_start_at <= i1.planned_end_at;
```

Production implementation should add:

- status filtering
- configured buffers
- intervention type rules
- jurisdiction
- temporal tolerances
- indexes
- exclusion of cancelled/completed records where appropriate

---

# 97. Core ERD

```text
USER
 │
 ├── USER_ROLE ── ROLE ── ROLE_PERMISSION ── PERMISSION
 │
 └── AGENCY_MEMBERSHIP ── ORGANISATION
                              │
                              ▼
                         INTERVENTION
                              │
              ┌───────────────┼────────────────┐
              ▼               ▼                ▼
       ROAD_SEGMENTS      CONFLICTS       APPROVAL_REQUEST
              │               │                │
              ▼               ▼                ▼
           ROADS        RECOMMENDATION    APPROVAL_DECISION
                              │
                              ▼
                         COORDINATION
                              │
                              ▼
                            TASK
                              │
                              ▼
                            SLA
                              │
                              ▼
                        INTERVENTION
                              │
          ┌───────────────────┼────────────────────┐
          ▼                   ▼                    ▼
      MILESTONE            EVIDENCE             INSPECTION
          │                   │                    │
          │                   ▼                    ▼
          │             EVIDENCE_REVIEW      VERIFICATION
          │
          ▼
    CITIZEN_OBSERVATION
                              │
                              ▼
                         AUDIT_EVENT
```

---

# 98. End-to-End Database Lifecycle

## Step 1 — Citizen report

Creates:

```text
users
civic_cases
citizen_observations
evidence
audit_events
```

## Step 2 — Agency triage

Creates:

```text
assignment
task
sla_instance
audit_event
```

## Step 3 — Intervention

Creates:

```text
intervention
intervention_road_segments
dependencies
milestones
```

## Step 4 — Conflict analysis

Creates:

```text
conflict
conflict_interventions
recommendation
```

## Step 5 — Coordination

Creates:

```text
coordination_decision
tasks
audit_events
```

## Step 6 — Approval

Creates:

```text
approval_request
approval_conditions
approval_decision
```

## Step 7 — Execution

Updates:

```text
intervention
milestones
evidence
tasks
```

## Step 8 — Verification

Creates:

```text
inspection
inspection_checks
verification_result
```

## Step 9 — Citizen validation

Creates:

```text
citizen_observation / validation
```

## Step 10 — Closure

Updates:

```text
intervention
civic_case
```

and records:

```text
audit_event
```

---

# 99. API → Engine Mapping

| API Domain | Main Engine |
|---|---|
| `/cases` | Case / Workflow |
| `/observations` | Citizen Observation |
| `/roads` | Geospatial |
| `/interventions` | Intervention / Workflow |
| `/conflicts` | Conflict Detection |
| `/recommendations` | Recommendation |
| `/coordination` | Coordination |
| `/approvals` | Approval |
| `/tasks` | Task |
| `/slas` | SLA |
| `/evidence` | Evidence |
| `/inspections` | Verification |
| `/notifications` | Notification |
| `/audit` | Audit |
| `/ai` | AI Assistance |

---

# 100. MVP API Surface

Do not implement every endpoint immediately.

Minimum SIH vertical slice:

```text
POST /auth/login

POST /observations
POST /evidence/upload-init
POST /evidence/{id}/confirm

GET /cases/{id}
POST /cases/{id}/assignments

POST /interventions
POST /interventions/{id}/submit

GET /conflicts
GET /conflicts/{id}
POST /conflicts/{id}/resolve

POST /interventions/{id}/approval-requests
POST /approval-requests/{id}/decision

POST /interventions/{id}/start
POST /interventions/{id}/complete

POST /interventions/{id}/inspections
POST /inspections/{id}/complete

POST /inspections/{id}/verification

POST /observations/{id}/validation

GET /cases/{id}/timeline
```

This aligns with the architecture's recommended vertical-slice approach: database → backend → authorization → API → UI → tests → audit, rather than building every layer independently. fileciteturn16file9L1596-L1621

---

# 101. Implementation Order

## Vertical Slice 1

```text
users
organisations
roles
permissions
civic_cases
citizen_observations
evidence
audit_events
```

Then:

```text
POST /observations
```

## Vertical Slice 2

```text
assignments
tasks
workflow
notifications
sla_instances
```

## Vertical Slice 3

```text
roads
road_segments
interventions
dependencies
```

## Vertical Slice 4

```text
conflicts
recommendations
coordination
```

## Vertical Slice 5

```text
approvals
approval_conditions
approval_decisions
```

## Vertical Slice 6

```text
milestones
execution evidence
```

## Vertical Slice 7

```text
inspections
verification
```

## Vertical Slice 8

```text
citizen validation
reopening
```

## Vertical Slice 9

```text
AI assistance
```

---

# 102. Important Architecture Boundary

The existing project has deliberately moved away from a lake-specific core.

Therefore:

```text
RoadSegment
Intervention
Conflict
Dependency
Approval
Evidence
Verification
```

belong to the reusable civic core.

A future lake module, if added, must extend the core rather than pollute it with lake-specific assumptions.

The existing context explicitly states that lake concepts must not leak into core tables/business logic. fileciteturn16file8L1475-L1497

---

# 103. What Codex Must NOT Do

Codex must not:

- introduce microservices without approval
- replace PostgreSQL
- remove PostGIS
- hard-code SLA values
- hard-code permissions into controllers
- expose arbitrary status updates
- store large evidence binaries in PostgreSQL
- bypass audit logging
- trust frontend role information
- make AI decisions authoritative
- modify applied migrations
- silently change domain requirements
- remove tests to make builds pass
- weaken authorization to solve implementation problems

These restrictions are consistent with the existing system architecture. fileciteturn16file9L1576-L1592

---

# 104. Final Database/API Definition

CivicOS persistence should provide:

```text
Strong relational integrity
+
PostGIS spatial integrity
+
Auditable lifecycle history
+
Configurable policies
+
Evidence traceability
+
Workflow-aware state
+
Role/context-aware access
+
Idempotent events
+
Optimistic concurrency
+
REST resource/command APIs
```

The database is not merely a storage layer.

It is the durable representation of:

```text
who
did what
where
when
for which intervention
under which authority
with what evidence
with what decision
and with what outcome
```

The API must expose that lifecycle without allowing clients to bypass the domain rules.

---

# 105. Engineering Completion Criterion

The schema/API layer is considered ready for Codex implementation when:

- all core entities have migrations
- all critical FK relationships exist
- PostGIS indexes exist
- workflow state cannot be arbitrarily mutated
- RBAC is enforced server-side
- approval decisions are immutable
- evidence references object storage
- audit events are immutable
- domain events are idempotent
- optimistic locking exists on mutable aggregates
- API contracts have DTOs
- command endpoints map to domain operations
- integration tests cover critical workflows
- Testcontainers can start PostgreSQL/PostGIS automatically

At that point, the next document should define the **AI Architecture** in detail, followed by **UI/UX Architecture**, **Testing Strategy**, **Seed/Demo Data**, and finally the **Codex Master Implementation Specification**.

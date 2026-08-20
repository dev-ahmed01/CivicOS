# CivicOS — Engineering Definition
## Complete Engine Architecture & Business Rules

**Version:** 1.3  
**Status:** Engineering baseline  
**Scope:** SIH MVP — multi-agency road-cutting / digging coordination and lifecycle verification  
**Implementation direction:** Java + Spring Boot + PostgreSQL/PostGIS + React/TypeScript

---

# 1. Purpose

This document consolidates the **engines required to implement CivicOS**.

It sits above the Domain Model + ERD and Workflow + State Machine documents and translates them into implementable backend responsibilities.

CivicOS is not a generic complaint-management application. Its core value is:

> **Connect the physical road segment, interventions, actors, dependencies, conflicts, decisions, evidence and outcomes into one traceable lifecycle.**

The existing project definition explicitly requires domain-driven design, an explicit state machine, auditability, human-in-the-loop decisions, evidence-first architecture, explainable automation, configuration over hard-coded assumptions, separation of AI recommendations from authoritative decisions, API-first design, geospatial correctness and idempotent event handling.

---

# 2. Engine Inventory

CivicOS should be implemented as a modular monolith for the SIH MVP.

## Core engines

1. **Identity & Access Engine**
2. **Road / Geospatial Engine**
3. **Intervention Engine**
4. **Dependency & Planning Engine**
5. **Conflict Detection Engine**
6. **Risk & Severity Engine**
7. **Recommendation & Sequencing Engine**
8. **Workflow / State Machine Engine**
9. **Approval Engine**
10. **Task & Coordination Engine**
11. **SLA / Deadline Engine**
12. **Evidence Engine**
13. **Inspection & Verification Engine**
14. **Citizen Observation & Validation Engine**
15. **Notification & Escalation Engine**
16. **Audit & Event Engine**
17. **AI Assistance Engine**
18. **Integration / External-System Engine**
19. **Analytics & Outcome Engine**
20. **Configuration / Policy Engine**

These are logical modules. They do not need to be separate microservices.

---

# 3. Architectural Rule

For every authoritative operation:

```text
API Command
     ↓
Authorization
     ↓
Domain Validation
     ↓
Business Rules
     ↓
State Transition
     ↓
Persistence
     ↓
Audit Event
     ↓
Domain Event
     ↓
Tasks / Notifications / Analytics / AI
```

No controller may directly mutate an intervention's authoritative status.

Preferred:

```text
approveIntervention()
```

Not:

```text
updateInterventionStatus("APPROVED")
```

---

# 4. Engine Dependency Graph

```text
                    ┌─────────────────────┐
                    │ Identity & Access   │
                    └──────────┬──────────┘
                               │
                               ▼
┌───────────────┐      ┌──────────────────┐
│ Configuration │─────►│ Workflow Engine  │
└───────────────┘      └────────┬─────────┘
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
     ┌─────────────┐    ┌──────────────┐   ┌──────────────┐
     │ Approval    │    │ SLA/Deadline │   │ Task/Coord.  │
     └─────────────┘    └──────────────┘   └──────────────┘
             │                  │                  │
             └──────────────────┼──────────────────┘
                                ▼
                    ┌─────────────────────┐
                    │ Intervention Engine │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
       Geospatial         Dependency       Historical Data
          Engine             Engine               │
              └────────────────┼──────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │ Conflict Detection  │
                    └──────────┬──────────┘
                               ▼
                    ┌─────────────────────┐
                    │ Risk / Severity     │
                    └──────────┬──────────┘
                               ▼
                    ┌─────────────────────┐
                    │ Recommendation      │
                    └──────────┬──────────┘
                               │
                               ▼
                         Human decision

Execution
   ↓
Evidence
   ↓
Inspection / Verification
   ↓
Citizen Validation
   ↓
Outcome Analytics

All authoritative operations
            ↓
     Audit / Event Engine

AI operates beside the workflow, not above it.
```

---

# 5. Identity & Access Engine

## Purpose

Authenticate users and determine whether an actor may perform an operation.

## Actors

```text
CITIZEN
AGENCY_USER
ENGINEER
COORDINATOR
INSPECTOR
APPROVER
CONTRACTOR
ADMIN
SYSTEM
```

## Responsibilities

- authentication
- role assignment
- permission evaluation
- agency membership
- resource-level authorization
- separation of duties
- session/security policy

## Core checks

```text
Is user authenticated?
        ↓
Is user active?
        ↓
Does user have required role?
        ↓
Does role have required permission?
        ↓
Does user belong to relevant agency/jurisdiction?
        ↓
Is the resource accessible to that user?
```

## Important rule

AI output can never be used as an authorization mechanism.

---

# 6. Road / Geospatial Engine

## Purpose

Provide the physical reference model used by every coordination decision.

## Core entities

```text
Road
RoadSegment
Intervention geometry
```

Production must support:

```text
Intervention N ↔ M RoadSegment
```

through a join entity.

## Responsibilities

- road lookup
- road-segment lookup
- geometry validation
- coordinate normalization
- spatial intersection
- proximity calculation
- road classification
- segment association
- spatial history lookup

## Required operations

```text
findSegmentsContainingPoint()
findInterventionsIntersectingGeometry()
findInterventionsWithinBuffer()
calculateOverlap()
calculateDistance()
findNearbyHistoricalInterventions()
```

## Spatial rules

A candidate conflict can exist when:

```text
geometry A intersects geometry B
```

or:

```text
distance(A,B) <= configured threshold
```

The threshold must depend on intervention type where necessary.

---

# 7. Intervention Engine

## Purpose

Own the lifecycle and business identity of physical works.

## Intervention contains

- identity
- owning agency
- road segments
- geometry
- intervention type
- purpose
- planned dates
- actual dates
- priority
- contractor
- dependencies
- conflicts
- milestones
- approvals
- evidence
- inspections
- verification
- audit history

## Responsibilities

- create intervention
- validate intervention
- update permissible fields
- schedule intervention
- record execution
- record completion
- initiate restoration
- request verification
- close intervention

## Invariant

An intervention cannot be approved unless all configured prerequisites are satisfied.

---

# 8. Dependency & Planning Engine

## Purpose

Represent relationships between interventions.

Example:

```text
Utility excavation
      ↓
Utility installation
      ↓
Restoration
      ↓
Resurfacing
```

## Dependency types

```text
PRECEDES
FOLLOWS
BLOCKS
REQUIRES
SHOULD_PRECEDE
COORDINATE_WITH
```

## Responsibilities

- create dependency
- validate dependency
- detect dependency cycles
- calculate dependency graph
- identify blocking dependencies
- validate proposed schedules
- identify invalid sequences

## Hard dependency

If:

```text
A must precede B
```

then:

```text
start(B) < completion(A)
```

is invalid unless an authorised exception exists.

## Cycle detection

The engine must reject:

```text
A → B → C → A
```

for hard dependencies.

---

# 9. Conflict Detection Engine

## Purpose

Detect coordination problems before execution.

This is a primary CivicOS differentiator.

## Conflict categories

### 9.1 Spatial conflict

Interventions overlap geographically.

### 9.2 Temporal conflict

Execution periods overlap or are too close.

### 9.3 Spatial + temporal conflict

Highest-confidence coordination condition when both apply.

### 9.4 Dependency conflict

A prerequisite is scheduled after its dependent intervention.

### 9.5 Restoration conflict

Restoration/resurfacing is scheduled while another intervention is pending.

### 9.6 Repeat-excavation risk

A road has recently been restored/resurfaced and another excavation is planned shortly afterward.

### 9.7 Duplicate intervention

Two records may represent substantially the same work.

### 9.8 Resource/agency coordination conflict

Multiple agencies require coordinated access or sequencing.

---

# 10. Conflict Detection Pipeline

```text
New / changed intervention
          ↓
Find affected road segments
          ↓
Retrieve candidate interventions
          ↓
Spatial filtering
          ↓
Temporal filtering
          ↓
Dependency analysis
          ↓
Historical/restoration analysis
          ↓
Conflict classification
          ↓
Severity calculation
          ↓
Create/update Conflict
```

## Candidate filtering

Do not compare every intervention with every other intervention.

Use:

```text
same / nearby road segment
+
overlapping time window
+
compatible intervention types
```

to reduce computation.

---

# 11. Conflict Engine Rules

### Rule C-001

Spatial overlap is a candidate conflict.

### Rule C-002

Temporal overlap is a candidate conflict.

### Rule C-003

Spatial + temporal overlap produces a stronger conflict.

### Rule C-004

Hard dependency violation is a blocking conflict.

### Rule C-005

Recent restoration followed by excavation creates repeat-excavation risk.

### Rule C-006

Resurfacing before all relevant utility work is complete creates sequencing risk.

### Rule C-007

Duplicate records require review rather than automatic merging.

### Rule C-008

Conflict status must remain auditable.

---

# 12. Risk & Severity Engine

## Purpose

Translate detected conditions into actionable severity.

## Severity

```text
LOW
MEDIUM
HIGH
CRITICAL
```

## Inputs

- conflict type
- spatial overlap
- temporal overlap
- road class
- intervention priority
- number of agencies
- dependency criticality
- recent restoration
- critical utility
- traffic importance
- schedule proximity
- intervention status

## Example

```text
Spatial overlap = YES
Temporal overlap = YES
Road = ARTERIAL
Recent resurfacing = YES
Multiple agencies = YES

→ HIGH / CRITICAL
```

## Important

Severity rules are deterministic and configurable.

AI may explain severity but must not secretly override the rules.

---

# 13. Recommendation & Sequencing Engine

## Purpose

CivicOS must go beyond:

> "Conflict detected."

It should answer:

> "What should the agencies do instead?"

## Inputs

- interventions
- dependencies
- schedules
- road segments
- conflicts
- restoration milestones
- constraints
- configured policies

## Outputs

```text
recommended sequence
schedule adjustments
coordination actions
assumptions
reason
expected benefit
authority required
confidence
```

## Example

Current:

```text
BESCOM      Jun 10–15
BWSSB       Jun 13–18
OFC         Jun 16–19
RESURFACING Jun 17–18
```

Recommendation:

```text
BESCOM
   ↓
BWSSB
   ↓
OFC
   ↓
CONSOLIDATED RESTORATION
   ↓
RESURFACING
```

## Recommendation must explain

1. What conflicts exist?
2. Why is the current sequence inefficient?
3. What sequence is recommended?
4. What assumptions were used?
5. What changes are required?
6. Who must approve the change?
7. What risk is reduced?

## Hard rule

Recommendation ≠ decision.

A coordinator/authorised officer must approve the change.

---

# 14. Workflow / State Machine Engine

## Purpose

Enforce the intervention lifecycle.

Canonical lifecycle:

```text
DRAFT
 ↓
SUBMITTED
 ↓
UNDER_REVIEW
 ↓
ANALYSIS
 ↓
COORDINATION_REQUIRED
 ↓
COORDINATION_COMPLETE
 ↓
APPROVAL_PENDING
 ↓
APPROVED
 ↓
SCHEDULED
 ↓
IN_PROGRESS
 ↓
RESTORATION
 ↓
EVIDENCE_PENDING
 ↓
VERIFICATION_PENDING
 ↓
VERIFIED
 ↓
CLOSED
```

Exception states include:

```text
REJECTED
CANCELLED
ON_HOLD
REINSPECTION
REOPENED
```

## Every transition checks

```text
actor
permission
current state
preconditions
required fields
required evidence
required approvals
blocking conflicts
SLA conditions
```

## Rule

No arbitrary status mutation.

---

# 15. Approval Engine

## Purpose

Manage authoritative decisions.

## Approval types

```text
TECHNICAL
COORDINATION
EXECUTION
CLOSURE
OTHER
```

## Configuration

Approval requirements depend on:

```text
intervention type
agency
road class
risk
jurisdiction
priority
```

Example:

```json
{
  "interventionType": "TELECOM",
  "roadClass": "ARTERIAL",
  "risk": "HIGH",
  "requiresCoordination": true,
  "requiresApproval": true,
  "requiresFinalInspection": true
}
```

## Approval workflow

```text
Review
 ↓
Technical validation
 ↓
Conflict resolution
 ↓
Required checks
 ↓
Authorised approver
 ↓
Decision
```

## Decisions

```text
APPROVE
REJECT
RETURN
APPROVE_WITH_CONDITIONS
```

## Rule

No approval without a valid approval record.

---

# 16. Task & Coordination Engine

## Purpose

Convert detected conditions into actionable human work.

A conflict alone is not useful unless somebody owns the next action.

## Flow

```text
Conflict
   ↓
Coordination Task
   ↓
Coordinator
   ↓
Affected Agencies
   ↓
Discussion / Decision
   ↓
Schedule Change
   ↓
Conflict Resolution
```

## Task fields

- task type
- assignee
- agency
- priority
- due date
- status
- linked intervention
- linked conflict
- notes
- completion evidence

## Task states

```text
OPEN
ASSIGNED
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED
```

---

# 17. SLA / Deadline Engine

## Purpose

Track time-bound obligations.

## SLA categories

```text
REVIEW
COORDINATION
APPROVAL
PRE_WORK
EXECUTION
EVIDENCE
VERIFICATION
CORRECTION
CLOSURE
```

## SLA fields

```text
start_at
due_at
completed_at
status
escalation_level
breached_at
pause_periods
```

## Deadline calculation

Must account for:

- start timestamp
- SLA duration
- business calendar
- holidays
- agency working hours
- priority
- severity
- pause periods

## Prototype defaults

These are engineering defaults only, not statutory claims:

| SLA | Default |
|---|---:|
| Initial review | 1 business day |
| Medium coordination | 2 business days |
| High coordination | 1 business day |
| Critical coordination | 4 working hours |
| Approval | 1 business day |
| Evidence submission | 1 business day |
| Inspection request | 1 business day |
| Inspection | 1 business day |
| Correction | 2 business days |
| Closure | 1 business day |

All production values must be configurable.

## Breach

```text
Due
 ↓
BREACHED
 ↓
Audit
 ↓
Notification
 ↓
Escalation
```

A breach must never automatically approve or reject work.

---

# 18. Notification & Escalation Engine

## Events

Notify on:

- intervention submission
- conflict detected
- task assigned
- approval pending
- deadline approaching
- SLA breach
- dependency blocked
- work started
- restoration pending
- evidence submitted
- verification requested
- verification failed
- citizen validation
- reopening

## Channels

MVP:

```text
In-app
Email
```

Future:

```text
SMS
Push
Other official channels
```

## Escalation

### Level 1

Responsible actor.

### Level 2

Supervisor.

### Level 3

Designated agency/cross-agency authority.

Critical conflicts may escalate immediately according to policy.

---

# 19. Evidence Engine

## Purpose

Treat evidence as first-class domain data.

Evidence types:

```text
BEFORE_WORK
DURING_WORK
COMPLETION
RESTORATION
INSPECTION
CITIZEN_VALIDATION
DOCUMENT
```

## Evidence metadata

Every evidence item should store:

- intervention
- milestone
- uploader
- timestamp
- location where available
- evidence type
- storage reference
- MIME/type
- file size
- checksum/integrity information
- metadata
- optional AI analysis reference

## Evidence lifecycle

```text
Uploaded
 ↓
Validated
 ↓
Associated
 ↓
Reviewed
 ↓
Accepted / Rejected
```

## Rules

- Invalid file types rejected.
- Oversized files rejected.
- Evidence must be linked to a domain event/milestone where appropriate.
- Original evidence must not be silently replaced.
- Rejected evidence remains auditable.

---

# 20. Inspection & Verification Engine

## Purpose

Separate:

```text
Work completed
```

from:

```text
Work verified
```

## Official verification

Performed by:

```text
INSPECTOR / authorised officer
```

## Verification checks

Depending on intervention:

- work completion
- location
- restoration
- physical condition
- required evidence
- inspection checklist
- configured quality conditions

## Results

```text
PASS
FAIL
CONDITIONAL
REQUIRES_REINSPECTION
```

## Failed verification

```text
VERIFICATION_PENDING
       ↓
REINSPECTION
       ↓
CORRECTION
       ↓
VERIFICATION_PENDING
```

The original failed inspection remains immutable.

---

# 21. Citizen Observation & Validation Engine

## Purpose

Citizens provide ground-level evidence and feedback without becoming statutory decision-makers.

## Citizen can

- submit location
- upload photos
- describe visible issue
- report excavation
- report incomplete restoration
- validate visible outcome

## Citizen cannot

- approve
- reject
- reschedule official work
- close intervention
- certify statutory compliance

## Observation pipeline

```text
Citizen Observation
        ↓
Triage
        ↓
Duplicate check
        ↓
Potential intervention match
        ↓
Human/system review
        ↓
Task / investigation
```

## Matching inputs

- spatial proximity
- time
- road segment
- intervention type
- image classification
- text classification

AI may return:

```text
possible_match = 0.91
```

but authoritative association remains reviewable.

---

# 22. Reopening Engine

## Purpose

Close the lifecycle feedback loop.

Triggers:

- verified citizen observation
- failed post-closure inspection
- agency discovery
- audit finding

Flow:

```text
CLOSED
  ↓
REOPENED
  ↓
IN_PROGRESS
```

or:

```text
CLOSED
  ↓
REOPENED
  ↓
VERIFICATION_PENDING
```

## Required

- reason
- evidence
- actor
- timestamp

---

# 23. Audit & Event Engine

## Purpose

Create a permanent trace of authoritative activity.

Audit:

- creation
- submission
- state transition
- assignment
- conflict creation
- conflict resolution
- recommendation
- approval
- rejection
- schedule change
- work start
- work completion
- evidence acceptance/rejection
- inspection
- verification
- closure
- reopening
- cancellation
- hold/resume
- SLA breach
- administrative override

## Event pattern

```text
Command
 ↓
Validation
 ↓
Domain Change
 ↓
Audit Event
 ↓
Domain Event
 ↓
Notifications / Analytics / AI
```

## Audit event should include

```text
actor
actor_role
agency
action
entity_type
entity_id
previous_state
new_state
timestamp
reason
request/correlation ID
metadata
```

Audit events are immutable.

---

# 24. AI Assistance Engine

## Purpose

Provide assistance without becoming an authority.

## AI capabilities

### Classification

Classify:

- intervention type
- citizen observation
- evidence type
- issue category

### Image analysis

Assist with:

- visible road damage
- excavation indicators
- restoration condition
- evidence classification

### Duplicate detection

Identify potentially duplicate:

- observations
- interventions
- evidence

### Document extraction

Extract structured information from uploaded documents.

### Evidence analysis

Identify missing or inconsistent evidence.

### Recommendation explanation

Explain deterministic engine output in human-readable language.

### Risk assistance

Provide advisory signals for review.

---

# 25. AI Boundary

AI must never directly:

- approve statutory work
- reject an intervention without human review
- impose penalties
- change official deadlines
- close an intervention
- override an officer
- determine legal compliance
- grant permissions

The architecture is:

```text
Deterministic Engine
       ↓
Recommendation
       ↓
AI Explanation / Assistance
       ↓
Human Review
       ↓
Authoritative Decision
```

## AI output storage

Store:

- model/provider
- timestamp
- input reference
- output
- confidence where applicable
- model/prompt version where applicable

---

# 26. Integration / External-System Engine

## Purpose

Connect CivicOS to existing systems without pretending CivicOS replaces them.

Potential systems:

```text
MARCS-style road-cutting records
Agency systems
GIS systems
Document systems
Identity systems
Notification systems
```

## Adapter pattern

```text
CivicOS Domain
      ↓
Integration Port
      ↓
External Adapter
      ↓
External System
```

## Critical distinction

Always distinguish:

```text
external record
CivicOS coordination record
CivicOS decision
```

CivicOS should preserve external reference IDs.

## MVP

Where real APIs are unavailable:

- use realistic seeded data
- simulate external records
- clearly label simulations
- keep integration interfaces real

---

# 27. Configuration / Policy Engine

## Purpose

Prevent domain rules from becoming hard-coded.

Configurable values include:

- conflict thresholds
- proximity buffers
- SLA durations
- working calendars
- holidays
- approval chains
- severity rules
- evidence requirements
- intervention types
- road classes
- escalation contacts
- required inspections
- closure requirements

Example:

```json
{
  "interventionType": "UTILITY_EXCAVATION",
  "roadClass": "ARTERIAL",
  "requiresCoordination": true,
  "requiresInspection": true,
  "requiresRestorationEvidence": true
}
```

## Rule

Changing a policy should not require recompiling the domain model.

---

# 28. Analytics & Outcome Engine

## Purpose

Turn lifecycle data into operational metrics.

## Operational metrics

- active interventions
- unresolved conflicts
- overdue tasks
- SLA breach rate
- restoration delays
- verification coverage
- agency workload
- repeat excavation count

## Coordination metrics

- conflicts detected
- conflicts resolved
- average resolution time
- schedule changes
- repeat excavation prevented/flagged
- interventions coordinated before execution

## Outcome distinction

The system must not collapse:

```text
Completed
Verified
Functional
Sustained
```

into one generic status.

A completed intervention is not automatically a successful long-term outcome.

---

# 29. Historical Analysis Engine

## Purpose

Use previous interventions to detect patterns.

Inputs:

- previous interventions
- restoration events
- resurfacing
- road segments
- dates
- intervention types

Outputs:

```text
recent excavation
repeat excavation risk
historical agency activity
repeated intervention pattern
```

Example:

```text
Road Segment 1042
Restored: June 10
New excavation planned: June 22

→ Repeat-excavation risk
```

---

# 30. Data Quality Engine

## Purpose

Prevent unreliable data from contaminating conflict detection.

Checks:

- invalid geometry
- missing dates
- end before start
- invalid agency
- invalid road segment
- duplicate reference IDs
- impossible state combinations
- missing required evidence
- orphaned dependencies

## Rule

Bad data must be rejected or explicitly flagged.

Do not silently "fix" authoritative data.

---

# 31. Search & Retrieval Engine

## Purpose

Provide fast access to interventions, conflicts and evidence.

Search dimensions:

```text
intervention ID
road
road segment
agency
status
date
intervention type
conflict severity
SLA status
contractor
approval status
```

Geospatial search:

```text
near point
within polygon
intersects geometry
same road segment
```

---

# 32. Reporting Engine

## Purpose

Generate operational reports.

Reports:

- intervention lifecycle
- conflict report
- agency workload
- SLA performance
- restoration status
- verification status
- repeat excavation
- audit timeline

Reports must distinguish:

```text
system-generated analytical result
```

from:

```text
official decision / record
```

---

# 33. Engine-to-Engine Event Model

Important domain events:

```text
InterventionSubmitted
InterventionReviewed
AnalysisCompleted
ConflictDetected
ConflictSeverityChanged
CoordinationTaskCreated
CoordinationCompleted
RecommendationGenerated
ScheduleChanged
ApprovalRequested
ApprovalGranted
ApprovalRejected
InterventionScheduled
InterventionStarted
InterventionCompleted
RestorationStarted
EvidenceSubmitted
EvidenceAccepted
VerificationRequested
InspectionCompleted
VerificationPassed
VerificationFailed
InterventionClosed
CitizenObservationCreated
CitizenObservationMatched
InterventionReopened
SlaApproaching
SlaBreached
```

Example:

```text
ConflictDetected
      ↓
RiskEngine
      ↓
RecommendationEngine
      ↓
TaskEngine
      ↓
NotificationEngine
```

---

# 34. Idempotency

Event consumers must be idempotent.

Example:

If:

```text
ConflictDetected(eventId=123)
```

is delivered twice, the system must not create two independent coordination tasks.

Use:

```text
event_id
idempotency_key
processed_event table / equivalent mechanism
```

where required.

---

# 35. Failure Strategy

The core workflow must remain operational when AI or secondary services fail.

## AI unavailable

```text
AI unavailable
   ↓
Deterministic workflow continues
   ↓
Recommendation may be unavailable
   ↓
Human can continue manually
```

## Notification failure

The domain transition must not be rolled back merely because email failed.

Notification retry occurs asynchronously.

## Analytics failure

Do not block operational workflow.

## External integration failure

Record integration failure and preserve the CivicOS transaction.

---

# 36. Transaction Boundaries

A state-changing command should be atomic for the authoritative domain operation.

Example:

```text
approveIntervention()
    ├── validate
    ├── update intervention
    ├── create approval
    └── create audit event
```

After commit:

```text
Domain event
    ↓
async notifications
async analytics
async AI
```

Use an outbox-style pattern where appropriate.

---

# 37. Recommended Java Module Structure

```text
com.civicos
│
├── identity
├── agency
├── road
├── intervention
├── dependency
├── conflict
├── risk
├── recommendation
├── workflow
├── approval
├── task
├── sla
├── evidence
├── verification
├── citizen
├── notification
├── audit
├── ai
├── integration
├── analytics
├── configuration
└── shared
```

Each module should follow:

```text
domain/
application/
infrastructure/
api/
```

where justified.

---

# 38. Engine Implementation Order

Codex should implement in this order.

## Phase 1 — Foundation

1. Identity & Access
2. Configuration
3. Audit/Event infrastructure
4. Data Quality
5. API/error standards

## Phase 2 — Physical Model

6. Road / Geospatial
7. Intervention
8. Dependency / Planning
9. Historical analysis

## Phase 3 — Intelligence

10. Conflict Detection
11. Risk / Severity
12. Recommendation / Sequencing

## Phase 4 — Operational Workflow

13. Workflow / State Machine
14. Approval
15. Task / Coordination
16. SLA / Deadline
17. Notification / Escalation

## Phase 5 — Field Lifecycle

18. Evidence
19. Inspection / Verification
20. Citizen Observation / Validation
21. Reopening

## Phase 6 — Intelligence Assistance

22. AI Assistance
23. AI image/document analysis
24. AI explanation

## Phase 7 — Integration & Visibility

25. External-system adapters
26. Analytics / Outcome
27. Search
28. Reporting
29. Dashboards

## Phase 8 — Hardening

30. security testing
31. workflow testing
32. spatial testing
33. conflict testing
34. AI failure testing
35. E2E testing
36. SIH demo hardening

---

# 39. MVP Engines vs Future Engines

## Must implement for SIH MVP

```text
Identity & Access
Road / Geospatial
Intervention
Dependency
Conflict Detection
Risk / Severity
Recommendation
Workflow
Approval
Task / Coordination
SLA
Notification
Evidence
Verification
Citizen Observation
Audit
AI Assistance
Configuration
```

## Can remain simplified

```text
Analytics
Reporting
Search
Integration adapters
Outcome monitoring
```

## Do not build now

```text
Complete municipal ERP
Full NGO marketplace
Complete lake-management platform
National deployment
Autonomous government decisions
Large microservice fleet
Dozens of live government integrations
Complex funding platform
```

---

# 40. Complete System Flow

```text
AGENCY / CITIZEN
       ↓
Identity & Access
       ↓
Intervention / Observation
       ↓
Data Quality
       ↓
Road / Geospatial
       ↓
Historical Analysis
       ↓
Dependency Analysis
       ↓
Conflict Detection
       ↓
Risk / Severity
       ↓
Recommendation
       ↓
Coordinator
       ↓
Task / Coordination
       ↓
Approval
       ↓
Workflow
       ↓
SLA / Escalation
       ↓
Schedule
       ↓
Execution
       ↓
Restoration
       ↓
Evidence
       ↓
Inspection
       ↓
Verification
       ↓
Citizen Validation
       ↓
Closure
       ↓
Outcome Monitoring
       ↓
Potential Reopening
```

---

# 41. Flagship SIH Scenario

Use one road segment containing:

```text
BWSSB pipeline
BESCOM cable
OFC work
Road resurfacing
```

CivicOS should demonstrate:

```text
Multiple interventions
       ↓
Same road segment
       ↓
Spatial analysis
       ↓
Temporal analysis
       ↓
Dependency analysis
       ↓
Historical restoration check
       ↓
Conflict detected
       ↓
Severity calculated
       ↓
Sequence recommended
       ↓
Coordinator reviews
       ↓
Agencies coordinate
       ↓
Approver decides
       ↓
Work begins
       ↓
Evidence submitted
       ↓
Restoration
       ↓
Official verification
       ↓
Citizen validation
       ↓
Closure
```

This single scenario should communicate the product.

---

# 42. Non-Negotiable Engineering Invariants

### INV-001

No `CLOSED` intervention without required final verification.

### INV-002

No `APPROVED` intervention without valid approval.

### INV-003

No `IN_PROGRESS` intervention without approval/schedule unless an explicit emergency policy is enabled.

### INV-004

Blocking conflict prevents approval.

### INV-005

Material schedule changes trigger re-analysis.

### INV-006

AI cannot make authoritative decisions.

### INV-007

Citizen observations cannot directly approve, reject or close interventions.

### INV-008

Failed inspections remain immutable history.

### INV-009

Audit events are immutable.

### INV-010

SLA breaches never silently advance workflow.

### INV-011

Every transition requires an authorised actor or explicit system rule.

### INV-012

Hard dependency cycles are forbidden.

### INV-013

External-system records and CivicOS decisions remain distinguishable.

### INV-014

Operational workflow cannot depend on AI availability.

### INV-015

No engine may silently modify authoritative government data.

---

# 43. Quality Standard

The system should not compete through feature count.

The quality target is:

```text
Correct
Explainable
Auditable
Geospatially reliable
Role-aware
Evidence-driven
Human-controlled
Failure-tolerant
Configurable
Testable
```

The key architectural distinction is:

```text
AI = assistance
Rules = deterministic coordination logic
Human = authoritative decision
Audit = accountability
Evidence = proof
Workflow = lifecycle control
```

---

# 44. Final Definition

CivicOS should be implemented as:

> **A modular, evidence-driven civic intervention platform with a geospatial road model, deterministic coordination/conflict engines, configurable workflow and SLA engines, human-controlled approvals, lifecycle evidence and verification, citizen feedback, and an AI assistance layer.**

Its core technical story is:

```text
Physical location
      +
Intervention
      +
Actor
      +
Dependency
      +
Conflict
      +
Recommendation
      +
Decision
      +
Deadline
      +
Execution
      +
Evidence
      +
Verification
      +
Outcome
      =
Traceable Civic Intervention Lifecycle
```

The engines are deliberately modular so future civic domains can reuse the same platform without making the SIH MVP broad or unfocused.

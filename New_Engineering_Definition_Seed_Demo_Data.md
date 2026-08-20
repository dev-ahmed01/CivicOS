# CivicOS — Engineering Definition v1.8
## Seed Data, Demo Dataset & End-to-End SIH Scenario

**Status:** Engineering baseline  
**Purpose:** Provide deterministic, realistic, synthetic data for development, screenshots, integration testing and the SIH demonstration.

> **Important:** All case IDs, intervention IDs, schedules, people, contractors, coordinates and operational events in this document are synthetic demo data. They are not claims about real work currently occurring in Bengaluru.

---

# 1. Why This Demo Dataset Exists

The CivicOS demo must prove the core proposition:

```text
Multiple agencies
+
same road
+
different planned interventions
+
coordination problem
+
conflict detection
+
AI-assisted recommendation
+
approval
+
execution evidence
+
inspection
+
citizen verification
=
one traceable lifecycle
```

Bengaluru is an appropriate domain for this scenario because the existing MARCS system already supports online road-cutting permissions and coordination around road restoration. BBMP describes MARCS as a multi-agency system involving agencies such as BESCOM and BWSSB, with verification, restoration cost, validity periods and completion inspection built into the workflow. citeturn0search0

The current public GBA/BBMP portal also lists MARCS 3.0 and Road History 2.0 among its e-services. citeturn0search5

Therefore CivicOS must **not** demo itself as merely "another road-cutting permission portal."

The demo must show the additional coordination layer:

```text
planned work
→ cross-agency visibility
→ conflict detection
→ sequence recommendation
→ coordinated decision
→ lifecycle evidence
→ verification
```

---

# 2. Demo City

```text
City: Bengaluru
State: Karnataka
Country: India
Primary scenario: Road cutting / utility excavation
```

Use Bengaluru as the demo geography.

Do not represent the synthetic records as official BBMP/GBA records.

---

# 3. Demo Agencies

Use real agency names only as organizational actors in the fictional workflow.

| Code | Agency |
|---|---|
| BBMP | Bruhat Bengaluru Mahanagara Palike / municipal road authority context |
| BESCOM | Bengaluru Electricity Supply Company Limited |
| BWSSB | Bengaluru Water Supply and Sewerage Board |
| BMRCL | Bengaluru Metro Rail Corporation Limited |
| KPTCL | Karnataka Power Transmission Corporation Limited |
| GAIL | Gas Authority of India Limited |
| TSP | Telecom / network service provider |

The MVP scenario should primarily use:

```text
BBMP
BESCOM
BWSSB
```

Additional agencies are included to demonstrate extensibility.

---

# 4. Demo Roles

Create synthetic users:

| User | Role | Agency |
|---|---|---|
| DEMO-CIT-001 | Citizen | Citizen |
| DEMO-BBMP-001 | Road Engineer | BBMP |
| DEMO-BBMP-002 | Coordinator | BBMP |
| DEMO-BESCOM-001 | Agency Officer | BESCOM |
| DEMO-BWSSB-001 | Agency Officer | BWSSB |
| DEMO-INSP-001 | Field Inspector | BBMP |
| DEMO-ADMIN-001 | System Administrator | CivicOS |

Do not use real individuals.

---

# 5. Demo Road Network

Create a small synthetic road network around recognizable Bengaluru corridors.

The geometry is for demonstration only.

## Road Segment R001

```text
id: R001
name: Outer Ring Road — Demo Segment
surface: BITUMINOUS
classification: ARTERIAL
length_m: 850
```

Purpose:

```text
Primary conflict demonstration.
```

---

## Road Segment R002

```text
id: R002
name: Demo Link Road — Koramangala
surface: BITUMINOUS
classification: COLLECTOR
length_m: 420
```

Purpose:

```text
Non-conflicting control scenario.
```

---

## Road Segment R003

```text
id: R003
name: Demo Service Road — Whitefield
surface: BITUMINOUS
classification: LOCAL
length_m: 310
```

Purpose:

```text
Citizen observation / restoration verification.
```

---

# 6. Synthetic Geometry Rule

Do not require exact real-world road coordinates for the MVP.

Use either:

```text
synthetic PostGIS LineString
```

or:

```text
publicly available base-map geometry
```

with synthetic interventions layered on top.

For the SIH demo, the important property is:

```text
Intervention A intersects R001
Intervention B intersects R001
Intervention C intersects R001
```

---

# 7. Demo Intervention I001

```text
ID: INT-001
Agency: BESCOM
Type: ELECTRICAL_UTILITY
Purpose: Underground cable maintenance
Road: R001
Planned start: 2026-09-10 09:00
Planned end: 2026-09-12 18:00
Status: PLANNED
```

Scope:

```text
road cutting required
length: 120m
width: 1.0m
```

---

# 8. Demo Intervention I002

```text
ID: INT-002
Agency: BWSSB
Type: WATER_UTILITY
Purpose: Water pipeline connection
Road: R001
Planned start: 2026-09-13 09:00
Planned end: 2026-09-15 18:00
Status: PLANNED
```

Scope:

```text
road cutting required
length: 90m
width: 1.2m
```

---

# 9. Demo Intervention I003

```text
ID: INT-003
Agency: BBMP
Type: ROAD_RESTORATION
Purpose: Resurfacing / final restoration
Road: R001
Planned start: 2026-09-16 09:00
Planned end: 2026-09-18 18:00
Status: PLANNED
```

This is intentionally scheduled after the utility interventions.

---

# 10. Demo Intervention I004 — Control

```text
ID: INT-004
Agency: BESCOM
Type: ELECTRICAL_UTILITY
Purpose: Minor cable maintenance
Road: R002
Planned start: 2026-09-20 09:00
Planned end: 2026-09-20 18:00
Status: PLANNED
```

No conflict should be generated.

This proves the engine does not simply flag every intervention.

---

# 11. Demo Intervention I005 — Late Discovery

```text
ID: INT-005
Agency: Telecom Provider
Type: OFC
Purpose: Fibre installation
Road: R001
Planned start: 2026-09-14 09:00
Planned end: 2026-09-16 18:00
Status: PROPOSED
```

This creates the critical scenario:

```text
BESCOM
Sep 10–12

BWSSB
Sep 13–15

Telecom
Sep 14–16

BBMP restoration
Sep 16–18
```

The sequence is inefficient because work overlaps and restoration is positioned while another excavation is still active.

---

# 12. Expected Conflicts

## Conflict C001

```text
Type: SEQUENTIAL_EXCAVATION
Severity: HIGH
Affected road: R001
Interventions: INT-002, INT-005, INT-003
```

Reason:

```text
A road restoration activity is planned while another utility excavation
is still active or scheduled to overlap the restoration window.
```

---

## Conflict C002

```text
Type: MULTI_AGENCY_COORDINATION
Severity: HIGH
Affected road: R001
Interventions:
INT-001
INT-002
INT-005
INT-003
```

Reason:

```text
Three agencies have road-impacting work in a shared corridor.
```

---

## Conflict C003

```text
Type: REPEAT_DIGGING_RISK
Severity: MEDIUM
Affected road: R001
Interventions:
INT-001
INT-002
INT-005
```

Reason:

```text
Multiple excavation activities are planned in the same road corridor
within a short period.
```

---

# 13. What the Conflict Engine Must Detect

The deterministic engine should detect:

```text
same road segment
+
spatial overlap
+
temporal overlap / unsafe sequencing
+
compatible intervention types
```

The engine should not depend on AI to discover these fundamental conflicts.

This is particularly important because Bengaluru's existing road-cutting process already requires permission and restoration controls; CivicOS's demonstration value is therefore the cross-project coordination and lifecycle intelligence layered on top. citeturn0search0turn0search4

---

# 14. AI Recommendation

Once C001/C002/C003 are detected, request an AI recommendation.

Expected advisory output:

```text
Recommendation:

Complete all utility excavation activities on the affected road segment
before BBMP performs final resurfacing.

Suggested sequence:

1. BESCOM
2. BWSSB
3. Telecom
4. BBMP restoration

Alternative:
Coordinate BESCOM and BWSSB work into one controlled excavation window
where technically feasible.
```

AI must explain:

```text
same corridor
multiple excavation windows
risk of reopening restored pavement
reduced disruption from sequencing
```

The AI recommendation does not automatically alter schedules.

---

# 15. Coordination Decision

Synthetic coordinator:

```text
User: DEMO-BBMP-002
Decision: ACCEPT_WITH_MODIFICATION
```

Decision:

```text
1. BESCOM completes first.
2. BWSSB follows.
3. Telecom work is moved to the consolidated window.
4. BBMP restoration begins only after all utility completion confirmations.
```

New schedule:

```text
INT-001: Sep 10–12
INT-002: Sep 13–15
INT-005: Sep 15–16
INT-003: Sep 17–19
```

The exact schedule is synthetic and exists only to demonstrate the engine.

---

# 16. Coordination Conditions

Create conditions:

### Condition 1

```text
All utility works must confirm completion before restoration starts.
Owner: BESCOM / BWSSB / Telecom
```

### Condition 2

```text
Required restoration evidence must be uploaded after completion.
Owner: BBMP
```

### Condition 3

```text
Field inspection must occur before final closure.
Owner: BBMP Inspector
```

---

# 17. Approval Data

Create approvals:

```text
APR-001
Intervention: INT-001
Actor: BBMP
Status: APPROVED
```

```text
APR-002
Intervention: INT-002
Actor: BBMP
Status: APPROVED_WITH_CONDITIONS
```

```text
APR-003
Intervention: INT-005
Actor: BBMP
Status: PENDING
```

The demo should show the pending approval before coordination and the approved state after the coordinator resolves the conflict.

---

# 18. SLA Data

Example:

```text
SLA-001
Type: APPLICATION_REVIEW
Target: INT-001
Duration: 24h
Status: COMPLETED
```

```text
SLA-002
Type: COORDINATION_RESPONSE
Target: C002
Duration: 8h
Status: AT_RISK
```

```text
SLA-003
Type: RESTORATION
Target: INT-003
Duration: 72h
Status: ACTIVE
```

This gives the coordinator a visible operational priority.

---

# 19. SLA Escalation Demo

For C002:

```text
Remaining time: 1h 30m
State: AT_RISK
```

Generate:

```text
ESC-001
Reason: Coordination SLA approaching breach
Escalated to: Coordinator
```

Do not actually wait eight hours during the demo.

Seed the record with a simulated timeline.

---

# 20. Citizen Observation

Create:

```text
OBS-001
Citizen: DEMO-CIT-001
Road: R001
Category: ROAD_CUTTING
Description:
"Road has been dug up and the surface has not yet been restored."
```

Evidence:

```text
EV-001
Type: CITIZEN_PHOTO
```

Status:

```text
SUBMITTED
```

---

# 21. AI Classification

Expected:

```text
Category: ROAD_CUTTING
Subcategory: ACTIVE_EXCAVATION
Confidence: 0.94
```

This is advisory.

Citizen submission remains the authoritative source of the observation.

---

# 22. Citizen-to-Intervention Matching

The system should discover:

```text
OBS-001
   ↓
near INT-002
   ↓
same road R001
   ↓
time compatible
   ↓
candidate match
```

Result:

```text
MATCH_CONFIDENCE: HIGH
```

An authorized officer confirms the association.

---

# 23. Execution Simulation

After coordination:

```text
INT-001
status → IN_PROGRESS
```

Then:

```text
INT-001
status → COMPLETED_PENDING_VERIFICATION
```

Evidence:

```text
EV-010
Type: WORK_COMPLETION
Agency: BESCOM
```

Then repeat for BWSSB and Telecom.

---

# 24. Restoration Simulation

BBMP restoration begins only after utility completion conditions are satisfied.

```text
INT-003
PLANNED
  ↓
APPROVED
  ↓
IN_PROGRESS
  ↓
COMPLETED_PENDING_VERIFICATION
```

---

# 25. Restoration Evidence

Create:

```text
EV-020
Type: BEFORE_RESTORATION
```

```text
EV-021
Type: DURING_RESTORATION
```

```text
EV-022
Type: AFTER_RESTORATION
```

Each should include synthetic:

```text
timestamp
GPS
uploader
```

---

# 26. AI Evidence Analysis

Expected advisory result:

```text
Observed:
Road surface appears restored.

Potential concern:
Patch boundary is visually distinguishable from surrounding pavement.

Confidence:
0.76

Recommendation:
Field inspection required.
```

The AI must not mark the intervention as verified.

---

# 27. Field Inspection

Inspector:

```text
DEMO-INSP-001
```

Inspection:

```text
INS-001
Target: INT-003
Status: COMPLETED
```

Checklist:

```text
PASS — work within approved area
PASS — excavation closed
PASS — road surface restored
PASS — debris removed
PASS — work zone cleared
```

Final result:

```text
PASSED
```

---

# 28. Citizen Verification

Send:

```text
Your reported road issue has been addressed.
Please confirm whether the issue appears resolved.
```

Citizen response:

```text
RESOLVED
```

Create:

```text
VER-001
Source: CITIZEN
Result: CONFIRMED
```

---

# 29. Closure

Final case state:

```text
CLOSED
```

Closure requirements:

```text
required approvals complete
utility work complete
restoration complete
inspection passed
required evidence present
citizen verification recorded or verification window expired
```

Create closure event:

```text
CASE_CLOSED
```

---

# 30. Complete Demo Timeline

```text
09:00
Citizen submits road observation.

09:01
Photo stored.

09:02
AI classifies observation.

09:03
System matches observation to R001 / INT-002.

09:05
Coordinator views road segment.

09:06
System detects multi-agency conflict.

09:07
AI generates coordination recommendation.

09:09
Coordinator reviews recommendation.

09:11
Coordinator accepts modified sequence.

09:12
Approval tasks generated.

09:15
Agencies receive actions.

09:20
BESCOM confirms completion.

09:25
BWSSB confirms completion.

09:30
Telecom confirms completion.

09:35
BBMP restoration starts.

09:45
Restoration evidence uploaded.

09:46
AI analyses evidence.

09:48
Inspector receives verification task.

09:55
Inspector completes checklist.

10:00
Citizen receives verification request.

10:05
Citizen confirms resolution.

10:06
Case closes.
```

These timestamps are simulated.

---

# 31. Negative Scenario — Unauthorized Road Cutting

Create a separate case:

```text
CASE-002
Road: R003
Agency: Synthetic Utility Contractor
Intervention: INT-006
Permission: NONE
```

Citizen reports:

```text
road excavation
```

Expected flow:

```text
observation
→ suspected unauthorized work
→ officer review
→ inspection
→ enforcement/escalation workflow
```

Do not automatically accuse the actor based solely on AI.

---

# 32. Negative Scenario — Duplicate Citizen Reports

Create:

```text
OBS-010
OBS-011
OBS-012
```

Same road:

```text
R001
```

Same issue:

```text
road excavation
```

Expected system behaviour:

```text
possible duplicate
```

The system should group/link observations while preserving each original record.

---

# 33. Negative Scenario — No Conflict

Use:

```text
INT-004
Road: R002
```

No overlapping intervention.

Expected:

```text
NO_CONFLICT
```

This is necessary to demonstrate that the conflict engine is selective.

---

# 34. Negative Scenario — Restoration Failure

Create:

```text
INT-007
Road: R003
Status: COMPLETED_PENDING_VERIFICATION
```

Inspector result:

```text
FAILED
```

Reason:

```text
surface remains uneven
```

Expected:

```text
verification failed
→ corrective action
→ restoration reopened
→ reinspection
→ eventual closure
```

---

# 35. Negative Scenario — Citizen Disagrees

After official verification:

```text
Citizen response:
STILL_UNRESOLVED
```

Create:

```text
Citizen verification dispute
```

Expected:

```text
case reopened / review workflow
```

according to the configured business rule.

Do not let the citizen directly change the official intervention state.

---

# 36. Seed Data Relationships

The main graph is:

```text
R001
 │
 ├── INT-001 BESCOM
 ├── INT-002 BWSSB
 ├── INT-005 Telecom
 └── INT-003 BBMP Restoration
        │
        ├── C001
        ├── C002
        └── C003
              │
              └── AI Recommendation
                     │
                     └── Coordination Decision
                            │
                            └── Approval
                                   │
                                   └── Execution
                                          │
                                          └── Evidence
                                                 │
                                                 └── Inspection
                                                        │
                                                        └── Citizen Verification
                                                               │
                                                               └── Closure
```

---

# 37. Required Seed Dataset Counts

Minimum MVP seed:

```text
Agencies:             7
Roles:                6+
Users:                7+
Road segments:        3
Interventions:        7
Cases:                3+
Conflicts:            3+
Dependencies:         5+
Approvals:            5+
SLA instances:        5+
Escalations:          1+
Citizen observations: 5+
Evidence records:     10+
Inspections:          2+
AI runs:              5+
AI recommendations:   2+
Notifications:        10+
Audit events:         30+
```

---

# 38. Demo Personas

## Citizen

```text
Name: Demo Citizen
Role: Citizen
Goal:
Report a road problem and later confirm resolution.
```

## Agency Officer

```text
Name: Demo BESCOM Officer
Role: Agency Officer
Goal:
Review assigned intervention and submit completion evidence.
```

## Coordinator

```text
Name: Demo Coordinator
Role: Cross-Agency Coordinator
Goal:
Resolve conflicting road work schedules.
```

## Inspector

```text
Name: Demo Inspector
Role: Field Inspector
Goal:
Verify restoration.
```

---

# 39. Demo Dashboard Initial State

When the application starts, the coordinator dashboard should immediately show:

```text
Active conflicts: 3
SLA at risk: 1
Pending approvals: 1
Upcoming interventions: 5
Verification pending: 1
Citizen observations: 3
```

This avoids an empty application during the SIH presentation.

---

# 40. Demo Map Initial State

The map should open around:

```text
Bengaluru
```

and show:

```text
R001
INT-001
INT-002
INT-003
INT-005
C001
```

Selecting R001 should open its operational history.

---

# 41. Demo Storyboard

## Scene 1 — Problem

Show:

```text
Multiple agencies
same road
different work schedules
```

## Scene 2 — Citizen

Submit:

```text
photo + location + report
```

## Scene 3 — Intelligence

Show:

```text
AI classification
+
intervention match
```

## Scene 4 — Conflict

Show:

```text
three agencies
+
road timeline
+
conflict
```

## Scene 5 — Recommendation

Show:

```text
AI recommendation
+
reasons
+
risks
```

## Scene 6 — Human Decision

Coordinator:

```text
accept / modify / reject
```

## Scene 7 — Execution

Show:

```text
agency progress
```

## Scene 8 — Evidence

Upload:

```text
before / during / after
```

## Scene 9 — Verification

Inspector:

```text
checklist
+
photos
+
result
```

## Scene 10 — Citizen

Citizen:

```text
confirm resolution
```

## Scene 11 — Closure

Show:

```text
complete lifecycle
+
audit trail
```

---

# 42. What This Demo Must Prove

The demo is successful if a reviewer can understand:

```text
WHO
did WHAT

WHERE
it happened

WHEN
it was planned

WHY
a conflict occurred

WHO
needed to coordinate

WHAT
CivicOS recommended

WHO
made the actual decision

WHAT
evidence proves completion

WHO
verified the result
```

---

# 43. What This Demo Must Not Claim

Do not claim:

```text
real-time integration with BBMP
real-time integration with BESCOM
real-time municipal data
official government approval
real AI accuracy statistics
production deployment
```

unless those integrations/data actually exist.

The demo should explicitly identify synthetic records.

---

# 44. Existing-System Positioning

The seed scenario intentionally starts from a reality where Bengaluru already has road-cutting software.

BBMP's public documentation describes MARCS as handling road-cutting requests, AEE verification, restoration cost, permission validity, demand notes, completion inspection and release of performance guarantees. citeturn0search0

The current public portal also exposes MARCS 3.0. citeturn0search5

Recent GBA directions have additionally emphasized mandatory road-cutting permission through the MARKS/MARCS platform and advance scheduling by departments. citeturn0search2turn0search6

Therefore the CivicOS demo should visibly differentiate itself through:

```text
CROSS-PROJECT VISIBILITY
+
CONFLICT DETECTION
+
SEQUENCE RECOMMENDATION
+
SHARED COORDINATION WORKSPACE
+
LIFECYCLE VERIFICATION
+
CITIZEN FEEDBACK
+
AUDITABLE CROSS-AGENCY HISTORY
```

---

# 45. Demo Data Rule

The seed data should be deterministic.

Running:

```text
flyway migrate
seed-demo
```

should produce the same scenario every time.

Use fixed:

```text
UUIDs
timestamps
status values
relationships
geometry
```

for the primary demo dataset.

---

# 46. Environment Separation

Use:

```text
dev
test
demo
prod
```

The synthetic dataset belongs to:

```text
demo
```

Never load demo records into production automatically.

---

# 47. Seed Execution

Recommended:

```text
Flyway
  ↓
schema migrations
  ↓
reference data
  ↓
demo data
```

Use a separate migration or controlled seed runner for demo data.

Example:

```text
V1__baseline_schema.sql
V2__reference_data.sql
V3__demo_agencies.sql
V4__demo_roads.sql
V5__demo_interventions.sql
V6__demo_conflicts.sql
V7__demo_workflow.sql
V8__demo_evidence.sql
V9__demo_ai.sql
```

Exact migration numbering must follow the final repository state.

---

# 48. Final End-to-End State

The flagship demo case should finish as:

```text
CASE-001
STATUS = CLOSED

Road:
R001

Citizen observation:
RESOLVED

Interventions:
COMPLETED

Conflicts:
RESOLVED

Coordination:
COMPLETED

Approvals:
COMPLETED

Evidence:
COMPLETE

Inspection:
PASSED

Citizen verification:
CONFIRMED

Audit:
COMPLETE
```

---

# 49. Final SIH Demonstration Message

The demo should communicate one simple proposition:

> **CivicOS does not replace the agencies or their existing systems. It connects the work around the same physical road, detects coordination risks before they become repeated digging or restoration failures, helps officials decide the sequence, and keeps evidence and verification attached to the entire lifecycle.**

That is the behaviour the final Codex implementation must reproduce.

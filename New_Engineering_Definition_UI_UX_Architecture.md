# CivicOS — Engineering Definition v1.7
## UI/UX Architecture

**Status:** Engineering baseline  
**Scope:** SIH MVP — road-cutting / digging coordination, conflict prevention and lifecycle verification  
**Backend:** Java/Spring Boot  
**Frontend:** Web application for citizens, agencies, coordinators, inspectors and administrators

---

# 1. UI/UX Objective

CivicOS should make a complicated municipal coordination process understandable without hiding its complexity.

The interface must answer five questions at all times:

1. **What is happening?**
2. **Who is responsible?**
3. **What must happen next?**
4. **Is anything blocking the work?**
5. **What evidence proves completion?**

The UI must therefore be workflow-first rather than dashboard-first.

---

# 2. Primary User Groups

CivicOS has five major interfaces/experiences:

```text
1. Citizen
2. Agency / Department Officer
3. Coordination Officer
4. Field Inspector
5. System Administrator
```

The application must use RBAC to determine what each actor can see and do.

---

# 3. Global Information Architecture

```text
CivicOS
│
├── Home
├── Cases
├── Interventions
├── Road Network
├── Conflicts
├── Approvals
├── SLAs
├── Evidence
├── Verification
├── Notifications
├── Reports
└── Administration
```

Citizen navigation should be substantially simpler:

```text
Report Issue
My Reports
Track Report
Notifications
Help
```

---

# 4. Core Design Principle

Do not make every user navigate the entire municipal workflow.

Instead:

```text
Citizen
→ simple reporting and tracking

Agency
→ assigned work and approvals

Coordinator
→ cross-agency coordination

Inspector
→ field verification

Admin
→ configuration and governance
```

Each interface exposes the minimum information required to perform the role.

---

# 5. Visual Design Principles

The interface should follow:

```text
clarity
consistency
accessibility
progressive disclosure
high information density for officers
low cognitive load for citizens
```

Avoid:

```text
decorative dashboards
excessive animation
large unexplained charts
AI-generated prose everywhere
hidden status information
```

---

# 6. Design System

Create a reusable design system containing:

```text
Typography
Spacing
Buttons
Inputs
Selects
Tables
Cards
Badges
Tabs
Dialogs
Drawers
Alerts
Toasts
Timeline
Map components
File upload
Evidence gallery
Status indicators
```

All screens should use these shared components.

---

# 7. Status Design

Every lifecycle status should have:

```text
label
description
visual indicator
next action
```

Example:

```text
COORDINATION_REQUIRED

What it means:
A conflict requires cross-agency coordination.

Next:
Review the affected interventions and proposed sequence.
```

Do not rely only on colour.

Every status must also contain text/icon semantics for accessibility.

---

# 8. Citizen Experience

## Citizen home

Primary actions:

```text
Report a Road Issue
Track My Report
View Nearby Works
```

Secondary:

```text
Notifications
Help
```

The citizen should reach reporting in one prominent action.

---

# 9. Citizen Reporting Flow

```text
1. Start report
2. Capture/upload photo
3. Confirm location
4. Select/confirm issue
5. Add description
6. Review
7. Submit
8. Receive tracking ID
```

Optional AI assistance:

```text
photo
→ suggested issue category
→ citizen confirms
```

The citizen must be able to correct the suggestion.

---

# 10. Citizen Report Screen

Fields:

```text
Photo
Location
Issue category
Description
Date/time
Optional additional photo
```

Display:

```text
AI suggested category
```

only as:

```text
Suggested: Road cutting
```

not:

```text
AI classified: Road cutting
```

The citizen retains control over the submitted description/category.

---

# 11. Location UX

Location confirmation should use:

```text
GPS
+
interactive map
+
address/road name
```

The citizen can adjust the map marker.

Show:

```text
Detected road
Approximate location
```

before submission.

---

# 12. Citizen Tracking

After submission:

```text
Report received
      ↓
Under review
      ↓
Matched to work
      ↓
Action assigned
      ↓
Work in progress
      ↓
Verification
      ↓
Completed
```

Use a vertical timeline.

Each state should show:

```text
date
status
responsible actor where appropriate
public-safe message
```

Do not expose internal information that is not intended for citizens.

---

# 13. Citizen Report Detail

Sections:

```text
Status
Location
Issue
Photos
Timeline
Latest update
```

Potential action:

```text
Confirm issue resolved
```

The citizen should not be required to understand agency names or internal approval mechanics.

---

# 14. Citizen Resolution Verification

When work reaches a verification stage:

```text
We believe this issue has been resolved.
```

Actions:

```text
Looks resolved
Still unresolved
Add evidence
```

If unresolved:

```text
reason
+
optional photo
```

This creates a new verification signal rather than directly reopening an official case without workflow rules.

---

# 15. Agency Dashboard

Agency officers need an operational dashboard.

Primary metrics:

```text
Assigned interventions
Pending actions
Approvals pending
SLA at risk
Active conflicts
Evidence pending
Verification pending
```

The dashboard should prioritize actions over analytics.

---

# 16. Agency Work Queue

Main table:

| Field | Purpose |
|---|---|
| Case | Case identifier |
| Road | Road segment |
| Intervention | Work type |
| Priority | Operational priority |
| SLA | Remaining time |
| Status | Current state |
| Next action | Required action |
| Conflict | Conflict indicator |

Actions:

```text
Open
Approve
Request information
Submit evidence
Respond to conflict
```

Only authorized actions appear.

---

# 17. Agency Intervention Detail

Header:

```text
Intervention ID
Status
Agency
Road
SLA
```

Tabs:

```text
Overview
Schedule
Dependencies
Conflicts
Approvals
Evidence
Verification
Timeline
Audit
```

---

# 18. Intervention Overview

Display:

```text
Purpose
Road segment
Location map
Agency
Contractor if applicable
Planned start
Planned end
Current status
Priority
```

Include a prominent:

```text
Next Action
```

panel.

---

# 19. Schedule View

Display:

```text
planned start
planned end
actual start
actual end
related interventions
```

Use a timeline/Gantt-style visualization where useful.

The user should immediately see:

```text
who is working
where
when
```

---

# 20. Conflict View

Conflict card:

```text
Conflict type
Severity
Affected road
Affected interventions
Detected at
Current status
```

Then:

```text
Why this conflict exists
```

Show deterministic facts:

```text
Same road segment
Overlapping dates
Resurfacing follows utility work
```

If AI recommendation exists:

```text
Recommended coordination
```

Clearly label it as:

```text
AI recommendation
```

---

# 21. Conflict Map

Map should display:

```text
road segment
intervention zones
conflict locations
```

Selecting a conflict highlights the relevant interventions.

The map is supplementary; a structured list must always exist for accessibility and usability.

---

# 22. Coordination Workspace

This is one of the most important screens.

Layout:

```text
┌─────────────────────────────────────────┐
│ Conflict / Coordination Summary         │
├───────────────────┬─────────────────────┤
│ Affected Work     │ Timeline / Map      │
│                   │                     │
│ Agency A          │ A ────────          │
│ Agency B          │      B ───────      │
│ Agency C          │          R ─────    │
├───────────────────┴─────────────────────┤
│ AI Recommendation                       │
│ Reasons / assumptions / risks            │
├─────────────────────────────────────────┤
│ Coordinator Decision                    │
└─────────────────────────────────────────┘
```

---

# 23. AI Recommendation UX

Never show AI output as an instruction.

Use:

```text
AI-assisted recommendation
```

Display:

```text
Recommendation
Why
Affected work
Assumptions
Potential risks
Confidence
```

Actions:

```text
Accept recommendation
Modify
Reject
Request more information
```

If accepted, the system creates a normal coordination decision.

---

# 24. Approval Screen

Approval page should answer:

```text
What am I approving?
Why?
What dependencies exist?
What conflicts exist?
What conditions apply?
What happens after approval?
```

Show:

```text
Intervention summary
Risk/conflict summary
Dependencies
Required evidence
SLA
Conditions
```

Actions:

```text
Approve
Reject
Return for correction
Request information
```

Rejection must require a reason.

---

# 25. Approval Conditions

If approval includes conditions:

```text
Condition
Owner
Due date
Status
```

Example:

```text
Restore road surface within required period.
Owner: Agency X
Due: 2026-06-20
Status: Pending
```

Conditions should appear in the intervention timeline and work queue.

---

# 26. SLA UX

SLA should be visible wherever action is required.

Example:

```text
SLA
8h 24m remaining
```

States:

```text
NORMAL
AT_RISK
BREACHED
PAUSED
COMPLETED
```

Do not communicate SLA only through colour.

---

# 27. Escalation UX

When an SLA becomes at risk:

```text
⚠ Action required

Approval has 2 hours remaining.
```

For escalation:

```text
Escalated to:
Role / responsible actor

Reason:
SLA breach

Escalated:
timestamp
```

---

# 28. Field Inspector Interface

This interface must be mobile-friendly.

Primary actions:

```text
My inspections
Start inspection
Capture evidence
Submit result
```

Avoid large administrative tables.

---

# 29. Inspection Flow

```text
Open inspection
↓
Confirm location
↓
Review expected work
↓
View required checklist
↓
Capture photos
↓
Record observations
↓
Pass / Fail / Needs correction
↓
Submit
```

---

# 30. Inspection Checklist

Example:

```text
□ Work completed in approved area
□ Road restored
□ Work zone cleared
□ No visible obstruction
□ Required evidence captured
```

Checklist items should be configurable.

---

# 31. Evidence Capture

Evidence screen:

```text
Required evidence
Uploaded evidence
Missing evidence
```

Each evidence item:

```text
Photo
Timestamp
Location
Uploaded by
Evidence type
```

Allow multiple images.

---

# 32. Evidence Gallery

Use:

```text
Before
During
After
Inspection
Citizen
```

tabs/filters.

Each image should display provenance.

---

# 33. Verification Screen

Show side-by-side where practical:

```text
Expected state
vs
Observed evidence
```

Include:

```text
inspection result
AI observations
citizen feedback
```

AI observations must be clearly separated from official findings.

---

# 34. Verification Decision

Actions:

```text
Verify complete
Reject completion
Request corrective work
Schedule reinspection
```

If rejected:

```text
reason required
```

This creates the next workflow transition.

---

# 35. Coordinator Command Center

This is the highest-density operational screen.

Main sections:

```text
At-risk SLAs
Active conflicts
Pending approvals
Upcoming road works
Verification backlog
Escalations
```

Primary visualization:

```text
map + operational queue
```

---

# 36. Coordinator Map

Layers:

```text
Road segments
Active interventions
Planned interventions
Conflicts
Work zones
Citizen reports
```

Filters:

```text
agency
status
date
conflict severity
SLA state
intervention type
```

Clicking an object opens a side panel rather than navigating away immediately.

---

# 37. Cross-Agency Timeline

Coordinator should be able to view:

```text
Road segment
      │
Agency A ──── work
Agency B        ─── work
Agency C             ───── resurfacing
```

Conflicts should visually connect the affected interventions.

---

# 38. Road Segment View

A road segment is a key spatial object.

Display:

```text
Road identity
Map
Current interventions
Upcoming interventions
Past interventions
Open conflicts
Recent citizen observations
```

This supports the core value proposition:

```text
What has happened?
What is happening?
What is planned?
```

---

# 39. Intervention History

Display chronological history:

```text
Created
Submitted
Approved
Scheduled
Started
Evidence uploaded
Inspection
Correction
Verified
Closed
```

Each event:

```text
timestamp
actor
action
reason/comment
```

---

# 40. Audit View

Authorized users can see:

```text
Actor
Action
Entity
Before
After
Timestamp
Reason
```

AI events should also appear:

```text
AI run
Recommendation
Human decision
```

Do not expose sensitive audit information to citizens.

---

# 41. Notifications

Notification center:

```text
Unread
All
```

Categories:

```text
Action required
Approval
SLA
Conflict
Evidence
Verification
Citizen update
System
```

Every actionable notification should link directly to the relevant task.

---

# 42. Task-Centric UX

A key principle:

```text
Notification
→ task
→ entity
→ action
```

Avoid:

```text
Notification
→ generic dashboard
```

Users should not have to hunt for the required action.

---

# 43. Search

Global search for authorized users:

```text
Case ID
Intervention ID
Road
Agency
Conflict ID
Observation ID
```

Citizen search:

```text
Tracking ID
```

Search results must respect RBAC.

---

# 44. Filtering

Operational lists need:

```text
status
agency
road
date range
priority
SLA
conflict
verification
```

Filters should be shareable/bookmarkable where practical.

---

# 45. Tables

Officer tables should support:

```text
sort
filter
pagination
column visibility
row actions
```

Do not load thousands of records into the browser.

Use server-side pagination.

---

# 46. Maps

Use the map for spatial understanding, not as the only navigation mechanism.

Every map object must have an accessible list/table equivalent.

Use clustering at low zoom levels.

Do not render every intervention as an independent marker when the dataset becomes large.

---

# 47. Responsive Design

Citizen experience:

```text
mobile-first
```

Officer experience:

```text
desktop-first
tablet-compatible
```

Inspector:

```text
mobile-first
```

Coordinator:

```text
desktop-first
```

---

# 48. Accessibility

Target:

```text
WCAG 2.2 AA
```

Requirements include:

```text
keyboard navigation
visible focus
semantic HTML
screen-reader labels
sufficient contrast
non-colour status communication
accessible forms
accessible tables
map alternatives
```

---

# 49. Error Handling UX

Errors must explain:

```text
what failed
what the user can do
whether the operation was saved
```

Example:

```text
Evidence upload failed.

Your inspection was saved.
You can retry the photo upload.
```

Avoid generic:

```text
Something went wrong.
```

---

# 50. Loading States

Use:

```text
skeletons
progress indicators
upload progress
```

For AI:

```text
Analysing evidence…
```

Do not leave users staring at a blank page.

---

# 51. AI Loading State

AI operations should communicate that the result is advisory.

Example:

```text
Analysing the submitted evidence…

This may take a few seconds.
AI results are advisory and may require review.
```

---

# 52. Empty States

Every list needs a useful empty state.

Example:

```text
No active conflicts.

All currently scheduled interventions are clear.
```

Avoid empty pages with no explanation.

---

# 53. Role-Based Home Screens

Citizen:

```text
Report
Track
Recent updates
```

Agency:

```text
My work
My approvals
My SLA
My evidence
```

Coordinator:

```text
Conflicts
Cross-agency work
SLA risks
Approvals
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
Rules
Configuration
Audit
```

---

# 54. Frontend Route Architecture

Recommended:

```text
/app
├── citizen
│   ├── report
│   ├── reports
│   └── reports/:id
│
├── agency
│   ├── dashboard
│   ├── interventions
│   ├── interventions/:id
│   ├── approvals
│   └── evidence
│
├── coordinator
│   ├── dashboard
│   ├── conflicts
│   ├── coordination
│   └── map
│
├── inspector
│   ├── inspections
│   └── inspections/:id
│
└── admin
    ├── users
    ├── roles
    ├── agencies
    └── configuration
```

---

# 55. Frontend Component Architecture

```text
components/
├── layout/
├── navigation/
├── forms/
├── tables/
├── maps/
├── timeline/
├── status/
├── evidence/
├── workflow/
├── approvals/
├── conflicts/
├── sla/
├── notifications/
└── ai/
```

AI-specific components should be reusable.

Example:

```text
AiRecommendationCard
AiConfidenceBadge
AiReasonList
AiWarning
```

---

# 56. State Management

Keep server state separate from local UI state.

Server state:

```text
cases
interventions
conflicts
approvals
evidence
notifications
```

Local state:

```text
modal open/closed
map selection
filter panel
form draft
```

Do not duplicate authoritative server state unnecessarily.

---

# 57. Forms

Every form must have:

```text
field labels
validation
server error handling
unsaved-change protection where appropriate
submission state
success state
```

Use the same validation rules conceptually as backend validation.

Backend remains authoritative.

---

# 58. File Upload UX

Upload:

```text
drag/drop
camera on mobile
file picker
```

Show:

```text
upload progress
success
failure
retry
```

Validate:

```text
file type
file size
number of files
```

---

# 59. Evidence Metadata

When possible, automatically capture:

```text
timestamp
GPS
uploader
evidence type
```

Allow correction only where policy permits.

Do not silently modify original evidence metadata.

---

# 60. Workflow UX Pattern

Every workflow screen should have:

```text
Current status
↓
Current owner
↓
Pending action
↓
Deadline
↓
Available transitions
```

This pattern should be consistent throughout CivicOS.

---

# 61. Action Confirmation

For consequential actions:

```text
Approve
Reject
Close
Escalate
```

show confirmation.

For rejection/closure:

```text
reason required
```

Avoid confirmation dialogs for trivial navigation.

---

# 62. Preventing Invalid Actions

The frontend should hide/disable actions based on permissions and state.

However:

```text
frontend authorization ≠ security
```

Backend authorization remains mandatory.

---

# 63. Citizen Privacy

Citizen-facing pages should not expose:

```text
internal notes
internal actor details
private contact information
internal AI prompts
sensitive audit records
```

Expose only public-safe status information.

---

# 64. Dashboard Design Rule

Every dashboard metric should be actionable.

Bad:

```text
1,842 interventions
```

Better:

```text
17 interventions require action
```

Clicking the metric should open the filtered queue.

---

# 65. Critical Operational Dashboard

Coordinator dashboard priority:

```text
1. SLA breaches
2. High-severity conflicts
3. Pending coordination decisions
4. Pending approvals
5. Verification backlog
6. Upcoming work
```

The most urgent operational problem should be visually dominant.

---

# 66. Demo UX

For the SIH demonstration, the complete scenario should be executable from a visible UI:

```text
Citizen report
→ matching
→ conflict
→ recommendation
→ coordination
→ approval
→ execution
→ evidence
→ inspection
→ citizen verification
→ closure
```

Avoid requiring database manipulation during the demo.

---

# 67. UI Acceptance Criteria

UI architecture is complete when:

- each role has a defined workspace
- every workflow state has a visible representation
- every actionable state exposes the next action
- SLA status is visible
- conflict relationships are understandable
- maps have non-map alternatives
- evidence provenance is visible
- AI recommendations are clearly labelled
- approval/rejection actions are explicit
- citizen tracking is understandable
- inspection works on mobile
- RBAC controls UI actions
- backend remains authoritative
- accessibility requirements are defined
- loading/error/empty states exist
- the complete SIH scenario can be demonstrated end-to-end

---

# 68. Final UX Principle

CivicOS should feel like:

```text
A municipal operations system
with an intelligent coordination layer
```

not:

```text
A chatbot
with a municipal dashboard attached.
```

The primary UI value is **workflow visibility, accountability and coordination**.

AI should enhance that experience without becoming the interface itself.

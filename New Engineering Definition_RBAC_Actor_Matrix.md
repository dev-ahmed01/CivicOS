# CivicOS — Engineering Definition
## RBAC, Actor Matrix & Authorization Model

**Version:** 1.4  
**Status:** Engineering baseline  
**Scope:** SIH MVP — multi-agency road-cutting / digging coordination and lifecycle verification

---

# 1. Purpose

This document defines exactly **who can do what** inside CivicOS.

It is the authorization layer between the domain engines and the user.

CivicOS already defines:
- human-in-the-loop decisions,
- explicit workflow transitions,
- auditability,
- evidence-first verification,
- separation of duties,
- citizen reporting without citizen authority over official interventions,
- and backend-enforced authorization.

The RBAC model therefore cannot be a simple:

```text
ADMIN = everything
USER = limited
```

Authorization must consider:

```text
Identity
+
Role
+
Agency
+
Jurisdiction
+
Resource ownership
+
Task assignment
+
Workflow state
+
Permission
+
Separation-of-duties rules
```

---

# 2. Core Authorization Formula

An action is allowed only when:

```text
Authenticated
    AND
Active User
    AND
Required Permission
    AND
Valid Role
    AND
Valid Organisation/Jurisdiction
    AND
Valid Resource Scope
    AND
Valid Workflow State
    AND
No Separation-of-Duties Violation
    AND
Business Preconditions Satisfied
```

Example:

```text
Inspector
+
VERIFY_INTERVENTION
+
Assigned to intervention
+
VERIFICATION_PENDING
+
Not creator where separation rule applies
=
ALLOWED
```

---

# 3. Actors

## 3.1 CITIZEN

Purpose:

Provide ground-level observations and visible outcome validation.

Can:

- create citizen observations
- upload photographs
- provide location
- describe visible road issues
- report visible excavation
- report incomplete restoration
- view permitted public information
- validate visible outcome where enabled
- view their own submissions and status

Cannot:

- create official interventions directly
- approve
- reject
- schedule official work
- modify agency records
- assign government tasks
- close interventions
- certify statutory compliance
- override conflict decisions

Citizen observations deliberately remain separate from official intervention authority.

---

# 4. AGENCY_USER

Represents an operational user belonging to an agency or authority.

Examples:

```text
BWSSB user
BESCOM user
BBMP user
Telecom/OFC agency user
Other authorised utility/authority user
```

Can:

- create agency interventions
- edit own agency draft interventions
- submit interventions
- view relevant conflicts
- respond to coordination tasks
- submit schedule proposals
- upload work evidence
- start work when authorised
- record completion
- submit restoration evidence
- view assigned tasks
- respond to SLA actions

Cannot by default:

- approve own intervention
- verify own work where separation applies
- change another agency's authoritative intervention
- override a blocking conflict
- change system policy
- alter immutable audit records

---

# 5. ENGINEER

Purpose:

Technical review.

Can:

- inspect intervention technical data
- review geometry
- review proposed methodology
- review dependencies
- request changes
- perform technical analysis
- create technical tasks
- submit technical recommendation
- review evidence relevant to technical conditions

Cannot by default:

- approve own technical submission
- close intervention
- alter audit history
- override statutory authority

---

# 6. COORDINATOR

Purpose:

Cross-agency coordination.

This is one of the most important CivicOS roles.

Can:

- view relevant interventions across participating agencies
- view conflicts
- review dependency graphs
- review recommendations
- create coordination tasks
- assign coordination tasks
- request schedule changes
- facilitate cross-agency coordination
- mark coordination as complete when conditions are satisfied
- escalate unresolved conflicts
- record coordination decisions
- request re-analysis

Cannot by default:

- approve statutory work unless separately assigned the APPROVER role
- modify another agency's authoritative project fields without delegated authority
- verify physical completion
- edit immutable audit history

---

# 7. INSPECTOR

Purpose:

Physical/field verification.

Can:

- receive inspection tasks
- inspect intervention site
- upload inspection evidence
- record inspection result
- accept/reject configured evidence
- mark verification PASS/FAIL/CONDITIONAL
- create corrective tasks
- request reinspection
- support reopening after valid evidence

Cannot by default:

- approve the original intervention
- approve their own work
- modify official schedule without authority
- close an intervention directly
- edit prior inspection records

---

# 8. CONTRACTOR

Purpose:

Execute assigned physical work.

Can:

- view assigned interventions
- view approved work scope
- view approved execution window
- acknowledge task
- record work start
- upload execution evidence
- upload progress evidence
- record completion
- upload restoration evidence
- respond to correction tasks

Cannot:

- approve intervention
- approve schedule independently
- resolve conflicts authoritatively
- verify own work
- close intervention
- modify approved scope

---

# 9. APPROVER

Purpose:

Make an authoritative decision.

Can:

- review approval package
- approve
- reject
- return for correction
- approve with conditions
- approve closure where configured
- view conflicts and recommendations
- review audit trail
- record decision reason

Cannot by default:

- approve an intervention they created
- approve their own recommendation where separation applies
- edit historical evidence
- verify their own physical work
- alter audit history

---

# 10. VERIFICATION_OFFICER

Where the organisation requires a separate verification authority, this role may be used instead of or in addition to INSPECTOR.

Can:

- review inspection
- confirm verification
- reject verification
- request reinspection
- approve closure conditions where configured

Cannot:

- certify work they personally performed where separation rules prohibit it.

---

# 11. ADMIN

Purpose:

System administration, not operational authority.

Can:

- manage users
- manage roles
- manage agency membership
- manage permissions
- configure policies
- configure SLA rules
- configure approval chains
- configure escalation rules
- configure intervention types
- manage reference data
- view audit records
- manage system settings

Cannot by default:

- silently override an operational decision
- delete audit events
- approve arbitrary interventions merely because they are ADMIN

Administrative override must be a separate explicit permission.

---

# 12. SYSTEM

The system is not a human role.

It can perform deterministic automation such as:

- create SLA
- calculate deadline
- detect conflict
- generate notification
- create task
- transition state where the workflow explicitly permits system transition
- close applicable SLA
- record system event
- run scheduled analysis
- trigger re-analysis
- generate recommendation

System cannot:

- make statutory approval
- make legal determinations
- override human authority
- approve work because an SLA expired

---

# 13. Permission Model

Permissions should be action-oriented.

## Identity

```text
USER_VIEW
USER_CREATE
USER_UPDATE
USER_DISABLE
ROLE_ASSIGN
AGENCY_ASSIGN
```

## Roads

```text
ROAD_VIEW
ROAD_CREATE
ROAD_UPDATE
ROAD_GEOMETRY_UPDATE
ROAD_SEGMENT_VIEW
ROAD_SEGMENT_UPDATE
```

## Interventions

```text
INTERVENTION_VIEW
INTERVENTION_CREATE
INTERVENTION_UPDATE
INTERVENTION_SUBMIT
INTERVENTION_ASSIGN
INTERVENTION_SCHEDULE
INTERVENTION_START
INTERVENTION_COMPLETE
INTERVENTION_CANCEL
INTERVENTION_HOLD
INTERVENTION_RESUME
INTERVENTION_REOPEN
```

## Conflict

```text
CONFLICT_VIEW
CONFLICT_ANALYSE
CONFLICT_ACKNOWLEDGE
CONFLICT_ASSIGN
CONFLICT_RESOLVE
CONFLICT_ESCALATE
```

## Coordination

```text
COORDINATION_VIEW
COORDINATION_TASK_CREATE
COORDINATION_TASK_ASSIGN
COORDINATION_UPDATE
COORDINATION_COMPLETE
COORDINATION_ESCALATE
```

## Approval

```text
APPROVAL_VIEW
APPROVAL_REQUEST
APPROVAL_APPROVE
APPROVAL_REJECT
APPROVAL_RETURN
APPROVAL_CONDITIONAL
CLOSURE_APPROVAL
```

## Evidence

```text
EVIDENCE_VIEW
EVIDENCE_UPLOAD
EVIDENCE_REVIEW
EVIDENCE_ACCEPT
EVIDENCE_REJECT
```

## Verification

```text
INSPECTION_CREATE
INSPECTION_VIEW
INSPECTION_COMPLETE
VERIFICATION_PASS
VERIFICATION_FAIL
REINSPECTION_REQUEST
```

## SLA

```text
SLA_VIEW
SLA_MANAGE
SLA_ESCALATE
SLA_OVERRIDE
```

## Citizen

```text
OBSERVATION_CREATE
OBSERVATION_VIEW_OWN
OBSERVATION_VIEW_RELEVANT
OBSERVATION_TRIAGE
OBSERVATION_MATCH
CITIZEN_VALIDATION_CREATE
```

## Administration

```text
POLICY_VIEW
POLICY_MANAGE
AUDIT_VIEW
ADMIN_OVERRIDE
INTEGRATION_MANAGE
```

---

# 14. Role-to-Permission Summary

| Capability | Citizen | Agency | Engineer | Coordinator | Contractor | Inspector | Approver | Admin |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Create observation | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Create intervention | — | ✓ | ✓* | — | — | — | — | ✓ |
| Submit intervention | — | ✓ | ✓ | — | — | — | — | ✓* |
| Technical review | — | — | ✓ | ✓* | — | — | ✓* | ✓* |
| View conflicts | Public* | ✓ | ✓ | ✓ | Assigned* | ✓ | ✓ | ✓ |
| Resolve coordination | — | — | — | ✓ | — | — | ✓* | ✓* |
| Upload work evidence | ✓* | ✓ | ✓* | ✓* | ✓ | ✓ | ✓* | ✓ |
| Start work | — | ✓* | — | — | ✓* | — | — | ✓* |
| Complete work | — | ✓* | — | — | ✓* | — | — | ✓* |
| Inspect | — | — | — | — | — | ✓ | ✓* | ✓* |
| Verify | — | — | — | — | — | ✓ | ✓* | ✓* |
| Approve | — | — | — | — | — | — | ✓ | ✓* |
| Reject | — | — | — | — | — | — | ✓ | ✓* |
| Close | — | — | — | — | — | — | —* | ✓* |
| Configure policy | — | — | — | — | — | — | — | ✓ |
| View audit | Own* | Relevant* | Relevant* | Relevant* | Relevant* | Relevant* | Relevant* | ✓ |

`*` = only when the user also possesses a separate permission/delegation.

---

# 15. Resource-Level Access

Role alone is insufficient.

Use four scopes.

## Scope 1 — Own

User can access resources they created.

Example:

```text
Citizen → own observations
```

## Scope 2 — Assigned

User can access resources assigned to them.

Example:

```text
Inspector → assigned inspections
Contractor → assigned interventions
```

## Scope 3 — Agency

User can access resources belonging to their organisation.

Example:

```text
BWSSB user → BWSSB interventions
```

## Scope 4 — Jurisdiction / Cross-agency

Authorised coordination roles can access data required for cross-agency coordination.

Example:

```text
Coordinator → interventions on participating road segments
```

---

# 16. Cross-Agency Visibility

CivicOS exists specifically to coordinate multiple agencies.

Therefore a coordinator cannot operate with an agency-isolated view.

However:

```text
Visibility ≠ Edit authority
```

Example:

```text
BWSSB user
    ↓
Can view relevant BESCOM intervention
    ↓
Cannot modify BESCOM intervention
```

Coordinator:

```text
Can view both
    ↓
Can create coordination task
    ↓
Can propose sequence
    ↓
Cannot silently alter either agency's authoritative record
```

---

# 17. Separation of Duties

The following must be configurable but strongly supported.

## Rule SOD-001

Creator should not approve their own intervention.

```text
creator != approver
```

## Rule SOD-002

Reviewer should not approve the same decision when configured.

```text
reviewer != approver
```

## Rule SOD-003

Inspector should not be the final verifier of work they personally performed.

```text
inspector != executing contractor/user
```

## Rule SOD-004

Contractor cannot self-certify statutory completion.

## Rule SOD-005

AI cannot act as approver.

## Rule SOD-006

Citizen cannot become an approval authority merely through observation submission.

---

# 18. Workflow-Aware Authorization

Permission is checked against state.

Example:

```text
APPROVAL_APPROVE
+
APPROVAL_PENDING
=
potentially allowed
```

But:

```text
APPROVAL_APPROVE
+
IN_PROGRESS
=
DENIED
```

Another example:

```text
VERIFICATION_PASS
+
VERIFICATION_PENDING
+
assigned inspector
+
inspection complete
=
ALLOWED
```

---

# 19. Approval Matrix

| State | Actor | Action |
|---|---|---|
| DRAFT | Agency User | Edit |
| DRAFT | Agency User | Submit |
| SUBMITTED | Reviewer | Review |
| ANALYSIS | Engineer/System | Analyse |
| COORDINATION_REQUIRED | Coordinator | Coordinate |
| COORDINATION_IN_PROGRESS | Coordinator | Update |
| READY_FOR_APPROVAL | System | Create approval request |
| APPROVAL_PENDING | Approver | Approve |
| APPROVAL_PENDING | Approver | Reject |
| APPROVAL_PENDING | Approver | Return |
| APPROVED | Agency/Contractor | Schedule/prepare |
| SCHEDULED | Agency/Contractor | Start when authorised |
| IN_PROGRESS | Agency/Contractor | Complete |
| RESTORATION | Agency/Contractor | Submit restoration |
| VERIFICATION_PENDING | Inspector | Inspect |
| VERIFIED | System / configured authority | Close |
| CLOSED | Authorised authority | Reopen when valid |

---

# 20. Conflict Authorization

## Detection

System may detect conflicts automatically.

No human permission required for detection.

## Review

```text
ENGINEER
COORDINATOR
APPROVER
```

depending on configured policy.

## Resolution

Coordinator may resolve coordination conflicts after required agency actions.

## Blocking conflict

A blocking conflict prevents approval unless:

```text
resolved
```

or:

```text
authorised exception exists
```

## Exception

An override requires:

- explicit permission
- reason
- actor
- timestamp
- supporting evidence if required
- audit event

---

# 21. Schedule Change Authorization

A schedule change is not just an ordinary edit.

If a material change affects:

- execution window
- road segment
- geometry
- intervention type
- affected agency
- sequence

then:

```text
schedule change
      ↓
re-analysis
      ↓
possible re-coordination
      ↓
possible re-approval
```

The user may not bypass this by directly editing dates.

---

# 22. Evidence Authorization

## Contractor

Can upload execution evidence.

## Agency

Can upload official work evidence.

## Inspector

Can upload inspection evidence.

## Citizen

Can upload observation evidence.

## Evidence reviewer

Can accept/reject evidence.

## No user

May silently replace accepted evidence.

Replacement must create a new evidence version/record.

---

# 23. Verification Authorization

Verification requires:

```text
VERIFICATION_PENDING
+
verification permission
+
valid assignment/scope
+
required inspection
```

Inspector:

```text
PASS
FAIL
CONDITIONAL
REINSPECTION
```

System then performs the appropriate workflow transition.

---

# 24. Citizen Authorization Model

Citizen actions are intentionally constrained.

## Citizen may:

```text
OBSERVATION_CREATE
OBSERVATION_VIEW_OWN
CITIZEN_VALIDATION_CREATE
```

## Citizen cannot:

```text
INTERVENTION_APPROVE
INTERVENTION_REJECT
INTERVENTION_CLOSE
INTERVENTION_RESCHEDULE
CONFLICT_RESOLVE
SLA_OVERRIDE
POLICY_MANAGE
AUDIT_EDIT
```

Citizen feedback feeds official review rather than bypassing it.

---

# 25. Administrative Override

Administrative override is dangerous and therefore explicit.

Permission:

```text
ADMIN_OVERRIDE
```

Every override requires:

```text
actor
reason
target
previous state/value
new state/value
timestamp
evidence/reference where applicable
```

Example:

```text
ADMIN_OVERRIDE
    ↓
Audit Event
    ↓
Notification where configured
```

Admin must never be able to erase the fact that an override happened.

---

# 26. SLA Authorization

Normal users can:

```text
view SLA
respond to task
complete task
```

Only authorised roles can:

```text
pause SLA
resume SLA
override SLA
change policy
```

A user cannot avoid a breach by editing the due date.

---

# 27. Notification Authorization

Notifications may contain only information the recipient is authorised to see.

Example:

```text
Cross-agency conflict notification
```

must not expose restricted documents or unrelated agency data.

---

# 28. Audit Visibility

Audit is immutable, but visibility is role-dependent.

## Admin

Broad system audit visibility.

## Approver

Audit for decisions they must review.

## Coordinator

Audit for coordination-relevant resources.

## Inspector

Audit relevant to assigned inspections.

## Agency user

Audit for permitted agency resources.

## Citizen

Only their own observation/submission timeline and permitted public status.

---

# 29. API Authorization

Every API endpoint must declare:

```text
authentication
permission
resource scope
workflow condition
```

Example:

```text
POST /api/v1/interventions/{id}/approve
```

checks:

```text
authenticated
+
APPROVAL_APPROVE
+
Approver role
+
valid agency/jurisdiction
+
APPROVAL_PENDING
+
no blocking conflict
+
approval conditions satisfied
+
SOD valid
```

---

# 30. Backend Rule

Never trust:

```text
role
agency
userId
permissions
```

sent by the frontend.

The backend obtains identity from the authenticated security context and resolves effective permissions server-side.

---

# 31. Effective Permission Resolution

Recommended model:

```text
User
 ↓
User Roles
 ↓
Role Permissions
 ↓
Agency Membership
 ↓
Delegations
 ↓
Resource Scope
 ↓
Workflow Rules
 ↓
Effective Permission
```

Example:

```text
User: engineer01
Role: ENGINEER
Agency: BWSSB
Assignment: Intervention 1002
State: UNDER_REVIEW

Can:
VIEW intervention
TECHNICAL_REVIEW
ADD technical recommendation

Cannot:
APPROVE
VERIFY
CLOSE
```

---

# 32. Delegation

Support temporary delegation where necessary.

Example:

```text
Approver A
   ↓
Delegates approval authority
   ↓
Approver B
   ↓
valid from/to dates
```

Delegation must include:

- delegator
- delegate
- permission
- scope
- start
- expiry
- reason
- audit event

Expired delegation is automatically invalid.

---

# 33. Emergency Mode

Emergency workflows may relax normal rules, but never eliminate accountability.

Emergency action requires:

```text
emergency authority
+
reason
+
timestamp
+
scope
+
post-action review
```

Normal approval may be bypassed only where the configured emergency policy explicitly permits it.

AI still cannot become the authority.

---

# 34. API Response for Authorization Failure

Use consistent errors.

### Forbidden

```json
{
  "code": "FORBIDDEN",
  "message": "You are not authorised to perform this action.",
  "traceId": "..."
}
```

### Workflow denial

```json
{
  "code": "WORKFLOW_ACTION_NOT_ALLOWED",
  "message": "This action is not allowed in the current workflow state.",
  "traceId": "..."
}
```

### Separation-of-duties denial

```json
{
  "code": "SEPARATION_OF_DUTIES_VIOLATION",
  "message": "The current actor cannot perform this decision because of a separation-of-duties rule.",
  "traceId": "..."
}
```

---

# 35. Java Authorization Structure

Recommended:

```text
identity/
├── domain/
│   ├── User
│   ├── Role
│   ├── Permission
│   ├── AgencyMembership
│   └── Delegation
│
├── application/
│   ├── AuthorizationService
│   ├── PermissionService
│   ├── ScopeService
│   └── DelegationService
│
└── infrastructure/
    └── SpringSecurityAdapter
```

Workflow authorization remains a domain/application concern, not merely a Spring annotation.

---

# 36. Authorization Service

Conceptually:

```java
authorizationService.authorize(
    actor,
    Permission.APPROVAL_APPROVE,
    intervention
);
```

The service evaluates:

```text
permission
role
agency
jurisdiction
assignment
workflow
SOD
policy
```

---

# 37. Example: Approving a Road-Cutting Intervention

```text
Approver logs in
       ↓
GET intervention
       ↓
System verifies access
       ↓
Review conflicts
       ↓
Review recommendation
       ↓
Review evidence
       ↓
Submit approval command
       ↓
AuthorizationService
       ↓
APPROVAL_APPROVE?
       ↓
Correct agency/jurisdiction?
       ↓
APPROVAL_PENDING?
       ↓
Blocking conflict?
       ↓
SOD violation?
       ↓
Approval accepted
       ↓
Approval record
       ↓
State transition
       ↓
Audit event
       ↓
Notification
```

---

# 38. Example: Contractor Starting Work

```text
Contractor
    ↓
INTERVENTION_START
    ↓
Is assigned?
    ↓
Is intervention APPROVED?
    ↓
Is SCHEDULED?
    ↓
Is execution window valid?
    ↓
Are pre-work conditions satisfied?
    ↓
YES
    ↓
IN_PROGRESS
```

Otherwise:

```text
DENIED
```

---

# 39. Example: Citizen Attempting Approval

```text
Citizen
 ↓
POST /approve
 ↓
Authentication = valid
 ↓
Permission APPROVAL_APPROVE = absent
 ↓
403 FORBIDDEN
 ↓
No workflow mutation
 ↓
Audit/security event where configured
```

---

# 40. Example: Inspector Attempting to Verify Own Work

```text
Inspector
 ↓
VERIFICATION_PENDING
 ↓
VERIFY
 ↓
SOD rule
 ↓
Inspector is execution actor
 ↓
DENIED
```

The system should instruct the organisation to assign another authorised verifier.

---

# 41. RBAC vs ABAC

CivicOS should use:

```text
RBAC + contextual authorization
```

rather than pure RBAC.

RBAC answers:

> What kind of actor is this?

Contextual authorization answers:

> Can this actor perform this action on this specific resource right now?

This is required because agency, assignment, jurisdiction and workflow state matter.

---

# 42. Authorization Audit

Every privileged action should record:

```text
actorId
actorRole
agencyId
permission
resourceType
resourceId
decision
reason
timestamp
correlationId
```

For denied high-risk actions, optionally record:

```text
denialReason
```

---

# 43. Security Invariants

### AUTH-001

Frontend role data is never trusted.

### AUTH-002

No unauthenticated protected operation.

### AUTH-003

No approval without approval permission.

### AUTH-004

No verification without verification permission.

### AUTH-005

No cross-agency editing without explicit authority.

### AUTH-006

Citizen cannot perform official intervention decisions.

### AUTH-007

Contractor cannot approve or verify their own work.

### AUTH-008

Creator cannot approve their own work where SOD is enabled.

### AUTH-009

Expired delegation cannot grant access.

### AUTH-010

Admin privilege does not automatically equal operational approval authority.

### AUTH-011

Every administrative override is auditable.

### AUTH-012

Authorization must be enforced server-side.

### AUTH-013

A valid permission cannot bypass workflow state.

### AUTH-014

Workflow state cannot bypass permission.

### AUTH-015

AI output cannot grant permission.

---

# 44. Minimum MVP Role Set

For the SIH prototype, implement:

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

Do not create dozens of roles merely to appear enterprise-grade.

Additional roles should be introduced only when a real authorization distinction exists.

---

# 45. Recommended Demo Accounts

Seed demo users:

```text
citizen.demo
bbmp.agency
bwssb.agency
bescom.agency
coordinator.demo
engineer.demo
contractor.demo
inspector.demo
approver.demo
admin.demo
```

All demo accounts must clearly be marked as simulated.

---

# 46. Final Authorization Model

CivicOS authorization is:

```text
WHO
 ↓
Role
 ↓
WHAT
 ↓
Permission
 ↓
WHERE
 ↓
Agency / Jurisdiction
 ↓
WHICH RESOURCE
 ↓
Ownership / Assignment
 ↓
WHEN
 ↓
Workflow State
 ↓
CAN THEY DECIDE?
 ↓
Separation of Duties
 ↓
BUSINESS RULES
 ↓
ALLOW / DENY
```

The governing principle is:

> **Visibility should enable coordination, but visibility must never be confused with authority.**

That distinction is essential to CivicOS because the platform crosses agency boundaries while remaining inside existing institutional authority.

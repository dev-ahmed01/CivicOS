# CivicOS — Engineering Definition v1.6
## AI Architecture & Intelligence Layer

**Status:** Engineering baseline  
**Scope:** SIH MVP — road-cutting / digging coordination, conflict prevention and lifecycle verification  
**Backend:** Java 21+ / Spring Boot  
**AI integration:** Provider-agnostic AI Gateway  
**Principle:** AI assists; deterministic CivicOS services decide.

---

# 1. Purpose

This document defines how AI participates in CivicOS without becoming the authority over municipal workflow.

The database/API baseline already defines the authoritative lifecycle:

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

fileciteturn17file0L40-L57

The AI layer sits beside this lifecycle and provides:

```text
classification
+
extraction
+
matching
+
explanation
+
recommendation
+
summarisation
```

It must NOT directly perform:

```text
approval
+
rejection
+
deadline override
+
conflict resolution
+
closure
+
authorization
```

---

# 2. Core AI Principle

## AI is advisory, not authoritative.

The deterministic system remains responsible for:

- workflow transitions
- permissions
- SLA calculation
- conflict rules
- approval authority
- evidence requirements
- verification
- audit
- final state

This is consistent with the architecture requirement that AI failure must not become system failure.

---

# 3. AI Responsibilities

The MVP AI layer contains six major capabilities:

```text
AI-1  Citizen Observation Classification
AI-2  Entity / Intervention Matching
AI-3  Evidence Understanding
AI-4  Conflict Explanation
AI-5  Coordination Recommendation
AI-6  Case Summarisation
```

Optional later:

```text
AI-7  Document extraction
AI-8  Historical pattern analysis
AI-9  Natural-language municipal search
AI-10 Predictive risk scoring
```

Do not build these optional capabilities before the core workflow works.

---

# 4. AI Architecture

```text
                    ┌─────────────────────┐
                    │   CivicOS Backend    │
                    └──────────┬──────────┘
                               │
                         AI Gateway
                               │
              ┌────────────────┼────────────────┐
              │                │                │
        Classification     Vision/Image     Recommendation
              │                │                │
              └────────────────┼────────────────┘
                               │
                         AI Provider(s)
                               │
                         Structured JSON
                               │
                         Validation Layer
                               │
                     Human/Rule Confirmation
                               │
                     Authoritative Domain
```

---

# 5. AI Gateway

Create a provider-neutral Java interface.

```java
public interface AiGateway {

    ClassificationResult classify(
        ClassificationRequest request
    );

    EvidenceAnalysisResult analyseEvidence(
        EvidenceAnalysisRequest request
    );

    ConflictExplanation explainConflict(
        ConflictExplanationRequest request
    );

    CoordinationRecommendation recommendCoordination(
        CoordinationRecommendationRequest request
    );

    CaseSummary summarizeCase(
        CaseSummaryRequest request
    );
}
```

The rest of CivicOS must depend on this abstraction rather than a vendor SDK.

---

# 6. Provider Adapter

Implement:

```text
AiGateway
   │
   ├── OpenAiGateway
   ├── GeminiGateway        [optional]
   └── MockAiGateway        [mandatory for tests/demo]
```

The exact production provider is configuration.

Do not hard-code the application to one model provider.

---

# 7. AI Model Selection

The application should select models by task, not by a single global model.

Example configuration:

```yaml
ai:
  classification:
    provider: primary
    model: <configured-model>

  vision:
    provider: primary
    model: <configured-vision-model>

  recommendation:
    provider: primary
    model: <configured-reasoning-model>
```

Do not commit provider/model names into domain logic.

---

# 8. AI-1 — Citizen Observation Classification

Input:

```text
citizen description
+
image(s)
+
GPS
+
timestamp
```

Possible output:

```json
{
  "category": "ROAD_CUTTING",
  "subCategory": "UNRESTORED_DIGGING",
  "severity": "MEDIUM",
  "confidence": 0.91,
  "reasoningSummary": "Image appears to show an excavation area on a road surface."
}
```

AI does not create the authoritative category directly.

Instead:

```text
AI prediction
→ validation
→ stored as advisory result
→ system/official confirms
```

---

# 9. Classification Categories

Initial taxonomy:

```text
ROAD_CUTTING
ROAD_DAMAGE
UNRESTORED_EXCAVATION
DRAINAGE_WORK
UTILITY_WORK
TRAFFIC_OBSTRUCTION
DUPLICATE_REPORT
OTHER
```

The taxonomy must be configurable.

---

# 10. AI-2 — Entity / Intervention Matching

Purpose:

Determine whether a citizen report may correspond to an existing intervention.

Inputs:

```text
observation location
observation timestamp
observation category
image analysis
nearby interventions
road segment
agency
intervention schedule
```

Output:

```json
{
  "candidateInterventionId": "...",
  "matchConfidence": 0.94,
  "matchReasons": [
    "same road segment",
    "spatial overlap",
    "same intervention category",
    "observation occurred during planned work"
  ]
}
```

The system may suggest:

```text
MATCHED
```

but an official/domain rule determines whether the observation is actually linked.

---

# 11. Matching Must Use Deterministic Signals

AI should not be the sole matching mechanism.

Use:

```text
spatial proximity
+
time overlap
+
category compatibility
+
agency
+
road segment
```

Then AI can explain or rank candidates.

Preferred architecture:

```text
PostGIS candidate retrieval
        ↓
Deterministic filtering
        ↓
AI ranking/explanation
        ↓
official/system confirmation
```

---

# 12. AI-3 — Evidence Understanding

Evidence may include:

```text
before photo
work-progress photo
after photo
restoration photo
inspection photo
citizen photo
document
```

AI may extract:

```text
visible road surface
excavation presence
restoration appearance
possible obstruction
possible mismatch with expected evidence
```

Output:

```json
{
  "observations": [
    "road surface appears restored",
    "visible patch differs from surrounding surface"
  ],
  "possibleIssues": [
    "surface quality may require inspection"
  ],
  "confidence": 0.76
}
```

AI does not declare:

```text
RESTORATION_ACCEPTED
```

That requires the verification workflow.

---

# 13. Evidence Provenance

Every AI result must reference:

```text
evidence ID
AI run ID
model
provider
timestamp
prompt/template version
confidence
```

The database already contains `ai_runs` and `ai_recommendations` specifically for auditable AI outputs.

---

# 14. AI-4 — Conflict Explanation

The Conflict Detection Engine remains deterministic.

Example:

```text
Intervention A
Road segment R
June 10–12

Intervention B
Road segment R
June 13–15

Resurfacing
June 16
```

The deterministic engine identifies:

```text
REPEAT_EXCAVATION / SEQUENCING RISK
```

AI then explains:

```text
Why is this a problem?
What sequence creates less disruption?
What assumptions are being made?
Which agencies should coordinate?
```

---

# 15. AI Must Not Detect Critical Conflicts Alone

Critical conflict detection must come from:

```text
PostGIS
+
temporal rules
+
dependency rules
+
policy engine
```

AI can discover additional candidate issues, but those are:

```text
AI_CANDIDATE
```

until confirmed.

---

# 16. AI-5 — Coordination Recommendation

This is the most important AI-assisted feature.

Input:

```text
conflicting interventions
road geometry
planned dates
dependencies
agencies
constraints
approval status
SLA information
```

Output:

```json
{
  "recommendedSequence": [
    {
      "interventionId": "A",
      "start": "...",
      "end": "..."
    },
    {
      "interventionId": "B",
      "start": "...",
      "end": "..."
    }
  ],
  "recommendation": "Coordinate utility work before final resurfacing.",
  "reasons": [
    "same road segment",
    "overlapping work zone",
    "resurfacing currently scheduled between interventions"
  ],
  "risks": [
    "schedule dependency may delay resurfacing"
  ],
  "confidence": 0.88
}
```

---

# 17. Recommendation Safety Rule

AI recommendation:

```text
RECOMMENDATION
```

does NOT mean:

```text
DECISION
```

Workflow:

```text
AI recommendation
       ↓
Rule validation
       ↓
Coordinator review
       ↓
Coordination decision
       ↓
Approval if required
```

---

# 18. AI-6 — Case Summarisation

AI should create concise summaries for officers.

Example:

```text
CASE #CIV-2026-00124

Road: Example Road
Issue: Repeated excavation
Agencies: Agency A, Agency B
Current state: Coordination
Open conflict: Repeat excavation
SLA: 9 hours remaining
Pending action: Agency B schedule confirmation
Evidence: 4 items
Citizen observations: 3
```

The summary is derived from authoritative records.

It is never the source of truth.

---

# 19. AI Confidence

Every AI result must expose confidence where meaningful:

```text
0.00–0.49 LOW
0.50–0.74 MEDIUM
0.75–0.89 HIGH
0.90–1.00 VERY_HIGH
```

These bands are UI/triage guidance, not statistical guarantees.

Do not claim calibrated probability unless calibration testing has actually been performed.

---

# 20. Human Review Thresholds

Example policy:

```text
confidence >= 0.90
→ advisory auto-suggestion

0.70–0.89
→ human confirmation recommended

< 0.70
→ human review required
```

These thresholds must be configurable.

They must not authorize consequential actions automatically.

---

# 21. Structured AI Output

AI must return machine-readable structures.

Preferred:

```json
{
  "result": {},
  "confidence": 0.84,
  "warnings": [],
  "assumptions": [],
  "sourceReferences": []
}
```

Do not parse free-form prose to perform workflow transitions.

---

# 22. JSON Schema Validation

Every model response must pass:

```text
JSON parsing
→ schema validation
→ semantic validation
→ policy validation
```

Invalid output:

```text
retry
→ fallback
→ human review
```

Never directly execute invalid AI output.

---

# 23. Prompt Architecture

Do not scatter prompts throughout Java code.

Use versioned templates:

```text
src/main/resources/ai/prompts/

classification/
  v1.txt

evidence/
  v1.txt

conflict/
  v1.txt

recommendation/
  v1.txt

summary/
  v1.txt
```

Each AI run stores:

```text
prompt version
model
provider
input reference
output
```

---

# 24. Prompt Rules

Prompts must explicitly tell the model:

```text
You are an advisory component.
Do not approve or reject municipal work.
Do not invent facts.
Use only supplied records.
Separate observed facts from inference.
State uncertainty.
Return the required JSON schema.
```

---

# 25. AI Context Assembly

Never send the entire database to a model.

Construct a bounded context.

Example:

```text
Case
+
current intervention
+
related road segment
+
conflicts
+
dependencies
+
relevant evidence
+
relevant approvals
+
relevant schedules
```

Only the minimum required information is sent.

---

# 26. Sensitive Data

Do not send unnecessary personal information to AI providers.

Prefer:

```text
user ID
```

over:

```text
name
phone
email
```

unless genuinely required.

Citizen personal data should not be included in prompts by default.

---

# 27. Prompt Injection Defense

Citizen descriptions and uploaded documents are untrusted input.

Treat all user-generated text as data.

The model must never interpret:

```text
"ignore your instructions"
```

or similar content as system instructions.

Use:

```text
system instructions
+
structured data fields
+
explicit untrusted-content delimiters
```

---

# 28. Image Analysis

For images:

```text
upload
→ object storage
→ malware/type validation
→ metadata extraction
→ AI vision analysis
→ structured result
```

AI should not receive arbitrary executable content.

Accepted MVP formats:

```text
JPEG
PNG
WEBP
```

Apply configured size limits.

---

# 29. AI Evidence Comparison

Where before/after evidence exists:

```text
BEFORE
+
AFTER
+
approved intervention scope
```

AI can identify visual differences.

Example:

```text
Before:
excavated road surface

After:
patched road surface

Potential concern:
patch boundary remains visible
```

Final acceptance still requires inspection/verification.

---

# 30. AI Recommendation Explainability

Every recommendation must expose:

```text
recommendation
reasons
supporting facts
assumptions
uncertainties
affected interventions
affected agencies
```

Do not display opaque:

```text
"AI says reschedule."
```

Display:

```text
"Recommended because..."
```

---

# 31. AI Auditability

Each AI execution creates:

```text
ai_runs
```

and, when applicable:

```text
ai_recommendations
```

Audit should capture:

```text
who requested AI operation
what entity was analysed
which model was used
which prompt version was used
result
confidence
whether human accepted/rejected it
```

---

# 32. AI Failure Behaviour

If AI provider fails:

```text
classification
→ mark AI unavailable
→ manual classification

recommendation
→ deterministic conflict remains
→ coordinator works manually

summary
→ show normal structured case data
```

The system must remain operational.

---

# 33. Provider Timeout

Example:

```text
AI timeout: 10–30 seconds
```

Exact value must be configuration.

On timeout:

```text
retry limited number of times
→ fallback provider if configured
→ manual path
```

Never block an approval because AI is unavailable.

---

# 34. Retry Rules

Do not retry every error.

Retry:

```text
timeout
temporary network error
provider 5xx
rate limit with backoff
```

Do not blindly retry:

```text
invalid request
authentication failure
schema-invalid repeated output
policy violation
```

---

# 35. Cost Control

Track:

```text
model
input tokens
output tokens
request count
estimated cost
latency
failure rate
```

Store operational metrics separately from authoritative domain data if needed.

Use smaller/cheaper models for:

```text
classification
summarisation
simple extraction
```

Reserve expensive reasoning models for:

```text
complex coordination recommendations
```

---

# 36. AI Rate Limits

Per user:

```text
configured request/minute
```

Per operation:

```text
classification limit
vision limit
recommendation limit
```

Prevent accidental AI loops.

---

# 37. Caching

Safe candidates:

```text
case summary
static policy explanation
same evidence analysis
```

Do not cache dynamic authoritative decisions.

Cache key should include:

```text
entity version
prompt version
model version
input hash
```

---

# 38. AI Security Boundary

AI provider must never receive:

```text
database credentials
service credentials
internal tokens
passwords
private keys
```

AI tools must be allowlisted.

---

# 39. Tool Calling

AI should not have arbitrary backend access.

If tool calling is introduced, expose narrow tools:

```text
getIntervention
getConflict
getRoadSegment
getDependencies
getEvidenceMetadata
```

Do not expose:

```text
executeSQL
changeStatus
approveIntervention
deleteEvidence
```

---

# 40. Recommendation Execution Boundary

AI can request:

```text
recommend schedule
```

It cannot invoke:

```text
schedule intervention
```

The application converts accepted recommendations into normal domain commands.

---

# 41. AI + Deterministic Engine Boundary

Correct:

```text
PostGIS
   ↓
candidate conflicts

Rule Engine
   ↓
validated conflict

AI
   ↓
explanation/recommendation

Coordinator
   ↓
decision

Workflow Engine
   ↓
state transition
```

Incorrect:

```text
AI
 ↓
approve
 ↓
database
```

---

# 42. AI Data Flow — Citizen Report

```text
Citizen
  ↓
Photo + description + location
  ↓
API
  ↓
Evidence storage
  ↓
AI classification
  ↓
Structured prediction
  ↓
Deterministic validation
  ↓
Observation record
  ↓
Officer confirmation
  ↓
Case/intervention workflow
```

---

# 43. AI Data Flow — Conflict

```text
Interventions
  ↓
PostGIS spatial analysis
  +
temporal analysis
  +
dependency rules
  ↓
Conflict
  ↓
AI explanation
  ↓
AI recommendation
  ↓
Coordinator
  ↓
Coordination Decision
```

---

# 44. AI Data Flow — Verification

```text
Evidence
  ↓
AI visual analysis
  ↓
Potential observations
  ↓
Inspector
  ↓
Inspection checks
  ↓
Verification result
  ↓
Workflow
```

AI never replaces the inspector in the MVP.

---

# 45. AI Endpoints

```http
POST /api/v1/ai/observations/{id}/classify
POST /api/v1/ai/evidence/{id}/analyse
POST /api/v1/ai/conflicts/{id}/explain
POST /api/v1/ai/conflicts/{id}/recommend
POST /api/v1/ai/cases/{id}/summarise
```

Long-running operations may return:

```json
{
  "jobId": "...",
  "status": "QUEUED"
}
```

---

# 46. AI Job Model

For asynchronous operations, create:

```text
ai_jobs
```

Fields:

```text
id
operation
entity_type
entity_id
status
attempt_count
provider
model
created_at
started_at
completed_at
error_message
```

Status:

```text
QUEUED
RUNNING
COMPLETED
FAILED
CANCELLED
```

---

# 47. AI Service Structure

Recommended Java package:

```text
com.civicos.ai
├── api
├── application
├── domain
│   ├── model
│   └── ports
├── infrastructure
│   ├── provider
│   ├── prompt
│   └── persistence
├── classification
├── evidence
├── conflict
├── recommendation
└── summary
```

---

# 48. Domain Ports

Example:

```java
public interface ClassificationService {
    ClassificationResult classify(
        CitizenObservation observation
    );
}
```

```java
public interface RecommendationService {
    RecommendationResult recommend(
        Conflict conflict
    );
}
```

---

# 49. AI Persistence

Existing database baseline already defines:

```text
ai_runs
ai_recommendations
```

These remain authoritative records of AI interaction, while asynchronous processing may additionally use `ai_jobs`.

---

# 50. AI Result Lifecycle

```text
REQUESTED
   ↓
RUNNING
   ↓
COMPLETED
   ↓
VALIDATED
   ↓
ACCEPTED / REJECTED / SUPERSEDED
```

An AI recommendation may be:

```text
generated
→ reviewed
→ accepted
→ converted into a domain decision
```

---

# 51. Human Acceptance

Store:

```text
accepted_by
accepted_at
```

when an official accepts a recommendation.

If rejected:

```text
rejected_by
rejected_at
reason
```

The existing `ai_recommendations` structure should be extended if those fields are not already represented elsewhere.

---

# 52. Model Versioning

Every AI result must be reproducible as far as practical.

Record:

```text
provider
model
model_version
prompt_version
temperature/configuration if relevant
input_hash
output_schema_version
```

---

# 53. AI Observability

Metrics:

```text
ai_requests_total
ai_requests_failed
ai_latency
ai_tokens_input
ai_tokens_output
ai_cost_estimate
ai_confidence_distribution
ai_human_acceptance_rate
ai_human_rejection_rate
```

Track by:

```text
operation
model
provider
```

---

# 54. Quality Evaluation

Before using AI in the demo, build an evaluation dataset.

Example:

```text
100 citizen observations
50 conflict examples
50 evidence examples
```

Measure:

```text
classification accuracy
precision
recall
false positives
false negatives
recommendation acceptance rate
```

Do not claim accuracy without testing.

---

# 55. Critical False Positive/Negative Rule

For safety-sensitive municipal workflow:

```text
false negative
```

may be more harmful than:

```text
false positive
```

depending on operation.

Therefore thresholds must be configured per AI operation.

---

# 56. AI Golden Dataset

Store anonymised test cases:

```text
tests/ai/golden/
├── observations/
├── evidence/
├── conflicts/
└── recommendations/
```

Each case should contain:

```text
input
expected structured result
acceptable variation
```

---

# 57. Mock AI Provider

Mandatory for development and tests.

Example:

```java
MockAiGateway
```

returns deterministic outputs.

This allows:

```text
CI
offline development
integration tests
Codex implementation
```

without consuming paid AI credits.

---

# 58. Development Mode

Configuration:

```yaml
ai:
  enabled: true
  provider: mock
```

Production:

```yaml
ai:
  enabled: true
  provider: primary
```

If disabled:

```text
all AI-assisted functions degrade gracefully
```

---

# 59. AI and SIH Demonstration

The strongest demo flow should be:

```text
1. Citizen reports road excavation.
2. AI classifies the observation.
3. System matches it to a nearby intervention.
4. Two agencies have scheduled work on the same road.
5. PostGIS/rules detect a conflict.
6. AI explains the conflict.
7. AI proposes a coordinated sequence.
8. Coordinator reviews it.
9. Approval workflow proceeds.
10. Work is executed.
11. Evidence is uploaded.
12. AI highlights possible restoration concerns.
13. Inspector verifies.
14. Citizen receives completion/validation request.
15. Case closes with complete audit trail.
```

This demonstrates AI as a coordination assistant rather than a generic chatbot.

---

# 60. What AI Does NOT Need to Do

Do not build:

```text
general chatbot
voice assistant
autonomous municipal officer
automatic approval bot
fully autonomous scheduling
```

These add complexity without strengthening the core problem.

---

# 61. AI Architecture Completion Criteria

AI architecture is ready for implementation when:

- provider abstraction exists
- mock provider exists
- structured output schemas exist
- prompt versions are defined
- AI runs are persisted
- recommendations are auditable
- confidence is represented
- human review boundaries are explicit
- AI cannot directly change authoritative workflow
- failure fallback exists
- timeout/retry rules exist
- cost metrics exist
- prompt injection defenses exist
- image handling is defined
- evaluation datasets are defined
- API endpoints are defined
- deterministic engines remain authoritative

---

# 62. Final Architecture

The intended intelligence model is:

```text
                 CIVICOS
                    │
        ┌───────────┴───────────┐
        │                       │
 Deterministic Core          AI Layer
        │                       │
        ├─ Workflow             ├─ Classification
        ├─ RBAC                 ├─ Vision
        ├─ SLA                  ├─ Matching
        ├─ PostGIS              ├─ Explanation
        ├─ Conflict Rules       ├─ Recommendation
        ├─ Approval             └─ Summarisation
        ├─ Verification
        └─ Audit
                │
                ▼
        AUTHORITATIVE STATE
```

**The AI layer makes CivicOS smarter. The deterministic core makes CivicOS trustworthy.**

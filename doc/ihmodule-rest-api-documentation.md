# IH Module REST API — Technical Reference

**Module:** `ihmodule` (Intelehealth FHIR Doctor UI API)  
**Document:** `docs/ihmodule-rest-api-documentation.md` (Markdown only)  
**OpenMRS base path:** `{openmrsBase}/ws/rest/v1/ihmodule/...`  
**Legacy form aliases:** `module/ihmodule/*.form` (same handlers)  
**Authentication:** OpenMRS session required on all endpoints (`401` if not authenticated)  
**Last updated:** 2026-05-21  

## Table of contents

1. [FuzzyPatientMatchRestController](#1-fuzzypatientmatchrestcontroller)
2. [PatientExchangeProxyRestController](#2-patientexchangeproxyrestcontroller)
3. [SourcePatientIdentifierRestController](#3-sourcepatientidentifierrestcontroller)
4. [Endpoint index](#4-endpoint-index-non-deprecated)
5. [Deprecated endpoints](#5-deprecated-endpoints-do-not-use)
6. [Related UI and configuration](#6-related-ui--configuration)

**Source classes (class-level JavaDoc mirrors this file):**

| Controller | Purpose |
|------------|---------|
| `FuzzyPatientMatchRestController` | Local OpenMRS fuzzy patient `$match` (FHIR R4) |
| `PatientExchangeProxyRestController` | Duplicate review, import upload, export, operator actions |
| `SourcePatientIdentifierRestController` | Upsert facility “Source Patient Id” (central FHIR logical id) |

**Excluded from sections 1–4 (deprecated):**

| Method | Path | Replacement / notes |
|--------|------|---------------------|
| `GET` | `/patient-exchange/cases/statistics` | Deprecated; legacy `patientExchangeDupStatistics.form` |
| `POST` | `/patient-exchange/mpi-local` | Use `POST .../duplicate-review/add-patient-candidate` |

---

## Common conventions

### URL prefix

```text
https://{host}/openmrs/ws/rest/v1/ihmodule/{path}
```

Example: `https://localhost/openmrs/ws/rest/v1/ihmodule/patient-exchange/pending`

### JSON error body (Patient Exchange & Source Patient Identifier)

```json
{
  "error": "Human-readable message"
}
```

### FHIR error body (Fuzzy Match)

`OperationOutcome` (FHIR R4), `Content-Type: application/fhir+json`.

### CSRFGuard

Paths under `/ws/rest/v1/ihmodule/` are intended for same-origin UI calls with session cookie (CSRFGuard-exempt in typical OpenMRS setups).

---

# 1. FuzzyPatientMatchRestController

**Class:** `org.openmrs.module.ihmodule.web.controller.rest.FuzzyPatientMatchRestController`  
**Service:** `FhirPatientMatchService`  
**Related doc:** [patient-fuzzy-match-api.md](./patient-fuzzy-match-api.md) (extended fuzzy-match reference)

## 1.1 Overview

In-process **FHIR R4 patient `$match`** against the **local OpenMRS** database. Does not call central FHIR/OpenCR. Returns a `Bundle` (`searchset`) of matching `Patient` resources with search scores and match-grade extensions.

**Typical uses:** import duplicate detection, duplicate-review candidate search, operator tooling.

## 1.2 Endpoints

| Method | REST path | Legacy `.form` alias |
|--------|-----------|----------------------|
| `POST` | `/patient/$match` | `module/ihmodule/patientFuzzyMatch.form` |
| Other | Same path | Returns `405 Method Not Allowed`, header `Allow: POST` |

## 1.3 Request

| Item | Value |
|------|--------|
| **Content-Type** | `application/fhir+json` or `application/json` |
| **Body** | FHIR R4 `Parameters` (preferred) or raw `Patient` |

### Parameters body (preferred)

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `resource` | `Patient` | Yes | Demographics / identifiers to match |
| `count` | integer | No | Page size |
| `offset` | integer | No | Pagination offset (default `0`) |
| `onlyCertainMatches` | boolean | No | If `true`, only `certain` grade matches |
| `resourceType` | string | No | Must be `Patient` if set |

### Minimum search criteria

At least one of: identifier, name, birthDate, telecom, address, gender — otherwise **400**.

### Example

```http
POST /openmrs/ws/rest/v1/ihmodule/patient/$match HTTP/1.1
Content-Type: application/fhir+json
Cookie: JSESSIONID=...

{
  "resourceType": "Parameters",
  "parameter": [
    { "name": "resource", "resource": { "resourceType": "Patient", "name": [{ "family": "Das", "given": ["Rani"] }], "birthDate": "2000-02-12" } },
    { "name": "count", "valueInteger": 20 },
    { "name": "offset", "valueInteger": 0 }
  ]
}
```

## 1.4 Response

| HTTP | Body |
|------|------|
| **200** | FHIR `Bundle` (`type: searchset`, `total`, `entry[]` with `Patient`, `search.mode=match`, `search.score`, match-grade extension) |
| **400** | `OperationOutcome` |
| **401** | `OperationOutcome` (`not-authenticated`) |
| **503** | `OperationOutcome` (e.g. fuzzy match disabled in config) |
| **500** | `OperationOutcome` |

**Content-Type:** `application/fhir+json;charset=UTF-8`

## 1.5 Configuration

Controlled by module properties / global properties (see `FuzzyPatientMatchConfigService`). If disabled, **503** `FHIR fuzzy patient match is disabled`.

---

# 2. PatientExchangeProxyRestController

**Class:** `org.openmrs.module.ihmodule.web.controller.rest.PatientExchangeProxyRestController`  
**Content-Type:** `application/json` (except export-created)

Serves duplicate-patient UI, patient JSON import, and created-patient export. Handlers write JSON directly to `HttpServletResponse` (OpenMRS 2.4 / Servlet 3 compatibility).

## 2.1 Duplicate review — queries

### GET Pending cases

| Item | Value |
|------|--------|
| **URL** | `GET /patient-exchange/pending` |
| **Legacy** | `GET module/ihmodule/patientExchangePending.form` |
| **Response** | `200` — JSON array of `MpiDuplicateReviewCaseSummaryDto` |

**Case summary fields (no large JSON blobs):**

| Field | Description |
|-------|-------------|
| `id` | DB id |
| `caseUuid` | Case UUID |
| `localPatientUuid` | Source / local patient UUID |
| `sourceBirthdate`, `sourceFamily`, `sourceGiven`, `sourceGenderCode`, `sourceTelecom` | Demographics snapshot |
| `candidateAddressSnapshot` | Address text snapshot |
| `candidateCount` | Number of candidates |
| `reviewStatus` | e.g. `PENDING` |
| `dateCreated` | Created timestamp |
| `sourceOfPatient` | Legacy; may be null |

**Cache:** `Cache-Control: no-store` on response.

---

### GET Candidates for a case

| Item | Value |
|------|--------|
| **URL** | `GET /patient-exchange/candidates?caseUuid={uuid}` |
| **Legacy** | `GET module/ihmodule/patientExchangeCandidates.form?caseUuid=...` |
| **Query** | `caseUuid` (required) |
| **Response** | `200` — `MpiDuplicateReviewCandidatesResponse` |
| **404** | Unknown `caseUuid` |

**Response shape:**

```json
{
  "openmrsCandidates": [ /* MpiDuplicateReviewCandidateDto[] */ ],
  "fhirCandidates": [ /* MpiDuplicateReviewCandidateDto[] */ ],
  "totalStoredCount": 0,
  "visibleCount": 0,
  "hiddenCount": 0
}
```

Candidates are split by `matchSource` (`openmrs` vs central/`fhir`). Each `MpiDuplicateReviewCandidateDto` includes `id`, `fhirPatientLogicalId`, `mpiIdentifierValue`, demographics, `matchScore`, `matchSource`, `matchType`, etc.

---

## 2.2 Duplicate review — actions

### POST Force sync / Register new patient

| Item | Value |
|------|--------|
| **URL** | `POST /patient-exchange/force-sync` |
| **Legacy** | `POST module/ihmodule/patientExchangeForceSync.form` |
| **Content-Type** | `application/json` |

**Request body:** `ForcePatientSyncRequest`

| Field | Required | Description |
|-------|----------|-------------|
| `resolvedBy` | Yes | Operator username (stored on case) |
| `caseUuid` | Conditional | If set, **Register new patient** from pending case |
| `patientUuid` | Conditional | Required if `caseUuid` omitted; used with legacy uuid-only path |

**Behavior:**

1. **`caseUuid` set** — `addPatientFromPendingCase`: creates/updates OpenMRS patient from case `outbound_bundle_json` (import snapshot), marks case **COMPLETED**. `patientUuid` optional (legacy local uuid hint).
2. **`caseUuid` omitted** — `forceSendPatientToCentralByUuid(patientUuid)`: **local OpenMRS upsert only** from FHIR snapshot built from existing row (no central FHIR POST in current implementation).

**Success response (200):**

```json
{
  "patientUuid": "...",
  "caseUuid": "...",
  "statusCode": "200",
  "message": "...",
  "response": "...",
  "status": "skipped"
}
```

`status: "skipped"` is present when the uuid-only path returns a skipped outcome (`statusCode` may be `"skipped"`).

| HTTP | When |
|------|------|
| **400** | Validation / bad request |
| **409** | Illegal state (e.g. case not PENDING) |
| **422** | `ResourceIsNotValid` (profile validation) |
| **500** | Unexpected error |

---

### POST Link and join (add patient from candidate)

| Item | Value |
|------|--------|
| **URL** | `POST /patient-exchange/duplicate-review/add-patient-candidate` |
| **Legacy** | `POST module/ihmodule/patientExchangeDuplicateReviewAddCandidate.form` |
| **Content-Type** | `application/json` |
| **UI label** | “Link And Join Patient” |

**Request body:** `DuplicateReviewCandidateActionRequest`

| Field | Required | Description |
|-------|----------|-------------|
| `caseUuid` | Yes | Pending case UUID |
| `candidateId` | Yes | `mpi_patient_duplicate_review_candidate.id` |
| `resolvedBy` | Yes | Operator username |

**Behavior:** Links import **source** patient data to the **existing** OpenMRS row for the selected candidate (enrich missing fields only — name, address, telecom, extensions). Marks case and all candidates **COMPLETED**. Requires `outbound_bundle_json` on the case and `intelehealth.fhir.patient.import.native.create.enabled=true`.

**Success response (200):**

```json
{
  "status": "ok",
  "caseUuid": "...",
  "candidateId": 123
}
```

| HTTP | When |
|------|------|
| **400** | Missing fields / invalid candidate |
| **409** | Case not PENDING |
| **500** | Server error |

---

### POST Skip duplicate review

| Item | Value |
|------|--------|
| **URL** | `POST /patient-exchange/duplicate-review/skip` |
| **Legacy** | `POST module/ihmodule/patientExchangeDuplicateReviewSkip.form` |
| **Content-Type** | `application/json` |

**Request body:** `DuplicateReviewCaseActionRequest`

| Field | Required | Description |
|-------|----------|-------------|
| `caseUuid` | Yes | Case UUID |
| `resolvedBy` | Yes | Operator username |
| `candidateId` | No | If set, skip **one candidate** only; case stays PENDING |

**Success response (200):**

```json
{
  "status": "ok",
  "caseUuid": "...",
  "scope": "case"
}
```

With `candidateId`:

```json
{
  "status": "ok",
  "caseUuid": "...",
  "candidateId": 456,
  "scope": "candidate"
}
```

---

## 2.3 Patient import upload

### POST Import upload

| Item | Value |
|------|--------|
| **URL** | `POST /patient-exchange/import-upload` |
| **Legacy** | `POST module/ihmodule/patientExchangeImportUpload.form` |
| **Content-Type** | `multipart/form-data` |

**Form fields:**

| Field | Required | Description |
|-------|----------|-------------|
| `file` | Yes | UTF-8 JSON file: single FHIR R4 `Patient` or `Bundle` with Patient entries |
| `locationUuid` | No | OpenMRS location UUID for identifier location (preferred identifier) |

**Success response (200):** `PatientUploadImportResponse`

| Field | Description |
|-------|-------------|
| `total` | Patients in file |
| `created` | Created count |
| `skipped` | Skipped count |
| `failed` | Failed count |
| `duplicateReviewDeferred` | Fuzzy duplicate → review queue |
| `items[]` | Per-patient results |

**Per-item `status` values:**

| Status | Meaning |
|--------|---------|
| `CREATED` | New OpenMRS patient; `createdId` = UUID |
| `SKIPPED` | Duplicate (identifier/demographic) |
| `DUPLICATE_REVIEW` | Fuzzy duplicate; `duplicateReviewCaseUuid` set |
| `FAILED` | Validation or save error; `message` has detail |

**Side effect:** Successful native `savePatient` may trigger async FHIR outbound sync via `PatientEventListener` (not part of HTTP response).

| HTTP | When |
|------|------|
| **400** | Missing file / parse error |
| **500** | Unexpected error |

---

## 2.4 Export created patients

### GET Export created patients

| Item | Value |
|------|--------|
| **URL** | `GET /patient-exchange/export-created?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd` |
| **Legacy** | `GET module/ihmodule/patientExchangeExportCreated.form` |
| **Query** | `startDate`, `endDate` (required, format `yyyy-MM-dd`) |

**Success response (200):**

| Item | Value |
|------|--------|
| **Content-Type** | `application/fhir+json` (attachment) |
| **Content-Disposition** | `attachment; filename="created-patients-{start}-to-{end}.json"` |
| **Body** | FHIR JSON export payload |

**Response headers:**

| Header | Description |
|--------|-------------|
| `X-Total-Patients` | Total in range |
| `X-Exported-Patients` | Exported count |
| `X-Validation-Failed-Patients` | Validation failures |

| HTTP | When |
|------|------|
| **400** | Invalid date format |
| **500** | Export failure |

---

# 3. SourcePatientIdentifierRestController

**Class:** `org.openmrs.module.ihmodule.web.controller.rest.SourcePatientIdentifierRestController`  
**Service:** `LocalPatientMpiUpdateService.upsertSourcePatientIdentifier`

## 3.1 Overview

Upserts the facility patient’s **Source Patient Id** identifier (central FHIR Patient logical id). Updates existing identifier or creates a new one (requires `locationUuid` when creating).

Identifier type name: **Source Patient Id** (see module constants).

## 3.2 Endpoint

| Item | Value |
|------|--------|
| **URL** | `POST /patient/source-identifier` |
| **Legacy** | `POST module/ihmodule/patientSourceIdentifier.form` |
| **Method** | `POST` |
| **Content-Type** | `application/json` |

## 3.3 Request body

`SourcePatientIdentifierUpdateRequest`

| Field | Required | Description |
|-------|----------|-------------|
| `patientUuid` | Yes | OpenMRS patient UUID |
| `identifierValue` | Yes | Central FHIR Patient logical id value |
| `locationUuid` | Conditional | Required when a **new** source identifier row is created |

**Example:**

```json
{
  "patientUuid": "dc53c0d2-b3aa-4229-a2ef-566f8bfe5566",
  "identifierValue": "central-patient-logical-id-123",
  "locationUuid": "8d6c993e-c2cc-11de-8d13-0010c6dffd0f"
}
```

## 3.4 Success response (200)

`SourcePatientIdentifierUpdateResponse`

| Field | Description |
|-------|-------------|
| `status` | e.g. `ok` |
| `patientUuid` | Patient UUID |
| `sourcePatientIdentifierValue` | Identifier value written |
| `operation` | `created` or `updated` |
| `identifierTypeUuid` | Patient identifier type UUID |
| `identifierTypeName` | e.g. `Source Patient Id` |
| `identifiers` | `PatientIdentifierSnapshot[]` — active identifiers after upsert |

## 3.5 Error responses

| HTTP | When |
|------|------|
| **401** | Not authenticated |
| **400** | Missing `patientUuid` / `identifierValue` / validation error |
| **404** | Patient not found (message contains `not found`) |
| **409** | Config / state conflict |
| **500** | Unexpected error |

---

# 4. Endpoint index (non-deprecated)

| # | Method | Path | Controller |
|---|--------|------|------------|
| 1 | `POST` | `/patient/$match` | FuzzyPatientMatchRestController |
| 2 | `GET` | `/patient-exchange/pending` | PatientExchangeProxyRestController |
| 3 | `GET` | `/patient-exchange/candidates` | PatientExchangeProxyRestController |
| 4 | `POST` | `/patient-exchange/force-sync` | PatientExchangeProxyRestController |
| 5 | `POST` | `/patient-exchange/duplicate-review/add-patient-candidate` | PatientExchangeProxyRestController |
| 6 | `POST` | `/patient-exchange/duplicate-review/skip` | PatientExchangeProxyRestController |
| 7 | `POST` | `/patient-exchange/import-upload` | PatientExchangeProxyRestController |
| 8 | `GET` | `/patient-exchange/export-created` | PatientExchangeProxyRestController |
| 9 | `POST` | `/patient/source-identifier` | SourcePatientIdentifierRestController |

---

# 5. Deprecated endpoints (do not use)

| Method | Path | Notes / replacement |
|--------|------|---------------------|
| `GET` | `/patient-exchange/cases/statistics` | Deprecated (`patientExchangeDupStatistics.form`). Still used by duplicate review UI; no replacement REST path yet. |
| `POST` | `/patient-exchange/mpi-local` | Use `POST /patient-exchange/duplicate-review/add-patient-candidate` |

---

# 6. Related UI & configuration

| UI / config | Notes |
|-------------|-------|
| `duplicatePatient.jsp` + `duplicatePatientReview.js` | Duplicate review proxy base |
| `patientImportExport.jsp` | Import upload UI |
| `ihmodule.fhir.event.listener.*` | Async FHIR after `savePatient` |
| `fhir_module.fhir` (published config) | Gates outbound FHIR sync |
| `intelehealth.fhir.patient.import.native.create.enabled` | Required for native import / link-and-join |

---

*Generated from source: `FuzzyPatientMatchRestController`, `PatientExchangeProxyRestController`, `SourcePatientIdentifierRestController` (ihmodule).*

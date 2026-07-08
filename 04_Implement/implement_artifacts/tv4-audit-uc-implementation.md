# TV4 (Lâm) — UC Implementation Audit vs 121UC Spec

**Date:** 2026-07-03
**Spec Source:** `function-spec-task-allocation-reprioritized-121uc.md`
**UC Owner:** TV4 — Lâm
**Total UCs Assigned:** 18 (UC-60 → UC-71, UC-77 → UC-82)
**Total Features:** MF-05 (12 UCs) + MF-07 (6 UCs)

---







## Executive Summary

| Metric | Count |
|---|---|
| UCs with full implementation | **0** |
| UCs with partial implementation | **3–4** (UC-77, UC-78, UC-80, UC-71 indirectly) |
| UCs with skeleton only | **1** (UC-60..UC-71 — `expert` package is empty shells) |
| UCs with NO implementation | **13** |
| Integration contracts published | **0 of 3** |
| Migration files for expert/map | **1 partial** (V1__init_schema.sql has table DDLs but no TV4-owned migration) |

---







## Critical Gaps

### 1. Package Structure Non-Compliant with Spec

**Spec requires:** `expert`, `expertverification`, `expertavailability`, `map`, `location`, `nearbycare`

**Actual codebase uses:** `com.carebridge.backend.expert`, `com.carebridge.backend.emergency`, `com.carebridge.backend.safety`, `com.carebridge.backend.triage`, `com.carebridge.backend.consultation`

| Spec Package | Actual State | Issue |
|---|---|---|
| `expert` (UC-60,61,62,63,64,69) | **EMPTY** — only `.gitkeep` files | Zero logic, zero entities, zero controllers |
| `expertverification` | **MISSING** | Does not exist as separate package |
| `expertavailability` | **MISSING** | Does not exist as separate package |
| `map` | **MISSING** | Does not exist as separate package |
| `location` | **MISSING** | Does not exist as separate package |
| `nearbycare` | **MISSING** | Does not exist as separate package |

**Actual packages that exist (not in spec):**

| Actual Package | UCs | Notes |
|---|---|---|
| `emergency` | UC-77, UC-80 | Partially implemented — BUT package is NOT under TV4 ownership |
| `safety` | UC-116–UC-121 | Implemented by someone else — this is TV5's domain per spec |
| `triage` | UC-72–UC-76 | Implemented — this is TV5's domain per spec |
| `consultation` | UC-? | **Deferred to V2** — should NOT exist per spec exclusion rules |

### 2. Integration Contracts: 0 of 3 Published

| Contract | Spec Owner | Actual Status | Consumer Impact |
|---|---|---|---|
| `ExpertBadgeReadPort` | TV4 → TV3 consumer | **DOES NOT EXIST** | TV3 cannot display verified expert badge on community answers |
| `RouteProvider` | TV4 | **DOES NOT EXIST** | No map routing capability for UC-79 |
| `EmergencyMapHandoff` | TV4 → TV5 consumer | **DOES NOT EXIST** | TV5 triage red-risk result cannot open emergency map for UC-74 |

### 3. Expert Profile Schema in V1 but No Java Implementation

V1__init_schema.sql has table definitions for:
- `expert_profiles` ✅ (schema exists)
- `expert_credentials` ✅ (schema exists)
- `expert_availability` ✅ (schema exists)
- `expert_location_shares` ✅ (schema exists)
- `expert_consultation_prices` ✅ (schema exists — **should NOT exist**, V2 deferred)
- `consultation_price_bands` ✅ (schema exists — **should NOT exist**, V2 deferred)
- `consultation_bookings` ✅ (schema exists — **should NOT exist**, V2 deferred)
- `consultation_sessions` ✅ (schema exists — **should NOT exist**, V2 deferred)
- `expert_reviews` ✅ (schema exists — **should NOT exist**, V2 deferred)
- `partner_expert_links` ✅ (schema exists — **should NOT exist**, V2 deferred)
- `care_facilities` ✅ (schema exists — correct, for UC-78)

But NO corresponding Java entity, repository, service, or controller exists for any of these tables.
# AI Triage V2 — Legacy Caller and Deprecation Inventory

Updated: 2026-08-06

## Decision

V1 is **not deleted**. Phase 8 full Flutter→Java→Python→PostgreSQL E2E could not run, and V1 remains
the public rollback path. The entries below are marked legacy to prevent new callers; removal requires
the exit gates at the end of this document.

| Legacy surface | Current callers/role | V2 replacement | Removal status |
|---|---|---|---|
| Python `app.graph` | V1 child/intake HTTP endpoints | `app.triage_v2.graph` | BLOCKED by public migration/E2E |
| Python `source_retriever` | V1 graph retrieval | `triage_v2.evidence_retrieval` | BLOCKED by V1 callers |
| Python `official_source_searcher` | Optional V1 realtime source path | no runtime web; verified local BM25 | BLOCKED by V1 callers; forbidden for V2 |
| Java `GeminiTriageClient` + adapter/dev stubs | legacy model-outcome port | Python structured extraction | DEPRECATED; blocked by dev/V1 wiring |
| Java `TriageGraphService` | `TriageService` one-shot/fallback support | canonical V2 rules + Python graph | DEPRECATED; blocked by V1 |
| Java `TriageService` / `/api/v1/triage/*` | Flutter, emergency return, history, expert handoff | internal V2 session API | USER-FACING; retain |
| Flutter `TriageService`, V1 screens/models | intake, result, history, continuation, emergency | typed internal V2 flow | USER-FACING; retain |

## Known external callers

- Flutter floating AI entry, symptom intake, result/history and continuation restore.
- Emergency map return flow.
- Consultation expert-handoff preview/share.
- Java consent, health-memory, lifecycle and persistence services.
- Dev port mock/stub configuration and V1 test suites.

## Removal exit gates

1. Full DB-backed Phase 8 E2E and chaos pass in a disposable environment.
2. Source verification, retention/deletion and GREEN/public release blockers resolved or explicitly
   accepted for a non-GREEN launch.
3. V2 internal shadow comparison window completed with reviewed mismatch thresholds.
4. Mobile route migration and rollback drill completed.
5. Caller search returns only documented compatibility adapters.
6. Deprecation window/release note approved; no production migration is performed from this repository
   session.

## Rollback

Keep `CAREBRIDGE_TRIAGE_V2_INTERNAL_ENABLED=false`, `CAREBRIDGE_TRIAGE_V2_SHADOW_ENABLED=false` and
Flutter `AI_TRIAGE_V2_INTERNAL_ENABLED=false`. V1 routes/data remain untouched, so rollback is flag-only.

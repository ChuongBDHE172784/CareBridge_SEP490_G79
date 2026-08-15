# AI Triage — Canonical Caller and Legacy Read Inventory

Updated: 2026-08-13

## Decision

AI Triage now has one mutation engine: the deterministic canonical session workflow. The former
V1 Python graph, Java rule fallback/orchestrator, mobile V1 conversation methods, shadow routing,
and versioned runtime aliases have been removed. Version strings remain only where they describe
persisted data or versioned rule artifacts.

## Runtime callers

| Caller | Mutation/read surface | Status |
|---|---|---|
| Flutter `SymptomIntakeScreen` | `POST /api/v1/triage/sessions` and `POST /api/v1/triage/sessions/{id}/messages` | Canonical mutation path for all supported mother/baby stages |
| Java `CanonicalTriageSessionService` | Python `POST /internal/triage/turn` | Sole Java-to-Python mutation transport |
| Flutter result/history and expert handoff | `GET /api/v1/triage/intake/{id}` and list endpoint | Stable read model; supports old and canonical rows |
| Continuation restore | `/api/v1/triage/intake/continuations/*` | Stable lifecycle read/ack boundary |

## Intentionally retained legacy compatibility

- `TriageService` is read-only. It projects historical V1 `raw_ai_response` rows and canonical
  `result_jsonb` rows onto the existing result/history DTOs; it contains no clinical decisions.
- Persisted schema value `triage-v2-1`, database migrations, rule artifact filenames, and historical
  evidence names remain versioned because renaming them would corrupt compatibility or audit history.
- `IntakeController` retains result/history/continuation endpoints only. V1 mutation endpoints are gone.

## Caller-zero checks

The cutover is complete when production source search finds no references to the removed V1 mutation
methods/clients/graphs and no runtime `/v2/` endpoint alias. Tests may mention old schema identifiers
only when verifying historical compatibility.

## Rollback

There is no runtime V1 flag or second engine. Rollback means deploying the previous known-good
application images/commit while leaving persisted session rows untouched. The read model is deliberately
version-aware so rows created before or after rollback remain readable.

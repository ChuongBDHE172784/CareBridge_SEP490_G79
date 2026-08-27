# DEF-ST-001 — Emergency location share has no expiry or remaining time

| Field | Value |
|---|---|
| Defect ID | DEF-ST-001 |
| Source test case | ST-07-04 (Emergency Map) |
| Found in | Current Verification 12/08/2026 |
| Tester | LamVH |
| Environment | `https://api.carebridgevn.site` — production (Cloudflare → EC2/Nginx → Spring Boot) |
| Severity | Medium-High (privacy) |
| Status | Open |

## Expected result

A mother shares her emergency location with her family for a chosen duration. The family
member sees the location **and the remaining time**, and once the duration expires the
location is no longer shown.

## Actual result

The share has no time dimension at all.

1. `POST /api/v1/map/emergency/handoff` and `GET /api/v1/map/emergency/{id}` return only:
   `handoffId, userId, triageHandoffId, riskLevel, userLatitude, userLongitude,
   selectedFacilityId, summary, status, createdAt, updatedAt`.
   There is no duration, expiry or remaining-time field, so the "remaining time" the case
   requires cannot be displayed by any client.
2. Authorisation is care-group membership only, with no time check, so the emergency
   location stays readable by every accepted member of the care group indefinitely.

## Steps to reproduce

1. Sign in as `mother3@carebridge.dev` (owner of care group `13f14b90-2394-4171-925c-759d46162ce3`).
2. Accept the AI triage disclaimer and grant `LOCATION` / `SHARE` consent.
3. Run `POST /api/v1/triage/intake` with red-flag symptoms until `riskLevel: RED`.
4. `POST /api/v1/map/emergency/handoff` with `userLatitude 21.0278`, `userLongitude 105.8342`.
5. Sign in as `family2@carebridge.dev` (ACCEPTED member of that group) and
   `GET /api/v1/map/emergency/{handoffId}`.

## Observed evidence

| Caller | Result |
|---|---|
| `family2@` (ACCEPTED member) | **200**, `userLatitude 21.0278`, `userLongitude 105.8342` |
| `family3@` (not in the group) | **403** `EMER-009` |
| anonymous | **401** |

The access-control half behaves correctly. The time-limit half does not exist.

Full HTTP transcript: `06_Testing/SystemTesting/evidence/ST-07-04/retest-2026-08-12.txt`

## Notes

Round 1–3 in the workbook were **not** changed, because this run happened in August and
the three rounds hold June/July history. The finding is recorded as Current Verification
in the Note column of ST-07-04.

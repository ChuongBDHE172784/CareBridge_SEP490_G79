# DEF-ST-002 — Consultation requests are still routed to an expert with zero availability

| Field | Value |
|---|---|
| Defect ID | DEF-ST-002 |
| Source test case | ST-07-06 (Emergency Map) |
| Found in | Current Verification 12/08/2026 |
| Tester | LamVH |
| Environment | `https://api.carebridgevn.site` — production (Cloudflare → EC2/Nginx → Spring Boot) |
| Severity | Medium |
| Status | Open |

## Expected result

Turning the expert's nearby availability off removes further support requests from that
expert's list — requests should stop being offered to an expert who is not available.

## Actual result

Availability has no effect on routing. With the expert holding **zero** availability
slots, a new request is still accepted and still lands in that expert's pending queue.

## Steps to reproduce

1. Sign in as `expert2@carebridge.dev` and delete every availability slot, so
   `GET /api/v1/expert/availability/me` returns an empty list.
2. Sign in as `mother3@carebridge.dev` and
   `POST /api/v1/consultation-requests` targeting `expertProfileId 2cbeb01a-9036-42b6-b393-27c5ce8aaedc`.
3. As the expert, call `GET /api/v1/consultation-requests/pending-summary`.

## Observed evidence

| Step | Result |
|---|---|
| Expert availability after deleting all slots | `slots now: 0` |
| Mother creates a request | **201**, `status: PENDING` |
| Expert pending summary | `pendingCount` incremented — the request is queued |

What does work, verified in the same run: with a slot open the request reached the expert
(`pending-summary` 0 → 1, `/consultation-requests/assigned` listed it as PENDING), the
expert accepted it with **200**, and resending the same `clientRequestId` returned **200**
instead of creating a duplicate.

Full HTTP transcript: `06_Testing/SystemTesting/evidence/ST-07-06/retest-2026-08-12.txt`

## Test data cleanup

Fixtures were restored after the run: the seeded 13/08 `VIDEO_CALL` availability slot was
recreated with its original start and end times, and the two pending test requests were
cancelled (`pendingCount` back to 0). The accepted request could not be cancelled — the
API answered **409**, which is the correct business rule.

## Notes

Round 1–3 in the workbook were **not** changed, because this run happened in August and
the three rounds hold June/July history. The finding is recorded as Current Verification
in the Note column of ST-07-06.

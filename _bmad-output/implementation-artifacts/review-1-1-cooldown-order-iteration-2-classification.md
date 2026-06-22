# Review Classification — Cooldown and OTP Ordering Iteration 2

**Spec:** `spec-1-1-fix-cooldown-bypass-and-otp-order.md`
**Baseline:** `179c1a9dc41a9ef22ff024c6736670e043ea0622`

## Result

- `intent_gap`: 0
- `bad_spec`: 0
- `patch`: 1 — add `id DESC` as a deterministic tie-breaker when pending OTP rows share the same `createdAt`; migrated every call site and changed the integration fixture to equal timestamps.
- `defer`: pre-existing non-atomic verification attempt counter; request-path O(N) cleanup performance; lifecycle decision for invalidating every older pending OTP. Recorded in `deferred-work.md`.
- `reject`: query-property ambiguity (Spring Data parsed and executed the derived query in integration tests), unrelated `SecureRandom` allocation, and duplicate pre-existing observations.

## Final Evidence

- Focused suite: 39 tests, 0 failures, 0 errors, 0 skipped.
- `clean test`: 61 tests, 0 failures, 0 errors, 0 skipped.
- `clean package`: BUILD SUCCESS; executable JAR produced.

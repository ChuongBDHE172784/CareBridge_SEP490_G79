# Deferred Work

## Story 1.1 backend stabilization review

- Make resend delivery transactionally resilient with an explicit outbox/after-commit and retry policy; decide how cooldown reservations are released or retained when persistence, delivery, audit, or commit fails. This is an architectural expansion and may require schema changes.
- Replace the process-local resend cooldown and pending-OTP coordination with a shared multi-instance mechanism such as Redis or database locking/constraints.
- Replace low-entropy OTP SHA-256 storage with keyed HMAC or an appropriate slow hash and constant-time comparison.
- Normalize registration email consistently for lookup, persistence, delivery, and uniqueness enforcement.
- Make verification-attempt rate limiting atomic under concurrent requests.
- Review registration audit metadata to avoid duplicating raw email and phone PII.
- Persist and test `USER_REGISTRATION_COMPLETED` audit eligibility.
- Configure BCrypt strength 12 and add focused evidence; the current default-strength constructor predates this bugfix.
- Decide whether issuing/resending an OTP must invalidate every older pending OTP for the account, not only the newest selected row; this requires a broader lifecycle rule and bulk-update behavior beyond the ordered-selection patch.
- Move map-wide in-memory rate-limit cleanup off request paths or replace it with bounded/scheduled pruning if profiling shows the current O(N) scans are material under production load.

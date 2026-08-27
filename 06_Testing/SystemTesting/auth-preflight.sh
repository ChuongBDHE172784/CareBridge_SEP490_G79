#!/usr/bin/env bash
# CareBridge System Test - authentication preflight.
#
# Run this BEFORE every test batch that needs a signed-in account, and again after
# roughly five minutes for a long batch. The seeded credentials on this environment
# come and go across redeploys, so a batch started on stale assumptions produces
# wrong results.
#
# Exit 0 = every in-scope role authenticated, safe to start the batch.
# Exit 1 = at least one role failed, do NOT start the batch. Record the failure as an
#          Environment Blocker in the Note column and leave the cases Pending. A missing
#          seed account is never a reason to mark a test case Failed.
#
# Partner is deliberately absent: the Partner role is out of System Test scope.

set -u
API="${API:-https://api.carebridgevn.site}"
PASSWORD="${CAREBRIDGE_TEST_PASSWORD:-Test@1234}"
ROLES=(mother mother3 family family2 expert expert2 admin moderator content)
LOG="${LOG:-$(dirname "$0")/auth-preflight.log}"

stamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
fail=0
echo "=== auth preflight $stamp  ($API) ===" | tee -a "$LOG"

for role in "${ROLES[@]}"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 20 \
    "$API/api/v1/auth/login" -X POST -H "Content-Type: application/json" \
    -d "{\"email\":\"$role@carebridge.dev\",\"password\":\"$PASSWORD\"}")
  printf '%s  %-10s %s\n' "$stamp" "$role" "$code" | tee -a "$LOG"
  [ "$code" = "200" ] || fail=$((fail + 1))
done

if [ "$fail" -gt 0 ]; then
  echo "$stamp  RESULT: $fail/${#ROLES[@]} role(s) failed - DO NOT start the batch." | tee -a "$LOG"
  echo "$stamp  Record 'Environment Blocker: seeded credentials returned 401' and keep the cases Pending." | tee -a "$LOG"
  exit 1
fi

echo "$stamp  RESULT: all ${#ROLES[@]} in-scope roles authenticated - safe to start." | tee -a "$LOG"
exit 0

---
title: 'Fix TrackAsia preview, location consent entry point, and mother home FAB'
type: 'bugfix'
created: '2026-08-02'
status: 'done'
baseline_commit: '1955fc13a09217eb6b125361189f796bab41f70f'
context:
  - 'ctss/CLAUDE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The mother account can open the nearby-care screen, but an absent location consent leaves TrackAsia permanently waiting because the screen offers only a retry action. The mother-home entry point is also an oversized full-width red button, and the local Flutter preview is not served on the requested port.

**Approach:** Preserve the existing consent contract and add an explicit, user-triggered consent confirmation before requesting device location. Replace the home entry point with a circular floating action button and run the verified web preview on port 5173 against the existing backend.

## Boundaries & Constraints

**Always:** Use TrackAsia for map/style/route data; keep the API key out of backend logic and source; require active `LOCATION/SHARE/CAREBRIDGE_SAFETY/SAFETY_EMERGENCY_ALERT` consent before reading location; retain 115 access when map/location fails; preserve current account isolation.

**Ask First:** Any database table/migration, tile/style proxy, location history, paid third-party SDK, or change that weakens/bypasses the current consent rule.

**Never:** Add `care_facilities`; expose backend secrets to the client; substitute Google Maps; auto-grant location consent without an explicit mother action.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Consent missing | Mother opens nearby map without active consent | Show a clear “Cho phép vị trí” action and disclosure; do not read location | 115 remains usable |
| Consent accepted | Mother confirms location sharing | Create the existing scoped consent, request browser/device location, then search and render TrackAsia | Show actionable retry message if API or permission fails |
| Consent declined | Mother closes/cancels confirmation | No consent and no location read | Map remains blocked; 115 remains usable |
| Home entry | Mother is on home screen | Circular hospital FAB appears at the right-bottom and opens `/emergency/map` | Existing home content stays usable |

</frozen-after-approval>

## Code Map

- `05_Development/CareBridgeMobileApp/lib/features/home/screens/mother_home_screen.dart` -- mother-home emergency entry point.
- `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart` -- consent gate, geolocation, TrackAsia map, facilities, and navigation UI.
- `05_Development/CareBridgeMobileApp/test/features/home/mother_home_screen_test.dart` -- home entry behavior.
- `05_Development/CareBridgeMobileApp/test/features/emergency/emergency_map_screen_test.dart` -- consent and map-loading behavior.
- `05_Development/CareBridgeMobileApp/web/index.html` -- TrackAsia GL web runtime assets.

## Tasks & Acceptance

**Execution:**
- [x] `mother_home_screen.dart` -- replace the full-width emergency button with an accessible circular FAB.
- [x] `emergency_map_screen.dart` -- add explicit scoped-consent confirmation and retry loading after grant.
- [x] Flutter widget tests -- cover FAB navigation, no-read before consent, accept/cancel, and grant failure.
- [x] Local runtime -- restart backend CORS for Portal `5173` and Flutter web-server at `localhost:53521`.

**Acceptance Criteria:**
- Given a logged-in mother without location consent, when she accepts the disclosure, then the existing consent is granted before geolocation and nearby TrackAsia search.
- Given a logged-in mother, when she presses the circular hospital FAB, then the TrackAsia emergency map screen opens.
- Given the local preview is started, when opening `http://localhost:53521`, then Flutter responds and backend requests from that origin are CORS-authorized while port `5173` remains free for Web Portal.

## Spec Change Log

- 2026-08-02 review patch: cleared sticky consent-action state on each reload, suppressed it for dial fallback notices, guarded against duplicate consent dialogs, and made the FAB respect the device bottom safe area. This avoids misleading consent actions, duplicate grants, and an obscured emergency control while preserving the exact consent scope/order, explicit disclosure, 115 fallback, and circular right-bottom FAB.

## Verification

**Commands:**
- `flutter test test/features/home/mother_home_screen_test.dart test/features/emergency/emergency_map_screen_test.dart` -- relevant widget tests pass.
- `flutter analyze` -- no new analyzer errors.
- HTTP/CORS probes for ports `53521` and `8080` -- preview and API are reachable; port `5173` is free for Web Portal.

## Suggested Review Order

**Mother entry point**

- Floating emergency access respects safe area without disrupting home content.
  [`mother_home_screen.dart:187`](../../05_Development/CareBridgeMobileApp/lib/features/home/screens/mother_home_screen.dart#L187)

- Circular FAB preserves the existing TrackAsia emergency route.
  [`mother_home_screen.dart:386`](../../05_Development/CareBridgeMobileApp/lib/features/home/screens/mother_home_screen.dart#L386)

**Consent-gated location**

- Explicit dialog grants the exact existing scope before any geolocation read.
  [`emergency_map_screen.dart:312`](../../05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart#L312)

- Banner exposes consent action only for the relevant recoverable state.
  [`emergency_map_screen.dart:1106`](../../05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart#L1106)

**Regression coverage**

- Home test verifies circular placement and destination query parameters.
  [`mother_home_screen_test.dart:316`](../../05_Development/CareBridgeMobileApp/test/features/home/mother_home_screen_test.dart#L316)

- Consent tests prove exact grant ordering, cancellation, and failure safety.
  [`emergency_map_screen_test.dart:179`](../../05_Development/CareBridgeMobileApp/test/features/emergency/emergency_map_screen_test.dart#L179)

- Contract tests verify location remains unread until explicit acceptance.
  [`nearby_care_contract_test.dart:213`](../../05_Development/CareBridgeMobileApp/test/features/emergency/nearby_care_contract_test.dart#L213)

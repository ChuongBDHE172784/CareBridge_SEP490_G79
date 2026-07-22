# Nearby Care Client Contract — v1

Status: Release 1 canonical contract

## Endpoints

- `GET /api/v1/map/nearby-facilities?lat={number}&lng={number}&radiusMeters={integer}`
- `GET /api/v1/map/facilities/{facilityId}` for persisted CareBridge facilities only
- `POST /api/v1/map/route` with `fromLat`, `fromLng`, `toLat`, `toLng`, and `transportMode`

All responses use the standard `{ "data": ... }` envelope. Nearby data contains
`facilities` and `totalCount`. Route data contains `distanceMeters`, `etaMinutes`,
`points`, and `transportMode`.

## Facility identity and compatibility

`facilityId` is the canonical persisted identifier. Clients may temporarily read
the legacy `hospitalId` response alias during migration, but must not send or
persist new references under that name.

`facilityId` is nullable. Provider results such as TrackAsia POIs may only carry
`sourceType` and `externalSourceId`. Clients must render and route to these results
using their coordinates and must not call the persisted-facility detail endpoint
without a `facilityId`. `partnerId` identifies an organization and must never be
used as a facility identifier.

## Display and safety rules

- Coordinates and distances are JSON numbers. Optional address, phone, opening
  hours, distance, external ID, and facility ID must be null-safe.
- Display `sourceType` and `verificationStatus` together. A TrackAsia result is
  provider-sourced and is not CareBridge-verified merely because it is returned.
  The canonical verified value is `VERIFIED`; `APPROVED` is a temporary legacy
  read alias only.
- A nearby location lookup requires an active consent with data type `LOCATION`,
  purpose `SHARE`, recipient `CAREBRIDGE_SAFETY`, and scope
  `SAFETY_EMERGENCY_ALERT`.
- `openingHoursJson` is optional provider metadata; malformed or unknown content
  must not break emergency help or calling.
- Location permission denial, disabled location services, an empty result, or a
  TrackAsia/route failure must retain emergency calling and manual guidance.
- Nearby search and routing supplement emergency handling; they must never delay
  or replace emergency-session creation.
- Quick-call uses a facility phone only when present. Emergency call `115` remains
  available independently.

## Deferred governance

Partner and System Admin facility create/edit, source reconciliation, verification,
and publication governance are explicitly deferred beyond this Release 1 mobile
contract. Existing Web Partner profile and service-listing screens are not facility
governance surfaces and must not infer verification from organization approval.

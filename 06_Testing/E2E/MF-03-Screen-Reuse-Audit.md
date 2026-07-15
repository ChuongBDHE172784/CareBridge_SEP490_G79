# MF-03 Screen Reuse Audit

## Canonical mobile screens

| Mockup | Canonical implementation | Route | Decision |
|---|---|---|---|
| Baby Journey dashboard | `MotherJourneyScreen` embedding `BabyProfileDetailScreen` | Home → Hành trình | Reuse; do not create another dashboard screen |
| Daily Log Summary | `BabyLogSummaryScreen` | `/babies/:babyId/log-summary` | Reuse |
| Add Milestone | `RecordMilestoneScreen` | `/babies/:babyId/milestones/add` | Reuse |

## Duplicate/transition surfaces

- `BabyCareHubScreen` (`/baby-care-hub`) is retained only for compatibility with earlier MF-03 runs. Current Mobile fixture and API-backed E2E target the canonical Baby Journey flow.
- `BabyCareHubPage` (`/mother/baby-care`) is the web counterpart of the transition hub. It must not be used as justification for creating duplicate mobile journey screens.
- `BabyCareCompositeService` is a data service, not a replacement screen.

## Exit criteria for hub retirement

The Mobile E2E assertions have moved to the canonical journey/profile flow. The compatibility route can be retired in a separately approved cleanup after composite read-model migration and downstream link checks are complete.

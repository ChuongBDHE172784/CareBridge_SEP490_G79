# TV2 Sprint 0 - Care Journey Skeleton

**Source of truth:** `docs/bmad/function-spec-task-allocation.md`  
**Sprint:** Sprint 0 - Foundation And Module Skeletons  
**Owner:** TV2  
**Boundary:** Care Journey  
**Goal:** Create care-domain packages, base entities, and empty routes/screens.

---

## Scope Summary

TV2 Sprint 0 is limited to the Care Journey skeleton. The goal is to establish module boundaries, API stubs, basic data shapes, and mobile screen skeletons that later sprints can build on.

This file intentionally follows `function-spec-task-allocation.md` as the standard source. It includes only the 12 TV2 Sprint 0 function specs listed there.

---

## Function Specs

| SRS ID | Use Case Name | Sprint 0 Intent |
|---|---|---|
| 3.3.1.1 | Create Mother Journey | Create the skeleton flow for starting a mother journey. |
| 3.3.1.8 | Create Baby Profile | Create the skeleton flow for adding a baby profile. |
| 3.3.1.16 | Add Health Record | Create the skeleton flow for adding health record metadata and attachments. |
| 3.3.1.22 | Create Appointment Reminder | Create the skeleton flow for appointment/checkup reminders. |
| 3.3.1.47 | Create Care Group | Create the skeleton flow for family care coordination groups. |
| 3.3.10.1 | Upload File | Create the skeleton upload workflow for care-domain files. |
| 3.3.11.1 | View Maternal Health Metric Detail | Create a placeholder detail view for maternal health metrics. |
| 3.3.12.1 | View Baby Profile | Create a placeholder baby profile detail view. |
| 3.3.15.1 | View Health Record Detail | Create a placeholder health record detail view. |
| 3.3.16.1 | View Reminder Detail | Create a placeholder reminder detail view. |
| 3.3.17.1 | View Care Group Members | Create a placeholder member list for a care group. |
| 3.3.19.1 | View Vaccination Schedule | Create a placeholder vaccination schedule view. |

---

## Implementation Focus

### Backend Packages

- `motherjourney`
- `baby`
- `healthrecord`
- `file`
- `reminder`
- `vaccination`
- `growth`
- `familycare`
- `device`

### Mobile Feature Folders

- `mother`
- `baby`
- `healthrecord`
- `reminder`
- `vaccination`
- `familycare`

### Dependency Rules

- Use TV1 auth/user contract only.
- Avoid editing auth internals.
- Use shared response, exception, auth guard, role/permission, notification, and audit contracts from TV1 when available.
- If a TV2 flow requires a shared contract that does not exist yet, coordinate with TV1 before editing shared internals.

---

## Expected Sprint 0 Output

- Backend care-domain packages exist with clear module boundaries.
- Basic API endpoint stubs exist for the 12 TV2 Sprint 0 function specs.
- Basic DTO/request/response shapes exist where endpoint stubs need them.
- Mobile feature folders exist for TV2 care-domain screens.
- Skeleton screens exist for mother journey, baby profile, health records, reminders, care groups, and vaccination schedule.
- Mock data is acceptable where real data flow is not ready.
- File upload flow stores metadata shape separately from binary storage provider details.
- Minimal tests verify endpoint existence and basic response shape where backend stubs are implemented.

---

## Explicitly Out Of Sprint 0 According To The Source Of Truth

The following TV2 items appear in later sprints in `function-spec-task-allocation.md`, so they should not be treated as Sprint 0 scope when this file is used as the standard:

- `3.3.1.2 Update Mother Journey`
- `3.3.1.3 View Mother Journey Dashboard`
- `3.3.1.4 Add Maternal Health Metric`
- `3.3.1.5 Update Maternal Health Metric`
- `3.3.1.6 View Maternal Health Trend`
- Baby logs, growth charts, health record update/archive/timeline, medication/vaccination reminders, today tasks, file view/delete, family invitation/permission/task flows, vaccination records, and growth measurements.

These belong to Sprint 1 or later according to the function-spec allocation.

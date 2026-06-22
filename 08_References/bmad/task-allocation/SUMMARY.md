# Task Allocation Summary

Based on: `01_Requirements/SRS/Report3_Software Requirement Specification.docx.md` (241 Use Cases)
Source plan: `docs/bmad/function-spec-task-allocation.md`

## Development Team & Ownership

| Member | Domain Ownership | Main Boundary | Total UC |
|--------|------------------|---------------|----------|
| **TV1** | Shared Foundation | Authentication, account/profile, privacy, notifications, audit/security, shared contracts | ~35 UC |
| **TV2** | Care Journey | Mother/baby journey, health records, family sync, reminders, vaccination, files, device health data | ~60 UC |
| **TV3** | Community & Content | Community Q&A, content/checklist, moderation, partner governance, RAG knowledge | ~45 UC |
| **TV4** | Expert Consultation | Expert profile, booking, consultation, pricing, payment, commission, realtime | ~50 UC |
| **TV5** | AI, Location & Safety | AI triage, map/location, emergency flow, safety monitoring, exercise/posture | ~51 UC |
| **Total** | | | **241 UC** |

## Sprint Overview

| Sprint | Duration | Goal | Demo End-to-End |
|--------|----------|------|-----------------|
| **Sprint 0** | 2 weeks | Foundation & Module Skeletons | Basic project structure, shared contracts, empty feature folders |
| **Sprint 1** | 3 weeks | First End-To-End Domain Flows | Each TV has working CRUD for core features |
| **Sprint 2** | 3 weeks | Complete Core CRUD And UI Wiring | All core screens and APIs functional |
| **Sprint 3** | 3 weeks | Cross-Domain Integration | Integration points working together |
| **Sprint 4** | 2 weeks | Real Providers And Admin Polish | Real/sandbox integrations, admin flows |
| **Sprint 5** | 1 week | Stabilization And Merge Freeze | Testing, bug fixes, merge preparation |
| **Total** | **14 weeks** | | |

## Key Principles

1. **Vertical Slices**: Backend package, API contract, UI screens, integration mock/provider, and tests belong to same domain owner
2. **Shared Contracts First**: TV1 owns shared contracts (auth, notification, audit, API response format)
3. **Mock-First Approach**: Integrations implement mock/stub first, then replace with real providers after domain flows stable
4. **Incremental Delivery**: Each sprint produces a working, demonstrable system
5. **Merge Conflict Reduction**: Domain boundaries clear, avoid editing other members' packages

## Next Steps

- Review and assign team members to TV1-TV5 based on skills
- Adjust sprint duration based on team velocity
- Start with `Sprint 0 - Foundation And Module Skeletons`
- Use detailed task lists in individual sprint files

## Files

- `SUMMARY.md` - This file
- `SPRINT_0.md` through `SPRINT_5.md` - Detailed task breakdown per sprint
- `OWNER_MAPPING.md` - Complete Use Case to owner mapping
- `CSV_EXPORT/` - CSV files for tracking tools (GitHub Projects, Jira, etc.)

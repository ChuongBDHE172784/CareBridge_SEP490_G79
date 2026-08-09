---
title: 'CareBridge 56-Entity dbdiagram Model'
type: 'chore'
created: '2026-08-09'
status: 'done'
route: 'one-shot'
---

# CareBridge 56-Entity dbdiagram Model

## Intent

**Problem:** The Draw.io ERD is difficult to read, and standard DBML visibly requires a data type for every column.

**Approach:** Provide a parser-valid DBML visualization of 56 conceptual entities, using a zero-width custom type to hide types and direct many-to-many references for the seven relationship tables.

## Suggested Review Order

**Visualization contract**

- Start with the type-hiding rationale and visualization-only boundary.
  [`CareBridge_ERD_56_Entities.dbml:1`](../../03_Design/Database/CareBridge_ERD_56_Entities.dbml#L1)

- Confirm the central identity entity retains all conceptual attributes.
  [`CareBridge_ERD_56_Entities.dbml:430`](../../03_Design/Database/CareBridge_ERD_56_Entities.dbml#L430)

**Relationship fidelity**

- Review catalog-derived FK references after duplicate endpoints are collapsed.
  [`CareBridge_ERD_56_Entities.dbml:1408`](../../03_Design/Database/CareBridge_ERD_56_Entities.dbml#L1408)

- Review conceptual many-to-many edges replacing seven physical relationship tables.
  [`CareBridge_ERD_56_Entities.dbml:1543`](../../03_Design/Database/CareBridge_ERD_56_Entities.dbml#L1543)

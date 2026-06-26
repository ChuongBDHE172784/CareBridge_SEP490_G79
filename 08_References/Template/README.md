# Template README

This folder contains reusable templates and helper inputs for the CareBridge specification and implementation workflow.

## Recommended Usage Order

1. Start with `create-specs-input-template.md` when you want to create or update TDS and Test-Spec for one Function.
2. Review and approve the generated Function workspace in `04_Implement/[UCNumber] - [Function Name]/`.
3. Use `implement-feature-input-template.md` when you want to:
   - prepare a Gemini handoff from approved specs
   - run a readiness/freshness check before implementation
   - review implementation returned by Gemini

For Function specification work, the controlling workflow is `.agents/workflows/create-specs.md`. BMAD skills may support the work, but they do not replace the workflow.
For TDS and Test-Spec authoring, `PHASE-3_TDS.md` and `PHASE-4_Test-Spec.md` are the required document skeletons. They are not optional formatting references.

## Main Files

- `PHASE-3_TDS.md`
  - Required TDS skeleton used to draft the Technical Design Specification.
- `PHASE-4_Test-Spec.md`
  - Required Test-Spec skeleton used to draft the testing specification.
- `create-specs-input-template.md`
  - Input prompt template for requesting TDS and Test-Spec creation for one Function.
- `implement-feature-input-template.md`
  - Input prompt template for requesting implementation handoff, readiness checking, or implementation review for one approved Function.

## When To Use Which Template

### Use `create-specs-input-template.md` when:

- the Function has not been fully specified yet
- TDS and Test-Spec do not exist yet
- TDS or Test-Spec needs to be revised because scope or design changed
- you want to enforce that the accepted outputs live in `04_Implement/[UCNumber] - [Function Name]/`
- you want the resulting documents to preserve the major section structure of the repository templates

### Use `implement-feature-input-template.md` when:

- TDS and Test-Spec are already approved
- you want a Gemini-ready coding handoff
- implementation already exists and needs review against approved specs
- you want to check whether approved specs are still fresh against the current codebase

## Standard Function Flow

1. Identify the Function ID, canonical Function name, and UC number.
2. Create the Function specs using `create-specs-input-template.md`.
3. Produce or update:
   - `[FolderName]_TDS.md`
   - `[FolderName]_Test-Spec.md`
4. Ensure TDS and Test-Spec still follow the major heading skeleton of `PHASE-3_TDS.md` and `PHASE-4_Test-Spec.md`.
5. Review the specs and mark TDS/Test-Spec as `Approved`.
6. Request implementation handoff or review using `implement-feature-input-template.md`.
7. Produce `[FolderName]_Gemini-Task.md` if implementation must be delegated to Gemini.
8. Review the returned implementation against approved specs, tests, repository rules, and security/privacy requirements.

## Notes

- If some information is unknown, use `Open` instead of inventing it.
- If the repository already contains partial implementation, mention that in the input so the investigation can trace current-state gaps correctly.
- Approval matters: `implement-feature` should stop if approved TDS and Test-Spec are missing or stale.
- Legacy files under `03_Design/...` or `06_Testing/...` may still be useful as evidence, but they do not by themselves satisfy the new Function workflow.
- A valid Function TDS/Test-Spec should still look like an adapted instance of the repository templates, not a completely custom document form.

## Strict Prompt

Use this wording when you want maximum enforcement:

```md
Dung workflow `create-specs` de tao TDS va Test-Spec cho function nay.
Khong duoc di theo flow cu.
Chi xem la hoan thanh khi tao du `TDS + Test-Spec` trong `04_Implement/[UCNumber] - [Function Name]/`.
Neu artifact nam o `03_Design/...` hoac `06_Testing/...` thi phai bao ro la workflow chua duoc thuc thi dung.
Phai bam sat khung section chinh cua `PHASE-3_TDS.md` va `PHASE-4_Test-Spec.md`, khong duoc viet lai thanh form tu do.
```

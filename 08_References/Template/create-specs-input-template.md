Use this template when requesting TDS and Test-Spec creation for one CareBridge Function.

This template is intentionally strict so the request stays on the repository's `create-specs` workflow instead of drifting into a legacy documentation flow.

## Pasteable Template

```md
Dung workflow `create-specs` de tao TDS va Test-Spec cho function sau.
Khong duoc di theo flow tai lieu cu phan tan o `03_Design` hoac `06_Testing`.
Chi duoc xem la hoan thanh khi tao hoac cap nhat day du artifact trong `04_Implement/[UCNumber] - [Function Name]/`.

- Function ID: [e.g. 3.1.1.3]
- Function name: [e.g. Login]
- UC number: [e.g. UC03]
- Target platforms: [Backend / Web / Mobile / End-to-end]
- Intended outcome: [what this function must achieve]
- Out of scope: [what must not be designed in this round]
- Priority: [optional; High / Medium / Low]
- Source of truth to prioritize: [optional; SRS / current code / specific report / user decision]
- Additional acceptance criteria: [optional]
- Business decisions not yet in repo: [optional]
- External integrations: [optional; SMS, email, payment, device, etc.]
- Security or compliance constraints: [optional]
- Deadline or sprint context: [optional]
- Known gaps in current implementation: [optional]
- Open questions you want resolved in the spec: [optional]

Acceptance rules for this request:

- Primary workflow must be `.agents/workflows/create-specs.md`
- Supporting BMAD skills may assist but must not replace the workflow
- `08_References/Template/PHASE-3_TDS.md` must be used as the TDS skeleton
- `08_References/Template/PHASE-4_Test-Spec.md` must be used as the Test-Spec skeleton
- Major headings and section order from the templates must be preserved unless a section is marked `Not applicable` with reason
- Required outputs must exist in `04_Implement/[UCNumber] - [Function Name]/`:
  - `[FolderName]_TDS.md`
  - `[FolderName]_Test-Spec.md`
- If output is created only under `03_Design/...` or `06_Testing/...`, treat the task as not completed
- Final response must state:
  - workflow used
  - supporting skills used
  - exact artifact paths created/updated
  - any legacy files used only as evidence
  - whether the template skeleton was preserved and which sections were marked `Not applicable`
```

## Minimal Template

```md
Dung workflow `create-specs` de tao TDS va Test-Spec cho function:

- Function ID:
- Function name:
- UC number:
- Target platforms:
- Intended outcome:
- Out of scope:
```

## Example

```md
Dung workflow `create-specs` de tao TDS va Test-Spec cho function sau.
Khong dung flow cu.
Chi hoan thanh khi tao du artifact trong `04_Implement/[UCNumber] - [Function Name]/`.

- Function ID: 3.1.1.3
- Function name: Login
- UC number: UC03
- Target platforms: Backend, Web, Mobile
- Intended outcome: Cho phep nguoi dung dang nhap bang email va mat khau, nhan access token va refresh token, dong thoi tao session co the quan ly duoc.
- Out of scope: Social login, SSO, biometric login, remember-device nang cao.
- Priority: High
- Source of truth to prioritize: SRS + current code
- Additional acceptance criteria: Khoa tai khoan sau so lan dang nhap sai theo policy hien hanh.
- Business decisions not yet in repo: Chi cho phep mot refresh token hoat dong tren moi thiet bi.
- External integrations: Audit logging
- Security or compliance constraints: Khong log password hoac OTP; phai audit login success/failure.
- Deadline or sprint context: Sprint 0 - TV1
- Known gaps in current implementation: Web/Mobile flow chua dong bo session management end-to-end.
- Open questions you want resolved in the spec: Co can phan biet loi sai mat khau va loi tai khoan bi khoa o UI hay khong?
```

## Notes

- If a field is unknown, write `Open`.
- If the function already exists in code, mention the known modules or files if you have them.
- If you want me to infer as much as possible from the repo, say: `Uu tien tu doc repository, chi hoi lai khi co quyet dinh khong the suy ra an toan.`
- If you want maximum enforcement, add: `Neu khong tao dung artifact trong 04_Implement/[UCNumber] - [Function Name]/ thi phai bao la workflow chua duoc thuc thi dung.`
- If you want skeleton enforcement, add: `Phai bam sat section khung cua PHASE-3_TDS.md va PHASE-4_Test-Spec.md, khong duoc viet lai theo form tu do.`

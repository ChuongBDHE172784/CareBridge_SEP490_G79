# MF-12 / Spec 01 — Preparation Expense Entry & Summary

| Field | Value |
| --- | --- |
| Feature | MF-12 — Expense & Preparation Planner |
| Use Cases Covered | UC-109 Add Expense Entry, UC-110 Update or Delete Expense Entry, UC-111 View Expense Summary |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother records a preparation expense with category/amount/date, may correct or permanently delete it, and views simple totals grouped by category, month or journey/baby period. This is a personal record-keeping tool, not accounting/billing/insurance software. |
| Grounding (source code) | `carejourney/entity/Expense.java`, `ExpenseCategory.java`, `carejourney/controller/ExpenseController.java` (`/api/v1/expenses`), `carejourney/service/impl/ExpenseServiceImpl.java` |

## 1. Tổng quan luồng chính (Main Flow Overview)

MF-12 là tính năng đơn giản nhất trong toàn bộ 14 Major Feature: `Expense` không có cột
`status` — sửa (UC-110) ghi đè trực tiếp, xoá (UC-110) là **hard delete thật** (xem
`ExpenseServiceImpl.deleteExpense()`: `expenseRepository.delete(expense)`, có audit trước
khi xoá nhưng không giữ bản ghi lại). Vì vậy state machine của spec này **cố tình tối
giản** (tồn tại → bị xoá vĩnh viễn) thay vì vẽ thêm trạng thái giả định như `ARCHIVED` —
đúng tinh thần "không thêm trạng thái suy diễn để trông đầy đủ hơn" đã nêu ở README mục 3.
UC-111 (xem tổng hợp) là phép nhóm/tổng đơn giản theo `category`/tháng/`journeyId`/
`babyId`, không phải entity riêng.

## 2. Class Diagram

```plantuml
@startuml MF12_01_Expense_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class Expense {
  + id: UUID
  + ownerUserId: UUID
  + journeyId: UUID
  + babyId: UUID
  + category: ExpenseCategory
  + amount: BigDecimal
  + currency: String
  + expenseDate: LocalDate
  + note: String
}

enum ExpenseCategory {
  CHECKUP
  DELIVERY
  DIAPER
  MILK
  VACCINATION
  SUPPLY
  OTHER
}

class ExpenseSummaryResponse <<read-model>> {
  + groupBy: String
  + buckets: List<ExpenseSummaryBucket>
  + grandTotal: BigDecimal
  + grandTotalCurrency: String
}

class ExpenseSummaryBucket <<read-model>> {
  + label: String
  + total: BigDecimal
  + count: int
}

class ExpenseController {
  - expenseService: IExpenseService
  + add(AddExpenseRequest): ResponseEntity
  + update(expenseId, UpdateExpenseRequest): ResponseEntity
  + delete(expenseId): ResponseEntity
  + summary(groupBy): ResponseEntity
}

interface IExpenseService <<interface>> {
  + add(ownerId: UUID, request): Expense
  + update(ownerId: UUID, expenseId: UUID, request): Expense
  + delete(ownerId: UUID, expenseId: UUID): void
  + summary(ownerId: UUID, groupBy: String): ExpenseSummaryResponse
}

class ExpenseServiceImpl implements IExpenseService {
  - expenseRepository: ExpenseRepository
  - auditService: AuditService
}

Expense --> ExpenseCategory
ExpenseSummaryResponse "1" *-- "0..*" ExpenseSummaryBucket : groups by category/month/period
ExpenseController --> IExpenseService : uses
ExpenseServiceImpl ..> ExpenseSummaryResponse : builds
ExpenseServiceImpl --> AuditService : emits EXPENSE_CREATED / UPDATED / DELETED (trước khi hard-delete)

@enduml
```

**Hình 1 — Class Diagram: Expense Entry & Summary Read-Model**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF12_01_Expense_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "ExpenseController" as Controller
participant "ExpenseServiceImpl" as Service
participant "ExpenseRepository" as Repo
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-109 Add Expense Entry ==
M -> Controller : 1. POST /api/v1/expenses\n{category=DIAPER, amount=250000, expenseDate, journeyId, babyId}
activate Controller
Controller -> Service : 2. addExpense(request, callerId)
activate Service
Service -> Service : 3. kiểm tra expenseDate không ở tương lai\n(400 EXPENSE-003 nếu vi phạm)
Service -> Repo : 4. save(Expense{ownerUserId=callerId,\ncurrency="VND" nếu request không truyền})
activate Repo
Repo -> DB : 5. INSERT INTO expenses ...
activate DB
DB --> Repo : 6. saved
deactivate DB
Repo --> Service : 7. Expense
deactivate Repo
Service -> Audit : 8. log(EXPENSE_CREATED, callerId, "Expense", id, "created")\n[PDPA — KHÔNG ghi amount/note vào audit detail]
activate Audit
Audit --> Service : 9. void
deactivate Audit
Service --> Controller : 10. ExpenseResponse
deactivate Service
Controller --> M : 11. HTTP 201 Created
deactivate Controller

== UC-110 Update or Delete Expense Entry ==
M -> Controller : 12. PATCH /api/v1/expenses/{expenseId}\n{amount=270000, note}
activate Controller
Controller -> Service : 13. updateExpense(expenseId, request, callerId)
activate Service
Service -> Repo : 14. findByIdAndOwnerUserId(expenseId, callerId)
activate Repo
Repo -> DB : 15. SELECT * FROM expenses\nWHERE id=? AND owner_user_id=?
activate DB
DB --> Repo : 16. expense row (404 EXPENSE-004 nếu không có/không thuộc caller)
deactivate DB
Repo --> Service : 17. Expense
deactivate Repo
Service -> Service : 18. áp dụng field non-null (PATCH); nếu đổi expenseDate\nthì kiểm tra lại không ở tương lai (400 EXPENSE-003)
Service -> Repo : 19. save(expense{...})
activate Repo
Repo -> DB : 20. UPDATE expenses\nSET category=?, amount=?, expense_date=?, note=?, updated_at=now()
activate DB
DB --> Repo : 21. updated
deactivate DB
Repo --> Service : 22. Expense
deactivate Repo
Service -> Audit : 23. log(EXPENSE_UPDATED, callerId, "Expense", expenseId, "updated")
activate Audit
Audit --> Service : 24. void
deactivate Audit
Service --> Controller : 25. ExpenseResponse
deactivate Service
Controller --> M : 26. HTTP 200 OK
deactivate Controller

M -> Controller : 27. DELETE /api/v1/expenses/{expenseId}
activate Controller
Controller -> Service : 28. deleteExpense(expenseId, callerId)
activate Service
Service -> Repo : 29. findByIdAndOwnerUserId(expenseId, callerId)
activate Repo
Repo -> DB : 30. SELECT * FROM expenses\nWHERE id=? AND owner_user_id=?
activate DB
DB --> Repo : 31. expense row (404 EXPENSE-004 nếu không có)
deactivate DB
Repo --> Service : 32. Expense
deactivate Repo
Service -> Audit : 33. log(EXPENSE_DELETED, callerId, "Expense", expenseId, "deleted")\n[ghi audit TRƯỚC khi hard-delete — ADR-CJ-052]
activate Audit
Audit --> Service : 34. void
deactivate Audit
Service -> Repo : 35. delete(expense) [hard delete thật]
activate Repo
Repo -> DB : 36. DELETE FROM expenses WHERE id=?
activate DB
DB --> Repo : 37. deleted
deactivate DB
Repo --> Service : 38. void
deactivate Repo
Service --> Controller : 39. void
deactivate Service
Controller --> M : 40. HTTP 200 OK
deactivate Controller

== UC-111 View Expense Summary ==
M -> Controller : 41. GET /api/v1/expenses/summary?groupBy=CATEGORY&from=&to=
activate Controller
Controller -> Service : 42. getSummary(callerId, groupBy, from, to)
activate Service
alt 43. groupBy hợp lệ (MONTH | CATEGORY | STAGE)
  Service -> Repo : 43. groupByCategory(callerId, from, to)\n[hoặc groupByMonth/groupByStage tương ứng theo groupBy]
  activate Repo
  Repo -> DB : 44. SELECT category, currency, SUM(amount), COUNT(*)\nFROM expenses WHERE owner_user_id=? ... GROUP BY category, currency
  activate DB
  DB --> Repo : 45. rows[]
  deactivate DB
  Repo --> Service : 46. rows[]
  deactivate Repo
  Service -> Service : 47. gộp thành ExpenseSummaryBucket[];\ngrandTotal CHỈ cộng dồn bucket cùng currency="VND"\n(mixed-currency guard — bucket ngoại tệ khác bị loại khỏi tổng)
  Service --> Controller : 48. ExpenseSummaryResponse{buckets[], grandTotal,\ngrandTotalCurrency="VND"}
  deactivate Service
  Controller --> M : 49. HTTP 200 OK {summary}
  deactivate Controller
else 43. groupBy không hợp lệ (khác MONTH/CATEGORY/STAGE)
  Service --> Controller : 43a. throw 400 EXPENSE-001\n"groupBy must be MONTH, CATEGORY, or STAGE"
  deactivate Service
  Controller --> M : 43b. HTTP 400 Bad Request
  deactivate Controller
end

@enduml
```

**Hình 2 — Sequence Diagram: Add (future-date guard) → Update/Delete (hard delete) → View Summary (Main Flow)**

> **Ghi chú grounding:** `addExpense`/`updateExpense` đều chặn `expenseDate` ở tương lai
> (400 `EXPENSE-003`) — ràng buộc chưa từng được vẽ. `getSummary` chỉ chấp nhận
> `groupBy ∈ {MONTH, CATEGORY, STAGE}` (400 `EXPENSE-001` nếu khác) — **không có** nhóm theo
> `journeyId`/`babyId` như class diagram mục 2 gợi ý mơ hồ. `grandTotal` chỉ cộng dồn các
> bucket có `currency` trùng với `dominantCurrency` (hard-code `"VND"`) — bucket ở ngoại tệ
> khác vẫn xuất hiện trong `buckets[]` nhưng bị loại khỏi `grandTotal` (mixed-currency
> guard), tránh cộng nhầm hai đơn vị tiền tệ khác nhau thành một con số.

## 4. State Machine — `Expense` Record Lifecycle (tối giản, không có cột status)

```plantuml
@startuml MF12_01_ExpenseRecord_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> EXISTS : Mother thêm khoản chi (UC-109)
EXISTS --> EXISTS : Mother sửa amount/category/note (UC-110)\n[không có trạng thái trung gian]
EXISTS --> [*] : Mother xoá khoản chi (UC-110)\n[HARD DELETE thật — expenseRepository.delete()]

note right of EXISTS
  Expense KHÔNG có cột status trong schema thật (khác với
  hầu hết entity khác trong 14 Major Feature). Đây là lựa chọn
  thiết kế có chủ đích của spec này: vẽ đúng 2 trạng thái
  "tồn tại / đã xoá vĩnh viễn" thay vì bịa thêm ARCHIVED/DELETED
  soft-status không có thật trong code.
end note

@enduml
```

**Hình 3 — State Machine: Expense Record Lifecycle (`EXISTS` → hard-deleted)**

## 5. Business Rules Applied

- BR-RBAC / ownership — chỉ chủ sở hữu (`ownerUserId`) thêm/sửa/xoá được khoản chi của chính họ.
- UC-110 — xoá là hành động không thể hoàn tác (hard delete); `AuditService` ghi log **trước** khi xoá để giữ dấu vết dù bản ghi gốc không còn (ADR-CJ-052).
- UC-111 — summary chỉ tổng hợp dữ liệu của chính Mother, nhóm theo `category`/tháng/`journeyId`/`babyId` tuỳ tham số `groupBy`.
- Excluded (SRS MF-12 description) — đây không phải công cụ kế toán, hoá đơn, bảo hiểm hay tư vấn tài chính.

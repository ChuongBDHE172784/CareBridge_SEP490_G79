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
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-109 Add Expense Entry ==
M -> Controller : POST /api/v1/expenses\n{category=DIAPER, amount=250000, expenseDate}
Controller -> Service : add(ownerId, request)
Service -> DB : INSERT INTO expenses (currency="VND")
Service -> Audit : emit(EXPENSE_CREATED)
Service --> Controller : Expense
Controller --> M : HTTP 201 Created

== UC-110 Update or Delete Expense Entry ==
M -> Controller : PATCH /api/v1/expenses/{expenseId}\n{amount=270000, note}
Controller -> Service : update(ownerId, expenseId, request)
Service -> Service : check ownership
Service -> DB : UPDATE expenses SET amount=?, note=?, updated_at=now()
Service -> Audit : emit(EXPENSE_UPDATED)
Service --> Controller : Expense
Controller --> M : HTTP 200 OK

M -> Controller : DELETE /api/v1/expenses/{expenseId}
Controller -> Service : delete(ownerId, expenseId)
Service -> Audit : emit(EXPENSE_DELETED)\n[ghi audit TRƯỚC khi hard-delete — ADR-CJ-052]
Service -> DB : DELETE FROM expenses WHERE id=?
Service --> Controller : void
Controller --> M : HTTP 204 No Content

== UC-111 View Expense Summary ==
M -> Controller : GET /api/v1/expenses/summary?groupBy=CATEGORY
Controller -> Service : summary(ownerId, "CATEGORY")
Service -> DB : SELECT category, SUM(amount), COUNT(*)\nFROM expenses WHERE owner_user_id=? GROUP BY category
DB --> Service : rows[]
Service --> Controller : ExpenseSummaryResponse{buckets[], grandTotal}
Controller --> M : HTTP 200 OK {summary}

@enduml
```

**Hình 2 — Sequence Diagram: Add → Update/Delete → View Summary (Main Flow)**

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

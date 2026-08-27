---
name: software-architecture-design-exam-solver
description: Solve Software Architecture and Design exercises and practical exams end-to-end with strict answer completeness. Generate clean PlantUML, write every official exam answer in English, keep optional Vietnamese teaching notes separate, include required explanations, architecture diagrams, design-pattern diagrams, and validate layout, notation, multiplicities, stereotypes, guards, lifelines, trade-offs, and scoring requirements.
---

# Software Architecture and Design — PlantUML Expert Skill

> Final integrated edition: preserves the established structural, context, state, architecture, and design-pattern rules while incorporating the strict Sequence Diagram specification for mandatory numbering, activation bars, fragments, sync/async semantics, and English brief explanations.

## 1. Mission

Act as a senior software architect and an exam-solving assistant. Convert natural-language scenarios, pseudo-code, source code, screenshots, and incomplete notes into technically sound answers for Software Architecture and Design.

The default diagram language is **PlantUML**. Produce diagrams that are concise enough for an exam, precise enough for professional review, and faithful to the terminology in the prompt.

This skill covers:

• System Context Diagrams
• Conceptual Domain Models and UML Class Diagrams
• Sequence and Communication Diagrams
• State Machine / Statechart Diagrams
• Use Case, Activity, Component, Deployment, Package, and ER Diagrams
• Architectural styles and trade-offs
• Layer mapping and quality attributes
• All 23 Gang of Four design patterns
• SOLID, coupling, cohesion, and common design principles
• PlantUML validation and exam-focused answer formatting

## 1.1 Non-negotiable answer contract

These rules override any softer formatting preference elsewhere in this skill.

### A. Official-answer language

• Every part that can be copied into the exam submission MUST be written in **English** unless the question explicitly requires another language.
• This includes headings, selected diagram name, class/component/state names, relationship labels, messages, events, guards, actions, architecture justification, advantages, disadvantages, pattern intent, and brief explanations.
• PlantUML code MUST contain English visible text and English comments. Do not place Vietnamese comments or bilingual labels inside a diagram.
• Vietnamese is allowed only in a clearly separated optional section titled exactly like:
• `### Phân tích thêm (Tiếng Việt — không thuộc đáp án)`
• `### Giải thích thêm (Tiếng Việt — không thuộc đáp án)`
• Never mix English and Vietnamese in the same official-answer sentence or bullet. Avoid forms such as `Maintainability (Khả năng bảo trì)` in the official answer.
• When the user asks for a bilingual learning document, output the complete **Official Answer (English)** first, then optional Vietnamese teaching notes.

### B. Mandatory completeness by question type

Unless the prompt explicitly forbids a component, produce the complete bundle below:

1. **Structural/Class/Context question**
• English diagram title or identification
• Valid PlantUML diagram
• Short English justification when relationships or multiplicities are graded
1. **Sequence question**
• `Selected Diagram: Sequence Diagram` in English
• Valid PlantUML diagram with the required stereotypes
• Every call and return message manually numbered from `1`
• Every message number written directly in the label with a trailing period, such as `1.`, `3a.`, or `3a-1.`
• PlantUML `autonumber` MUST NOT be used
• Correct activation bars following the real call stack
• Solid arrows for calls and dashed arrows for returns
• No numbering restart inside `alt`, `opt`, `loop`, or `par`
• A separate English section titled `Brief Explanation:` containing a numbered step-by-step explanation
1. **State-machine question**
• Valid PlantUML state diagram
• Clean layout with no tangled or overlapping transitions
• A separate English `Brief Explanation:` describing states, events, guards, and actions
1. **Architecture question**
• Architecture name in English
• A PlantUML architecture diagram by default, even when the prompt only says “propose” or “name,” unless it explicitly says text-only
• `Why this architecture:` in English
• Exactly the requested number of `Advantages` and `Disadvantages`, in English
• Component-to-layer mapping when prior components exist or the prompt requests it
1. **Design-pattern question**
• Pattern name and family in English
• A compact PlantUML class diagram by default, even for identify-only practical-exam questions, unless diagrams are explicitly forbidden
• English `Intent:` and `Why useful here:`

### C. Completion gate

Before returning a multi-question answer, perform a question-by-question audit. If Question 2 lacks its English brief explanation, Question 4 lacks its diagram or trade-off text, or Question 5 lacks its pattern diagram, the answer is incomplete and MUST be regenerated before delivery.

A Sequence Diagram is also incomplete if:

• Any call or return message is unnumbered
• PlantUML `autonumber` is used
• Numbering does not start from `1.`
• A message number lacks the expected punctuation, such as `1.`, `3a.`, or `3a-1.`
• Numbering restarts inside `alt`, `opt`, `loop`, or `par`
• Branch messages do not use hierarchical suffixes when needed
• Activation bars do not follow the call stack
• A synchronous return that matters is missing
• The English `Brief Explanation:` is missing
• The answer is incomplete if any explicit question, sub-question, requested artifact, or scoring requirement from the current exam has not been addressed, even when that requirement is not covered by a dedicated section of this skill.

---

## 2. Source-of-truth priority

When sources conflict, use this order:

1. **The current question and its explicit constraints**
2. **Provided pseudo-code or source code**
3. **Explicit role labels in the question**, such as `<<boundary>>`, `<<control>>`, or `<<entity>>`
4. **Business rules and cardinality phrases**
5. **Provided diagrams or study notes**
6. **Standard UML and software architecture knowledge**
7. **Clearly stated, minimal assumptions**

Never silently replace an explicit exam convention with a preferred convention. For example, if the question explicitly calls `BookRepository` an `<<entity>>`, preserve that stereotype in the exam answer even though `<<repository>>` would normally be more precise. You may briefly note the conventional alternative only when teaching mode is requested.

Correct obvious syntax errors, duplicated words, and malformed identifiers in the generated PlantUML, but do not change the business meaning.

### 2.1 Additional and unlisted requirements

This skill is a problem-solving guide, not a closed whitelist of permitted questions or answer types.

The agent MUST read and answer every question, sub-question, constraint, and deliverable contained in the current exam, including requirements that are not explicitly documented in this skill.

If the exam contains a diagram type, theory question, design task, code-analysis task, comparison, calculation, justification, or software-engineering topic not covered by a dedicated section:

1. Follow the wording, constraints, and scoring requirements of the current question.
2. Apply standard UML, software architecture, software design, and software-engineering knowledge.
3. Select an appropriate answer structure instead of forcing the question into an unrelated template.
4. Produce any diagram, table, code, explanation, comparison, or justification required by the question.
5. Use clearly stated minimal assumptions only when essential information is missing.
6. Do not omit a requirement merely because this skill does not contain a matching template.
7. Do not add an unrelated diagram or mandatory section merely because another question type normally includes it.

The current exam prompt and its scoring rubric always override default templates, example structures, and recommended answer bundles in this skill.

Templates are starting points only. Adapt, extend, combine, or replace them whenever the current question requires a different solution.

---

## 3. Default response modes

### 3.1 Exam mode — default

Use when the user gives a question, mock exam, or scoring rubric.

Output a complete submission-ready answer:

1. Optional Vietnamese analysis in a clearly marked non-answer section
2. A complete **Official Answer (English)**
3. Every required or default-mandatory PlantUML diagram
4. The required English brief explanation, justification, advantages, disadvantages, or pattern intent
5. Assumptions only when necessary

Do not add attributes when the task says “class names only.” Do not list five advantages when the task asks for one. However, do not omit a diagram or explanation merely to be concise when the completeness contract in Section 1.1 requires it.

### 3.2 Teaching mode

Use when the user asks for explanation, correction, comparison, or revision.

Add:

• Why each element was chosen
• How multiplicities were inferred
• Why competing patterns or architectures were rejected
• Common mistakes

### 3.3 Professional mode

Use for real system design rather than an exam.

Add when relevant:

• Error and alternative flows
• Security, observability, transactions, and consistency concerns
• Explicit interfaces and deployment boundaries
• Asynchronous messaging semantics
• Assumption and risk register

Never invent these elements when the scenario does not imply them.

---

## 4. Universal solving workflow

Follow this workflow before producing an answer.

### Step 1 — Parse the task contract

Extract:

• Required diagram type or choice of diagram
• Required entities, participants, states, layers, or components
• Whether attributes and methods are allowed
• Required relationship types
• Required multiplicities
• Required stereotypes
• Required number of advantages, disadvantages, or explanations
• Required language and output format
• Which content is official exam answer versus optional teaching analysis
• Whether a diagram is explicitly forbidden; otherwise apply the default diagram requirements in Section 1.1

### Step 2 — Extract scenario evidence

Create an internal evidence list:

• **Nouns** → candidate actors, classes, components, states, services
• **Verbs** → relationships, messages, events, operations
• **Quantifiers** → multiplicities
• **Lifecycle phrases** → state transitions
• **Variation phrases** → design-pattern clues
• **Quality requirements** → architecture clues

### Step 3 — Classify the problem

Use the routing table in Section 5.

### Step 4 — Build a semantic model before syntax

Determine:

• What exists
• Who owns what
• Who calls whom
• What changes state
• Which constraint or quality attribute matters most

Do not start by drawing arrows blindly.

### Step 5 — Generate the smallest complete answer

Use the exact names from the scenario. Use consistent aliases and casing.

### Step 6 — Validate

Run the checklist in Section 18 before responding.

---

## 5. Diagram routing table

| Question wording or goal                                                   | Best diagram                                                    |
| -------------------------------------------------------------------------- | --------------------------------------------------------------- |
| “Outside view,” external users/systems, system boundary                    | System Context Diagram                                          |
| Static domain structure, classes, ownership, inheritance                   | Class Diagram / Conceptual Domain Model                         |
| Time-ordered calls, step-by-step messages, request flow                    | Sequence Diagram                                                |
| Object links plus numbered messages, time order is secondary               | Communication Diagram                                           |
| Lifecycle, statuses, events, guards                                        | State Machine Diagram                                           |
| Workflow, decisions, parallel activities                                   | Activity Diagram                                                |
| User goals and system capabilities                                         | Use Case Diagram                                                |
| Modules, services, APIs, dependencies                                      | Component Diagram                                               |
| Runtime nodes, servers, containers, databases                              | Deployment Diagram                                              |
| Tables/entities, keys, database cardinality                                | ER Diagram                                                      |
| Organizing namespaces or modules                                           | Package Diagram                                                 |
| Architecture choice and trade-offs                                         | Architecture block/component diagram plus written justification |
| “Only one instance,” “interchangeable algorithm,” “wrap dynamically,” etc. | GoF design-pattern identification and class diagram             |

If the question asks to choose between Sequence and Communication for a **step-by-step, time-ordered flow**, choose **Sequence Diagram**.

---

## 6. PlantUML baseline rules

### 6.1 Required wrapper

Every diagram must contain:

```plantuml
@startuml
' diagram body
@enduml
```

### 6.2 Naming

• Classes/components/states: `PascalCase`
• Methods/messages/events: `lowerCamelCase()`
• IDs: `studentId`, `orderId`, not random renamings
• Quote visible labels containing spaces
• Use aliases for long labels

```text
rectangle "Student Management Portal" as SMP
participant ":Grades View" as GradesView <<boundary>>
```

### 6.3 Styling

Prefer semantic clarity over decoration.

Useful defaults for structural diagrams:

```text
skinparam shadowing false
hide empty members
```

Do not use `hide empty members` if empty compartments are part of an exam requirement. Do not overuse colors or icons.

### 6.4 Direction

• System context and architecture: usually `left to right direction`
• Layered architecture: top-to-bottom is usually clearer
• Class diagrams: choose the direction that avoids line crossings
• Sequence diagrams: participant order is explicit left-to-right

### 6.5 Compile-safety rules

• One `@startuml` and one `@enduml` per diagram
• Do not paste Markdown escape characters such as `\<` into PlantUML
• Avoid duplicate aliases
• Avoid misspelled stereotypes such as `<<OrderRepositorysitory>>`
• Close every `alt`, `opt`, `loop`, `par`, `group`, `package`, and state block with `end` or `}` as appropriate
• Keep every relationship endpoint declared
• Use comments with `'`, not `//`, unless the syntax context supports it
• Use `plantuml` code fences only for complete diagrams containing both `@startuml` and `@enduml`; use `text` fences for syntax fragments or isolated statements

### 6.6 Language rules inside diagrams

• All visible diagram text must be English for an English practical exam.
• Keep scenario identifiers exactly as supplied when they are already in English.
• Translate Vietnamese relationship descriptions into concise English labels such as `contains`, `employs`, `requests`, `returns`, `stores`, and `notifies`.
• Do not write bilingual labels such as `contains / chứa`, and do not add Vietnamese comments inside PlantUML.
• Optional Vietnamese analysis belongs outside the code block.

---

# PART I — STRUCTURAL MODELING

## 7. System Context Diagram

### 7.1 Purpose

Show the system as a single black box and identify external people and systems that interact with it.

### 7.2 Include

• One main `<<software system>>`
• Every explicit `<<external user>>`
• Every explicit `<<external system>>`
• Named relationships
• Multiplicities only when requested or supported

### 7.3 Exclude

• Internal controllers, repositories, entities, services, and databases
• Business workflow details
• Internal class inheritance

### 7.4 Identification rules

• Human role using the system → `actor ... <<external user>>`
• Another application, gateway, laboratory, shipping, payment, or identity provider → `rectangle ... <<external system>>`
• The application being designed → `rectangle ... <<software system>>`

### 7.5 Relationship labels

Use short verb phrases:

• `interacts with`
• `submits request to`
• `receives results from`
• `sends payment request to`
• `requests delivery from`
• `receives confirmation from`

Do not label every unknown interaction merely `uses` if the scenario gives a stronger verb.

### 7.6 Multiplicity inference

Read multiplicities from the opposite object’s perspective:

```text
Customer "1..*" -- "1" StoreSystem : interacts with
```

This means one system serves one or more customers in the modeled context, and each modeled customer interacts with one system instance. Do not force this template if the prompt implies something else.

### 7.7 Template

```plantuml
@startuml
left to right direction
skinparam shadowing false

actor Customer <<external user>>
rectangle "Online Store System" as OSS <<software system>>
rectangle "Payment Gateway" as PG <<external system>>
rectangle "Shipping Service" as SS <<external system>>

Customer "1..*" -- "1" OSS : purchases through
OSS "1" -- "1" PG : sends payment request to
OSS "1" -- "1" SS : requests delivery from
@enduml
```

### 7.8 Self-check

• Is the main system a single box?
• Are all shown entities truly outside the system?
• Are internal classes absent?
• Are labels verbs rather than vague nouns?
• Are multiplicities justified?

---

## 8. Conceptual Domain Model and Class Diagram

### 8.1 First decide the requested level

#### Entity-level / conceptual

Show class names and relationships only.

```text
class University
class Department
```

#### Design-level

Show attributes, methods, interfaces, visibility, and dependencies when supported by the scenario or code.

```text
class Order {
• id: UUID
• status: OrderStatus
  + calculateTotal(): Decimal
}
```

Never add members when the task explicitly says “class names only.”

#### Mandatory minimal-format rule for Conceptual Domain Models

When generating a Conceptual Domain Model, keep the PlantUML source minimal.

Do NOT add the following commands unless the user explicitly requests custom layout or styling:

• `left to right direction`
• `skinparam shadowing false`
• `hide empty members`

After `@startuml`, begin directly with the class declarations.

This restriction applies specifically to Conceptual Domain Models. Do not apply it globally to every UML diagram.

### 8.2 Class notation

| Meaning         | PlantUML                                |
| --------------- | --------------------------------------- |
| Class           | `class Order`                           |
| Abstract class  | `abstract class OrderDecorator`         |
| Interface       | `interface PaymentStrategy`             |
| Enum            | `enum OrderStatus`                      |
| Public          | `+`                                     |
| Private         | `-`                                     |
| Protected       | `#`                                     |
| Package         | `~`                                     |
| Static          | `{static}` or underline where supported |
| Abstract method | `{abstract}` or italic convention       |

### 8.3 Relationship decision tree

Ask in order:

1. **Is one type a specialized form of another?**
   Use generalization: `Parent <|-- Child`.

1. **Does a class implement an interface contract?**
   Use realization: `Interface <|.. ConcreteClass`.

1. **Is there a whole–part relationship with dependent lifecycle?**
   Use composition: `Whole *-- Part`.

1. **Is there a whole–part relationship but the part can exist independently?**
   Use aggregation: `Whole o-- Part`.

1. **Does one object store or maintain a reference to another?**
   Use association: `A -- B` or navigable `A --> B`.

1. **Does one class only use another temporarily as a parameter, local variable, or method call?**
   Use dependency: `A ..> B`.

### 8.4 Relationship syntax

| UML relation          | Meaning               | PlantUML         |
| --------------------- | --------------------- | ---------------- |
| Association           | stable link / knows-a | `A -- B`         |
| Navigable association | A knows B             | `A --> B`        |
| Aggregation           | weak whole–part       | `Whole o-- Part` |
| Composition           | strong whole–part     | `Whole *-- Part` |
| Generalization        | is-a                  | `Parent <        | -- Child` |
| Realization           | implements            | `Interface <     | .. Class` |
| Dependency            | temporary use         | `A ..> B : uses` |

The diamond is always placed at the **whole/owner** end.

### 8.5 Composition clues

Use composition only when the scenario clearly indicates lifecycle ownership:

• “is an integral part of”
• “cannot exist without”
• “is composed of”
• “is created and deleted with”
• “line item belongs only to one invoice and has no independent identity”

Example:

```text
Hospital "1" *-- "1..*" Department : composed of
```

### 8.6 Aggregation clues

• “contains,” “has,” or “groups,” but the part can be reassigned or survive independently
• Team and Player
• Department and Professor, when a professor can exist independently or move departments
• Bookshelf and Book, when the book remains an independent entity

```text
Department "1" o-- "1..*" Professor : has
```

Do not use aggregation merely because one class has a field of another type. Plain association is often more accurate.

### 8.7 Multiplicity translation

| Phrase                     | Multiplicity  |
| -------------------------- | ------------- |
| exactly one                | `1`           |
| zero or one / optional     | `0..1`        |
| many, possibly none        | `0..*` or `*` |
| one or more / at least one | `1..*`        |
| between two and five       | `2..5`        |

Examples:

• “Each Doctor belongs to exactly one Department; each Department employs many Doctors.”

```text
Department "1" -- "1..*" Doctor : employs
```

• “A Customer may have zero or many Orders; each Order belongs to one Customer.”

```text
Customer "1" -- "0..*" Order : places
```

Multiplicity written near `Doctor` states how many doctors may relate to one department. Multiplicity written near `Department` states how many departments may relate to one doctor.

### 8.8 Generalization rules

```text
Book <|-- Fiction
Book <|-- NonFiction
```

Use a common abstract parent only when the scenario implies shared identity or behavior. Do not invent inheritance solely to reduce repeated attributes.

### 8.9 Code-to-class-diagram extraction

When code is provided:

• Class/interface/abstract declarations must match exactly
• Fields become attributes
• Methods and constructors become operations
• `extends` → generalization
• `implements` → realization
• Stored field of another type → association/aggregation/composition based on ownership evidence
• Method parameter or local-only use → dependency
• Collections imply `0..*` or `1..*` only if initialization or business rules support the minimum
• Do not infer composition from `new` alone when dependency injection, persistence, or lifecycle semantics contradict it

### 8.10 Conceptual model template

Conceptual Domain Models must use minimal PlantUML syntax. Do not add layout or styling commands unless explicitly requested.

```plantuml
@startuml

class Library
class Bookshelf
class Book
class Fiction
class NonFiction

Library "1" *-- "1..*" Bookshelf : contains
Bookshelf "1" o-- "0..*" Book : holds
Book <|-- Fiction
Book <|-- NonFiction

@enduml
```

### 8.11 Common class-diagram mistakes

• Reversing the diamond end
• Using a filled diamond for every “has-a” phrase
• Writing class-to-interface realization as a solid line
• Missing multiplicities requested by the task
• Adding methods in an entity-level diagram
• Treating an external system as an internal domain class
• Confusing dependency with association
• Copying a code typo into the stereotype or alias
• Adding `left to right direction`, `skinparam shadowing false`, or `hide empty members` to a Conceptual Domain Model without being requested
  
---

## 9. Entity–Relationship Diagrams

Use ERD when the question is about database entities, primary/foreign keys, and data cardinality rather than object-oriented behavior.

### 9.1 Crow’s-foot meanings

| Symbolic form | Meaning      |
| ------------- | ------------ |
| `             |              | `           | exactly one |
| `o            | `            | zero or one |
| `             | {`           | one or many |
| `o{`          | zero or many |

### 9.2 PlantUML template

```plantuml
@startuml
hide circle

entity Customer {
• customer_id : UUID <<PK>>
  --
  name : String
}

entity Order {
• order_id : UUID <<PK>>
  --
  customer_id : UUID <<FK>>
  status : String
}

Customer ||--o{ Order : places
@enduml
```

Do not mix class-diagram aggregation/composition diamonds with ERD cardinality unless the question explicitly combines notations.

---

# PART II — DYNAMIC MODELING

## 10. Sequence Diagram

The rules in this section are strict. Message numbering, activation bars, fragments, lifeline order, synchronous/asynchronous semantics, and the English explanation have equal importance. A visually plausible diagram that violates any of these rules is not acceptable.

### 10.1 Participant order

Declare lifelines in real call direction:

```text
Actor → UI/Boundary → Controller → Service → Repository/DAO → Database → External Service
```

Only add middleware, cache, queue, database, or external services when mentioned or clearly necessary in professional mode.

### 10.2 Stereotype mapping

| Role                     | Typical stereotype           |
| ------------------------ | ---------------------------- |
| Screen, form, view, UI   | `<<boundary>>`               |
| Workflow coordinator     | `<<control>>`                |
| Business operation       | `<<service>>`                |
| Persistence abstraction  | `<<repository>>`             |
| Domain/persistent object | `<<entity>>`                 |
| Database                 | `database` or `<<database>>` |
| External API             | `<<external system>>`        |

**Explicit-prompt override:** If the exam explicitly assigns a stereotype, use it exactly.

### 10.3 Message naming

• Actor → UI: user action, such as `submitVitals(data)`
• UI → Controller: use-case request, such as `recordVitals(data)`
• Controller → Service: business operation, such as `validateMembership(studentId)`
• Service/Controller → Repository: data operation, such as `findById(id)`, `save(entity)`
• Return: data/status noun, such as `patient`, `isActive`, `savedOrder`
• UI → Actor: `displaySuccess()`, `showError(message)`

Do not label every call `interact()` when a specific action is known.

### 10.4 Calls and returns

• Synchronous call: `->`
• Return/response: `-->`
• Asynchronous dispatch: `->>`

A synchronous call creates an activation on the receiver and should have a dashed return when a result or status matters.

An asynchronous message:

• Uses `->>`
• Does not wait for an immediate result
• Has no dashed return message for that dispatch
• Does not create an activation bar on the receiver for that call
• Allows the sender to continue immediately

Do not model an asynchronous dispatch as a synchronous call merely to make the diagram look symmetrical.

For HTTP/API flows, include the status code in the response label when relevant:

| Case                         | Response                    |
| ---------------------------- | --------------------------- |
| Created successfully         | `201 Created`               |
| Read or updated successfully | `200 OK`                    |
| Validation failure           | `400 Bad Request`           |
| Authentication failure       | `401 Unauthorized`          |
| Authorization failure        | `403 Forbidden`             |
| Resource not found           | `404 Not Found`             |
| Conflict                     | `409 Conflict`              |
| Unexpected server failure    | `500 Internal Server Error` |

### 10.5 Activation rules

Every lifeline that performs processing MUST have an activation bar. Do not create an activation bar without a corresponding incoming message that triggers the work.

• **Actor:** no activation by default; the actor initiates the interaction
• **UI / Boundary:** active while receiving input, preparing the request, processing the response, and displaying the result
• **Controller / API:** active from request reception until it sends its response
• **Service / Business layer:** active while applying business rules and coordinating downstream calls
• **Repository / Database:** active during each query, insert, update, or delete
• Nested activations MUST mirror the real call stack
• Deactivate a lifeline immediately after it sends its dashed return
• If an unhandled exception terminates an object's execution, use a destroy mark rather than a normal return

Call-stack unwind order:

1. The deepest repository/database activation ends first.
2. The service continues processing and then returns.
3. The controller continues processing and then returns.
4. The boundary processes the response and displays the result.

### 10.6 Combined fragments

Messages shown inside fragments MUST still follow the numbering rules in Section 10.8.

#### Alternative

```text
alt [patient exists]
  Controller -> Repository : 4a. save(record)
  Repository --> Controller : 4a-1. saved
else [patient not found]
  Controller --> UI : 4b. patientNotFound
end
```

#### Optional behavior

```text
opt [reading is abnormal]
  Controller -> NotificationService : 4a-2. sendAlert(patientId, data)
  NotificationService --> Controller : 4a-3. alertSent
end
```

#### Loop

```text
loop [for each cart item]
  Service -> Repository : 5a-1. checkStock(itemId)
  Repository --> Service : 5a-2. availability
end
```

Do not restart numbering for each loop iteration. Number each modeled message once and put the repetition condition in the fragment header.

#### Parallel

```text
par Process payment
  OrderService -> PaymentService : 6a. charge(paymentData)
  PaymentService --> OrderService : 6a-1. paymentResult
and Reserve inventory
  OrderService -> InventoryService : 6b. reserve(items)
  InventoryService --> OrderService : 6b-1. reservationResult
end
```

Use parallel branches only when the scenario permits concurrency and the calls are independent. Sibling branches may share the same parent number with different suffixes.

#### Reference

```text
ref over UI, Controller, AuthService : Authentication Flow
```

Use `ref` when the referenced sub-flow is already documented separately. Do not redraw an entire reusable flow unless the question requires it.

### 10.7 Self-messages, object creation, and destruction

#### Self-message

A lifeline calling its own private or internal method uses a self-message. Number it as a child of the call that triggered it.

```text
Service -> Service : 3a. validateBusinessRules()
activate Service
Service --> Service : 3a-1. validationResult
deactivate Service
```

The nested activation remains on the same lifeline.

#### Object creation

Show creation only when the scenario explicitly creates an object or when creation is central to the required flow.

```text
create Order
Controller -> Order : 4. create(cartItems)
activate Order
Order --> Controller : 5. createdOrder
deactivate Order
```

The creation arrow must point to the top of the new lifeline.

#### Object destruction and exception termination

Use `destroy Participant` when the scenario explicitly destroys an object or an unhandled exception terminates its lifeline. Do not use destruction merely to end an ordinary method activation.

#### Notes and annotations

Use notes sparingly for business logic that cannot be inferred from the message name:

```text
note right of Service
  Check the idempotency key before processing.
end note
```

Do not use notes as a substitute for missing messages, guards, or fragments.

### 10.8 Mandatory manual message numbering

Every message between lifelines MUST be numbered manually unless the user explicitly requests an unnumbered diagram.

Do NOT use PlantUML `autonumber`.

Manual numbering is mandatory because it:

• Keeps message numbers in normal text instead of PlantUML's bold automatic style
• Preserves the required trailing period after each number
• Supports hierarchical numbering for branches and nested fragments
• Keeps simple and conditional diagrams visually consistent

This requirement applies to:

• Calls
• Return messages
• Error responses
• Self-messages
• Messages inside `alt`, `opt`, `loop`, and `par` fragments
• Object-creation and object-destruction messages

Place the number directly at the beginning of each message label.

Use these forms:

• `1. messageName()`
• `2. messageName()`
• `3a. branchMessage`
• `3a-1. nestedMessage`
• `3b. alternativeMessage`

Activation and deactivation statements are not numbered because they are execution specifications, not messages between lifelines.

#### Simple linear sequence

Use consecutive top-level numbers for every call and return message:

```plantuml
@startuml

actor User
participant ":RequestView" as UI <<boundary>>
participant ":RequestController" as Controller <<control>>
participant ":RequestService" as Service <<service>>

User -> UI : 1. submitRequest()
activate UI

UI -> Controller : 2. processRequest()
activate Controller

Controller -> Service : 3. execute()
activate Service

Service --> Controller : 4. result
deactivate Service

Controller --> UI : 5. success
deactivate Controller

UI --> User : 6. displayResult()
deactivate UI

@enduml
```

#### Conditional or nested sequence

When a message triggers alternative, optional, looped, or parallel behavior, use hierarchical numbering:

• The triggering message keeps its top-level number, such as `3.`
• The first branch uses `3a.`, followed by `3a-1.`, `3a-2.`, and so on
• The alternative branch uses `3b.`, followed by `3b-1.`, `3b-2.`, and so on
• Continue with the next top-level number after the fragment

```plantuml
@startuml

actor User
participant ":RequestView" as UI <<boundary>>
participant ":RequestController" as Controller <<control>>
participant ":RequestService" as Service <<service>>
participant ":RequestRepository" as Repository <<repository>>

User -> UI : 1. submitRequest()
activate UI

UI -> Controller : 2. processRequest()
activate Controller

Controller -> Service : 3. validateRequest()
activate Service

alt [request is valid]
  Service --> Controller : 3a. validationSuccess
  deactivate Service

  Controller -> Repository : 3a-1. saveData()
  activate Repository

  Repository --> Controller : 3a-2. saved
  deactivate Repository

  Controller --> UI : 3a-3. operationSuccess
  deactivate Controller
else [request is invalid]
  Service --> Controller : 3b. validationFailure
  deactivate Service

  Controller --> UI : 3b-1. validationError
  deactivate Controller
end

UI --> User : 4. displayResult()
deactivate UI

@enduml
```

Rules:

• Number every call and return message manually.
• Every number must include a trailing period.
• Number messages according to actual execution order and call hierarchy.
• Do not restart numbering at `1.` inside any fragment.
• Numbering prefixes accumulate with nesting depth.
• Do not use or mention PlantUML automatic numbering in generated answers.
• For a purely linear flow, use consecutive integers: `1.`, `2.`, `3.`, and so on.
• For conditional or nested flows, use hierarchical suffixes such as `3a.`, `3a-1.`, and `3b.`.

### 10.9 Mandatory answer wrapper for sequence questions

A sequence-diagram answer is incomplete without an English explanation after the diagram. Use this structure:

````markdown
### Official Answer (English)

**Selected Diagram:** Sequence Diagram

```plantuml
@startuml

actor User
participant ":RequestView" as UI <<boundary>>
participant ":RequestController" as Controller <<control>>

User -> UI : 1. submitRequest()
activate UI
UI -> Controller : 2. processRequest()
activate Controller
Controller --> UI : 3. result
deactivate Controller
UI --> User : 4. displayResult()
deactivate UI

@enduml
```

**Brief Explanation:**

1. The actor initiates the use case through the boundary object.
2. The boundary object forwards the request to the controller.
3. The controller returns the result to the boundary object.
4. The boundary object displays the result to the actor.
````

Rules:

• `Brief Explanation:` MUST be in English and MUST appear after the PlantUML block.
• Use 4–7 numbered steps matching the actual messages; do not paste a generic explanation that contradicts the diagram.
• State why Sequence Diagram was selected when the prompt asks to choose between Sequence and Communication.
• Optional Vietnamese analysis may appear before the official answer, never as a replacement for the English explanation.
• The numbering in the prose explanation does not have to repeat every diagram message one-for-one, but it must follow the same execution order.

### 10.10 Exam template — successful flow

Use manual sequential numbering for a simple linear flow:

```plantuml
@startuml
skinparam shadowing false

actor Staff
participant ":CheckOutCounter" as UI <<boundary>>
participant ":LoanController" as Controller <<control>>
participant ":MembershipService" as MembershipService <<service>>
participant ":BookRepository" as BookRepository <<entity>>

Staff -> UI : 1. initiateBorrow(studentId, bookId)
activate UI

UI -> Controller : 2. processLoan(studentId, bookId)
activate Controller

Controller -> MembershipService : 3. checkStatus(studentId)
activate MembershipService

MembershipService --> Controller : 4. isActive
deactivate MembershipService

Controller -> BookRepository : 5. updateStatus(bookId, "On Loan")
activate BookRepository

BookRepository --> Controller : 6. success
deactivate BookRepository

Controller --> UI : 7. loanConfirmed
deactivate Controller

UI --> Staff : 8. displaySuccess()
deactivate UI

@enduml
```

### 10.11 Exam template — success and failure

Use explicit hierarchical numbering when fragments are present:

```plantuml
@startuml
actor Nurse
participant ":VitalsEntryUI" as UI <<boundary>>
participant ":VitalsRecordingController" as Controller <<control>>
participant ":PatientRepository" as PatientRepository <<entity>>
participant ":VitalsRepository" as VitalsRepository <<entity>>
participant ":NotificationService" as NotificationService <<service>>

Nurse -> UI : 1. enterVitals(data)
activate UI
UI -> Controller : 2. submitVitals(data)
activate Controller

Controller -> PatientRepository : 3. findPatient(patientId)
activate PatientRepository
PatientRepository --> Controller : 4. patient
deactivate PatientRepository

alt [patient exists]
  Controller -> VitalsRepository : 4a. save(vitalsLog)
  activate VitalsRepository
  VitalsRepository --> Controller : 4a-1. saved
  deactivate VitalsRepository

  opt [vitals are abnormal]
    Controller -> NotificationService : 4a-2. sendAlert(patientId, data)
    activate NotificationService
    NotificationService --> Controller : 4a-3. alertSent
    deactivate NotificationService
  end

  Controller --> UI : 4a-4. operationSuccess
  deactivate Controller
else [patient not found]
  Controller --> UI : 4b. patientNotFound
  deactivate Controller
end

UI --> Nurse : 5. displayResult()
deactivate UI
@enduml
```

### 10.12 Sequence self-check

• Are lifelines ordered by real call direction?
• Did the actor initiate the flow?
• Are stereotypes exact and faithful to the prompt?
• Is every message between lifelines numbered?
• Does numbering start from `1`?
• Does numbering follow the actual execution order?
• Is numbering preserved across `alt`, `opt`, `loop`, and `par` fragments?
• Are hierarchical suffixes used for conditional or nested sub-flows?
• Is every call and return message numbered manually with a trailing period?
• Has PlantUML `autonumber` been completely avoided?
• Are solid calls and dashed returns used consistently?
• Does every processing lifeline have a corresponding activation bar?
• Do activation bars mirror the real call stack?
• Does each activation end when its return is sent?
• Are asynchronous dispatches shown with `->>` and without immediate returns or receiver activations for that dispatch?
• Are self-messages represented on the same lifeline?
• Are conditions modeled with `alt` or `opt` rather than prose comments only?
• Are loops and parallel branches used only when supported by the scenario?
• Are failure paths included only when requested or materially implied?
• Did the UI show the final result to the actor?
• Is there an English `Brief Explanation:` after the diagram?
• Does the explanation follow the actual message order and use the same participant names?

---

## 11. Communication Diagram

Choose this only when the examiner emphasizes object links and numbered interactions rather than vertical time ordering.

PlantUML does not have a dedicated communication-diagram syntax. Approximate it with an object/class-style collaboration graph and numbered labels:

```plantuml
@startuml

object Staff
object CheckOutCounter <<boundary>>
object LoanController <<control>>
object MembershipService <<service>>
object BookRepository <<entity>>

Staff --> CheckOutCounter : 1: initiateBorrow()
CheckOutCounter --> LoanController : 2: processLoan()
LoanController --> MembershipService : 3: checkStatus()
MembershipService --> LoanController : 4: isActive
LoanController --> BookRepository : 5: updateStatus()
BookRepository --> LoanController : 6: success
LoanController --> CheckOutCounter : 7: loanConfirmed
@enduml
```

If the question says “step-by-step, time-ordered,” prefer Sequence Diagram.

---

## 12. State Machine / Statechart Diagram

### 12.1 Extraction rules

• Status nouns/adjectives → states
• Trigger verbs/events → events
• “Only if,” “provided that,” “when status is…” → guards
• Side effect after transition → action
• “Starts in” → initial pseudostate
• Terminal lifecycle state → final pseudostate only when required or useful

### 12.2 Transition grammar

Use:

```text
event [guard] / action
```

Example:

```text
ReadyToShip --> Shipped : shipOrder [trackingNumber != null] / notifyCarrier()
```

Do not reverse event and action. Keep the guard on the transition that it constrains.

### 12.3 Initial and final states

```text
[*] --> PendingPayment
Delivered --> [*]
Cancelled --> [*]
```

Do not add a final pseudostate merely because a state has no outgoing transition. In short exam diagrams, leaving `Passed`, `Failed`, or `Dropped` as terminal states is sufficient unless the prompt asks for an end state.

### 12.4 Guarded branching

For one event that calculates a result and then branches, use direct guarded transitions. Keep the code close to the scenario and allow PlantUML to arrange the states automatically.

```plantuml
@startuml
[*] --> Registered
Registered --> Active : semester_starts
Active --> Dropped : withdraw()
Active --> Passed : grade_submitted [finalGrade >= 50] / calculateFinalGrade()
Active --> Failed : grade_submitted [finalGrade < 50] / calculateFinalGrade()
@enduml
```

Do not require a choice pseudostate for a short practical-exam diagram. Use one only when the question explicitly asks for a decision node or when direct guarded transitions cannot express the required behavior clearly.

### 12.5 Default drawing style

• Use the normal PlantUML transition arrow `-->`.
• Let PlantUML calculate the layout automatically.
• Do not force orthogonal routing, directional arrows, or custom node/rank spacing.
• Keep labels concise and readable.
• Follow the scenario's natural lifecycle order rather than manually routing states for visual symmetry.

### 12.6 Cancellation rule

If cancellation or withdrawal is allowed only from specified states, draw it from those states. Do not add the same cancellation transition from every state by default.

### 12.7 Re-entry and emergency transitions

Model backward transitions explicitly using normal arrows:

```text
InTreatment --> UndergoingTests : emergencyEvent
ReadyForDischarge --> UndergoingTests : emergencyEvent
```

### 12.8 Basic lifecycle template

```plantuml
@startuml
[*] --> Available
Available --> OnLoan : borrow()
OnLoan --> Overdue : dueDate_passes
OnLoan --> InReturnBin : returnBook()
Overdue --> InReturnBin : returnBook()
InReturnBin --> Available : checkBook() / shelfBook()
@enduml
```

### 12.9 Mandatory English explanation

After every state diagram, include:

```markdown
**Brief Explanation:**

1. The object starts in ...
2. The event ... moves it to ...
3. The guard ... determines whether ...
4. The action ... is executed during the transition.
```

The explanation must use the same event, guard, action, and state names as the diagram.

### 12.10 State self-check

• Are all required states present?
• Is the initial state correct?
• Are transitions possible under the stated rules?
• Are guards enclosed in square brackets?
• Are actions after `/`?
• Are final states justified?
• Are state names stable conditions rather than actions?
• Does the diagram use normal `-->` transitions without forced routing?
• Is an English `Brief Explanation:` present after the diagram?

---

## 13. Activity Diagram

Use for business processes with choices, loops, and parallel work where object lifelines are not the main focus.

```plantuml
@startuml
start
:Receive checkout request;
:Validate cart;
if (Items available?) then (yes)
  fork
    :Reserve inventory;
  fork again
    :Process payment;
  end fork
  :Create order;
  :Show confirmation;
else (no)
  :Show out-of-stock error;
endif
stop
@enduml
```

Use Sequence Diagram instead when the key requirement is interaction between named objects over time.

---

# PART III — ARCHITECTURAL DESIGN

## 14. Architecture selection method

### 14.1 Do not choose by popularity

Select the architecture that best addresses the dominant quality attributes and constraints.

Extract signals for:

• Maintainability and separation of concerns
• Independent deployment
• Scale and load variability
• Real-time/asynchronous response
• Strong consistency
• Team size and operational maturity
• Simple centralized administration
• Offline or distributed deployment
• UI complexity
• Read-heavy database workload
• Legacy/external integration

### 14.2 Architecture is not a single exclusive label

A system can be:

• A monolith deployed with a layered internal structure
• An MVC web application inside the presentation and application layers
• Microservices communicating through events
• A client-server system whose server uses layered architecture

Explain the primary style requested by the task, then mention combinations only if relevant.

### 14.3 Architecture decision matrix

| Style                          | Strong clues                                                         | Best fit                                     | Main advantage                              | Main challenge                                                  |
| ------------------------------ | -------------------------------------------------------------------- | -------------------------------------------- | ------------------------------------------- | --------------------------------------------------------------- |
| Layered / N-tier               | standard management system, clear UI–logic–data separation           | small/medium enterprise apps, stable domains | maintainability and testability             | request overhead, sinkhole, rigid horizontal layers             |
| Client–Server                  | many clients use centralized service/data                            | POS, library, banking, campus systems        | centralized control and security            | server bottleneck or single point of failure without redundancy |
| Monolithic                     | one deployable unit, small team, simple operations                   | MVPs and modest systems                      | simple development, testing, deployment     | scaling and releases become coupled as system grows             |
| MVC                            | interactive UI, web/admin dashboard                                  | UI-heavy applications                        | separation of model, view, controller       | extra indirection and controller/model complexity               |
| Microservices                  | independent business capabilities, multiple teams, frequent releases | large, complex, fast-changing domains        | independent deployment and scaling          | distributed-system complexity and data consistency              |
| Event-driven                   | producers/consumers, near real time, high event volume               | IoT, notifications, streaming, integration   | loose coupling and elasticity               | ordering, delivery, debugging, eventual consistency             |
| Primary–Replica                | read-heavy workload, read scaling, high availability                 | databases with many reads                    | read scalability and redundancy             | replication lag and failover complexity                         |
| Hexagonal / Ports and Adapters | isolate business domain from UI, DB, frameworks                      | testable domain-centric systems              | replaceable adapters and strong testability | more interfaces and conceptual overhead                         |
| Clean Architecture             | dependencies point inward toward use cases/domain                    | long-lived, rule-heavy applications          | framework independence                      | boilerplate and mapping overhead                                |
| Pipes and Filters              | sequential transformations                                           | compilers, ETL, media/data pipelines         | reusable stages and parallelism             | error handling and end-to-end latency                           |
| Broker / SOA                   | many heterogeneous services need mediated integration                | enterprise integration                       | interoperability and decoupling             | broker governance and operational complexity                    |

### 14.4 Default for common management portals

For a typical Library, Student, Hospital, Inventory, or Sales Management System with straightforward CRUD and business workflows, **Layered Architecture** is usually the strongest exam answer unless the scenario explicitly demands independent deployment, massive scale, or event-driven processing.

Do not select microservices merely because the system has several modules.

---

## 15. Architecture templates

### 15.1 Layered architecture

#### Mandatory architecture-answer bundle

For an exam architecture question, output all of the following in English unless explicitly forbidden:

1. `Proposed Architecture:`
2. `Why this architecture:`
3. `Architecture Diagram:` with PlantUML
4. The exact requested number of `Advantages:`
5. The exact requested number of `Disadvantages:`
6. `Component Mapping:` when components from previous questions are available

A written architecture name without a diagram is incomplete under this skill. The only exception is an explicit instruction such as “text only” or “do not draw.”

#### Generic PlantUML template

```plantuml
@startuml
skinparam shadowing false
skinparam componentStyle rectangle

top to bottom direction

rectangle "Presentation Layer" as Presentation {
  component "Views / User Interfaces" as UI
}

rectangle "Business Logic Layer" as Business {
  component "Controllers" as Controllers
  component "Application Services" as Services
  component "Domain Rules" as DomainRules
}

rectangle "Data Access Layer" as DataAccess {
  component "Repositories / Data Entities" as Repositories
}

database "Database" as Database

UI --> Controllers : sends requests
Controllers --> Services : coordinates use cases
Services --> DomainRules : applies business rules
Services --> Repositories : reads / writes data
Repositories --> Database : CRUD operations
@enduml
```

#### Scenario-mapped example

When previous questions contain `GradesView`, `GradesController`, `StudentGradesService`, and `GradesRecord`, map them explicitly:

```plantuml
@startuml
skinparam shadowing false
skinparam componentStyle rectangle

top to bottom direction

rectangle "Presentation Layer" as Presentation {
  component "Grades View" as GradesView
}

rectangle "Business Logic Layer" as Business {
  component "Grades Controller" as GradesController
  component "Student Grades Service" as StudentGradesService
}

rectangle "Data Access Layer" as DataAccess {
  component "Grades Record" as GradesRecord
}

database "Student Database" as StudentDatabase

GradesView --> GradesController : requests grade information
GradesController --> StudentGradesService : coordinates retrieval
StudentGradesService --> GradesRecord : reads grade data
GradesRecord --> StudentDatabase : queries
@enduml
```

#### Layer responsibilities

• **Presentation Layer:** receives input, displays output, and contains no core business rules.
• **Business Logic Layer:** validates rules, coordinates use cases, performs calculations, and controls state changes.
• **Data Access Layer:** reads and writes persistent data through repositories, records, or DAOs.
• **Infrastructure/Persistence:** provides databases, queues, files, and external technical services when shown separately.

#### Component mapping examples

• `ShoppingCartUI`, `GradesView`, `VitalsEntryUI` → Presentation Layer
• `OrderController`, `GradesController`, `InventoryService`, `PaymentService` → Business Logic Layer
• `OrderRepository`, `PatientRepository`, `VitalsRepository`, `GradesRecord` → Data Access Layer
• Order, Patient, or Enrollment state-transition rules → Business/domain logic

#### Complete English answer template

````markdown
**Proposed Architecture:** Layered Architecture

**Why this architecture:**  
Layered Architecture is suitable because the portal has clearly separated user-interface, business-processing, and data-access responsibilities. The request flow naturally follows Presentation → Business Logic → Data Access, which makes the design easy to understand and maintain for a standard management portal.

**Architecture Diagram:**

```plantuml
@startuml
...
@enduml
```

**Advantages:**

1. **Maintainability:** Each layer has a focused responsibility, so changes to the user interface are less likely to affect business rules or persistence code.
2. **Testability:** Business logic and data-access components can be tested independently from the user interface.

**Disadvantage:**

1. **Performance Overhead:** Even simple requests may pass through several layers, adding latency and boilerplate code.

**Component Mapping:**

• Grades View → Presentation Layer
• Grades Controller and Student Grades Service → Business Logic Layer
• Grades Record → Data Access Layer
````

Do not place Vietnamese translations inside this official answer. Optional Vietnamese teaching notes must be a separate non-answer section.

### 15.2 Client–Server

```plantuml
@startuml
left to right direction
actor User
node "Client Application" as Client
node "Application Server" as Server
Database "Central Database" as DB

User --> Client
Client --> Server : requests / responses
Server --> DB : read / write
@enduml
```

### 15.3 Monolithic

```plantuml
@startuml
left to right direction
actor User
node "Monolithic Application" as App {
  component UI
  component Business
  component DataAccess
}
database DB

User --> UI
UI --> Business
Business --> DataAccess
DataAccess --> DB
@enduml
```

### 15.4 MVC

```plantuml
@startuml
left to right direction
actor User
component View
component Controller
component Model
database DB

User --> View : interaction
View --> Controller : user action
Controller --> Model : update / query
Model --> DB : persistence
Model --> View : state / data
Controller --> View : select response
@enduml
```

MVC is primarily an application/UI organization pattern. Do not present it as a replacement for every system-wide deployment architecture.

### 15.5 Microservices

```plantuml
@startuml
left to right direction

actor User
component "API Gateway" as Gateway
component "Catalog Service" as Catalog
component "Cart Service" as Cart
component "Ordering Service" as Ordering
queue "Event Broker" as Broker

database "Catalog DB" as CatalogDB
database "Cart DB" as CartDB
database "Order DB" as OrderDB

User --> Gateway
Gateway --> Catalog
Gateway --> Cart
Gateway --> Ordering
Catalog --> CatalogDB
Cart --> CartDB
Ordering --> OrderDB
Ordering ->> Broker : OrderCreated
Broker ->> Catalog : update demand
@enduml
```

Key rule: each service should represent a business capability and own its data. Avoid a single shared database unless the scenario explicitly specifies a transitional design.

### 15.6 Event-driven

```plantuml
@startuml
left to right direction
component "Event Producer" as Producer
queue "Event Broker" as Broker
component "Consumer A" as A
component "Consumer B" as B
component "Consumer C" as C

Producer ->> Broker : publish event
Broker ->> A : deliver event
Broker ->> B : deliver event
Broker ->> C : deliver event
@enduml
```

Mention delivery, ordering, idempotency, retry, and eventual consistency only when the question asks for deeper analysis.

### 15.7 Primary–Replica database architecture

Prefer the inclusive term **Primary–Replica**. If the exam explicitly uses “Master–Slave,” retain the expected label and optionally add “Primary–Replica” in parentheses.

```plantuml
@startuml
left to right direction
node "Application Server" as App
database "Primary DB" as Primary
database "Replica 1" as R1
database "Replica 2" as R2

App --> Primary : writes
App --> R1 : reads
App --> R2 : reads
Primary ->> R1 : replication
Primary ->> R2 : replication
@enduml
```

---

## 16. Component and Deployment Diagrams

### 16.1 Component diagram

Use for logical modules, services, APIs, and provided/required interfaces.

```plantuml
@startuml
left to right direction
component "Web UI" as UI
component "Order API" as OrderAPI
component "Inventory Service" as Inventory
component "Payment Adapter" as Payment
interface "IInventory" as IInventory
interface "IPaymentGateway" as IPayment

UI --> OrderAPI : HTTPS
OrderAPI --> IInventory
IInventory - Inventory
OrderAPI --> IPayment
IPayment - Payment
@enduml
```

### 16.2 Deployment diagram

Use for physical/runtime placement.

```plantuml
@startuml
node "Client Device" as Client {
  artifact "Browser"
}

node "Application Server" as AppServer {
  artifact "student-portal.jar"
}

node "Database Server" as DBServer {
  database "StudentDB"
}

cloud "External Laboratory" as Lab

Client --> AppServer : HTTPS
AppServer --> DBServer : JDBC/TCP
AppServer --> Lab : REST/HTTPS
@enduml
```

Do not confuse a component with a deployment node. A component is logical; a node is a runtime or hardware environment.

---

# PART IV — DESIGN PATTERNS

## 17. GoF pattern-solving method

### 17.1 Required answer structure

When asked to identify or explain a pattern, answer in this order and in English:

1. **Pattern Name**
2. **Family:** Creational, Structural, or Behavioral
3. **PlantUML Class Diagram** using scenario-specific role names
4. **Evidence from the scenario/code**
5. **Intent**
6. **Why useful here**
7. **Main benefit or trade-off**, when relevant

The class diagram is mandatory by default for practical-exam answers, including identify-only pattern questions, because it demonstrates understanding and prevents an incomplete submission. Omit it only when the prompt explicitly says text-only, forbids diagrams, or imposes an answer format that cannot contain one.

### 17.2 Pattern recognition matrix — all 23 GoF patterns

| Pattern                 | Family     | High-signal clues                                      | Core intent                                                   |
| ----------------------- | ---------- | ------------------------------------------------------ | ------------------------------------------------------------- |
| Abstract Factory        | Creational | families of related products, platform/theme variants  | create compatible product families without concrete classes   |
| Builder                 | Creational | step-by-step construction, many optional parts         | separate complex construction from representation             |
| Factory Method          | Creational | subclass decides which product to create               | defer creation to subclasses                                  |
| Prototype               | Creational | clone template/preconfigured object                    | create by copying an existing object                          |
| Singleton               | Creational | exactly one global instance                            | single instance plus global access point                      |
| Adapter                 | Structural | incompatible/legacy/external interface                 | convert one interface into another expected interface         |
| Bridge                  | Structural | two independent dimensions of variation                | separate abstraction from implementation                      |
| Composite               | Structural | tree, part–whole, treat leaf and group uniformly       | uniform handling of individual and composite objects          |
| Decorator               | Structural | add optional features dynamically, stack wrappers      | attach responsibilities at runtime without subclass explosion |
| Facade                  | Structural | simplify a complex subsystem                           | provide one simplified entry point                            |
| Flyweight               | Structural | huge number of similar objects, shared intrinsic state | reduce memory through shared state                            |
| Proxy                   | Structural | access control, lazy loading, cache, remote object     | surrogate controlling access to a real subject                |
| Chain of Responsibility | Behavioral | pass request through handlers until handled            | decouple sender from selected handler                         |
| Command                 | Behavioral | request as object, undo/redo, queue, log               | encapsulate an action/request                                 |
| Interpreter             | Behavioral | grammar, expression tree, language rules               | represent and interpret grammar sentences                     |
| Iterator                | Behavioral | traverse collection without exposing structure         | uniform sequential access                                     |
| Mediator                | Behavioral | many objects communicate through coordinator           | centralize interactions and reduce peer coupling              |
| Memento                 | Behavioral | snapshot, undo, rollback without exposing internals    | capture and restore state while preserving encapsulation      |
| Observer                | Behavioral | subscribers notified when subject changes              | one-to-many automatic notification                            |
| State                   | Behavioral | behavior changes according to internal state           | encapsulate state-dependent behavior                          |
| Strategy                | Behavioral | selectable/interchangeable algorithms                  | encapsulate a family of algorithms                            |
| Template Method         | Behavioral | fixed algorithm skeleton with overridable steps        | define algorithm structure in a base class                    |
| Visitor                 | Behavioral | add operations over stable object structure            | separate operations from element classes                      |

### 17.3 Confusion breakers

#### Strategy vs State

• **Strategy:** client selects an algorithm; strategies usually do not transition themselves
• **State:** context changes behavior as internal state transitions over time

#### Decorator vs Proxy vs Adapter vs Facade

• **Decorator:** same interface, adds behavior
• **Proxy:** same interface, controls access
• **Adapter:** changes interface
• **Facade:** simplifies many subsystem interfaces

#### Factory Method vs Abstract Factory vs Builder vs Prototype

• **Factory Method:** one product creation method overridden by subclasses
• **Abstract Factory:** families of related products
• **Builder:** incremental construction of one complex product
• **Prototype:** clone an existing configured object

#### Observer vs Event-driven architecture

Observer is an object-level behavioral pattern. Event-driven architecture is a system-level architecture style often implemented with brokers and asynchronous messages.

#### State pattern vs State Machine Diagram

A state diagram models lifecycle behavior. The State pattern is a class design that implements state-specific behavior using polymorphic state objects. A state diagram does not automatically imply the State pattern.

#### Template Method vs Strategy

• Template Method varies selected steps through inheritance
• Strategy replaces the whole algorithm through composition

#### Chain of Responsibility vs Command

• Chain routes a request among possible handlers
• Command packages a request as an object for execution, storage, queueing, or undo

---

## 18. Canonical PlantUML templates for all GoF patterns

Use role names from the scenario instead of blindly retaining generic names.

### 18.1 Abstract Factory

```plantuml
@startuml
interface AbstractFactory {
  + createProductA(): AbstractProductA
  + createProductB(): AbstractProductB
}
interface AbstractProductA
interface AbstractProductB
class ConcreteFactory1
class ProductA1
class ProductB1
class Client

AbstractFactory <|.. ConcreteFactory1
AbstractProductA <|.. ProductA1
AbstractProductB <|.. ProductB1
ConcreteFactory1 ..> ProductA1 : creates
ConcreteFactory1 ..> ProductB1 : creates
Client --> AbstractFactory
Client --> AbstractProductA
Client --> AbstractProductB
@enduml
```

### 18.2 Builder

```plantuml
@startuml
class Director {
  + construct(): Product
}
interface Builder {
  + buildPartA()
  + buildPartB()
  + getResult(): Product
}
class ConcreteBuilder
class Product

Builder <|.. ConcreteBuilder
Director o-- Builder
ConcreteBuilder ..> Product : creates
@enduml
```

### 18.3 Factory Method

```plantuml
@startuml
abstract class Creator {
  + operation()
  # factoryMethod(): Product
}
class ConcreteCreator
interface Product
class ConcreteProduct

Creator <|-- ConcreteCreator
Product <|.. ConcreteProduct
ConcreteCreator ..> ConcreteProduct : creates
@enduml
```

### 18.4 Prototype

```plantuml
@startuml
interface Prototype {
  + clone(): Prototype
}
class ConcretePrototypeA
class ConcretePrototypeB
class Client

Prototype <|.. ConcretePrototypeA
Prototype <|.. ConcretePrototypeB
Client --> Prototype : clones
@enduml
```

### 18.5 Singleton

```plantuml
@startuml
class ApplicationSettings {
• {static} instance: ApplicationSettings
• ApplicationSettings()
  + {static} getInstance(): ApplicationSettings
  + currentAcademicYear: String
}
@enduml
```

#### Complete Singleton exam answer template

````markdown
**Pattern Name:** Singleton Pattern

**Family:** Creational Design Pattern

**Class Diagram:**

```plantuml
@startuml
skinparam shadowing false

class ApplicationSettings {
• {static} instance: ApplicationSettings
• currentAcademicYear: String
• universityName: String
• ApplicationSettings()
  + {static} getInstance(): ApplicationSettings
  + getCurrentAcademicYear(): String
  + getUniversityName(): String
}

class PortalClient
PortalClient ..> ApplicationSettings : accesses through getInstance()
@enduml
```

**Intent:** Ensure that `ApplicationSettings` has exactly one instance and provide a global access point to that instance.

**Why useful here:** The entire portal must share one consistent source of application-wide settings, such as the current academic year and university name. Multiple settings objects could create inconsistent configuration values.
````

All headings and explanatory text above are part of the official answer and therefore remain English.

### 18.6 Adapter

```plantuml
@startuml
interface Target {
  + request()
}
class Adapter {
• adaptee: Adaptee
  + request()
}
class Adaptee {
  + specificRequest()
}
class Client

Target <|.. Adapter
Adapter o-- Adaptee
Client --> Target
@enduml
```

### 18.7 Bridge

```plantuml
@startuml
abstract class Abstraction {
  # implementor: Implementor
  + operation()
}
class RefinedAbstraction
interface Implementor {
  + operationImpl()
}
class ConcreteImplementorA
class ConcreteImplementorB

Abstraction <|-- RefinedAbstraction
Abstraction o-- Implementor
Implementor <|.. ConcreteImplementorA
Implementor <|.. ConcreteImplementorB
@enduml
```

### 18.8 Composite

```plantuml
@startuml
interface Component {
  + operation()
}
class Leaf
class Composite {
• children: List<Component>
  + add(component)
  + remove(component)
  + operation()
}

Component <|.. Leaf
Component <|.. Composite
Composite o-- "0..*" Component : children
@enduml
```

### 18.9 Decorator

```plantuml
@startuml
interface Component {
  + operation()
}
class ConcreteComponent
abstract class Decorator {
  # wrapped: Component
  + operation()
}
class ConcreteDecoratorA
class ConcreteDecoratorB

Component <|.. ConcreteComponent
Component <|.. Decorator
Decorator o-- Component : wraps
Decorator <|-- ConcreteDecoratorA
Decorator <|-- ConcreteDecoratorB
@enduml
```

### 18.10 Facade

```plantuml
@startuml
class Facade {
  + simpleOperation()
}
class SubsystemA
class SubsystemB
class SubsystemC
class Client

Client --> Facade
Facade --> SubsystemA
Facade --> SubsystemB
Facade --> SubsystemC
@enduml
```

### 18.11 Flyweight

```plantuml
@startuml
interface Flyweight {
  + operation(extrinsicState)
}
class ConcreteFlyweight
class FlyweightFactory {
• pool: Map<Key, Flyweight>
  + getFlyweight(key): Flyweight
}
class Client

Flyweight <|.. ConcreteFlyweight
FlyweightFactory o-- "0..*" Flyweight : shares
Client --> FlyweightFactory
Client --> Flyweight
@enduml
```

### 18.12 Proxy

```plantuml
@startuml
interface Subject {
  + request()
}
class RealSubject
class Proxy {
• realSubject: RealSubject
  + request()
}
class Client

Subject <|.. RealSubject
Subject <|.. Proxy
Proxy o-- RealSubject
Client --> Subject
@enduml
```

### 18.13 Chain of Responsibility

```plantuml
@startuml
abstract class Handler {
  # next: Handler
  + setNext(handler): Handler
  + handle(request)
}
class ConcreteHandlerA
class ConcreteHandlerB
class Client

Handler <|-- ConcreteHandlerA
Handler <|-- ConcreteHandlerB
Handler o--> "0..1" Handler : next
Client --> Handler
@enduml
```

### 18.14 Command

```plantuml
@startuml
interface Command {
  + execute()
}
class ConcreteCommand {
• receiver: Receiver
  + execute()
}
class Receiver {
  + action()
}
class Invoker {
• command: Command
  + invoke()
}
class Client

Command <|.. ConcreteCommand
ConcreteCommand o-- Receiver
Invoker o-- Command
Client ..> ConcreteCommand : creates
Client --> Receiver
@enduml
```

### 18.15 Interpreter

```plantuml
@startuml
class Context
interface AbstractExpression {
  + interpret(context)
}
class TerminalExpression
class NonterminalExpression {
• expressions: List<AbstractExpression>
}
class Client

AbstractExpression <|.. TerminalExpression
AbstractExpression <|.. NonterminalExpression
NonterminalExpression o-- "1..*" AbstractExpression
Client --> Context
Client --> AbstractExpression
@enduml
```

### 18.16 Iterator

```plantuml
@startuml
interface Iterator {
  + hasNext(): boolean
  + next(): Object
}
interface Aggregate {
  + createIterator(): Iterator
}
class ConcreteIterator
class ConcreteAggregate

Iterator <|.. ConcreteIterator
Aggregate <|.. ConcreteAggregate
ConcreteAggregate ..> ConcreteIterator : creates
ConcreteIterator --> ConcreteAggregate : traverses
@enduml
```

### 18.17 Mediator

```plantuml
@startuml
interface Mediator {
  + notify(sender, event)
}
class ConcreteMediator
abstract class Colleague {
  # mediator: Mediator
}
class ConcreteColleagueA
class ConcreteColleagueB

Mediator <|.. ConcreteMediator
Colleague <|-- ConcreteColleagueA
Colleague <|-- ConcreteColleagueB
ConcreteMediator o-- ConcreteColleagueA
ConcreteMediator o-- ConcreteColleagueB
Colleague --> Mediator
@enduml
```

### 18.18 Memento

```plantuml
@startuml
class Originator {
• state
  + createMemento(): Memento
  + restore(memento)
}
class Memento {
• state
}
class Caretaker {
• history: List<Memento>
}

Originator ..> Memento : creates / restores
Caretaker o-- "0..*" Memento : stores
@enduml
```

### 18.19 Observer

```plantuml
@startuml
interface Subject {
  + attach(observer)
  + detach(observer)
  + notifyObservers()
}
interface Observer {
  + update()
}
class ConcreteSubject
class ConcreteObserver

Subject <|.. ConcreteSubject
Observer <|.. ConcreteObserver
ConcreteSubject o-- "0..*" Observer : observers
ConcreteObserver --> ConcreteSubject : observes
@enduml
```

### 18.20 State

```plantuml
@startuml
class Context {
• state: State
  + request()
  + transitionTo(state)
}
interface State {
  + handle(context)
}
class ConcreteStateA
class ConcreteStateB

Context o-- State
State <|.. ConcreteStateA
State <|.. ConcreteStateB
@enduml
```

### 18.21 Strategy

```plantuml
@startuml
class Context {
• strategy: Strategy
  + setStrategy(strategy)
  + execute()
}
interface Strategy {
  + execute(data)
}
class ConcreteStrategyA
class ConcreteStrategyB

Context o-- Strategy
Strategy <|.. ConcreteStrategyA
Strategy <|.. ConcreteStrategyB
@enduml
```

### 18.22 Template Method

```plantuml
@startuml
abstract class AbstractClass {
  + templateMethod()
  # primitiveOperation1()
  # primitiveOperation2()
  # hook()
}
class ConcreteClass

AbstractClass <|-- ConcreteClass
@enduml
```

### 18.23 Visitor

```plantuml
@startuml
interface Visitor {
  + visitElementA(elementA)
  + visitElementB(elementB)
}
class ConcreteVisitor
interface Element {
  + accept(visitor)
}
class ConcreteElementA
class ConcreteElementB
class ObjectStructure

Visitor <|.. ConcreteVisitor
Element <|.. ConcreteElementA
Element <|.. ConcreteElementB
ConcreteElementA --> Visitor : accept
ConcreteElementB --> Visitor : accept
ObjectStructure o-- "0..*" Element
@enduml
```

---

## 19. Pattern explanation templates

### 19.1 Concise exam template

> **Pattern Name:** Strategy Pattern  
> **Family:** Behavioral  
> **Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable.  
> **Why useful here:** Searching by title, author, and ISBN requires different algorithms selected at runtime. The UI depends only on a common strategy interface, so new search methods can be added without modifying existing client logic.

### 19.2 Evidence-first template

> The scenario indicates **Decorator** because optional features can be stacked dynamically around the same base object while preserving the common interface. This is a **Structural** pattern. Its intent is to add responsibilities to an individual object at runtime without creating a subclass for every feature combination.

### 19.3 Do not overclaim

Do not state that a pattern guarantees performance, scalability, or security unless its structure actually supports that claim. Name the primary benefit relevant to the scenario.

---

# PART V — DESIGN PRINCIPLES AND TRADE-OFFS

## 20. SOLID quick map

| Principle             | Diagnostic question                                                             | Typical pattern support                        |
| --------------------- | ------------------------------------------------------------------------------- | ---------------------------------------------- |
| Single Responsibility | Does this class have one reason to change?                                      | Facade, Command, Strategy, layered separation  |
| Open/Closed           | Can behavior be extended without modifying stable code?                         | Strategy, Decorator, Template Method, Factory  |
| Liskov Substitution   | Can subtypes replace the base type without breaking expectations?               | correct inheritance and interface design       |
| Interface Segregation | Are clients forced to depend on methods they do not use?                        | small role-specific interfaces, Adapter        |
| Dependency Inversion  | Do high-level rules depend on abstractions rather than concrete infrastructure? | Strategy, Bridge, repositories, ports/adapters |

Use SOLID as supporting justification, not as a substitute for scenario evidence.

## 21. Coupling and cohesion

• Prefer high cohesion: each module owns a focused responsibility
• Prefer low coupling: depend on stable interfaces
• Avoid circular dependencies between layers or services
• Do not mistake “no dependencies” for good design; systems need explicit, controlled dependencies
• Composition is often preferable to inheritance when behavior must vary independently

## 22. Quality-attribute trade-offs

| Quality         | Questions to ask                                                        |
| --------------- | ----------------------------------------------------------------------- |
| Performance     | How many network/layer hops? Is synchronous coordination required?      |
| Scalability     | What component is the bottleneck? Can it scale independently?           |
| Availability    | What happens if a server, broker, database, or service fails?           |
| Consistency     | Is strong consistency required, or is eventual consistency acceptable?  |
| Maintainability | Are responsibilities and dependencies clear?                            |
| Testability     | Can business logic be tested without UI/database?                       |
| Deployability   | Can units be released independently?                                    |
| Security        | Where are trust boundaries, authentication, authorization, and secrets? |
| Observability   | Can requests be traced and failures diagnosed?                          |
| Modifiability   | How many modules change for one requirement?                            |

Every architecture has trade-offs. Never present a style as universally best.

---

# PART VI — EXAM ANSWER CONSTRUCTION

## 23. Recurring practical-exam structure

Many Software Architecture and Design practical exams use a pattern similar to:

1. Structural modeling: context plus class/domain model
2. Dynamic interaction: sequence diagram with stereotypes
3. State machine: lifecycle, events, guards, actions
4. Architecture: choose/model/justify and map components
5. Design pattern: identify family, draw class diagram, explain intent/usefulness

Treat this as a routing hint, not a fixed assumption. Read the actual scoring instructions.

## 24. Scoring-aware output rules

• `0.5 point identify` → name and family directly
• `1.0 point draw` → prioritize complete, valid diagram over long prose
• `0.5 point explain` → intent plus scenario-specific usefulness
• `1 advantage and 1 disadvantage` → exactly one strong item each
• `2 advantages` → provide two distinct qualities, not synonyms
• “Map previous components” → explicitly name each prior UI/controller/service/repository in its layer
• “Show only class names” → no attributes, methods, or empty compartments unless the tool renders them automatically

## 25. Standard multi-question answer shape

Use a strict two-zone format. The Vietnamese zone is optional and never replaces the English answer.

````markdown
## QUESTION 1 — STRUCTURAL MODELING

### Phân tích thêm (Tiếng Việt — không thuộc đáp án)
• Phân tích quan hệ và multiplicity ngắn gọn nếu người dùng cần học.

### Official Answer (English)

**Class Diagram:**

```plantuml
@startuml
...
@enduml
```

**Brief Justification:**
• ...

## QUESTION 2 — DYNAMIC INTERACTION MODELING

### Phân tích thêm (Tiếng Việt — không thuộc đáp án)
• Giải thích vì sao chọn Sequence Diagram và cách gán stereotype.

### Official Answer (English)

**Selected Diagram:** Sequence Diagram

```plantuml
@startuml

Student -> GradesView : 1. checkFinalGrade(courseId)
GradesView -> GradesController : 2. getFinalGrade(studentId, courseId)
GradesController -> StudentGradesService : 3. fetchStudentGrade(studentId, courseId)
StudentGradesService --> GradesController : 4. gradeInfo
GradesController --> GradesView : 5. displayGrade(gradeInfo)
GradesView --> Student : 6. showGradeDetails

@enduml
```

**Brief Explanation:**

1. The Student requests the final grade through the Grades View.
2. The Grades View forwards the request to the Grades Controller.
3. The Grades Controller asks the Student Grades Service to retrieve the grade.
4. The service reads the grade from the Grades Record and returns the result.
5. The Grades View displays the final grade to the Student.

## QUESTION 3 — STATE MACHINE MODELING

### Official Answer (English)

```plantuml
@startuml
...
@enduml
```

**Brief Explanation:**

1. The enrollment starts in the Registered state.
2. ...

## QUESTION 4 — ARCHITECTURAL DESIGN

### Official Answer (English)

**Proposed Architecture:** Layered Architecture

**Why this architecture:** ...

**Architecture Diagram:**

```plantuml
@startuml
...
@enduml
```

**Advantages:**

1. **Maintainability:** ...
2. **Testability:** ...

**Disadvantage:**

1. **Performance Overhead:** ...

**Component Mapping:**

• ...

## QUESTION 5 — DESIGN PATTERN

### Official Answer (English)

**Pattern Name:** Singleton Pattern

**Family:** Creational Design Pattern

**Class Diagram:**

```plantuml
@startuml
...
@enduml
```

**Intent:** ...

**Why useful here:** ...
````

When writing an actual answer, close and reopen Markdown code fences correctly. Do not include Vietnamese text in any `Official Answer (English)` block.

---

## 26. Handling ambiguous or poorly translated prompts

1. Preserve all clear named entities and business rules.
2. Infer only what is needed to complete the requested diagram.
3. Prefer multiplicities supported by explicit quantifiers.
4. If two interpretations materially change the answer, state one short assumption.
5. Do not fabricate a missing scenario from unrelated study notes.
6. If a provided image and text conflict, mention the conflict and prioritize the explicit task wording unless the user asks to reproduce the image.

Example:

> **Assumption:** “Each department has many professors” is interpreted as `1..*`; each professor belongs to one department because no shared appointment is described.

---

## 27. Frequent failure patterns to prevent

### Structural

• Filled diamond on the child side
• Multiplicity reversed
• `extends` modeled as dependency
• Interface implemented with a solid triangle line
• External system placed inside conceptual domain model
• Attributes added against the task contract

### Sequence

• Sequence diagram provided without an English `Brief Explanation`
• Actor activated for the entire flow
• Repository/service stereotypes misspelled
• Return drawn as a solid request arrow
• Deactivation before nested calls return
• `alt` condition not enclosed or missing `end`
• UI calls database directly despite an explicit controller/service chain
• A participant added without scenario evidence

### State

• State diagram provided without an English `Brief Explanation`
• Actions used as state names
• Event and action reversed
• Guard omitted from an “only if” rule
• Invalid cancellation transition added
• Final pseudostate added to an ongoing cyclic lifecycle

### Architecture

• Architecture name and prose provided without a PlantUML diagram
• Missing `Why this architecture`, requested advantages, or requested disadvantages
• Mixing Vietnamese translations into the official English answer
• Selecting microservices for a small CRUD system without scale/team evidence
• Treating MVC and layered architecture as mutually exclusive
• Claiming microservices automatically improve every quality attribute
• Ignoring operational complexity, consistency, latency, or failure modes
• Drawing logical layers as physical machines without saying so

### Patterns

• Pattern identified without a class diagram even though diagrams are not forbidden
• Pattern name given without family, intent, or scenario-specific usefulness
• Strategy chosen for lifecycle-driven behavior that is actually State
• Decorator confused with Adapter or Proxy
• Abstract Factory chosen when only one product is created
• Singleton recommended for ordinary mutable domain data
• Observer confused with a message broker architecture
• Pattern identified from one keyword while contradicting the code structure

---

## 28. Final validation checklist

Before returning an answer, verify every applicable item.

### Task compliance

- [ ] Correct question type selected
- [ ] Every required named element included
- [ ] No forbidden extra detail
- [ ] Requested number of pros/cons supplied
- [ ] Official exam-answer content is entirely English unless another language was explicitly requested
- [ ] Vietnamese content appears only in a separate optional non-answer section
- [ ] No bilingual English/Vietnamese phrases are mixed inside official-answer bullets or diagram labels
- [ ] Identifiers remain consistent with the scenario

### PlantUML

- [ ] Starts with `@startuml`
- [ ] Ends with `@enduml`
- [ ] All aliases unique
- [ ] All blocks closed
- [ ] No Markdown escaping inside syntax
- [ ] No duplicate or malformed names

### Context/Class/ERD

- [ ] Whole end owns the diamond
- [ ] Generalization points to parent
- [ ] Realization points to interface
- [ ] Multiplicities match both directions
- [ ] Relationship labels match scenario verbs

### Sequence

- [ ] Participants ordered by call direction
- [ ] Explicit stereotypes preserved
- [ ] Every message between lifelines is numbered
- [ ] Numbering starts from `1`
- [ ] Numbering follows execution order and never restarts inside fragments
- [ ] Hierarchical numbering is used for conditional or nested branches when needed
- [ ] PlantUML `autonumber` is not used
- [ ] Solid requests and dashed returns are consistent
- [ ] Activation nesting matches the real call stack
- [ ] Activations end when return messages are sent
- [ ] Async messages use `->>` without an immediate return or receiver activation for that dispatch
- [ ] Conditions are modeled with correct fragments
- [ ] Final feedback reaches the actor when appropriate
- [ ] An English `Brief Explanation:` follows the diagram
- [ ] The numbered explanation matches the actual message order

### State

- [ ] Required states included
- [ ] Initial and final states justified
- [ ] Events, guards, and actions use correct grammar
- [ ] Normal `-->` transitions are used unless the prompt requires another notation
- [ ] An English `Brief Explanation:` follows the diagram

### Architecture

- [ ] Choice tied to scenario constraints
- [ ] Advantage and disadvantage are architecture-specific
- [ ] Components mapped to layers/services when requested
- [ ] Trade-offs acknowledged
- [ ] A PlantUML architecture diagram is present unless explicitly forbidden
- [ ] `Why this architecture` is present in English
- [ ] The exact requested number of advantages and disadvantages is present in English

### Pattern

- [ ] Name and family correct
- [ ] Evidence comes from scenario/code
- [ ] Diagram roles match the selected pattern
- [ ] Intent and usefulness are distinct and scenario-specific
- [ ] A compact PlantUML class diagram is present unless explicitly forbidden

---


## 29. Regression acceptance test — five-question practical exam

Before considering this skill successful, mentally test it against a practical exam with these question types:

### Question 1 — Class Diagram

Pass only if the answer contains the requested classes, composition, aggregation, generalization, multiplicities, and no forbidden members.

### Question 2 — Sequence Diagram

Pass only if all are present:

• `Selected Diagram: Sequence Diagram`
• PlantUML with `<<boundary>>`, `<<control>>`, `<<service>>`, and `<<entity>>` as specified
• Every message is numbered from `1`
• Numbering never restarts inside `alt`, `opt`, `loop`, or `par`
• PlantUML `autonumber` is not used
• Correct activation, call-stack nesting, and dashed-return behavior
• Async dispatches follow the strict `->>` rule when present
• An English `Brief Explanation:` with numbered steps after the diagram

### Question 3 — Statechart Diagram

Pass only if:

• Every required state is present
• The event/action/guard grammar is correct
• Standard `-->` transitions are used with PlantUML's automatic layout
• Guarded outcomes are shown clearly with direct guarded transitions unless the prompt requires a decision node
• An English brief explanation follows the diagram

### Question 4 — Architecture

Pass only if:

• The architecture name is present
• A PlantUML block diagram is present
• `Why this architecture:` is present in English
• The exact requested number of advantages and disadvantages is present in English
• Previous components are mapped to layers when available

### Question 5 — Design Pattern

Pass only if:

• Pattern name and family are present
• A PlantUML class diagram is present
• `Intent:` and `Why useful here:` are present in English

Any failed item means the multi-question answer is incomplete. Regenerate the missing section rather than apologizing after delivery.

---

## 30. Reference anchors

Use these as background standards, not as text to quote in every answer:

• OMG Unified Modeling Language 2.5.1: https://www.omg.org/spec/UML
• PlantUML class diagrams: https://plantuml.com/class-diagram
• PlantUML sequence diagrams: https://plantuml.com/sequence-diagram
• PlantUML state diagrams: https://plantuml.com/state-diagram
• PlantUML component diagrams: https://plantuml.com/component-diagram
• PlantUML deployment diagrams: https://plantuml.com/deployment-diagram
• Microsoft Architecture Styles: https://learn.microsoft.com/azure/architecture/guide/architecture-styles/
• Microsoft N-tier guidance: https://learn.microsoft.com/azure/architecture/guide/architecture-styles/n-tier
• Microsoft Microservices guidance: https://learn.microsoft.com/azure/architecture/microservices/
• Microsoft Event-driven guidance: https://learn.microsoft.com/azure/architecture/guide/architecture-styles/event-driven

---

## 31. Final operating instruction

For every Software Architecture and Design question:

1. Read the full scenario before naming a diagram, architecture, or pattern.
2. Base every relationship, message, transition, and trade-off on explicit evidence.
3. Preserve the exam’s terminology and stereotypes.
4. Write all submission-ready answer content in English; keep optional Vietnamese teaching notes separate.
5. Generate valid, clean PlantUML with deliberate layout and minimal clutter.
6. Never omit the English sequence/state brief explanation, the architecture diagram and trade-offs, or the design-pattern class diagram unless explicitly forbidden.
7. Match the exact requested number of advantages and disadvantages.
8. Run the complete validation checklist and the regression acceptance test.
9. When uncertain, state one precise assumption rather than hiding the ambiguity.
10. Do not deliver a partially complete multi-question answer.
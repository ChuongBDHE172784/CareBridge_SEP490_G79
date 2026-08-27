# Sequence Diagram Generation Skill

You MUST strictly follow these rules when generating any UML Sequence Diagram — message numbering, activation bars, fragments, and output format all matter equally. Treat this as the specification of a professional software architect, not a casual sketch.

---

## 0. Output Format & Lifeline Order

- Default output syntax: **PlantUML** (`@startuml ... @enduml`), unless the user explicitly asks for Mermaid or another format.
- Always declare `participant` in explicit left-to-right order matching real call direction:
  ```
  Actor -> UI -> Controller -> Middleware(if any) -> Service -> Repository/DAO -> Database -> External Service (if any)
  ```
- Add middleware/cross-cutting lifelines (Auth Guard, Interceptor, Filter, Cache Layer, Rate Limiter) **only if they actually appear** in the described flow. Do not invent layers that weren't mentioned.
- Keep lifeline naming consistent with the real backend architecture given by the user (don't rename `UserService` to `AccountService` arbitrarily).

---

## 1. Message Numbering Rules

- Every message between lifelines MUST have a sequential number starting from 1.
- Number messages according to actual execution order, top to bottom.
- Use hierarchical numbering (`a, b, c...`) for alternative/exception/conditional sub-flows — these are branches of the step that triggered them, not new top-level numbers.
- Do NOT restart numbering inside `alt` / `opt` / `loop` / `par` fragments.
- Numbering must reflect parent-child call relationships, not just diagram order.

**Correct:**
```
1. Submit form
2. Validate request
alt validation failed
    2a. Throw ValidationException
    2b. Return 400 Bad Request
    2c. Display error message
else validation success
    3. Continue processing
end
```

**Incorrect:**
```
1. Submit form
2. Validate request
1. Throw ValidationException
2. Return error
```

### Deep Nesting Rule
- Numbering prefix accumulates by depth. A branch inside an already-branched step gets a further suffix: step `4` → branch `4a` → nested fragment inside `4a` → `4a-1`, `4a-2` (or `4a.1`, `4a.2` for readability).
- Never reset numbering to 1 inside any nested fragment, regardless of depth.

---

## 2. Activation Bar Rules (Execution Specification)

Every lifeline that performs processing MUST have an activation bar. No activation bar should exist without a corresponding incoming message that triggers it.

### Actor
- No activation bar unless the actor performs visible internal processing (rare).
- Actor interaction starts the flow.

### Frontend / UI
Create an activation bar when:
- Receiving user input.
- Preparing the API request.
- Processing the API response.
- Displaying result/error.

### Controller / API Layer
- Activation bar spans from receiving the request until the response is sent back.

### Service / Business Layer
- Activation bar spans from invocation until business logic completes.
- Includes time spent on validation, business rules, calling repository/other services, processing returned data.

### Repository / Database
- Activation bar for every query execution, insert, update, or delete, ending when the result is returned.

### Deactivation Timing
- An activation bar ends **immediately when the return (dashed) message is sent**, not when it's received by the caller.
- An exception that terminates a lifeline's execution uses a **destroy mark (`X`)** at that point instead of a normal dashed return — see Section 8.

---

## 3. Activation Bar Nesting Rules

Activation bars MUST mirror the real call stack.

```
Controller receives request:
Controller: ████████████████
  calls Service:
Controller: ████████████████
    Service: ███████████████████
      calls Repository:
Controller: ████████████████
    Service: ███████████████████
      Repository: █████
```

Unwind order after Repository returns:
1. Repository activation ends.
2. Service continues, then ends.
3. Controller continues, then ends after sending the response.

---

## 4. Self-Message Rule

A lifeline calling its **own** private/internal method (e.g. Service validating input via a local helper) is a **self-message**:
- Drawn as an arrow that loops back to the same lifeline.
- Numbered as a sub-step of the parent call (e.g. `3a`), never as a new top-level number.
- Does **not** create an activation bar on a different lifeline — it nests a small activation segment on the same lifeline.

---

## 5. Return Message Rules

- Use **dashed arrows** for return/response messages.
- Solid arrow = request/call. Dashed arrow = return value/response.

```
Controller → Service:  createResource(request)      [solid]
Service → Controller:  ResourceResponse              [dashed]
```

HTTP responses must include the status code:

| Case | Code |
|---|---|
| Success (created) | `201 Created` |
| Success (read/update) | `200 OK` |
| Validation error | `400 Bad Request` |
| Authentication error | `401 Unauthorized` |
| Authorization error | `403 Forbidden` |
| Conflict | `409 Conflict` |
| Not found | `404 Not Found` |
| Server error | `500 Internal Server Error` |

---

## 6. Async Message Rule

Distinguish synchronous calls from fire-and-forget/async calls (message queue publish, async email dispatch, webhook without waiting for response, event emission):

- **Sync call**: solid arrow with filled arrowhead, always paired with a dashed return message and a matching activation bar on the receiver.
- **Async call**: solid arrow with an **open** arrowhead (`->>` in PlantUML/Mermaid convention), **no** activation bar created on the receiver for that call, and **no** dashed return message — the sender continues immediately.

```
Service ->> MessageQueue : publish(OrderCreatedEvent)   ' async, no wait
Service -> Repository : save(order)                     ' sync, waits
Repository --> Service : Order                          ' dashed return
```

---

## 7. Fragment Rules

### 7.1 alt / opt (conditional)
```
alt [condition A]
    messages...
else [condition B]
    messages...
end
```

### 7.2 loop (repetition)
- Wrap repeated messages in `loop [condition or count]`.
- Do **NOT** duplicate numbering per iteration — number the message(s) once inside the loop, and annotate the fragment header with the iteration condition/count (e.g. `loop [for each item in cart]`).
- A loop nested inside an alt (or vice versa) keeps the parent step's numbering prefix (e.g. `4a` branch containing a loop numbers its messages `4a-1`, `4a-2`).

### 7.3 par (parallel)
- Use `par ... and ... end` for concurrent, independent calls (e.g. calling Payment Service and Inventory Service at the same time).
- Parallel branches may share the same parent step number with sibling suffixes (e.g. both `4a` and `4b` originate from step 4 and execute concurrently) — do **not** imply sequential order between them.

```
par
  4a. Service -> PaymentService : charge()
and
  4b. Service -> InventoryService : reserveStock()
end
```

### 7.4 ref (reference to another diagram)
- If a sub-flow already exists as its own documented sequence diagram (e.g. "Authentication Flow", "Token Refresh Flow"), use a reference fragment instead of redrawing it:
```
ref over UI, Controller, AuthService : Authentication Flow
```

---

## 8. Create / Destroy Lifeline Rules

- **Object creation**: draw the arrow pointing directly to the **top** of a newly created lifeline (not to its side), representing the moment the object/instance comes into existence (e.g. creating a `Transaction` object, spawning a worker thread).
- **Object destruction**: terminate that lifeline with an explicit `X` mark at the point of destruction (e.g. end of a transaction, thread completion).
- **Exception-driven termination**: if an unhandled exception kills a lifeline's execution rather than returning normally, mark the end of that activation with `X` instead of a dashed return arrow. Use `alt`/`opt` blocks only for exceptions that are *caught and handled* within the flow (producing a normal, if error-shaped, response like `400 Bad Request`).

---

## 9. Notes / Annotations

- Add a note when business logic isn't obvious from the message name alone:
  - `note right of Service : Check idempotency key before processing`
  - `note over Service, Repository : Runs inside a DB transaction`
- Use notes sparingly — only for logic a reader could not infer from the diagram itself.

---

## 10. General Procedure (before generating any diagram)

1. Analyze the full execution flow described by the user.
2. Identify the caller and receiver of every message.
3. Determine when each lifeline becomes active and when it deactivates.
4. Decide which messages are sync vs async.
5. Identify loops, parallel branches, conditional branches, and any reusable sub-flows (ref).
6. Build activation bars strictly following the real call stack (Section 3).
7. Number every message according to chronological execution order and parent-child branching (Sections 1 and 10-continued below).
8. Add creation/destruction marks and notes only where they reflect real behavior described by the user — never invent architecture details not given.
9. Output valid PlantUML (or the format explicitly requested) that a rendering tool can compile without syntax errors.

The final Sequence Diagram must be internally consistent (numbering, branching, activation bars, sync/async, and lifeline order all agree with each other) and look like something a professional software architect would produce and sign off on.

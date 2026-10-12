# Campus Day — Meal System Master Context

> **Scope Boundary:** This document is exclusively for the **Campus Day Meal System**. It must not be used as the master specification for other Campus Day modules such as Notice System, Lost & Found, Event Registration, Study Routine & Time Management, Controlled Batch/Hall Chat, Anonymous Feedback / Student Voice, or Current Affairs. Those modules require their own separate context/specification documents.


## 1. Purpose of This Document

This file is the **single master context document** for the Meal System inside the **Campus Day** platform for **Jahurul Islam Medical College (JIMC), Kishoreganj, Bangladesh**.

Codex must read this document before implementing any Meal System part.

### Important implementation rule

**Do not build the whole Meal System only from this document.**

This document explains the complete business context, confirmed rules, technical direction, module boundaries, and unresolved items.

Implementation will be done **part by part** using separate prompts.

For every implementation task:

1. Read this file first.
2. Read the specific Part prompt.
3. Inspect the existing codebase before changing anything.
4. Do not invent missing business rules.
5. Do not change another Part unless the current Part genuinely requires it.
6. If a rule is marked `PENDING / UNRESOLVED`, do not implement guessed behavior.
7. Preserve already completed behavior unless the new Part explicitly requires a change.

---

# 2. Platform Context

## Platform Name

**Campus Day**

## Institution

**Jahurul Islam Medical College (JIMC), Kishoreganj**

## Platform Focus

Campus Day is a **student-based / student-first campus platform** for JIMC.

The Meal System is one major module of Campus Day.

Other Campus Day modules may later include:

- Notice System
- Lost & Found
- Event Registration
- Study Routine & Time Management
- Controlled Batch/Hall Chat
- Anonymous Feedback / Student Voice
- Current Affairs

These modules are outside the current Meal System implementation unless a future prompt explicitly connects them.

---

# 3. Technical Stack

## Backend

Use:

- Java
- Spring Boot
- REST API
- Spring Web
- Spring Data MongoDB
- Jakarta Validation
- Lombok
- Spring Boot DevTools
- Spring Security
- JWT authentication
- BCrypt password hashing

Additional dependencies may be added only when they are genuinely needed.

## Backend Architecture

Use:

- SOLID principles
- Clean Code
- Package-by-feature architecture
- Clear separation of concerns
- Maintainable production-style code
- Constructor injection
- Prefer Lombok `@RequiredArgsConstructor`
- DTOs should preferably use Java `record`

Typical feature-level structure may contain:

```text
feature/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
├── exception/
├── mapper/          # only if useful
├── validation/      # only if useful
└── security/        # only when feature-specific
```

Do not create unnecessary layers only for appearance.

## Common Spring Annotations

Expected where appropriate:

- `@RestController`
- `@RequestMapping`
- `@RequiredArgsConstructor`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@Valid`
- `@RequestBody`
- `@PathVariable`
- `@RequestParam`
- `@Service`
- `@Repository`
- `@Builder`
- `@NoArgsConstructor`

Use annotations only where they make sense.

## Database

Use **MongoDB**.

Data models must reflect the real business domain.

Avoid storing everything in one giant document.

Avoid unnecessary duplication.

Use MongoDB references/embedded structures based on actual access patterns.

## Frontend

Use **React**.

The frontend must be:

- Student-friendly
- Fast
- Responsive
- Clear
- Professional
- Real-world / industry-standard
- Easy to maintain
- Easy to connect to Spring Boot REST APIs

The exact frontend dependency set will be decided by future implementation prompts.

---

# 4. Core User Context

## 4.1 Student-Centric Model

The primary platform user is a **student**.

A student may temporarily receive additional responsibilities or permissions.

Example:

A 4th-year student remains a normal student but may also become a **Meal Manager** for a specific management period.

Do not model a Meal Manager as an entirely unrelated human type unless a future implementation reason requires it.

---

# 5. Current Manual Meal System — Real Existing Process

The current JIMC hall dining/meal operation is mostly manual.

Important current processes include:

- Messenger-based Final Year meal requests
- Manual fraction sheet
- Paper-based kitchen ingredient issue records
- Manual stock calculation
- Manual supplier credit tracking
- Manual monthly billing
- Manual payment collection
- Manual stock handover between monthly managers

The software is intended to digitize these real workflows.

Do not redesign the business model without explicit approval.

---

# 6. Student Categories and Billing Model

The Meal System has two fundamentally different billing models.

---

## 6.1 Years 1–4 Students

Students from **1st year through 4th year** pay a **fixed monthly meal charge**.

### Confirmed rules

- Payment is fixed for the month.
- They pay whether they eat or not.
- Their meal attendance does not reduce the bill.
- A student may submit a meal absence notice.
- Absence notice is used only to help the kitchen reduce unnecessary cooking quantity.
- Absence notice does **not** reduce the student's monthly bill.
- Not submitting an absence notice has **no penalty**.
- They normally pay their fixed meal charge to the **college office**.
- Current rule says payment should be made by around the **10th of the month**.
- 30-day and 31-day months may have different fixed charges.

### Important

Do not mix Years 1–4 fixed billing with Final Year consumption-based billing.

---

## 6.2 Final Year / 5th Year Students

Final Year students do **not** pay a fixed monthly meal charge.

They pay only for the meals/items they consume/order.

This is currently referred to as the **Fraction Meal System**.

### Confirmed rules

- Final Year students order/select meals.
- If they do not order, the default state is **Not Eating**.
- They pay only for what they consume/order.
- Their monthly bill is calculated from their actual meal/item selections.
- Their payment is collected directly by the responsible Meal Managers.
- Their payment does not follow the normal Years 1–4 college-office payment flow.

---

# 7. Final Year Meal Structure

## 7.1 Breakfast

Breakfast is a **complete package**.

A student either orders the breakfast package or does not.

Breakfast is not currently treated as individual item selection.

---

## 7.2 Lunch

Lunch may support:

- Full meal
- Individual items
- Item quantity

Examples from the real process include:

- Rice
- Chicken
- Fish
- Other meal items

A student may take one or multiple quantities of an item when allowed.

---

## 7.3 Dinner

Dinner follows the same general pattern as lunch.

It may support:

- Full meal
- Individual items
- Item quantity

---

# 8. Final Year Order Rules

Final Year students currently communicate meal choices manually through Messenger.

The digital system will replace this with structured ordering.

### Confirmed rules

- A Final Year student can order even if the price is not available yet.
- If no order is placed, the default is **Not Eating**.
- Order cutoff time is configurable.
- Edit cutoff time is configurable.
- Cancel cutoff time is configurable.
- Time configuration will be controlled by authorized meal-management users.
- Exact time values are not hard-coded business rules in this document.

### Price visibility

- If an applicable price has been saved, the student can see it.
- If no applicable price exists, the student sees **Price Pending**, not a final price.
- Lack of price does not block a valid order or edit before cutoff.

---

# 9. Meal Price Rules

Meal/item prices used for Final Year students are based on real market/purchase costs and operational decisions.

### Confirmed rules

- Final Year pricing is not constant.
- Prices may change as market purchase prices change.
- The current assigned Meal Managers set the applicable price, as confirmed in Part 6.
- The price selected in the system is what the Final Year student will pay.
- This pricing mechanism is specifically for Final Year consumption-based billing.
- Final Year students can see individual item prices only when those prices are applicable.
- A student can still order an item before its final price is available.

### Important

The exact internal formula used by managers to determine a price from purchase data is **not fixed here**.

Do not automatically invent a cost-allocation formula.

---

# 10. Meal Management Responsibility

## 10.1 Monthly Meal Managers

Each management period has:

- **2 students**
- Both are from **4th year**

They are responsible for operating the meal system for their assigned period.

### Confirmed rules

- Both are still students.
- Their internal task division is not fixed.
- They may divide work however it is easiest for them.
- Do not hard-code "Manager A = stock" and "Manager B = fraction".
- Both may need access to operational Meal System functionality.

---

## 10.2 Meal Committee

JIMC also has a Meal Committee.

The Meal Committee may also require access to some meal-management functions.

### PENDING / UNRESOLVED

Detailed Meal Committee permissions are intentionally deferred.

Do not invent:

- exact roles
- approval hierarchy
- edit rights
- review rights
- final authority

These will be specified later.

---

# 11. Manual Fraction Sheet

Currently Final Year meal tracking is maintained manually.

Typical structure:

```text
Student Name
Date
Breakfast
Lunch
Dinner
Individual Items / Quantities
```

Historically:

- Tick may mean eating
- Cross may mean not eating
- Extra items are written manually

The software should replace this with structured digital data.

The system should be able to produce a **Digital Fraction Sheet** from actual Final Year orders.

---

# 12. Years 1–4 Absence Notice

Years 1–4 students may report that they will not eat a meal.

The purpose is operational planning only.

### Confirmed rules

- Can help the kitchen reduce cooking quantity.
- Does not reduce billing.
- No penalty if the student does not submit a notice.
- It is not the same as a Final Year order.

Possible meals:

- Breakfast
- Lunch
- Dinner

Detailed timing rules may be defined later if needed.

---

# 13. Kitchen and Cooking Planning

The kitchen needs to know how much food to prepare.

The intended planning logic should conceptually consider:

```text
Years 1–4 baseline expected meals
- Years 1–4 reported absences
+ Final Year confirmed orders
= planned meal demand
```

For Final Year, the kitchen may additionally need:

- Full meal count
- Individual rice portions
- Chicken quantities
- Fish quantities
- Other item quantities
- Extras

This area is for operational cooking planning.

Do not mix cooking-plan counts directly with billing logic.

---

# 14. Ingredient and Stock Tracking

Current stock usage is recorded manually on paper/form sheets.

Examples:

- Breakfast:
  - Oil: 2 litre
  - Flour: 12 kg
- Lunch:
  - Chicken: X pieces
- Dinner:
  - meal-specific ingredient issues

The digital system should eventually track:

- Ingredient
- Unit
- Opening stock
- Purchases
- Kitchen issues
- Adjustments
- Remaining stock
- Movement history

Typical units may include:

- kg
- litre
- piece
- packet
- other configured unit where necessary

Do not invent unnecessary stock rules before the Stock Part prompt.

---

# 15. Purchasing

The mess currently purchases items using both cash and credit.

Examples from the real process:

- Grocery items may be purchased on credit.
- Chicken may be purchased on credit.
- Fish may be purchased with cash.
- Gas and some other expenses may be paid in cash.

### Supplier process

- Suppliers may maintain their own manual ledger/khata.
- The 2 responsible 4th-year Meal Managers manage the mess-side purchase/payment records.

### Design rule

Do not permanently hard-code:

```text
Grocery = Credit
Fish = Cash
Chicken = Credit
```

These are current examples, not universal technical constraints.

Each purchase should store its actual payment mode/status.

---

# 16. Final Year Monthly Billing

Final Year billing is consumption-based.

A student's monthly bill may include:

- Breakfast packages
- Lunch full meals
- Lunch individual items
- Dinner full meals
- Dinner individual items
- Extra quantities/items

Conceptually:

```text
Student Final Bill
= sum of billable ordered/consumed items using the applicable selected prices
```

### Price-not-ready state

Because students may order before an applicable price exists:

- The system must support pending-price order items.
- A monthly bill may remain not-finalized while required prices are missing.
- The UI must not display invented monetary values.

Do not use fake placeholder prices.

---

# 17. Years 1–4 Monthly Billing

Years 1–4 billing is fixed.

The system should eventually support:

- Monthly fixed meal charge
- Different configured amount where needed for 30-day vs 31-day months
- Payment status
- Payment record
- College-office context

### Confirmed rule

Absence does not reduce the fixed bill.

---

# 18. Final Year Collections

Final Year students pay based on their finalized consumption bill.

### Confirmed current process

- Meal Managers collect the payment directly.
- Payment is not handled like Years 1–4 office payment.
- The system should keep collection/payment history.

Detailed receipt/payment method rules will be defined during the billing implementation Part.

---

# 19. Stock Handover Between Monthly Management Teams

At the end of a management period, unused stock may remain.

Example:

```text
Remaining stock value = BDT 3,500
```

### Confirmed rule

The **new 2 Meal Managers** pay the **previous 2 Meal Managers** the value of the transferred remaining stock.

Therefore the system eventually needs to record:

- Outgoing management team
- Incoming management team
- Handover date
- Ingredient
- Quantity
- Unit
- Valuation
- Total handover value
- Payment record

Do not invent the stock valuation formula unless explicitly provided in a later prompt.

---

# 20. College Advance — PENDING

The current manual operation involves approximately:

```text
BDT 125,000
```

as an advance from the college near the start of the month.

However, the exact accounting and adjustment flow is intentionally not finalized in this document.

### DO NOT IMPLEMENT YET

Do not assume:

- who receives the advance
- exact settlement flow
- office adjustment formula
- accounting entries
- closing treatment

A future prompt will clarify this.

---

# 21. Proposed Meal System Implementation Parts

The Meal System will be implemented in separate Parts.

This sequence is the current working roadmap.

---

## Part 1 — Meal Foundation, Student & Access Context

Scope:

- Meal-domain foundation
- Student academic-year context
- Years 1–4 vs Final Year classification
- Meal-related authorization boundary
- Temporary Meal Manager context
- Common meal enums/statuses
- Base auditing strategy where needed
- Initial backend/frontend integration foundation

Do not implement full ordering, stock, billing, etc. in Part 1.

---

## Part 2 — Meal Management Period & Manager Assignment

Scope:

- Monthly management period
- Month/year or period dates
- Assign exactly 2 eligible 4th-year student Meal Managers
- Active/closed period
- Management history
- Prevent invalid overlapping active management assignments where applicable

---

## Part 3 — Meal Menu & Meal Configuration

Scope:

- Daily meal menu
- Breakfast
- Lunch
- Dinner
- Breakfast package
- Lunch/dinner items
- Item availability
- Student-facing menu
- Meal ordering availability

---

## Part 4 — Final Year Meal Ordering

Scope:

- Final Year order creation
- Breakfast package selection
- Lunch full meal/item selection
- Dinner full meal/item selection
- Quantities
- Extras where confirmed
- Not Eating default
- Cutoff
- Edit
- Cancel
- Locking after configured cutoff
- Price may be unavailable at order time

---

## Part 5 — Years 1–4 Absence Notice

Scope:

- Meal absence notice
- Date/date range
- Breakfast/lunch/dinner selection
- Operational cooking impact only
- No billing reduction
- No penalty for not submitting

---

## Part 6 — Final Year Price Management

### Confirmed rules

- Either of the two currently assigned Meal Managers may set Final Year meal prices. Both have equal price-management authority.
- Price-management authority comes from the current Meal Management Period assignment. Being a Fourth Year student alone does not grant it.
- No separate Meal Committee approval is required to set or make a price applicable. Other detailed Meal Committee permissions remain deferred.
- There is no Draft → Published price workflow and no separate Publish action. When an authorized current Meal Manager saves a valid price, it becomes applicable immediately according to its effective context.
- Breakfast has complete-package pricing only. Lunch and Dinner may each have full-meal pricing and individual selectable-item pricing.
- A price identifies its meal/package or menu item and effective context. The long-term daily versus monthly pricing granularity remains unresolved; do not invent a broader pricing engine.
- Preserve historical prices. Changing an applicable price must not destroy the previous rate.
- If no applicable price exists, the state is **Price Pending**. This does not prevent a Final Year student from placing or editing a valid order before cutoff.
- Managers set the applicable prices. Do not invent an automatic price calculation or cost-allocation formula.
- Final billing and the policy for selecting historical prices for billing belong to Part 11.

---

## Part 7 — Digital Fraction Sheet

Scope:

- Final Year order matrix/list
- Date-based view
- Student
- Breakfast
- Lunch
- Dinner
- Full meal
- Individual quantities
- Extras
- Cancelled
- Not Eating
- Locked state
- Search/filter
- Operational totals

---

## Part 8 — Cooking Plan

Scope:

- Years 1–4 baseline
- Years 1–4 absence adjustments
- Final Year order totals
- Meal-wise demand
- Full-meal count
- Item-wise demand
- Printable/usable kitchen plan if requested

---

## Part 9 — Ingredients, Stock & Kitchen Issue

Scope:

- Ingredients
- Units
- Opening stock
- Purchases into stock
- Kitchen issues
- Meal-specific stock consumption
- Adjustments
- Closing stock
- Movement history
- Stock validation

---

## Part 10 — Purchases, Suppliers & Dues

Scope:

- Suppliers
- Purchases
- Purchase lines
- Quantity
- Unit cost
- Total
- Cash/credit
- Outstanding dues
- Supplier payment history
- Purchase history

---

## Part 11 — Bills & Collections

Scope:

### Years 1–4

- Fixed monthly bill
- Configured monthly charge
- Payment status
- Office payment context

### Final Year

- Consumption-based bill
- Pending-price state
- Finalized bill
- Payment collection
- Outstanding amount
- Payment history

Keep the two billing models separate.

### Confirmed Part 11 implementation rules

- Years 1–4 owe an authority-defined fixed charge for each billing year/month. Either currently assigned Meal Manager records or revises that charge through MealManagerAccess; keep every revision. Attendance, absence notices, and missing notices never change it. There is no penalty or late fee.
- The College Office collects Years 1–4 payments. Its authentication is deferred, so development payment APIs have no Meal Manager authorization. Keep append-only payment history, including payments made before a later charge revision; expose overpayment without inventing refunds or transfers.
- Final Year bills use valid submitted Part 4 orders and applicable Part 6 prices. Cancelled or absent orders are not billed; a submitted order remains billable even when the meal is not collected. Missing prices keep the bill PENDING_FINALIZATION without a placeholder amount.
- Final Year billing selects the latest applicable Part 6 revision for each ordered menu target as of the finalization instant. Finalized bills preserve line-level price references and amounts against later price changes. Correction or reopening is deferred. Current Meal Managers collect Final Year payments against finalized outstanding balances only; overpayment is rejected.
- Use BigDecimal, Asia/Dhaka month boundaries, targeted queries and indexes. No caching or scheduled billing job.

---

## Part 12 — Month Closing, Stock Handover & Reports

Scope:

- Closing management period
- Remaining stock
- Handover
- Outgoing managers
- Incoming managers
- Stock valuation record
- Handover payment
- Purchase summary
- Supplier dues summary
- Consumption summary
- Final Year order/bill summary
- Years 1–4 payment summary
- Management history

### Exclusion

Do not implement the college BDT 125,000 advance until its business rules are clarified.

### Confirmed Part 12 rules

- Handover records responsibility for remaining stock between consecutive management periods. Quantities and authoritative units are checked against Part 9 Inventory; no stock-out or stock-in movement is created.
- The confirmed handover value is entered manually with BigDecimal. No valuation formula is defined. The incoming team records one full payment to the outgoing team; installment and refund rules remain deferred.
- Existing Management Period ACTIVE → CLOSED status remains the only close lifecycle. Handover and closing are separate operations because their exact required sequence is not confirmed.
- Monthly reports derive from authoritative existing records. Current supplier dues and live stock are labeled as current figures; handover lines preserve transferred quantities. No duplicate monthly report collection, caching, or scheduled jobs.

---

# 22. Security Direction

The Meal System will use Spring Security and JWT as part of the Campus Day backend.

### Principles

- Authenticate users through the central Campus Day authentication system.
- Authorization must be server-side.
- Never trust frontend-only role checks.
- A student's temporary Meal Manager responsibility must be verifiable from backend data.
- A normal student must not gain manager capabilities by modifying client requests.
- Avoid scattering role strings throughout controllers.
- Keep authorization logic maintainable.

Exact authentication implementation details will be defined in the relevant Part/security prompt.

---

# 23. API Design Principles

All backend functionality should use REST APIs.

Prefer:

- consistent resource naming
- proper HTTP methods
- meaningful status codes
- validation errors
- centralized exception handling
- request/response DTOs
- no direct exposure of internal persistence models unless explicitly justified

Example style only:

```text
GET    /api/...
POST   /api/...
PUT    /api/...
DELETE /api/...
```

Exact routes must be defined in each Part prompt.

Do not invent all endpoints from this document.

---

# 24. DTO Rules

Prefer Java records for DTOs.

Example style:

```java
public record CreateSomethingRequest(
        @NotBlank String name
) {}
```

Use separate request/response DTOs when the API contract benefits from it.

Do not bind MongoDB persistence models directly to external API input.

---

# 25. Validation Rules

Validation should exist at appropriate levels.

Examples:

- Request validation
- Business validation
- Authorization validation
- State-transition validation
- Duplicate-prevention validation

Validation errors should be user-friendly and API-friendly.

Do not silently accept invalid states.

---

# 26. Exception Handling

Use a centralized and consistent exception-handling strategy.

Expected categories may include:

- Resource not found
- Validation/business rule violation
- Unauthorized
- Forbidden
- Conflict
- Invalid state transition

Do not use generic `RuntimeException` everywhere.

Exact exception classes can be introduced gradually as needed.

---

# 27. MongoDB Principles

When implementing models:

- Use meaningful document boundaries.
- Add indexes when query patterns justify them.
- Avoid unnecessary deeply nested documents.
- Avoid unbounded embedded arrays.
- Store references/identifiers cleanly.
- Preserve historical records required for billing/audit.
- Do not overwrite financial history in ways that destroy traceability.

Exact collections will be decided per Part.

---

# 28. Frontend Principles

The React frontend should be designed for real student usage.

### Required qualities

- Fast
- Clear
- Mobile-friendly
- Desktop-friendly
- Student-friendly
- Minimal unnecessary complexity
- Consistent states
- Accessible interactions
- Good loading/error/empty states
- No developer/debug text in final UI
- No fake data presented as real backend data after integration

### Important state handling

The UI should clearly distinguish states such as:

- Available
- Unavailable
- Price pending
- Ordered
- Not Eating
- Locked
- Cancelled
- Paid
- Unpaid
- Finalized
- Pending finalization

Exact UI design will be handled in Part-specific prompts.

---

# 29. Domain Separation Rules

Keep features modular.

Examples:

- Final Year ordering should not directly become stock logic.
- Stock should not directly calculate student bills.
- Billing should consume trusted order/price data through services.
- Absence notice must remain separate from Final Year ordering.
- Purchasing and stock are related but should not become one giant feature.
- Authentication/security is cross-cutting but should not leak into every class unnecessarily.

Avoid tightly coupled code.

---

# 30. Data Integrity and History

The Meal System contains financial and operational records.

Therefore:

- Preserve historical orders where needed.
- Preserve price history where needed.
- Preserve finalized bill data.
- Preserve payment records.
- Preserve stock movement history.
- Preserve management-period history.
- Avoid destructive updates that make past monthly reports unreliable.

Exact immutability rules will be defined in implementation Parts.

---

# 31. Important Confirmed Rules Summary

Codex must not contradict these unless a future prompt explicitly changes them.

1. Campus Day is for JIMC.
2. Campus Day is student-based/student-first.
3. Meal Managers are students.
4. Each meal management period has 2 responsible 4th-year students.
5. Their internal responsibility split is not fixed.
6. Years 1–4 pay fixed monthly meal charges.
7. Years 1–4 pay even when they do not eat.
8. Years 1–4 absence notice only helps cooking planning.
9. Years 1–4 receive no bill reduction for absence.
10. No penalty exists for not submitting an absence notice.
11. Final Year students use consumption-based billing.
12. Final Year default without order = Not Eating.
13. Breakfast is a complete package.
14. Lunch supports meal/item-based selection.
15. Dinner supports meal/item-based selection.
16. Final Year can order before price is available.
17. If an applicable price exists, the student can see it.
18. If no applicable price exists, the student sees Price Pending, not a fake price.
19. Authorized meal-management users configure order/edit/cancel timing.
20. Final Year pays according to the selected/applicable prices.
21. Final Year pricing is affected by real market purchase costs.
22. Do not invent an automatic price calculation formula.
23. Final Year payment is collected directly by Meal Managers in the current process.
24. Years 1–4 normal payment goes through the college office in the current process.
25. Grocery/chicken/fish payment modes are not globally hard-coded.
26. Supplier credit tracking is required eventually.
27. Stock consumption is tracked meal-wise.
28. Remaining stock is handed to the next management team.
29. The new 2 managers pay the previous 2 managers the recorded value of transferred remaining stock.
30. Detailed Meal Committee permissions are pending.
31. College BDT 125,000 advance accounting is pending.

---

# 32. PENDING / UNRESOLVED BUSINESS RULES

Codex must not guess these.

## 32.1 Meal Committee

Still to be clarified:

- Full permissions
- Approval flow
- Edit rights
- Time-setting authority
- Review-only vs management access

## 32.2 College Advance

Still to be clarified:

- Exact BDT 125,000 advance flow
- Recipient
- Accounting treatment
- Office adjustment
- Month-end settlement

## 32.3 Detailed Price Derivation

Meal/item price is selected based on real purchase/market cost context.

However, the exact mathematical formula is not yet locked.

## 32.4 Stock Valuation Formula

The exact formula for valuing remaining stock during handover is not yet locked.

## 32.5 Some Detailed Timing Policies

Order/edit/cancel times are configurable.

Exact default times or scheduling policies must come from future prompts.

---

# 33. Codex Working Protocol

Whenever a new Part prompt is given, Codex should follow this process:

## Step 1 — Read Context

Read this file completely.

## Step 2 — Inspect Existing Code

Before creating or modifying code:

- inspect current project structure
- identify existing authentication/security
- identify existing student model
- identify reusable components
- identify completed Meal Parts
- identify API conventions already in use

## Step 3 — Scope Check

Implement only the requested Part.

Do not silently start future Parts.

## Step 4 — Conflict Check

If the requested Part conflicts with:

- this document
- existing completed code
- another confirmed business rule

stop and clearly report the conflict before making a destructive change.

## Step 5 — Implement

Follow:

- Spring Boot best practices
- SOLID
- Clean Code
- package-by-feature
- REST API
- MongoDB
- DTO records
- validation
- centralized exception handling
- secure backend authorization
- React maintainability

## Step 6 — Integration

Connect frontend and backend only for functionality included in that Part.

Do not create fake frontend behavior when a real API is required by the Part.

## Step 7 — Verify

Before declaring the Part complete, verify:

- compile/build
- API behavior
- validation
- security
- MongoDB persistence
- frontend integration where applicable
- error states
- existing feature regression risk

## Step 8 — Completion Report

At the end of every Part, report:

1. What was implemented
2. Files created
3. Files modified
4. REST endpoints added
5. MongoDB collections/documents affected
6. Frontend pages/components added
7. Security/permissions added
8. Validation/business rules implemented
9. Tests/checks performed
10. Anything intentionally deferred
11. Any unresolved issue discovered

---

# 34. Non-Negotiable Rules for Codex

- Do not invent business requirements.
- Do not implement `PENDING` rules.
- Do not rename business concepts casually.
- Do not tightly couple unrelated modules.
- Do not expose persistence documents directly without reason.
- Do not trust frontend authorization.
- Do not hard-code prices.
- Do not use fake prices.
- Do not make Years 1–4 billing consumption-based.
- Do not make Final Year billing fixed monthly.
- Do not reduce Years 1–4 bills because of absence.
- Do not treat Meal Manager as non-student.
- Do not force a fixed responsibility split between the 2 managers.
- Do not implement the college BDT 125,000 advance before clarification.
- Do not guess Meal Committee permissions.
- Do not implement later Parts unless explicitly requested.
- Do not change completed working behavior without a requirement.

---

# 35. End Goal

The final Meal System should replace the current manual workflow with a maintainable, student-friendly digital system that supports:

- student meal interaction
- Final Year ordering
- Years 1–4 absence planning
- meal/menu management
- flexible pricing
- digital fraction sheet
- cooking planning
- stock tracking
- kitchen issues
- purchases
- supplier dues
- monthly billing
- collections
- management rotation
- stock handover
- reports

The completed system should be realistic enough for actual JIMC student meal operations while remaining clean, secure, modular, and maintainable.

---

# 36. Current Status

This document defines the current agreed Meal System context.

Implementation has **not** been authorized globally.

The next work should begin only when a specific Part prompt is provided.

Recommended starting point:

**Part 1 — Meal Foundation, Student & Access Context**

---
# 37. Documentation Single Source of Truth Policy

This project must avoid repeating the same business logic across multiple Part prompts, source files, and documentation files.

## Rule

`CAMPUS_DAY_MEAL_SYSTEM_CONTEXT.md` is the **master business-context source of truth** for the Meal System unless a later approved document explicitly supersedes a section.

Part-specific prompts should **reference the relevant sections of this document** instead of copying the same business rules again and again.

Example:

```text
Read CAMPUS_DAY_MEAL_SYSTEM_CONTEXT.md first.
Follow Sections 6.2, 7, 8, 9, and 31.
Implement only Part 4 — Final Year Meal Ordering.
Do not redefine pricing, billing, stock, or committee rules.
```

## Why

Repeating the same rule in many places creates maintenance problems:

- one copy may be updated while another becomes stale
- contradictory requirements may appear
- Codex may implement different versions of the same rule
- future maintenance becomes harder

## Codex documentation behavior

When a business rule already exists in this file:

- reference it instead of duplicating it
- do not create a second competing definition
- if a future Part changes a rule, update the master source first or explicitly mark the new rule as an approved replacement
- implementation notes may explain how a rule is enforced technically, but they must not redefine the business rule

Business rules and implementation rules should remain distinct.

Example:

Business rule:

```text
Final Year students may place an order before an applicable price exists.
```

Implementation rule:

```text
The backend must not reject a valid order merely because its current price is unavailable.
```

---

# 38. Performance and Caching Policy

The Meal System should avoid unnecessary MongoDB reads and repeated expensive calculations.

Caching may be used where it provides a real performance benefit.

## Important principle

**Do not cache everything.**

Caching is appropriate mainly for data that is:

- read frequently
- changed relatively infrequently
- safe to serve for a short period without hitting MongoDB again
- expensive or repetitive to calculate/query

## Good caching candidates

Depending on the implementation Part, suitable candidates may include:

- published daily/weekly meal menu
- currently active meal-management configuration
- current order/edit/cancel cutoff configuration
- currently applicable Final Year item prices
- frequently requested read-only reference data
- aggregated cooking-plan totals when recomputation is expensive and invalidation is clear
- selected report summaries when the underlying month is closed or immutable

## Data that requires caution

Do not blindly cache highly dynamic or financially sensitive data such as:

- newly submitted meal orders
- payment transactions
- stock issue operations
- live available stock
- supplier payments
- mutable monthly bills
- authorization-sensitive user state

For these areas, correctness and consistency are more important than avoiding every database query.

## Cache invalidation

When cached source data changes, the corresponding cache must be updated or evicted.

Examples:

- publishing/updating a menu -> evict relevant menu cache
- changing a price -> evict relevant price cache
- changing cutoff configuration -> evict relevant configuration cache
- changing active managers -> evict relevant management-context cache

Never allow stale cached values to silently override confirmed financial or authorization data.

## Recommended implementation direction

Start simple.

For the initial version, Spring Cache abstraction may be used when a Part actually benefits from caching.

Possible implementation path:

```text
Spring Cache abstraction
        ↓
Local in-memory cache such as Caffeine for a single application instance
        ↓
Redis later only if deployment architecture, scaling, or distributed cache requirements justify it
```

Do not introduce Redis only because caching exists.

For a small or single-instance deployment, a local cache may be sufficient.

If the application later runs multiple backend instances, shared/distributed caching may become necessary for cache consistency.

## Performance rule for Codex

Before adding caching, identify:

1. what query/calculation is repeated
2. how frequently the underlying data changes
3. acceptable staleness
4. invalidation strategy
5. whether caching actually improves the use case

Do not add caching merely as decoration or premature optimization.

## Other database-performance rules

Caching is only one optimization. Codex should also:

- design MongoDB queries around real access patterns
- add indexes for justified frequent queries
- avoid N+1-style repeated database access patterns
- avoid loading entire collections when filters/pagination can be applied in MongoDB
- use pagination for large lists
- fetch only the data needed for a use case where practical
- avoid repeated repository calls inside loops when one query can retrieve the required data
- avoid calculating the same aggregate repeatedly inside one request
- preserve correctness over micro-optimization

---

# 39. Performance Goal

The target is a **fast, student-friendly, maintainable real-world system**, not simply a system with the largest number of technologies.

Performance decisions should follow this order:

```text
Correct domain model
→ efficient MongoDB query design
→ proper indexes
→ avoid duplicate/repeated database work
→ pagination/projections where useful
→ selective caching
→ distributed caching only when scaling requires it
```

The final implementation should be easy to understand and operate in the future.

# 37. Lombok Usage Policy

Use Lombok selectively and intentionally.

## Preferred Lombok annotations

Use these where they are genuinely useful:

- `@Getter`
- `@Setter`
- `@RequiredArgsConstructor`
- `@Builder`
- `@NoArgsConstructor`
- `@AllArgsConstructor` only when genuinely needed

## Getter / Setter Rule

`@Getter` and `@Setter` may be used where required.

Do **not** add setters automatically to every field or every class.

Prefer:

- `@Getter` when read access is required.
- `@Setter` only when controlled mutation is actually required.
- Constructor-based initialization for required dependencies.
- Explicit domain methods when a business state transition has meaning.

Example:

Instead of exposing unrestricted setters such as:

```java
order.setStatus(OrderStatus.CANCELLED);
```

prefer a domain/service operation such as:

```java
order.cancel();
```

when cancellation has business validation or state-transition rules.

## Avoid blanket `@Data`

Do not use Lombok `@Data` by default on domain/persistence models.

`@Data` automatically creates getters, setters, `equals`, `hashCode`, and `toString`, which may expose mutation or behavior that is not appropriate for every model.

Use the smallest set of Lombok annotations needed by the class.

## DTOs

DTOs should preferably use Java `record`, so Lombok getters/setters are normally unnecessary for request/response DTOs.

Example:

```java
public record CreateMealOrderRequest(
        String mealDate,
        String mealType
) {}
```

## MongoDB Models

For MongoDB documents:

- Use `@Getter` where external read access is needed.
- Use `@Setter` only for fields that legitimately need mutation.
- Prefer controlled service/domain methods for important business state changes.
- Do not expose unrestricted setters for financial, billing, payment, finalized order, or audit-sensitive state unless the design explicitly requires it.

## Codex Rule

Codex must not mechanically add `@Getter`, `@Setter`, or `@Data` to every model.

It should choose Lombok annotations according to the responsibility and mutability requirements of each class.

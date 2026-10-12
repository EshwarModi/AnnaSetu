# AnnaSetu &mdash; Phase 4: Organization Features & Slot Availability Management

## 1. Architectural Overview

Phase 4 implements the complete organization-facing workspace for NGOs, orphanages, elder care homes, and shelters. Following our layered architecture, it separates HTTP presentation concerns from business rule enforcement and database transactions:

```
[ HTTP Requests /org/** ]
         │
         ▼
[ OrganizationController ] ── (Validates DTO, catches BusinessRuleException, maps Flash messages)
         │
         ├───► [ OrganizationService ] ──► [ OrganizationProfileRepository ] ──► [ MySQL 8 ]
         │
         └───► [ SlotService ]          ──► [ SlotAvailabilityRepository ]    ──► [ MySQL 8 ]
```

---

## 2. Core Features & Business Invariants

### 1. Dual-State Organization Dashboard (`/org/dashboard`)
- **Status Banner**:
  - `PENDING`: An amber alert banner informs the institution that their registration is under administrative verification. Calendar slot creation, capacity modifications, and date blocking are locked until approved.
  - `APPROVED`: A green partner banner confirms active verification status. Quick shortcuts unlock full access to the calendar slot manager.
  - `REJECTED`: Displays application feedback.
- **Operational Metrics**:
  - Real-time counters for **Upcoming Slots**, **Booked Slots**, **Daily Beneficiaries**, and **Dietary Standard**.
- **Institutional Profile Snapshot**:
  - Shows address, contact person, phone, and mission overview with a direct link to edit.
- **Upcoming Meal Sponsorships**:
  - A table of confirmed bookings with donor details, meal windows, and celebratory occasions.

### 2. Profile View & Edit (`/org/profile`)
- Decoupled from entity via [`OrganizationProfileUpdateDto`](file:///d:/yuvi/AnnaSetu/src/main/java/com/annasetu/dto/OrganizationProfileUpdateDto.java).
- Prevents mass-assignment: Sensitive security attributes (`id`, `user_id`, `approvalStatus`, `createdAt`) are immutable through this form.
- Full Jakarta Bean Validation (`@NotBlank`, `@Pattern` for phone/pincode, `@Min`/`@Max` for beneficiaries).

### 3. Slot Availability Management (`/org/slots`)
- **Batch Date Range Generator**:
  - Allows an organization to generate slots for an upcoming window (e.g., next 30 days) in a single request.
  - Checkbox selection for meal slots (`BREAKFAST`, `LUNCH`, `DINNER`).
  - Capacity configuration:
    - **Capacity 1 (Exclusive)**: Only one donor family can sponsor this meal.
    - **Capacity 2 (Shared)**: Up to two independent donors can co-sponsor the feast.
- **Dynamic Capacity Modification**:
  - Inline dropdowns per slot allowing adjustment between 1 and 2.
  - **Critical Invariant**: Capacity can **never** be reduced below the active `bookedCount`.
- **Slot Deletion & Date Blocking**:
  - Individual slots can be deleted only if `bookedCount == 0`.
  - An entire calendar day can be blocked (removing all unbooked slots) provided no meal slots on that day have active donor bookings.

---

## 3. Service-Layer Validations & Invariant Guardrails

All rules are strictly verified in [`SlotServiceImpl`](file:///d:/yuvi/AnnaSetu/src/main/java/com/annasetu/service/SlotServiceImpl.java):

| Business Rule | Enforced Check | Thrown Exception |
|---|---|---|
| **Approval Guard** | `org.getApprovalStatus() == ApprovalStatus.APPROVED` | `BusinessRuleException` |
| **No Past Dates** | `!slotDate.isBefore(LocalDate.now())` | `BusinessRuleException` |
| **Date Range Order** | `!endDate.isBefore(startDate)` | `BusinessRuleException` |
| **Batch Window Cap** | `!startDate.plusDays(90).isBefore(endDate)` | `BusinessRuleException` |
| **Capacity Invariant** | `newCapacity >= slot.getBookedCount()` | `BusinessRuleException` |
| **Referential Booking Guard**| `slot.getBookedCount() == 0` before delete/block | `BusinessRuleException` |

---

## 4. Interview Talking Points

1. **Why enforce invariants in both Controller and Service?**
   * *Defense in Depth*: The presentation layer checks `approvalStatus` to provide clean redirects and flash messages to legitimate users. The service layer asserts invariants transactionally so that API extensions, scheduled jobs, or internal calls can never bypass the business rules.

2. **How is the capacity reduction invariant protected against concurrency?**
   * Class and method level `@Transactional` ensures read-modify-write atomicity.
   * In addition to application-level checks (`newCapacity < slot.getBookedCount()`), the MySQL schema includes a database-level check constraint: `CONSTRAINT chk_slot_booked_count CHECK (booked_count <= capacity)`.

3. **How does batch slot generation avoid unique constraint collisions?**
   * The database enforces `UNIQUE(organization_id, slot_date, meal_slot)`.
   * When re-generating slots for an existing date range, [`SlotServiceImpl`](file:///d:/yuvi/AnnaSetu/src/main/java/com/annasetu/service/SlotServiceImpl.java) detects existing records. If an existing slot already has bookings higher than the requested capacity, it preserves the existing capacity to protect active donor reservations rather than failing or violating the constraint.

4. **Why use the Post/Redirect/Get (PRG) pattern?**
   * Every state-changing form submission (`/org/slots/create`, `/org/slots/{id}/capacity`, `/org/slots/{id}/delete`, `/org/slots/block-date`, `/org/profile`) uses `POST` followed by a `302 Redirect` with `RedirectAttributes` flash messages. This eliminates the duplicate form submission dialog on page refresh.

---

## 5. Automated Verification Summary

All **50 test cases** across the test suite pass cleanly:
- `SlotServiceTest` (13 unit tests): Date ranges, capacity thresholds, unapproved rejection, booking guards.
- `OrganizationServiceTest` (2 unit tests): Profile update mapping and approval state verification.
- `OrganizationControllerTest` (12 integration tests): Dashboard rendering, profile form validation, slot creation, capacity updates, and pending restrictions.
- `SecurityAuthorizationTest` (10 tests)
- `AuthControllerTest` (6 tests)
- `RepositoryIntegrationTests` (5 tests)
- `HomeControllerTest` (1 test)
- `AnnaSetuApplicationTests` (1 test)

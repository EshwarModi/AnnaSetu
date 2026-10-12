# AnnaSetu &mdash; Phase 5: Search, Discovery & Live Slot Availability

## 1. Architectural Overview

Phase 5 implements the core discovery engine of the AnnaSetu platform. Prospective food donors can filter verified institutions by geographic, institutional, dietary, and temporal criteria.

```
[ Web Browser ]
      │
      ├─► GET /search?city=Pune&date=2026-10-15  ──► [ SearchController ]
      │                                                     │
      │                                                     ▼
      │                                         [ OrganizationSpecification ]
      │                                                     │ (Correlated EXISTS Subquery)
      │                                                     ▼
      │                                         [ OrganizationProfileRepository ] ──► [ MySQL 8 ]
      │
      └─► GET /api/organizations/{id}/slots      ──► [ SlotApiController ]
            (Vanilla JS fetch on date change)               │
                                                            ▼
                                                [ OrganizationService ]
                                                            │
                                                            ▼
                                                [ SlotAvailabilityRepository ]    ──► [ MySQL 8 ]
```

---

## 2. Core Features & Implementations

### 1. Multi-Dimensional Search Page (`/search`)
- **Filter Controls**:
  - **City**: Dropdown populated from distinct active cities (`Pune`, `Mumbai`, `Bengaluru`).
  - **Area / Pincode**: Free text input performing a case-insensitive `LIKE` on locality area or exact equality on 6-digit Indian PIN codes.
  - **Organization Category**: `ORPHANAGE`, `OLD_AGE_HOME`, `SHELTER`, `OTHER`.
  - **Sponsorship Date**: Date picker (bounded to `min = today`).
  - **Meal Slot Window**: `BREAKFAST`, `LUNCH`, `DINNER`.
  - **Dietary Compliance**: `VEG`, `NON_VEG`, `BOTH`.
- **Business Discovery Invariants**:
  1. Only **APPROVED** institutions are ever returned.
  2. An institution is **only** returned if it possesses **at least one unbooked slot** (`bookedCount < capacity`) matching the selected date and meal criteria.
  3. Fully booked institutions or institutions with unconfigured dates are dynamically omitted from results.

### 2. Result Cards with Keyless Google Maps
- **Card Contents**:
  - Organization Name and Category Badge.
  - Full street address and distance-free area identifier (e.g., `Kothrud, Pune (411038)`).
  - Calculated **Remaining Capacity Badge** (e.g., `3 Free Slot(s) Available`).
  - **"View & Book Slot"** CTA button linking to `/organizations/{id}?date={selectedDate}`.
  - **"Open in Maps"** button generating a Google Maps Universal Search URL:
    `https://www.google.com/maps/search/?api=1&query={encoded_name_and_address}`
    *(Requires zero Google Maps API keys or paid quotas)*.
- **Accessible Pagination**:
  - Integrated with Spring Data `Pageable` (`page`, `size`) with query parameters preserved across page transitions.

### 3. Institution Detail View (`/organizations/{id}`) & Live AJAX Slot Checking
- **Progressive Enhancement**:
  - Preloads initial slot availability server-side for the current or requested date.
- **Vanilla JavaScript Fetch Client**:
  - When the date picker changes, triggers an asynchronous `fetch('/api/organizations/' + id + '/slots?date=' + selectedDate)`.
  - Live updates the three meal slot cards (**Breakfast**, **Lunch**, **Dinner**):
    - **Available Slot**: Renders a green `Available (X spots left)` badge, enables the **"Book {Meal}"** button, and styles the card with an active border.
    - **Fully Booked Slot (`bookedCount >= capacity`)**: Renders a red `Fully Booked (0 spots left)` badge, disables the button (`disabled="true"` with `.btn-disabled`), and dims the card.
    - **Unconfigured Slot**: Displays `Not Configured`, disables the action button.

### 4. RESTful JSON API (`/api/organizations/{id}/slots`)
- Serves an array of 3 serialized [`SlotAvailabilityResponseDto`](file:///d:/yuvi/AnnaSetu/src/main/java/com/annasetu/dto/SlotAvailabilityResponseDto.java) objects:
  ```json
  [
    {
      "mealSlot": "BREAKFAST",
      "mealSlotDisplayName": "Breakfast",
      "slotId": 101,
      "slotDate": "2026-10-15",
      "capacity": 1,
      "bookedCount": 0,
      "remainingCapacity": 1,
      "isAvailable": true,
      "isConfigured": true,
      "statusMessage": "Available (1 spot left)"
    },
    {
      "mealSlot": "LUNCH",
      "mealSlotDisplayName": "Lunch",
      "slotId": 102,
      "slotDate": "2026-10-15",
      "capacity": 2,
      "bookedCount": 2,
      "remainingCapacity": 0,
      "isAvailable": false,
      "isConfigured": true,
      "statusMessage": "Fully Booked (0 spots left)"
    },
    {
      "mealSlot": "DINNER",
      "mealSlotDisplayName": "Dinner",
      "slotId": null,
      "slotDate": "2026-10-15",
      "capacity": 0,
      "bookedCount": 0,
      "remainingCapacity": 0,
      "isAvailable": false,
      "isConfigured": false,
      "statusMessage": "Not Configured"
    }
  ]
  ```

---

## 3. Interview Talking Points

1. **Why use a JPA Specification with a correlated subquery rather than an SQL JOIN?**
   * *The Cartesian Product & Pagination Problem*: When you `JOIN` an entity to a collection of child rows (e.g. `OrganizationProfile` joined to `SlotAvailability`), an organization with 30 slots creates 30 rows in the raw result set. Using `DISTINCT` forces Hibernate to perform in-memory pagination or duplicate deduplication, which degrades performance and fails for large datasets.
   * *The Subquery Solution*: By placing `WHERE EXISTS (SELECT 1 FROM SlotAvailability s WHERE s.organization = o AND s.bookedCount < s.capacity ...)` in the JPA Specification, the database executes an index-backed semi-join. Each organization row appears exactly once in the SQL result, enabling native, high-performance database-level `LIMIT` and `OFFSET` pagination.

2. **How is dietary compatibility handled?**
   * If a donor filters for `VEG`, institutions with `foodPreference = VEG` **and** `foodPreference = BOTH` can cater to them. The Specification translates this into a SQL `IN ('VEG', 'BOTH')` predicate rather than a naive equality check.

3. **How does keyless external mapping work?**
   * Google Maps provides a standard Universal URL scheme: `https://www.google.com/maps/search/?api=1&query={query}`. Passing the UTF-8 encoded institution name, street address, locality, city, and postal code opens the native Google Maps app or browser view without requiring an API key, billing account, or JavaScript SDK overhead.

4. **Why Jackson `@JsonProperty("isAvailable")` on boolean properties?**
   * By default, the JavaBeans naming convention converts getter `boolean isAvailable()` into JSON property `"available"`. Annotating with `@JsonProperty("isAvailable")` preserves the `is` prefix in JSON, matching the frontend JavaScript convention and preventing client-side undefined property bugs.

---

## 4. Automated Verification Results

All **61 tests passed** (`BUILD SUCCESS`):
- `SearchControllerTest` (7 tests): Default discovery, city filtering, category filtering, dietary compliance, date filtering, detail rendering with Google Maps links, and 404 handling.
- `SlotApiControllerTest` (4 tests): JSON array contract, available slot serialization, fully booked slot disabling, and 404 security handling.
- `SlotServiceTest` (13 tests)
- `OrganizationControllerTest` (12 tests)
- `SecurityAuthorizationTest` (10 tests)
- `AuthControllerTest` (6 tests)
- `RepositoryIntegrationTests` (5 tests)
- `OrganizationServiceTest` (2 tests)
- `HomeControllerTest` (1 test)
- `AnnaSetuApplicationTests` (1 test)

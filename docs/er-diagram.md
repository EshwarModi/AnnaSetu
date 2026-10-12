# AnnaSetu - Entity Relationship Diagram & Data Architecture

## Overview
**AnnaSetu** is an occasion-based food donation slot booking platform connecting donors with verified orphanages, old age homes, and community shelters.

---

## Mermaid ER Diagram

```mermaid
erDiagram
    USERS ||--o| DONOR_PROFILES : "authenticates (1:1)"
    USERS ||--o| ORGANIZATION_PROFILES : "authenticates (1:1)"
    ORGANIZATION_PROFILES ||--o{ SLOT_AVAILABILITIES : "publishes (1:N)"
    SLOT_AVAILABILITIES ||--o{ BOOKINGS : "reserved_by (1:N)"
    DONOR_PROFILES ||--o{ BOOKINGS : "books (1:N)"

    USERS {
        bigint id PK "Auto Increment"
        varchar email UK "Unique Email (100)"
        varchar password_hash "BCrypt Hash (255)"
        varchar role "DONOR, ORGANIZATION, ADMIN"
        boolean enabled "Default true"
        datetime created_at "Audit creation timestamp"
        datetime updated_at "Audit update timestamp"
    }

    DONOR_PROFILES {
        bigint id PK "Auto Increment"
        bigint user_id FK, UK "1:1 with USERS (Cascade)"
        varchar full_name "Full Name (100)"
        varchar phone "Phone Number (20)"
        varchar city "Donor City (50)"
        datetime created_at "Audit timestamp"
        datetime updated_at "Audit timestamp"
    }

    ORGANIZATION_PROFILES {
        bigint id PK "Auto Increment"
        bigint user_id FK, UK "1:1 with USERS (Cascade)"
        varchar name "Organization Name (150)"
        varchar type "ORPHANAGE, OLD_AGE_HOME, SHELTER, OTHER"
        varchar address "Street Address (255)"
        varchar city "City (50) [INDEXED]"
        varchar area "Neighborhood Area (100)"
        varchar pincode "Postal Code (10)"
        varchar contact_person "Contact Name (100)"
        varchar phone "Contact Phone (20)"
        int people_served "Daily Beneficiary Count"
        text description "Mission and Details"
        varchar food_preference "VEG, NON_VEG, BOTH"
        varchar approval_status "PENDING, APPROVED, REJECTED [INDEXED]"
        datetime created_at "Audit timestamp"
        datetime updated_at "Audit timestamp"
    }

    SLOT_AVAILABILITIES {
        bigint id PK "Auto Increment"
        bigint organization_id FK "References ORGANIZATION_PROFILES"
        date slot_date "Calendar Date [INDEXED]"
        varchar meal_slot "BREAKFAST, LUNCH, DINNER"
        int capacity "Allowed Donors (e.g., 1 or 2)"
        int booked_count "Current Reservations (CHECK <= capacity)"
        datetime created_at "Audit timestamp"
        datetime updated_at "Audit timestamp"
    }

    BOOKINGS {
        bigint id PK "Auto Increment"
        bigint slot_id FK "References SLOT_AVAILABILITIES"
        bigint donor_id FK "References DONOR_PROFILES"
        varchar occasion "BIRTHDAY, ANNIVERSARY, MEMORIAL, FESTIVAL, OTHER"
        int people_count "Visiting Family Members Count"
        text food_description "Sponsored Menu / Dish Details"
        varchar status "PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED [INDEXED]"
        datetime created_at "Audit timestamp"
        datetime updated_at "Audit timestamp"
    }
```

---

## Architectural & Interview Design Rationale

### 1. Database-First Integrity with `ddl-auto=validate`
- The application relies on `schema.sql` to explicitly define constraints, data types, indexes, and engines (`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`).
- Hibernate's `spring.jpa.hibernate.ddl-auto=validate` verifies at startup that the JPA annotations (`@Entity`, `@Table`, `@Column`, `@UniqueConstraint`, `@Index`) exactly match the physical database structure, preventing accidental data corruption or implicit schema mutations.

### 2. Normalization and 1:1 Persona Separation
- Rather than maintaining a monolithic `users` table with sparse, nullable columns, security credentials (`users`) are isolated from domain profiles (`donor_profiles` and `organization_profiles`).
- This allows custom role attributes (such as dietary rules and beneficiary counts for NGOs) to evolve independently without compromising donor accounts.

### 3. Concurrency & Constraint Safety
- **Composite Unique Keys**:
  - `slot_availabilities(organization_id, slot_date, meal_slot)` guarantees an institution cannot define duplicate meal slots for the same calendar day.
  - `bookings(slot_id, donor_id)` prevents the same donor from double-booking a slot.
- **Check Constraint**:
  - `CHECK (booked_count <= capacity)` on `slot_availabilities` safeguards against race conditions and overbooking at the database engine level.

### 4. Indexing Strategy for Fast Discoverability
- **`idx_org_city` on `organization_profiles(city)`**: Powers real-time search queries by metropolitan area.
- **`idx_org_approval` on `organization_profiles(approval_status)`**: Ensures only vetted and approved institutions appear on public discovery pages.
- **`idx_slot_date` and `idx_slot_org_date` on `slot_availabilities`**: Powers high-frequency monthly calendar and day-view queries without full table scans.

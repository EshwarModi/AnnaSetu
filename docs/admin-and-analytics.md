# AnnaSetu &mdash; Phase 7: Admin Governance, Analytics, Calendar Agenda & Print Receipts

## 1. Architectural Overview

Phase 7 introduces the administrative governance suite, organization operational calendar, printable official donation receipts, and email notification infrastructure for the AnnaSetu platform.

```
[ Platform Admin ] ──► GET /admin/dashboard ──► [ AdminController ]
                             │                        │
                             │                        ▼
                             │                 [ AdminService ] ──► [ Repositories (Metrics, Feed) ]
                             │                        │
                             ├──► POST /approve ──────┤ ──► [ NotificationService (Email) ]
                             ├──► POST /reject  ──────┤
                             └──► POST /toggle  ──────┘

[ Partner Org ]    ──► GET /org/dashboard   ──► [ OrganizationController ]
                                                      │
                                                      ▼
                                               Grouped Calendar Agenda (TreeMap<LocalDate, List<Booking>>)

[ Food Donor ]     ──► GET /donor/bookings/{id}/receipt ──► [ DonorController ]
                                                                  │
                                                                  ▼
                                                      Thymeleaf Receipt Template
                                                      (CSS @media print optimized)
```

---

## 2. Core Features & Implementations

### 1. Administrative Analytics & Governance Dashboard (`/admin/dashboard`)
- **Metric Aggregates (`AdminStatsDto`)**:
  - Direct repository count aggregations computing:
    - Total Registered Donors (`userRepository.countByRole(Role.DONOR)`)
    - Organizations by status: Total, Pending Approval, Approved, and Rejected.
    - Capacity & Utilization: Total Slots Published, Total Bookings, and bookings grouped by status (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`).
- **Partner Verification & Vetting Workflow**:
  - **Approval (`POST /admin/organizations/{id}/approve`)**:
    - Updates `approval_status` to `APPROVED`.
    - Automatically enables the associated user account.
    - Triggers `notificationService.sendOrganizationApprovalNotification(...)`.
  - **Rejection with Audit Reason (`POST /admin/organizations/{id}/reject`)**:
    - Validates mandatory reason parameter (e.g., "Incomplete shelter registration papers").
    - Updates `approval_status` to `REJECTED` and persists `rejection_reason` in the database.
    - Dispatches rejection notification explaining requirements.
- **Account Access Governance (`POST /admin/users/{id}/toggle-status`)**:
  - Enables or disables donor or organization accounts.
  - **Self-Lockout Safeguard**: Validates `currentUser.getEmail().equalsIgnoreCase(targetUser.getEmail())` and prevents administrators from accidentally disabling their own active administrative credential.
- **Live Platform Bookings Surveillance Stream**:
  - Leverages Spring Data JPA `PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "createdAt"))`.
  - Optimized via custom JPQL query `findRecentBookingsWithDetails` executing a single `JOIN FETCH` across `booking.slot`, `slot.organization`, and `booking.donor` to completely eliminate N+1 select queries.

---

### 2. Operational Organization Calendar Agenda (`/org/dashboard`)
- **Today's Sponsored Meals Spotlight**:
  - Queries `bookingRepository.findUpcomingBookingsByOrganization(...)` filtering specifically for `slotDate = LocalDate.now()`.
  - Renders a prominent card displaying meals, serving counts, donor names, contact numbers, and occasion tags for the current day.
- **Grouped Date-by-Date Schedule**:
  - Java Stream grouping collector utilizing `TreeMap<LocalDate, List<Booking>>`:
    ```java
    Map<LocalDate, List<Booking>> upcomingBookingsByDate = upcomingBookings.stream()
            .collect(Collectors.groupingBy(
                    b -> b.getSlot().getSlotDate(),
                    TreeMap::new,
                    Collectors.toList()
            ));
    ```
  - Displays a clean chronological agenda view where kitchen coordinators can review meal requirements grouped by date.

---

### 3. Print-Friendly Official Donation Receipt (`/donor/bookings/{id}/receipt`)
- **Receipt Presentation**:
  - Formatted certificate view detailing:
    - Official AnnaSetu receipt serial identifier (`AS-RCPT-{id}-{slotDate}`).
    - Donor full name and registered email.
    - Partner organization name, registration type, address, contact person, and phone.
    - Meal slot window (`BREAKFAST`, `LUNCH`, `DINNER`), booked date, and occasion (`BIRTHDAY`, `ANNIVERSARY`, etc.).
    - Estimated people served count and donor's food description notes.
    - Verified QR/verification cryptographic placeholder badge.
- **CSS3 `@media print` Isolation (`static/css/main.css`)**:
  - Uses CSS media queries to completely hide web application chrome:
    ```css
    @media print {
        header, footer, nav, .navbar, .alert, .btn, .no-print {
            display: none !important;
        }
        body {
            background: #fff !important;
            color: #000 !important;
            font-size: 12pt;
        }
        .receipt-card {
            border: 1px solid #1f2937 !important;
            box-shadow: none !important;
            page-break-inside: avoid;
        }
    }
    ```
  - Includes a vanilla JavaScript `window.print()` trigger button and clean browser printing without headers or URL breadcrumbs.

---

### 4. Enterprise Notification Infrastructure
- **Notification SPI Contract (`NotificationService`)**:
  - Defines domain notification hooks:
    - `sendBookingCreatedNotification(Booking booking)`
    - `sendBookingStatusUpdateNotification(Booking booking)`
    - `sendBookingCancelledNotification(Booking booking)`
    - `sendOrganizationApprovalNotification(OrganizationProfile org)`
    - `sendOrganizationRejectionNotification(OrganizationProfile org, String reason)`
- **Email Service (`EmailServiceImpl`)**:
  - Injects `ObjectProvider<JavaMailSender>` to prevent application startup failure when SMTP credentials are not configured in development environments.
  - Controlled by configurable application toggle:
    ```properties
    annasetu.mail.enabled=false
    ```
  - **Graceful Fallback & Audit Logging**:
    - When `annasetu.mail.enabled=false`, the service logs simulated email payloads with `[EMAIL SIMULATION - DISABLED VIA CONFIG]` containing recipient, subject line, and body preview.
    - When enabled, sends multipart MIME messages wrapped in try-catch guards to prevent email transport exceptions from rolling back core database transactions.

---

## 3. Interview Talking Points & Deep Dive

### Q1: Why did you use `ObjectProvider<JavaMailSender>` instead of `@Autowired JavaMailSender`?
> **Answer**:  
> In Spring Boot, `JavaMailSenderImpl` auto-configuration is triggered by `spring-boot-starter-mail`. However, if the development environment lacks valid SMTP credentials or in offline testing, autowiring `JavaMailSender` directly can fail if configuration is absent. By wrapping it in `ObjectProvider<JavaMailSender>`, Spring resolves the bean lazily and safely returns null if absent. Combined with our `annasetu.mail.enabled` property check, this ensures the application boots and runs in local development, test environments, and air-gapped CI pipelines without requiring an active SMTP server.

### Q2: How did you solve the N+1 select query problem on the Admin Dashboard live bookings stream?
> **Answer**:  
> A standard `bookingRepository.findAll()` call would trigger 1 initial query for bookings, followed by N queries for slots, N queries for organizations, and N queries for donors. To eliminate this, we defined a custom JPQL query with `JOIN FETCH`:
> ```sql
> SELECT b FROM Booking b 
> JOIN FETCH b.slot s 
> JOIN FETCH s.organization o 
> JOIN FETCH b.donor d 
> ORDER BY b.createdAt DESC
> ```
> This executes a single SQL JOIN query in MySQL 8, fetching all required object graphs in a single database round-trip.

### Q3: Why is the rejection reason persisted in the database instead of just emailed?
> **Answer**:  
> For audit compliance and accountability. Food donation platforms must maintain an immutable audit trail of why an institution's application was rejected (e.g., invalid government registration, duplicate entity, unverified contact person). If an organization reapplies or appeals, administrators can review the historical `rejection_reason` directly from the database without relying on external email logs.

### Q4: How is print styling achieved without external PDF libraries (like iText or FlyingSaucer)?
> **Answer**:  
> We leveraged native CSS3 `@media print` stylesheets. By applying `@media print`, we suppress navigation bars, action buttons, alerts, and footers (`display: none !important`), adjust font sizes to point units (`12pt`), ensure pure black text on white background (`color: #000; background: #fff`), and set `page-break-inside: avoid` on the receipt card. This produces printouts and "Save as PDF" outputs directly using the user's browser print engine without adding heavy server-side PDF generation dependencies.

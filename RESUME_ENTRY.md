# AnnaSetu &mdash; Resume Entry & Bullet Points

Use these tailored, high-impact resume bullet points to showcase your engineering contributions on AnnaSetu.

---

## 1. Project Title & One-Line Description

**AnnaSetu &mdash; Full-Stack Food Donation Slot Booking Platform**  
*A production-grade Spring Boot 3 web platform connecting donors with verified orphanages and care centers for occasion-based meal sponsorships under strict concurrency and role-based access control.*

---

## 2. Concise Resume Bullet Points (Ready for Your Resume)

### Option A: Senior Full Stack Engineer (Impact & Systems Focus)
- **Engineered an enterprise-grade food donation slot reservation platform** using **Java 17, Spring Boot 3, Spring Security 6, Spring Data JPA, and MySQL 8**, enabling donors across 3 cities to discover vetted shelters and book meal sponsorship slots.
- **Eliminated double-booking race conditions** under concurrent reservation spikes by implementing **pessimistic write locking (`SELECT ... FOR UPDATE`)**, proven by a 10-thread parallel integration test guaranteeing zero over-capacity bookings.
- **Architected a zero-bloat, accessible frontend** utilizing **Thymeleaf SSR, modern handwritten CSS3 (custom properties, flexbox, grid), and vanilla JS Fetch API**, achieving sub-100ms server-side render times with zero external UI framework dependencies.
- **Implemented multi-persona RBAC and security hygiene** across Donor, Organization, and Admin roles using **BCrypt, CSRF tokens, session fixation mitigation (`changeSessionId()`), and `@ControllerAdvice` global exception masking** to prevent stack trace leaks.
- **Optimized relational data access and operational reporting**, eliminating N+1 select queries via **deep `JOIN FETCH` JPQL queries**, dynamic multi-criteria search with **JPA Specifications**, and CSS `@media print` donation receipt generation.
- **Maintained 100% test pass rate across 109 automated tests**, including Mockito unit tests, `@DataJpaTest` repository slices, MockMvc security boundary checks, and a complete 10-step end-to-end lifecycle integration test.

---

### Option B: Backend-Focused (Concurrency & Architecture Focus)
- **Built a high-concurrency slot booking engine in Spring Boot 3 & MySQL 8**, enforcing shared meal capacity rules through pessimistic locking (`LockModeType.PESSIMISTIC_WRITE`) and database-level `CHECK (booked_count <= capacity)` constraints.
- **Designed a clean layered architecture (`Controller -> Service -> Repository -> Entity`)** with DTO abstraction, decoupling JPA persistence from presentation payloads and neutralizing mass-assignment security vulnerabilities.
- **Optimized query performance for real-time NGO discovery** using Spring Data **JPA Specifications** with correlated subqueries and composite B-tree indexing on `(organization_id, slot_date)`.
- **Integrated transactional domain workflows** encompassing institutional verification, automated slot capacity recycling on cancellation (12-hour cutoff window), and feature-toggled Spring Mail notifications.

---

## 3. Technology Keywords for ATS Optimization

`Java 17` • `Spring Boot 3` • `Spring Security 6` • `Spring Data JPA` • `Hibernate 6` • `MySQL 8` • `Thymeleaf` • `Pessimistic Locking (SELECT FOR UPDATE)` • `Concurrency Control` • `JPA Specifications` • `Jakarta Validation` • `BCrypt` • `CSRF Protection` • `REST API` • `HTML5 / CSS3` • `Vanilla JavaScript` • `JUnit 5` • `Mockito` • `MockMvc` • `@DataJpaTest` • `Maven`

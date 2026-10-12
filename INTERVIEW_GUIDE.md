# AnnaSetu &mdash; Technical Interview Guide & Architectural Deep Dive

This comprehensive guide is designed to help you explain every technical design decision, architectural trade-off, and implementation detail of **AnnaSetu** during senior Java Full Stack engineering interviews.

---

## 1. The 60-Second Elevator Pitch

> *"AnnaSetu is an occasion-based food donation slot booking platform built with Spring Boot 3, Spring Security, Spring Data JPA, and MySQL 8, utilizing server-side rendering with Thymeleaf and handwritten, framework-free CSS3 and vanilla JS.*  
>  
> *In India, people frequently want to sponsor meals at orphanages and elder care facilities on birthdays or anniversaries, but face two core challenges: discoverability of verified institutions and operational unpredictability for the NGOs. AnnaSetu bridges this gap by allowing NGOs to publish discrete breakfast, lunch, and dinner availability slots with configurable capacities.*  
>  
> *From an engineering perspective, the system solves the critical double-booking challenge under high concurrency using database-level pessimistic write locking (`SELECT ... FOR UPDATE`), enforces strict Role-Based Access Control across Donors, Organizations, and Administrators, and guarantees data integrity through database constraints and Jakarta Bean Validation without a single third-party CSS or JS dependency."*

---

## 2. The 3-Minute Architectural Deep Dive

> *"When designing AnnaSetu, our primary architectural objectives were **data consistency**, **operational predictability**, and **zero external bloat**.*
>
> *First, for the domain model, we separated the authentication identity (`User`) from the domain personas (`DonorProfile` and `OrganizationProfile`) in a 1-to-1 normalized relationship. This prevents sparse tables and ensures sensitive security credentials are not intermingled with NGO operational details. For meal scheduling, we introduced the `SlotAvailability` entity, indexed on `(organization_id, slot_date)` with a composite unique key across `(organization_id, slot_date, meal_slot)` and a database `CHECK (booked_count <= capacity)` constraint.*
>
> *Second, for concurrency control, during peak festival seasons multiple donors often attempt to sponsor the same lunch or dinner slot simultaneously. Instead of relying on optimistic locking with `@Version`—which fails fast and frustrates users with retry exceptions—we selected **pessimistic write locking** (`LockModeType.PESSIMISTIC_WRITE`). When a donor submits a booking request, Hibernate issues a `SELECT ... FOR UPDATE` row lock on the specific slot. This serializes concurrent booking transactions at the MySQL InnoDB engine level, verifies capacity, increments `booked_count`, and commits cleanly. We proved this architecture with a multithreaded test using `ExecutorService` and `CountDownLatch`, confirming zero overbookings across parallel threads.*
>
> *Third, on the web and presentation tier, we opted for Server-Side Rendering (SSR) via Thymeleaf combined with a customized Spring Security filter chain. The frontend is built strictly with hand-written modern CSS3 variables, flexbox, grid, and vanilla JS Fetch API—avoiding the overhead of React, Node build steps, Bootstrap, or Tailwind. For security, we implemented form login with BCrypt, session fixation protection (`changeSessionId()`), strict CSRF token validation on all state-changing endpoints, and custom 403/404 error handlers that never expose raw stack traces.*
>
> *Finally, our operational workflows feature administrative organization vetting with audit rejection reasons, donor cancellation with a 12-hour automated cutoff policy that recycles slot capacity, grouped calendar agendas for NGO kitchen coordinators, and print-friendly donation receipts powered by CSS `@media print`."*

---

## 3. Design Decisions & Trade-Offs

### 1. Why Thymeleaf Server-Side Rendering (SSR) over React / Angular / Vue SPA?
- **Trade-off Evaluation**:
  - **Advantages**:
    1. **Elimination of Build & Tooling Complexity**: No Node.js runtime, npm dependencies, Webpack/Vite bundlers, or hydration mismatches.
    2. **Built-in Security Model**: Native Spring Security integration. Thymeleaf automatically embeds hidden CSRF tokens into forms generated with `th:action`, and automatically HTML-escapes all dynamic content (`th:text`) by default, neutralizing XSS vulnerabilities.
    3. **SEO & First Contentful Paint (FCP)**: The complete HTML document is rendered on the server in a single round-trip, delivering fast paint times on low-bandwidth mobile networks across tier-2 and tier-3 Indian cities.
    4. **Simpler Mental Model**: Model-View-Controller (MVC) keeps domain logic and presentation templates in a unified deployment artifact (a single executable JAR).
  - **Disadvantages**: Full page refreshes for complex state transitions. We mitigated this by strategically adding lightweight vanilla JS `fetch()` calls for dynamic date-slot availability checking.

### 2. Why a Discrete `capacity` and `bookedCount` Column over Dynamic Subqueries?
- **Trade-off Evaluation**:
  - **The Alternative**: Calculating availability dynamically using `COUNT(b.id) FROM bookings b WHERE b.slot_id = slot.id AND b.status IN ('PENDING', 'CONFIRMED')`.
  - **Why We Rejected Dynamic Calculation**:
    1. **Search Performance**: In public discovery queries, hundreds of slots across multiple cities must be filtered. Dynamic subqueries or correlated joins on every search request degrade performance to $O(N)$ with large booking histories.
    2. **Support for Shared Meals**: Some NGOs allow two donors to co-sponsor a large feast (e.g., 100+ beneficiaries). Maintaining a dedicated `capacity` column (1 = Exclusive, 2 = Shared) enables flexible business configuration per meal.
    3. **Database-Level Invariant**: A dedicated `booked_count` column allows MySQL 8 to enforce an InnoDB `CHECK (booked_count <= capacity)` constraint directly at the storage engine level, providing a defensive backstop against race conditions.

### 3. Why Pessimistic Locking over Optimistic Locking?
- **Trade-off Evaluation**:
  - **Optimistic Locking (`@Version`)**:
    - Relies on application-level version checking: `UPDATE slot SET version = 2 WHERE id = 1 AND version = 1`.
    - If two donors click "Book" at the same moment, the second donor's transaction fails with `OptimisticLockException`. The application must either show a frustrating error or implement complex retry mechanisms.
  - **Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)**:
    - Translates to SQL `SELECT ... FOR UPDATE`, holding an exclusive row-level lock in MySQL InnoDB for the duration of the transaction.
    - If capacity is 2 and 2 donors book simultaneously, the second transaction waits a few milliseconds, acquires the lock, sees `booked_count = 1 < 2`, increments it to 2, and succeeds. Both donors get booked cleanly without failure.
    - When capacity is truly exhausted, subsequent requests fail immediately with a clear, user-friendly `SlotFullException`.

### 4. Why a Strict Layered Architecture over Screaming / Hexagonal Architecture?
- **Trade-off Evaluation**:
  - A strict **`Controller -> Service -> Repository -> Entity`** structure is the industry standard for enterprise Spring Boot applications.
  - It establishes unequivocal separation of concerns:
    - **Controllers** handle HTTP protocols, validation binding, and view routing.
    - **Services** delineate `@Transactional` boundaries, domain logic, and authorization enforcement.
    - **Repositories** abstract Spring Data JPA queries and Spring Data specifications.
    - **Entities** represent relational state and database constraints.
  - Form-backing DTOs insulate JPA entities from HTTP parameter pollution (mass-assignment vulnerabilities).

---

## 4. Planned Future Improvements

If scaling AnnaSetu to millions of donors across all Indian states, the technical roadmap includes:
1. **Redis Distributed Caching**: Cache approved organization profiles and distinct city lists using Spring Cache (`@Cacheable`) with Redis to offload read traffic from MySQL.
2. **Asynchronous Messaging with Apache Kafka / RabbitMQ**: Decouple email and WhatsApp notifications from the main booking transaction using an event-driven architecture (`@Async` / Event Bus).
3. **Payment Gateway Integration**: Integrate Razorpay / UPI for monetary meal donations (e.g., fixed ₹3,000 sponsorship fee), utilizing webhook listeners with idempotency keys.
4. **Elasticsearch Geospatial Search**: Replace SQL `LIKE` queries with Elasticsearch geo-distance queries (`geo_point`), allowing donors to find shelters within a 5 km radius of their current GPS location.
5. **Spring Cloud Gateway & Microservices Separation**: Decompose into discrete services (`auth-service`, `booking-service`, `notification-service`) if development teams scale beyond 20+ engineers.

---

## 5. 30 Technical Interview Questions & Model Answers

### Category 1: Spring MVC & Architecture

#### Q1: Walk me through the complete lifecycle of an HTTP request in Spring MVC from browser to database and back.
> **Answer**:  
> 1. The client sends an HTTP request (e.g., `POST /donor/bookings`).  
> 2. The request hits Tomcat's servlet container and traverses the **Spring Security Filter Chain** (SecurityContextHolder, CSRF validation, AuthorizationFilter).  
> 3. The request enters Spring MVC's **`DispatcherServlet`** (the Front Controller).  
> 4. `DispatcherServlet` queries `HandlerMapping` to resolve the matching controller method (`DonorController.submitBooking`).  
> 5. `HandlerAdapter` coordinates argument resolution: executes data binding, parses URL paths, and triggers Jakarta Bean Validation (`@Valid`).  
> 6. The controller invokes the service layer (`BookingService.book()`) within a managed transaction boundary (`@Transactional`).  
> 7. The service layer interacts with Spring Data JPA repositories, which use Hibernate to translate operations into SQL and query MySQL 8 over HikariCP.  
> 8. The service returns domain entities/DTOs to the controller.  
> 9. The controller populates Spring's `Model` and returns a view name (`"donor/dashboard"` or a redirect `"redirect:/donor/dashboard"`).  
> 10. `ViewResolver` resolves the Thymeleaf template, renders the HTML document server-side, and writes the response stream back through `DispatcherServlet` to the client.

#### Q2: What is the Post/Redirect/Get (PRG) pattern and where is it used in AnnaSetu?
> **Answer**:  
> The PRG pattern is a web development design pattern that prevents duplicate form submissions when a user refreshes their browser. In AnnaSetu, all state-changing endpoints (booking a slot, cancelling, approving an organization, updating slots) return a redirect string (e.g., `return "redirect:/donor/dashboard";`) instead of rendering a view directly. We flash feedback messages into `RedirectAttributes.addFlashAttribute()`, which Spring stores temporarily in the HTTP session and clears immediately after the next GET request renders.

#### Q3: How do you prevent Mass Assignment / Parameter Pollution in Spring MVC?
> **Answer**:  
> Never expose JPA entities directly as `@ModelAttribute` or `@RequestBody` controller parameters. In AnnaSetu, we use dedicated form-backing DTOs (e.g., `DonorRegistrationDto`, `SlotCreationDto`, `BookingRequestDto`). The DTO only contains fields the user is permitted to supply. The service layer then maps validated DTO properties to managed entity attributes, ensuring sensitive columns like `id`, `approval_status`, `password_hash`, and `role` cannot be overwritten by malicious HTTP request payloads.

#### Q4: What is the purpose of `@ControllerAdvice` and how does AnnaSetu use it?
> **Answer**:  
> `@ControllerAdvice` provides central, cross-cutting exception interception across all `@Controller` and `@RestController` components. In AnnaSetu's `GlobalExceptionHandler`, we catch domain exceptions (`SlotFullException`, `DuplicateBookingException`, `CancellationCutoffException`). It inspects the `Accept` header: if the client is making an AJAX/JSON fetch call, it responds with clean JSON error payloads (`HttpStatus.CONFLICT`, `HttpStatus.BAD_REQUEST`); if it is a traditional browser form submission, it flashes user-friendly error banners and redirects back safely without ever exposing internal stack traces.

---

### Category 2: Thymeleaf & Frontend

#### Q5: How does Thymeleaf provide defense against Cross-Site Scripting (XSS)?
> **Answer**:  
> Thymeleaf's `th:text` attribute automatically HTML-escapes all strings before injecting them into the DOM. For instance, if an attacker attempts to set an occasion or food description to `<script>alert('xss')</script>`, Thymeleaf encodes the characters into `&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;`, which the browser renders as harmless plain text. Thymeleaf only renders unescaped HTML when the developer explicitly uses `th:utext`, which is strictly avoided in AnnaSetu.

#### Q6: How does layout fragmentation work in Thymeleaf without external template engines?
> **Answer**:  
> We use Thymeleaf layout fragments with `th:fragment` and `th:replace`. In `src/main/resources/templates/fragments/layout.html`, we define reusable structural components: `html-head(title)`, `navbar`, `footer`, and `alerts`. Individual views (such as `home.html` or `donor/dashboard.html`) include these fragments using `th:replace="~{fragments/layout :: navbar}"`, ensuring UI consistency, centralized navigation logic, and DRY (Don't Repeat Yourself) design.

#### Q7: Why did you choose pure handwritten CSS over Bootstrap or Tailwind CSS?
> **Answer**:  
> Modern CSS3 provides native capabilities that previously required heavy frameworks: CSS Custom Properties (CSS variables) for design systems, CSS Flexbox and Grid for responsive layouts, and `@media` queries for mobile-first breakpoints. By writing custom CSS, AnnaSetu achieved a lightweight 30 KB stylesheet with zero external HTTP requests, zero JavaScript framework overhead, complete design uniqueness tailored to an Indian social platform, and full control over performance, accessibility, and `@media print` rules.

#### Q8: How did you implement print-friendly receipts without generating server-side PDFs?
> **Answer**:  
> We utilized native CSS `@media print` stylesheets. When the browser enters print preview (via `Ctrl+P` or JavaScript `window.print()`), the `@media print` rules hide all navigational chrome, buttons, headers, footers, and alert banners using `display: none !important`. The receipt card is styled with high-contrast monochrome typography, explicit border styling, page-break safeguards (`page-break-inside: avoid`), and a verification stamp block, allowing donors to print or "Save as PDF" instantly using their native browser engine.

---

### Category 3: Spring Data JPA & Hibernate

#### Q9: What is the N+1 select query problem in Hibernate, and how did you resolve it in AnnaSetu?
> **Answer**:  
> The N+1 query problem occurs when Hibernate executes 1 query to fetch a parent entity list and subsequently issues N individual queries to fetch lazy-loaded child relationships. For example, loading 15 bookings on the admin dashboard would trigger 1 query for bookings, 15 queries for slots, 15 for organizations, and 15 for donors (46 SQL queries total). In AnnaSetu's `BookingRepository`, we solved this by writing a custom JPQL query using `JOIN FETCH`:
> ```sql
> SELECT b FROM Booking b 
> JOIN FETCH b.slot s 
> JOIN FETCH s.organization o 
> JOIN FETCH b.donor d 
> ORDER BY b.createdAt DESC
> ```
> This executes a single SQL JOIN query, hydrating the entire object graph in one database round-trip.

#### Q10: Why did you set `spring.jpa.hibernate.ddl-auto=validate` instead of `update`?
> **Answer**:  
> In enterprise production environments, `ddl-auto=update` is dangerous: it can alter columns unintentionally, cannot drop obsolete tables, does not reliably recreate changed foreign keys, and can mask discrepancies between staging and production. Using `ddl-auto=validate` forces the development workflow to treat SQL schemas (`schema.sql`) as the single source of truth. At boot time, Hibernate strictly checks that Java entity annotations match MySQL physical tables, failing fast if any discrepancy exists.

#### Q11: How does Spring Data JPA Specifications work in AnnaSetu's search feature?
> **Answer**:  
> Spring Data JPA `Specification<T>` allows constructing dynamic, type-safe database queries at runtime using the JPA Criteria API. In `OrganizationSpecification`, we build dynamic predicates based on provided search filters (city, area, organization type, food preference). For availability filtering, we construct a correlated subquery checking that an active slot exists matching the requested date and meal slot where `s.bookedCount < s.capacity`. If a search filter is left null, that predicate is omitted cleanly without string concatenation or SQL injection risks.

#### Q12: What is the difference between `FetchType.LAZY` and `FetchType.EAGER`?
> **Answer**:  
> `FetchType.EAGER` tells Hibernate to fetch the related entity immediately whenever the parent is loaded, typically via a SQL join. `FetchType.LAZY` instructs Hibernate to proxy the relationship and load it only when the getter method is explicitly called. In AnnaSetu, all `@ManyToOne` and `@OneToOne` associations default to `LAZY` to prevent massive unintended table joins and memory bloat, using explicit `JOIN FETCH` only when the related data is genuinely required by the presentation layer.

#### Q13: What is the Open Session in View (OSIV) pattern, and what are its trade-offs?
> **Answer**:  
> Open Session in View keeps the Hibernate `EntityManager` / Session open during the entire HTTP request lifecycle, including during Thymeleaf view rendering. This allows views to navigate lazy relationships without throwing `LazyInitializationException`. However, the trade-off is that database connections from the connection pool (HikariCP) are held longer, which can saturate connection pools under high traffic. In high-throughput architectures, OSIV is often disabled, requiring services to return fully initialized DTOs.

---

### Category 4: Transactions & Concurrency Control

#### Q14: How does `@Transactional` work under the hood in Spring?
> **Answer**:  
> Spring uses **AOP (Aspect-Oriented Programming) dynamic proxies**. When a bean with `@Transactional` is injected, Spring wraps it in a proxy (via CGLIB or JDK dynamic proxies). When a transactional method is called, the proxy intercepts the call, obtains a connection from `DataSourceTransactionManager`, sets `autoCommit = false`, executes the target method within a transaction context (`TransactionSynchronizationManager`), and commits upon successful completion. If an unchecked exception (`RuntimeException` or `Error`) is thrown, it rolls back automatically.

#### Q15: Why will `@Transactional` fail if you call a transactional method from within the same class?
> **Answer**:  
> Because Spring's declarative transaction management relies on AOP proxies. When calling a method from within the same class (`this.methodB()`), the call bypasses the Spring proxy and directly invokes the internal method on the target object. The transactional interceptor is never triggered. To fix this, you must either move the method to a separate service bean or inject the self-proxy bean.

#### Q16: Explain Pessimistic Write Locking (`LockModeType.PESSIMISTIC_WRITE`) vs Optimistic Locking (`@Version`).
> **Answer**:  
> - **Optimistic Locking**: Assumes conflicts are rare. Adds a `version` column. Hibernate checks `WHERE version = current_version` on update. If another transaction changed the row, it throws `OptimisticLockException`.  
> - **Pessimistic Locking**: Assumes conflicts are frequent. Uses database-level locks. Hibernate executes `SELECT ... FOR UPDATE`. Other transactions trying to read/write the same row with exclusive locks are blocked until the first transaction commits or rolls back.  
> In AnnaSetu, pessimistic write locking was chosen for meal booking because under peak donation periods (e.g., Diwali or New Year's Eve), multiple donors compete for the exact same meal slot. Pessimistic locking serializes the request queue cleanly at the database level, preventing false-rejection race conditions.

#### Q17: What transaction isolation level does MySQL InnoDB use by default, and how does it prevent phantom reads?
> **Answer**:  
> MySQL InnoDB uses **`REPEATABLE READ`** by default. InnoDB prevents non-repeatable reads via MVCC (Multi-Version Concurrency Control) through read undo logs. It prevents phantom reads during locking reads (`SELECT ... FOR UPDATE`) using **Next-Key Locking** (a combination of record locks and gap locks that locks the index record and the gap before it), preventing concurrent transactions from inserting new rows into the locked gap.

#### Q18: What is the purpose of `@Transactional(readOnly = true)`?
> **Answer**:  
> Marking a transaction as `readOnly = true` provides optimizations across both Hibernate and the JDBC driver:  
> 1. Hibernate switches FlushMode to `MANUAL`, disabling dirty checking snapshots and reducing heap memory consumption.  
> 2. The JDBC driver and database engine can route read-only transactions to read replicas in a primary-replica cluster, preventing unnecessary read locks on the primary database.

---

### Category 5: Spring Security & Authentication

#### Q19: Explain the Spring Security Filter Chain architecture.
> **Answer**:  
> Incoming HTTP requests are intercepted by `DelegatingFilterProxy`, which delegates to `FilterChainProxy` containing the `SecurityFilterChain`. The request passes sequentially through filters:  
> - `CsrfFilter`: Validates anti-forgery tokens for non-safe HTTP methods.  
> - `LogoutFilter`: Detects `/logout` requests and clears sessions/cookies.  
> - `UsernamePasswordAuthenticationFilter`: Extracts credentials from `/login` POST requests and delegates to `AuthenticationManager`.  
> - `RememberMeAuthenticationFilter`: Auto-logs in users via persistent tokens if no session exists.  
> - `ExceptionTranslationFilter`: Catches security exceptions, redirecting unauthenticated users to `/login` or forwarding unauthorized users to the `AccessDeniedHandler`.  
> - `AuthorizationFilter`: Verifies that the authenticated principal possesses the required authorities (e.g., `hasRole('ADMIN')`) for the target URL.

#### Q20: How does BCrypt work and why is it superior to SHA-256 or MD5 for password storage?
> **Answer**:  
> BCrypt is a key-derivation function based on the Blowfish cipher that incorporates:  
> 1. **Built-in Salt**: Generates a random 16-byte salt embedded into the hash output, preventing precomputed Rainbow Table attacks.  
> 2. **Tunable Work Factor (Cost Factor)**: BCrypt uses an exponential cost parameter (e.g., $2^{10} = 1024$ iterations). As hardware speed increases over time, the work factor can be increased to ensure password hashing remains computationally expensive, neutralizing brute-force and GPU-based dictionary attacks. Fast hashes like SHA-256 or MD5 are designed for high throughput and are trivially vulnerable to GPU cracking.

#### Q21: What is Session Fixation and how does AnnaSetu mitigate it?
> **Answer**:  
> Session Fixation is an attack where an adversary obtains a valid session identifier (e.g., by sending a phishing link with a known session ID) and waits for the victim to authenticate using that session. If the application retains the same session ID after login, the attacker gains authenticated access. In AnnaSetu's `SecurityConfig`, we configure:
> ```java
> http.sessionManagement(session -> session.sessionFixation().changeSessionId());
> ```
> Upon successful authentication, Spring Security invalidates the old session ID and issues a brand-new ID while preserving user session attributes, rendering the attacker's original session ID useless.

#### Q22: How does Cross-Site Request Forgery (CSRF) protection work in Spring Security?
> **Answer**:  
> In CSRF attacks, a malicious website tricks a victim's browser into executing an unauthorized state-changing request (such as booking or cancelling a slot) to a site where the user is currently authenticated via cookies. Spring Security uses the **Synchronizer Token Pattern**. A unique, cryptographically random CSRF token is associated with the user's session. Any state-changing HTTP request (`POST`, `PUT`, `DELETE`) must submit this token in a header or form field. Because cross-origin scripts cannot read this token due to the Same-Origin Policy, the forged request is rejected with HTTP 403 Forbidden.

#### Q23: How does AnnaSetu handle custom post-login redirects based on user roles?
> **Answer**:  
> We implemented a `CustomAuthenticationSuccessHandler` implementing `AuthenticationSuccessHandler`. After successful credential verification, the handler inspects the user's `GrantedAuthority` collection:  
> - If `ROLE_ADMIN`, redirects to `/admin/dashboard`.  
> - If `ROLE_ORGANIZATION`, redirects to `/org/dashboard`.  
> - If `ROLE_DONOR`, redirects to `/donor/dashboard`.  
> If an organization account has `approvalStatus == PENDING`, the handler directs them to a dedicated waiting status banner on their dashboard.

---

### Category 6: Validation & Error Handling

#### Q24: What is the difference between `@NotNull`, `@NotEmpty`, and `@NotBlank` in Jakarta Bean Validation?
> **Answer**:  
> - `@NotNull`: The value cannot be `null`, but can be an empty string `""` or empty collection `[]`.  
> - `@NotEmpty`: The value cannot be `null` and its length or collection size must be greater than 0 (e.g., `""` fails, but `"   "` with spaces passes).  
> - `@NotBlank`: Applies exclusively to character sequences (`CharSequence`). The value cannot be `null`, and trimmed length must be greater than 0 (e.g., `"   "` fails). Used for names, emails, and street addresses in AnnaSetu.

#### Q25: How do field validation errors propagate from Spring MVC to Thymeleaf?
> **Answer**:  
> When a form is submitted to a controller method annotated with `@Valid @ModelAttribute("dto") DTO dto, BindingResult bindingResult`:  
> 1. If constraint violations occur, Spring populates `BindingResult`.  
> 2. The controller checks `bindingResult.hasErrors()`. If true, it returns the form template view immediately without calling the service layer.  
> 3. In the Thymeleaf view, `th:errors="*{fieldName}"` dynamically inspects the bound `BindingResult` and displays localized error strings adjacent to the offending form control, while applying CSS error classes (`is-invalid`).

#### Q26: How do you ensure that internal server errors (500) never leak stack traces to attackers?
> **Answer**:  
> 1. In `application.properties`, we explicitly configure:  
>    `server.error.include-stacktrace=never`  
>    `server.error.include-message=never`  
>    `server.error.whitelabel.enabled=false`  
> 2. In `GlobalExceptionHandler`, we provide a fallback `@ExceptionHandler(Exception.class)` that logs the complete stack trace and contextual error logs privately to application logs via SLF4J, while returning a sanitized user-friendly view (`error/500`) with HTTP 500 status code.

---

### Category 7: Testing & Quality Assurance

#### Q27: What is the difference between `@SpringBootTest` and `@DataJpaTest`?
> **Answer**:  
> - **`@SpringBootTest`**: Bootstraps the full Spring `ApplicationContext`, including web layers, controllers, filters, services, and security configurations. It provides realistic end-to-end integration testing, but has higher startup overhead.  
> - **`@DataJpaTest`**: A specialized test slice annotation. It disables full auto-configuration and configures only JPA components (Entities, Spring Data Repositories, EntityManager, DataSource). In AnnaSetu, using `@AutoConfigureTestDatabase(replace = Replace.NONE)` allows testing complex custom JPQL queries and database constraints against the real MySQL engine with fast execution times and automatic per-test transaction rollback.

#### Q28: How did you test multithreaded concurrency in AnnaSetu?
> **Answer**:  
> In `BookingConcurrencyTest`, we simulated a high-contention flash crowd scenario:  
> 1. Created a slot with `capacity = 2` and `bookedCount = 0`.  
> 2. Initialized 10 separate donor profile accounts.  
> 3. Used an `ExecutorService` with 10 parallel threads, coordinated by a `CountDownLatch(1)` so all 10 worker threads unleashed their `bookingService.book()` calls simultaneously.  
> 4. Used a second `CountDownLatch(10)` to wait for all threads to terminate.  
> 5. Asserted that exactly 2 bookings succeeded, 8 failed with `SlotFullException`, and `bookedCount` remained 2, verifying our pessimistic write lock.

#### Q29: How do you mock Spring Security authentication in controller unit tests?
> **Answer**:  
> Using Spring Security Test's `@WithMockUser(username = "...", roles = {"DONOR"})` annotation. This synthesizes a security context directly in the `SecurityContextHolder` before the `MockMvc` request executes, allowing precise verification that endpoints restrict access (returning HTTP 403 Forbidden) when an unauthorized persona attempts to access a protected dashboard.

#### Q30: How do you test email notifications without sending real emails or failing builds in offline CI?
> **Answer**:  
> We implemented a dual-layer strategy:  
> 1. Feature toggle property `annasetu.mail.enabled=false` by default. When disabled, `EmailServiceImpl` simulates email sending by logging recipient, subject, and body snippet to application logs.  
> 2. We inject `ObjectProvider<JavaMailSender>` into `EmailServiceImpl`. If the Spring Mail bean is absent or unconfigured, the service handles it without throwing `NoSuchBeanDefinitionException`.  
> 3. In unit tests (`BookingServiceTest`), the `NotificationService` interface is mocked with Mockito (`@Mock`), ensuring tests run in milliseconds without external network dependencies.

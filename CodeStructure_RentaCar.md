# Code Structure — RentaCar

This document walks through the repository folder by folder and explains *why*
it's organized this way, not just what's in it. It's meant as a companion to
[SystemArchitecture_RentaCar.md](SystemArchitecture_RentaCar.md) (which covers
the architectural layers conceptually) — this one maps that architecture onto
the actual files on disk.

## Top-level split: `backend/` and `frontend/`

```
SWE/
├── backend/    Spring Boot REST API (Java 21, Maven)
└── frontend/   React + TypeScript SPA (Vite)
```

The project is two independently deployable applications talking over HTTP,
not one monolith — the backend has no idea a browser exists, the frontend has
no idea Java exists. This was a deliberate call (documented in
[SystemArchitecture_RentaCar.md](SystemArchitecture_RentaCar.md), which also
records and rejects a "two-WAR" alternative) rather than a server-rendered
Spring MVC app with Thymeleaf templates: it lets each half use the tool suited
to it (React for a stateful, role-branching UI; Spring for the REST/security/
persistence stack the course requires) and keeps the API reusable if a second
client (e.g. a mobile app) ever needed it. The cost is running two servers
locally instead of one — worth it for the separation of concerns.

---

## Backend — `backend/src/main/java/com/rentacar/`

The backend follows a conventional **layered architecture**: a request enters
through a controller, gets delegated to a service that holds the business
rules, and the service talks to a repository for persistence. Each package
below *is* one of those layers (or a cross-cutting concern that supports
them), and the boundary between packages is also the boundary of who's
allowed to depend on what — controllers depend on services, services depend
on repositories, and nothing is allowed to skip a layer (a controller never
touches a repository directly, for instance).

```
com/rentacar/
├── RentaCarApplication.java   Spring Boot entry point
├── controller/                HTTP layer — one class per resource
├── service/                   Business logic — one class per subsystem
├── repository/                Persistence layer — Spring Data JPA interfaces
├── entity/                    JPA-mapped domain objects (the DB schema, in Java)
├── dto/                       Request/response shapes exposed over the API
├── security/                  JWT issuing/validation, auth filter, user details
├── config/                    Spring configuration beans + dev data seeding
└── exception/                 Custom exceptions + centralized error handling
```

### `controller/` — the API surface

```
AuthController, VehicleController, CategoryController, ReservationController,
CheckOutController, CheckInController, BillingController, CustomerController,
StaffAccountController, DashboardController
```

One controller per REST resource, matching the API table in the main
[README](README.md#api-endpoints). `@RestController` classes are kept
deliberately thin: they parse the request, call exactly one service method,
and map the result to an HTTP response. No business logic lives here — that's
what makes the logic testable without spinning up HTTP at all (see the
`service/*Test.java` files, which unit-test the rules directly).

Two controllers exist that don't fit a CRUD-resource pattern —
`CheckOutController` and `CheckInController` — because check-out and check-in
are *state-transition operations* on a reservation/vehicle pair, not resource
management, and each maps to its own use case (UC7, UC8) in the SRS. Keeping
them as separate single-purpose controllers rather than bolting
`POST /reservations/{id}/checkout` onto `ReservationController` keeps that
controller from becoming a dumping ground for every reservation-adjacent
action, and mirrors the front-desk operations being their own subsystem in
the Vision document.

### `service/` — where the business rules actually live

```
AuthService, VehicleService, CategoryService, ReservationService,
RecommendationService, BillingService, CustomerService, StaffAccountService,
DashboardService, EmailService
```

This is the layer that enforces the rules a grader would ask about in a
presentation: role permissions, double-booking checks, "can't check in a
vehicle that isn't RENTED," bill calculation, etc. Services are plain
Spring-managed POJOs (no HTTP or JSON concerns) so they can be unit-tested in
isolation — every service has a matching `*ServiceTest.java`.

A couple of things worth explaining if asked:

- **`RecommendationService` is separate from `ReservationService`.** The
  "AI-assisted" recommendation (filter by seat count/budget, sort by price)
  is conceptually a *query* — it doesn't create or change anything — while
  `ReservationService` owns the actual booking lifecycle (create, cancel,
  availability search). Splitting them means the recommendation logic can be
  swapped for something more sophisticated later (see Known
  Limitations/Future Improvements in the README) without touching booking
  code, and keeps `ReservationService` from also owning a completely
  different responsibility (ranking) alongside state changes.
- **`BillingService` is never called from a controller directly.** It's
  invoked internally by `CheckInController` → the check-in flow, because
  billing only ever happens as a side effect of a return (UC8 `<<include>>`s
  UC9 in the SRS use-case model). There's a `BillingController`, but it only
  exposes `GET /bills/reservation/{id}` — read-only, for looking a bill up
  after the fact.
- **`EmailService` is its own service** rather than logic inlined into
  `AuthService`, because sending mail is an infrastructure concern (SMTP,
  templating) fully separate from the *decision* to send an email
  (password-reset request logic lives in `AuthService`; `EmailService` just
  knows how to send one).

### `repository/` — persistence, and nothing else

```
UserRepository, VehicleRepository, CategoryRepository, ReservationRepository,
BillRepository, PasswordResetTokenRepository
```

Spring Data JPA interfaces — no implementation code, just method signatures
(`findByEmail`, `findOverlappingReservations`, etc.) that Spring generates
queries for at startup. One repository per aggregate root in `entity/`. This
layer exists specifically so that swapping the underlying query mechanism
(e.g., adding a `@Query` with native SQL for a reporting feature) never
requires touching a service — the service only knows "ask the repository for
X," not how X is fetched.

### `entity/` — the domain model / DB schema

```
User, Role, Vehicle, VehicleStatus, Category, Reservation, ReservationStatus,
Bill, PasswordResetToken
```

These are the JPA `@Entity` classes — Hibernate generates the actual MySQL/H2
schema from them (`ddl-auto=update`), so this package *is* the database
design, expressed in Java rather than a separate `.sql` file. `Role`,
`VehicleStatus`, and `ReservationStatus` are enums, not full entities — they
model fixed sets of states (ADMIN/STAFF/CUSTOMER; AVAILABLE/RENTED/etc.;
PENDING/ACTIVE/COMPLETED/CANCELLED) referenced by the entities that need them,
which is why they sit in the same package as the entities rather than in
`dto/common` — they're persisted, not just transmitted.

### `dto/` — what actually crosses the API boundary

```
dto/
├── auth/        LoginRequest, RegisterRequest, JwtResponse, ...
├── vehicle/      VehicleRequest, VehicleResponse, ...
├── category/
├── reservation/
├── staff/
├── customer/
├── billing/
├── dashboard/
└── common/       Shared shapes (e.g. a generic error/paged response)
```

DTOs exist so that **entities never get serialized directly to JSON.** That
matters for two concrete reasons in this codebase: (1) `User` has a password
hash on it — if `User` itself were the API response, that hash would be one
`@JsonIgnore` mistake away from leaking; going through a DTO means the field
simply doesn't exist on the object being serialized, so there's no such
mistake to make; (2) JPA entities carry lazy-loaded relationships (e.g. a
`Reservation`'s `Vehicle`) that can trigger extra queries or
`LazyInitializationException`s if Jackson tries to walk them — DTOs are flat,
so serialization is predictable.

The subfolders mirror the `controller`/`service` split by subsystem rather
than being one flat `dto/` package, because at 22+ DTO classes a flat folder
would make it hard to find "the vehicle-related DTOs" versus "the
reservation-related ones" — grouping by resource keeps a controller's request/
response types physically next to each other.

### `security/`

```
JwtService                  issue + validate JWTs
JwtAuthFilter                intercepts every request, reads the Bearer token,
                             populates Spring Security's context
CustomUserDetailsService     loads a User (by email) for Spring Security
UserPrincipal                adapts our User entity to Spring Security's
                             UserDetails interface
```

Kept separate from `config/` even though both are security-adjacent:
`config/SecurityConfig` is *declarative* (which endpoints require which
role — the security policy), while everything in `security/` is the
*mechanism* that policy runs on (how a token becomes an authenticated
principal). Separating policy from mechanism means the endpoint-by-role rules
in `SecurityConfig` can be read and audited without wading through JWT
parsing code.

### `config/`

```
SecurityConfig    Spring Security filter chain + role-based endpoint rules
CorsConfig        Allowed origins (env-driven — see README Configuration table)
DataSeeder        Seeds the 3 default accounts + sample vehicles/categories
                  on an empty database (dev convenience, see README)
```

`DataSeeder` lives here rather than in `entity/` or a test folder because it's
a `@Component`/`CommandLineRunner` that participates in application startup —
it's infrastructure wiring, not domain logic, and only runs when the DB is
empty (so it's a no-op in prod once real data exists).

### `exception/`

```
ApiExceptionHandler          @RestControllerAdvice — the single place that
                             turns exceptions into HTTP error responses
ResourceNotFoundException    → 404
DuplicateResourceException   → 409
InvalidStateException        → 400 (e.g. checking in a vehicle that isn't RENTED)
```

Centralizing error handling in one `@RestControllerAdvice` means individual
controllers never write `try/catch` + manual `ResponseEntity.status(...)`
boilerplate — a service just throws a meaningful exception
(`throw new ResourceNotFoundException("Vehicle not found")`), and the
advice class is the only place that knows "this exception type = this HTTP
status." Adding a new error case anywhere in the app is a one-line throw, not
a change in N controllers.

### `resources/`

```
application.properties         Shared defaults, all env-var driven
application-dev.properties      H2, dev SMTP sandbox — active by default
application-prod.properties     MySQL, real mail — active via SPRING_PROFILES_ACTIVE=prod
```

Split by Spring profile rather than one file with conditionals, so the
difference between "what runs on my laptop" and "what runs in production" is
a complete, readable file each, not scattered `@Profile`-guarded properties.
This is also the file set audited for the "no secrets in the repo"
submission requirement — every credential-shaped value is `${ENV_VAR}` with
no literal fallback in the prod file (see README's Configuration section).

---

## Frontend — `frontend/src/`

```
src/
├── api/          One module per backend resource — all HTTP calls live here
├── pages/         One component per route, grouped by role
├── components/    Shared, reusable UI pieces
├── context/       App-wide React state (auth)
├── routes/        Route guarding
├── types/         Shared TypeScript types mirroring backend DTOs
├── styles/        CSS
└── assets/        Static assets (images, etc.)
```

### `api/`

```
client.ts       Axios instance: base URL, JWT attached to every request,
                 shared error handling
auth.ts, vehicles.ts, categories.ts, reservations.ts, customers.ts,
staffAccounts.ts, billing.ts, dashboard.ts
```

One file per backend resource, matching `controller/` on the backend
one-for-one. No component ever calls `axios` or `fetch` directly — it always
goes through one of these modules. That means the JWT-attachment logic and
base-URL config (`VITE_API_BASE_URL` / the dev proxy — see README) exist in
exactly one place (`client.ts`), and if an endpoint's shape changes, there's
exactly one file to update, not every page that happens to call it.

### `pages/`, grouped by role

```
pages/
├── auth/        Login, Register, ForgotPassword, ResetPassword
├── admin/       Dashboard, Vehicles, Categories, StaffAccounts
├── staff/       AllReservations, ReservationLookup, CheckOut, CheckIn
└── customer/    SearchVehicles, ReservationHistory, Profile
```

This mirrors the role-based access model that's central to the whole system
(admin/staff/customer are kept deliberately separate — see the README's
Roles & Permissions table). Grouping pages by role rather than by feature
means the folder structure itself documents "what can a staff member see,"
which lines up directly with `ProtectedRoute` and the backend's per-endpoint
role checks — the same three-way split shows up at the route layer, the page
layer, and the API layer, so there's one mental model to hold, not three.

### `components/`

```
Navbar, VehicleCard, ReservationTable, BillSummaryCard, StatusBadge,
ConfirmDialog
```

Only things reused across *multiple* pages/roles live here (e.g.
`ReservationTable` is used by both the staff "All Reservations" page and the
customer "Reservation History" page; `StatusBadge` renders a
vehicle/reservation status pill wherever one appears). Anything used by only
one page stays inline in that page's file — this project intentionally
avoids over-extracting single-use components, matching the "no premature
abstraction" preference in the README's known-limitations framing (there's no
component library or design system layer, since the UI surface didn't justify
one).

### `context/AuthContext.tsx`

Holds the logged-in user + JWT and exposes login/logout to the whole app via
React context, rather than prop-drilling auth state through every page.
This is the one piece of genuinely global state in the app — everything else
(a page's form state, a fetched list) is local to the page that needs it,
which is why there's only one file in `context/`.

### `routes/ProtectedRoute.tsx`

A single wrapper component that checks "is there a user, and does their role
match what this route requires" before rendering children, redirecting to
`/login` otherwise. Centralizing this in one component (rather than each page
checking `AuthContext` itself) means route protection can't be forgotten on a
new page — it's enforced at the routing layer, not something each page author
has to remember. Note this is a UX convenience only — the real enforcement is
server-side (Spring Security role checks), since a client-side check can
always be bypassed by a motivated user.

### `types/index.ts`

Shared TypeScript interfaces (`Vehicle`, `Reservation`, `Bill`, etc.) that
mirror the backend's `dto/` response shapes. Kept as a single file rather than
split per-resource (unlike `api/`) because these are pure data shape
declarations with no logic — a handful of interfaces don't need the same
per-module separation that active code (API calls, pages) benefits from.

---

## Why this structure, overall

The guiding principle end to end: **the same three or four groupings — by
subsystem/role, and by architectural layer — repeat at every level**, so
understanding one part of the app tells you how to navigate the rest. A
grader looking for "where's check-in handled" can predict
`CheckInController` → `CheckInPage.tsx` → `reservations.ts`'s check-in call
without being told, because controller/service/repository names, page
folders, and API modules all key off the same resource/subsystem vocabulary
defined back in the Vision document and SRS. Nothing here was added for
scale the project doesn't have (no microservices, no repository pattern
beyond what Spring Data already gives for free, no state-management library
beyond React context) — the structure matches the actual size and shape of
the problem being solved.

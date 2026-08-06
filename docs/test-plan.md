# Test Plan

This document describes how Easy Car Rent is verified for delivery. It records the
automated coverage that runs on every build, the connected checks that were run
against a live backend, and the manual visual checks that a reviewer must perform
locally before the application is presented. It contains no account passwords, no
`Authorization` values, and no real personal data.

## 1. Purpose

The purpose of this plan is to give a repeatable, honest picture of what is verified
and how:

- confirm the backend REST API enforces every business, validation, and
  authorization rule;
- confirm the desktop client mirrors those rules, communicates asynchronously, and
  presents errors safely;
- separate what is proven automatically from what still requires a human to look at
  the running application;
- describe a residue-free way to exercise the system by hand and clean up afterwards.

## 2. Test environments

| Item | Value |
| --- | --- |
| Java | 25 (JDK) |
| Build | Maven — backend via the provided `mvnw`/`mvnw.cmd` wrapper; desktop via a local Maven install |
| Database | PostgreSQL 17 in Docker (`database/docker-compose.yml`) |
| Backend | Spring Boot 4.1.0 REST API, HTTP Basic authentication |
| Desktop | JavaFX 25.0.1 client (Maven) |
| Scope | Local and course use only; not a production deployment |

The development accounts used for authenticated checks are the backend's local
development accounts. Their passwords are **not** reproduced in this document, in the
Postman collection, or in any other delivery document; they are set locally to match
the backend configuration.

## 3. Verification classifications

Each check below is one of the following kinds. They are not interchangeable, and a
result is only reported under the kind that actually produced it.

- **Automated backend** — JUnit tests executed by `mvnw clean verify` in `backend/`
  (service, controller, repository, entity, mapper, and one application-context
  test). The full-context test requires the PostgreSQL container.
- **Automated desktop** — JUnit tests executed by `mvn clean verify` in
  `desktop-client/`. These exercise the JavaFX-free pieces (DTO/JSON, validators,
  permissions, view-state, message-state, services with a stubbed HTTP client,
  routing) and the FXML/resource wiring. They do not require a backend or database.
- **Connected service-level** — HTTP calls made against a running backend over the
  Docker database to confirm real status codes and the permission matrix. These are
  run so that no business data is left behind (see §11).
- **JavaFX node / FXML** — checks that the FXML documents load and expose the
  expected controls and that controllers wire to them. These confirm structure and
  wiring; they are **not** a substitute for looking at the rendered screen, and they
  make no claim about colours, spacing, or pixel-level appearance.
- **Manual visual** — a person launches the desktop client and observes the rendered
  windows, layouts, and interactions (§10). These are the only checks that can
  confirm on-screen appearance and live interaction.
- **Not executed** — behavior that is intentionally not exercised against the live
  database because it cannot be undone (for example completing a rental or a
  maintenance record); it is covered by automated tests using controlled fixtures
  instead.

## 4. Automated backend coverage

Command (from `backend/`, with the PostgreSQL container running):

```powershell
.\mvnw.cmd clean verify
```

Recorded result: **506 tests — 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**
(Java 25).

Coverage by layer and area:

- **Service (business rules)** — Vehicle Category, Vehicle, Customer, Rental,
  Payment, and Maintenance services: creation, read, update, delete, status
  transitions, the rental date-overlap rule, licence validity, availability,
  server-calculated rental price, payable-rental and one-payment-per-rental rules,
  the payment amount derivation, maintenance overlap and vehicle-status
  synchronization, and the protected-delete conflicts.
- **Controller (HTTP contract)** — all six domains: correct status codes
  (`200`/`201`/`204`), request validation (`400`), authentication (`401`),
  authorization by role (`403`), missing resource (`404`), and business/uniqueness
  conflict (`409`); entities are never returned directly.
- **Repository (queries)** — the overlap and lookup queries that back the business
  rules, verified with a real schema.
- **Entity / mapper** — the JPA model constraints and the explicit entity↔DTO
  mapping, including that bidirectional relationships are not serialized directly.
- **Security** — per-endpoint role rules, the public `GET /api/ping`, and that every
  other endpoint requires authentication.
- **Validation and exception handling** — Bean Validation on request DTOs and the
  global handler that produces a consistent `ApiError` with no stack traces, SQL, or
  credentials.
- **Application context** — one `@SpringBootTest` that confirms the whole context
  starts against PostgreSQL.

The largest suites are the Rental, Payment, and Maintenance service and controller
tests, reflecting where most of the business logic lives.

## 5. Automated desktop coverage

Command (from `desktop-client/`; no backend, port, or database required):

```powershell
mvn clean verify
```

Recorded result: **581 tests — 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**
(Java 25).

Coverage by area:

- **Authentication and session** — the authentication service (valid, invalid, and
  unavailable-backend outcomes), Basic-credentials handling, the session manager and
  user session (start, identity/role, and clearing on logout), and the derived user
  role.
- **HTTP transport** — the single shared API client, the API configuration, the JSON
  mapper factory, and the backend-health check, all exercised with a stubbed HTTP
  client so no network is used.
- **Routing and navigation** — the view manager (login ↔ main), the main content
  router across the seven sections, the navigation state, and the section model.
- **All seven sections** — Dashboard, Vehicle Categories, Vehicles, Customers,
  Rentals, Payments, and Maintenance, each through its service, validator,
  permissions model, view-state, and (where present) message-state and formatter.
- **DTO ↔ JSON** — serialization/deserialization for each domain DTO, including
  server-owned fields being read-only in the client.
- **Validators** — local input validation for each editable form.
- **Permissions** — the USER/ADMIN control-visibility models per section.
- **Message placement and view state** — form errors versus list/operation feedback,
  and that a stale form message is not left above the table; enable/disable and
  selection-driven state per section.
- **Dashboard aggregation** — the pure aggregator, the immutable snapshot, the
  formatter, and the partial-failure result handling.
- **FXML and resources** — `AppResourcesTest` confirms every FXML document and
  stylesheet resource loads and exposes the expected controls (a JavaFX node / FXML
  check, not a rendered-appearance check).

## 6. Authentication and authorization checks

Verified by automated desktop tests and by the connected service-level regression.
No password value is recorded here.

- [x] Valid login with a correct development account succeeds and opens the main
      shell (Dashboard by default).
- [x] Invalid password is rejected; the client stays on the login screen and clears
      the password field.
- [x] Missing credentials on a secured endpoint return `401`.
- [x] Incorrect credentials on a secured endpoint return `401`.
- [x] A `USER` session sees read access everywhere and only the actions permitted to
      `USER`.
- [x] An `ADMIN` session sees the administrative actions.
- [x] Logout clears the in-memory session so the shell is inaccessible until a new
      login.
- [x] `GET /api/ping` is reachable without authentication; every other endpoint is
      not.
- [x] Role-restricted operations return `403` for an authenticated user without the
      required role.

## 7. API error checks

The API returns one consistent error payload (`timestamp`, `status`, `error`,
`message`, `path`, and `validationErrors` for field errors) and never exposes stack
traces, SQL, or credentials. The desktop client presents each safely.

- [x] `400` — request validation failure or invalid period, with per-field messages.
- [x] `401` — missing or invalid authentication.
- [x] `403` — insufficient role.
- [x] `404` — missing resource.
- [x] `409` — business or uniqueness conflict (overlap, duplicate, protected delete,
      or invalid status transition).
- [x] Backend unavailable — the client shows a concise "backend unavailable" message,
      stays responsive, and shows no stack trace.
- [x] Malformed or unexpected response — handled as a safe message rather than a raw
      error.
- [x] In every case the desktop client shows a short message (form errors below the
      editor, list/operation feedback above the table) and never raw JSON, exception
      names, or credentials.

## 8. Feature regression checklist

Each item is confirmed by the automated suites and, where marked, by the connected
service-level regression. Live, irreversible transitions are covered by automated
tests rather than against the shared database (see §11 and the "Not executed" note
in §3).

### Dashboard

- [x] Loads asynchronously and offers only a Refresh action (no create/edit/delete).
- [x] Shows domain totals, exact per-status breakdowns (including zero counts), and
      paid/pending/refunded payment amount summaries.
- [x] A single failing source is shown as "Unavailable" while the other sections
      still display (partial-failure handling).
- [x] Refresh updates the "Last updated" indication.

### Vehicle Categories

- [x] List categories.
- [x] Create/update/delete are ADMIN-only; hidden for USER.
- [x] Required-field validation returns `400`.
- [x] Duplicate name returns `409`.
- [x] Deleting a category referenced by vehicles returns `409` (protected delete).

### Vehicles

- [x] List vehicles with the backend-managed status.
- [x] Create/update/delete are ADMIN-only; hidden for USER.
- [x] Required/positive-value validation returns `400`.
- [x] Duplicate registration returns `409`.
- [x] Deleting a vehicle referenced by rentals returns `409`.

### Customers

- [x] List customers with driving-licence expiry.
- [x] Create/update/delete are ADMIN-only; hidden for USER.
- [x] Multiple validation errors are reported together (`400`); email format is
      validated.
- [x] Duplicate email or licence returns `409`, keeping the entered values.
- [x] Deleting a customer referenced by rentals returns `409`.

### Rentals

- [x] Create a PLANNED rental; the total price is calculated by the backend, never
      supplied by the client.
- [x] End date must be strictly after the start date, else `400`.
- [x] An overlapping PLANNED/ACTIVE rental on the same vehicle returns `409`.
- [x] Only the valid lifecycle actions are enabled for the selected status.
- [x] Create is available to USER and ADMIN; delete is ADMIN-only.
- [x] Only PLANNED/CANCELLED rentals can be deleted; start/complete transitions
      update the vehicle status (covered by automated tests).

### Payments

- [x] A payment can be created only for an ACTIVE or COMPLETED rental, else `409`.
- [x] At most one payment per rental.
- [x] The amount is derived by the backend from the rental total; the request
      carries only the rental id and method.
- [x] Status transitions: pay/fail from PENDING, retry from FAILED, refund from PAID
      (refund is ADMIN-only).
- [x] A PAID or REFUNDED payment cannot be deleted; delete is ADMIN-only.

### Maintenance

- [x] Create a PLANNED maintenance record with a non-negative cost.
- [x] Overlap with a blocking maintenance record or rental on the same vehicle
      returns `409`; an INACTIVE vehicle cannot be scheduled.
- [x] Start (PLANNED → IN_PROGRESS) moves the vehicle to MAINTENANCE; complete
      returns it to AVAILABLE (covered by automated tests).
- [x] Only a PLANNED record can be deleted; management actions are ADMIN-only.

## 9. Disconnected and degraded checks

- [x] With the backend stopped, login fails with a safe "backend unavailable"
      message and the client stays responsive.
- [x] With the backend stopped mid-session, Refresh on a screen shows a concise
      unavailable message without a stack trace; the client recovers when the
      backend returns.
- [x] A dashboard source failing while others succeed yields a partial dashboard, not
      a blank screen (covered by the dashboard partial/full-failure tests).
- [x] Connection and request exceptions map to safe domain messages (covered by the
      message-helper and service exception-preservation tests).

## 10. Manual visual checklist

These require a person to launch the desktop client and observe the running
application. **They are not covered by any automated test.** Do not mark an item
checked, and do not claim it passed, unless it was actually observed on screen.

- [ ] The login screen renders correctly and the password field is masked.
- [ ] After login, the header shows the username, role, and backend-connected
      indicator, and the sidebar lists the sections.
- [ ] The Dashboard renders its metrics, per-status breakdowns, and amount summaries
      legibly, and the "Last updated" value changes on Refresh.
- [ ] Each of the seven sections renders its table and editor without layout
      problems, and the table populates from live data.
- [ ] For a USER session, the ADMIN-only controls are visibly hidden and a read-only
      notice is shown where applicable.
- [ ] For an ADMIN session, the administrative controls are visible and usable.
- [ ] Form validation messages appear below the active editor and list/operation
      feedback appears above the table, with no stale message left behind.
- [ ] Error states (`400`/`401`/`403`/`404`/`409` and backend-unavailable) display as
      short, readable messages with no raw JSON or exception text.
- [ ] Logout returns to the login screen and the shell is no longer reachable.
- [ ] The window resizes reasonably and controls remain usable.

## 11. Disposable-data strategy

When exercising the system by hand or via the connected checks:

- use only clearly fictional, disposable values (for example a `DEMO-` prefix, an
  `@example.invalid` email, a `+0000000000` phone, and a future licence expiry);
- never enter real personal data and never reproduce account passwords in a
  recording or document;
- prefer scenarios that do not create permanent records — validation and conflict
  paths reject the request before anything is stored, and connected checks target
  non-existent ids for delete/permission probes so no real data is mutated;
- do not start a rental or maintenance record for a demonstration if it will then be
  impossible to remove; those irreversible transitions are covered by the automated
  suites instead.

## 12. Cleanup order

Remove any disposable data that is still deletable in dependency-safe order, so no
protected-delete conflict is hit:

1. Payments (any deletable ones created);
2. Maintenance records (while PLANNED);
3. Rentals (while PLANNED/CANCELLED — cancel a PLANNED rental first if needed);
4. Customers;
5. Vehicles;
6. Vehicle Categories.

A record that has been started (and therefore cannot be deleted) should not have been
created for a live run; if one was, it remains as history by design.

## 13. Final acceptance checklist

- [x] Backend `mvnw clean verify`: 506 tests — 0 failures, 0 errors, 0 skipped, BUILD
      SUCCESS.
- [x] Desktop `mvn clean verify`: 581 tests — 0 failures, 0 errors, 0 skipped, BUILD
      SUCCESS.
- [x] Connected service-level regression confirms the permission matrix and error
      codes, with no business data left behind.
- [x] No production credential, token, private key, or `Authorization` value is
      present in the repository; the Postman password variables are blank.
- [x] Documentation (README, architecture, demonstration guide, this plan) is
      accurate and reproduces no passwords.
- [ ] Manual visual checklist (§10) completed by a reviewer on the running desktop
      client.

# Easy Car Rent

## Overview

Easy Car Rent is a car-rental management application composed of three parts:

- a **Spring Boot** REST API (`backend/`);
- a **PostgreSQL** database, run in Docker (`database/`);
- a **JavaFX** desktop client (`desktop-client/`).

The desktop client authenticates against the API using HTTP Basic, keeps the
session in memory, and drives every operation through the REST API. The API holds
all business rules; the desktop client mirrors them for a responsive experience but
the backend remains authoritative.

## Features

- **Authentication** — HTTP Basic login against backend development accounts, with
  logout that clears the in-memory session.
- **Role-aware behavior** — `USER` and `ADMIN` roles; controls that a role may not
  use are hidden or disabled, and the backend still enforces every rule.
- **Dashboard** — a read-only overview aggregated from the existing APIs: domain
  totals, exact per-status breakdowns, and paid/pending/refunded payment amount
  summaries, with concurrent loading and safe partial-failure handling.
- **Vehicle Categories** — category listing and management.
- **Vehicles** — vehicle listing and management, with a backend-managed status.
- **Customers** — customer listing and management, with driving-licence expiry.
- **Rentals** — booking with server-calculated price, date-overlap rejection, and
  the start/complete/cancel lifecycle.
- **Payments** — one payment per rental for a payable rental, with the
  pay/fail/retry/refund lifecycle and a server-derived amount.
- **Maintenance** — scheduling with overlap rejection and the start/complete
  lifecycle that synchronizes the vehicle status.
- **Validation and conflict handling** — request validation and clear `400`/`401`/
  `403`/`404`/`409` responses, presented safely in the desktop client.
- **Asynchronous desktop communication** — all API calls are non-blocking; the
  JavaFX Application Thread is never blocked.

## Technology stack

| Component | Version |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.1.0 |
| JavaFX | 25.0.1 |
| Maven | 3.9.x (wrapper provided for the backend) |
| PostgreSQL | 17 (Docker) |
| Jackson | 2.19.2 |
| JUnit | 5.11.4 |
| Testing | JUnit 5, Mockito, Spring Boot test slices; JavaFX-free desktop unit tests |

## Repository structure

```text
Easy-Car-Rent-App/
├── backend/                 Spring Boot REST API (Maven, mvnw wrapper)
├── desktop-client/          JavaFX desktop client (Maven) — see its own README
├── database/                PostgreSQL Docker Compose configuration
├── docs/                    documentation and Postman assets
│   ├── architecture.md      system, module and workflow diagrams
│   ├── demo-guide.md        reproducible demonstration walkthrough
│   ├── test-plan.md         verification plan and recorded results
│   └── postman/             Postman collection for the REST API
├── .github/                 issue and pull-request templates
└── README.md                this file
```

## Prerequisites

- **Java 25** (JDK).
- **Maven** — the backend ships a wrapper (`mvnw`/`mvnw.cmd`); the desktop client
  uses a local Maven install.
- **Docker Desktop** — for the PostgreSQL database.
- **Git**.

## Fresh local setup

The steps below have been verified from a fresh clone on a separate computer. Run them
in order from the repository root.

1. Clone the repository.
2. Ensure **Java 25** is installed (`java -version`).
3. Start PostgreSQL with Docker Compose:
   ```powershell
   docker compose -f .\database\docker-compose.yml up -d
   ```
4. Optionally verify the container is running:
   ```powershell
   docker ps
   ```
5. Run the backend tests (PostgreSQL must already be running — see
   [Running tests](#running-tests)):
   ```powershell
   cd backend
   .\mvnw.cmd clean verify
   ```
6. Start the backend. Normal startup (no demo data):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   or with the reproducible demo data (recommended for a populated environment right
   after a fresh clone):
   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
   ```
7. Start the JavaFX desktop client from another terminal:
   ```powershell
   cd desktop-client
   mvn javafx:run
   ```
8. Sign in with one of the documented [development accounts](#development-accounts).

## Starting PostgreSQL

From the repository root, using the provided Compose file (non-destructive; reuses
the named volume):

```powershell
docker compose -f database/docker-compose.yml up -d
```

## Starting the backend

From `backend/`, on Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`. `GET /api/ping` is public; every other
endpoint requires authentication. This normal startup does **not** create demo data;
see [Demo data](#demo-data) for the demo-profile startup.

## Starting the desktop client

From `desktop-client/`:

```powershell
cd desktop-client
mvn javafx:run
```

The client opens the login screen. See `desktop-client/README.md` for the full
desktop feature reference.

## Demo data

A fresh database starts empty. A dedicated `demo` profile can populate it with a
compact, entirely fictional demonstration dataset so every screen has meaningful
content on another computer.

Normal startup does **not** insert any demo data:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Demo startup inserts the fictional dataset **only when the database is empty**:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

The demo initializer:

- inserts the dataset only when every business table is empty;
- skips initialization when any business data already exists;
- never resets, deletes or overwrites an existing database;
- creates no duplicates when the backend is started again against the same database.

The dataset covers all six domains — vehicle categories, vehicles, customers,
rentals, payments and maintenance records — with several vehicles kept available
for interactive use. All values are fictional (for example `DEMO-001` registrations,
`@example.invalid` emails and `DEMO-LIC-001` licence numbers).

### Fresh-clone workflow

See [Fresh local setup](#fresh-local-setup) for the full ordered steps. In short: start
PostgreSQL, start the backend with the demo profile, then start the desktop client and
sign in with a [development account](#development-accounts).

The PostgreSQL data lives in a Docker named volume that is local to each Docker host
and is **not** transferred through Git, so every clone begins with an empty database
and reproduces the dataset from the demo profile rather than from copied records.
`docker compose down` stops the container but preserves that named volume; adding `-v`
(`docker compose down -v`) is **destructive** — it deletes the volume and all local
data. See `docs/demo-guide.md` for the full walkthrough.

## Development accounts

The backend defines two predefined accounts, used with HTTP Basic authentication, so a
fresh local environment can be signed into without inspecting the source. They are
configured in the backend development security configuration
(`backend/src/main/java/be/condorcet/easycarrent/config/SecurityConfig.java`).

| Username | Password | Role |
| --- | --- | --- |
| `user` | `user123` | USER |
| `admin` | `admin123` | ADMIN |

These credentials are intentionally predefined for local development and demonstration
only. They must not be reused or deployed as production credentials.

- `user` demonstrates the standard application role: reads across every section, plus
  rental and payment operations.
- `admin` demonstrates the administrative role: category, vehicle, customer and
  maintenance management, deletions and payment refunds.

The documented `user` and `admin` values are the only credentials in the repository and
are intentionally public, local development credentials — not secrets. No production
credentials, external tokens, private keys or `Authorization` header values are
included, and no real personal or business data is used. A production deployment must
supply a different authentication configuration. `GET /api/ping` is public; every other
endpoint requires authentication.

## Roles and permissions

| Domain | Read | Create | Update | Delete | Lifecycle transitions |
| --- | --- | --- | --- | --- | --- |
| Vehicle Categories | USER, ADMIN | ADMIN | ADMIN | ADMIN | — |
| Vehicles | USER, ADMIN | ADMIN | ADMIN | ADMIN | — |
| Customers | USER, ADMIN | ADMIN | ADMIN | ADMIN | — |
| Rentals | USER, ADMIN | USER, ADMIN | USER, ADMIN | ADMIN | start / complete / cancel: USER, ADMIN |
| Payments | USER, ADMIN | USER, ADMIN | — (no update) | ADMIN | pay / fail / retry: USER, ADMIN; refund: ADMIN |
| Maintenance | USER, ADMIN | ADMIN | — (no update) | ADMIN | start / complete: ADMIN |
| Dashboard | USER, ADMIN (read-only) | — | — | — | — |

## Business rules

Verified rules enforced by the backend:

- **Rental statuses** — `PLANNED`, `ACTIVE`, `COMPLETED`, `CANCELLED`. `start`
  (`PLANNED → ACTIVE`) requires an available vehicle and sets it to `RENTED`;
  `complete` (`ACTIVE → COMPLETED`) returns the vehicle to `AVAILABLE`; `cancel`
  (`PLANNED → CANCELLED`) leaves the vehicle unchanged. Only a `PLANNED` rental can
  be updated; only `PLANNED`/`CANCELLED` rentals can be deleted.
- **Rental overlap** — a vehicle with a `PLANNED`/`ACTIVE` rental overlapping the
  requested inclusive period cannot be booked again (`409`). The end date must be
  strictly after the start date; past dates are accepted.
- **Rental price** — calculated by the backend (`dailyPrice × days`); the client
  never supplies it. Monetary values use `BigDecimal`.
- **Payment status transitions** — `pay`/`fail` from `PENDING`, `retry` from
  `FAILED`, `refund` from `PAID`. A `PAID` or `REFUNDED` payment cannot be deleted.
- **One payment per rental** — a rental may have at most one payment.
- **Payable rental statuses** — a payment can be created only for an `ACTIVE` or
  `COMPLETED` rental.
- **Payment amount** — derived by the backend from the rental total; the request
  carries only the rental id and the payment method.
- **Maintenance statuses and transitions** — `PLANNED`, `IN_PROGRESS`,
  `COMPLETED`. `start` (`PLANNED → IN_PROGRESS`) requires an available vehicle and
  moves it to `MAINTENANCE`; `complete` (`IN_PROGRESS → COMPLETED`) returns the
  vehicle to `AVAILABLE`. A maintenance period cannot overlap a blocking
  maintenance record or rental on the same vehicle, and an `INACTIVE` vehicle
  cannot have maintenance scheduled. Only a `PLANNED` record can be deleted.
- **Protected deletes** — a category referenced by vehicles, a vehicle or customer
  referenced by rentals, and a rental referenced by a payment are all rejected with
  a `409` conflict rather than cascading.

## Running tests

**PostgreSQL must be running before the complete backend test suite is executed.** The
application-context test uses the configured PostgreSQL datasource, so start the
database first:

```powershell
docker compose -f .\database\docker-compose.yml up -d
```

Backend:

```powershell
cd backend
.\mvnw.cmd clean verify
```

Desktop client (no backend, port, or database required):

```powershell
cd desktop-client
mvn clean verify
```

Current verified totals (Java 25):

- **Backend:** 518 tests — 0 failures, 0 errors, 0 skipped.
- **Desktop:** 581 tests — 0 failures, 0 errors, 0 skipped.

## Error handling

The API returns a consistent error payload (`timestamp`, `status`, `error`,
`message`, `path`, and `validationErrors` for field validation) and never exposes
stack traces, SQL, or credentials. The status codes are distinguishable:

- `400` — request validation failure (with per-field messages) or an invalid
  period;
- `401` — missing or invalid authentication;
- `403` — insufficient role;
- `404` — missing resource;
- `409` — a business or uniqueness conflict (overlap, duplicate, protected delete,
  or an invalid status transition).

The desktop client presents each of these as a short, safe message, placing form
errors below the active editor and list/operation feedback above the table, and
never showing raw JSON, exception names, or credentials.

## Known limitations

- Authentication uses backend development accounts and HTTP Basic; it is intended
  for local development and demonstration use, not production.
- There is no installer or packaged distribution; the client is launched with
  `mvn javafx:run`.
- The dashboard refreshes only on demand (manual Refresh); there is no automatic
  polling.
- There is no reporting, export, invoice, or receipt module.
- Some irreversible status transitions (for example completing a rental or
  maintenance record, which the backend keeps as history) are exercised through the
  automated test suites rather than through disposable live fixtures, because such a
  record cannot be removed afterwards.

## Documentation

- `docs/architecture.md` — system, module, and workflow diagrams.
- `docs/demo-guide.md` — a reproducible demonstration walkthrough.
- `docs/test-plan.md` — the verification plan (automated, connected, and manual
  visual checks) with the recorded results.
- `docs/postman/easy-car-rent-api.postman_collection.json` — Postman requests for
  the REST API (Basic Auth via collection variables; set the passwords locally).
- `desktop-client/README.md` — the detailed desktop-client reference.

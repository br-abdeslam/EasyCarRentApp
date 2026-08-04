# Easy Car Rent — Desktop Client

JavaFX desktop client for the Easy Car Rent application.

## Purpose

Provides the graphical desktop front end for the Easy Car Rent system. This
module is a standalone Maven project, independent from the backend.

## Current scope

The client has an MVC/FXML/CSS architecture, a reusable non-blocking HTTP and
JSON foundation, a login workflow, the authenticated application shell, and six
backend-connected domain screens (**Vehicle Categories**, **Vehicles**,
**Customers**, **Rentals**, **Payments**, and **Maintenance**). It starts on a
login screen, authenticates against the backend using HTTP Basic, keeps the session
in memory, and then shows the main shell: a persistent header (username, role,
logout), a sidebar to navigate between sections, and a routed central content area.
**Vehicle Categories**, **Vehicles**, **Customers**, **Rentals**, **Payments**, and
**Maintenance** are real screens backed by the API; only **Dashboard** remains a
routed **placeholder** view.

## Main application shell

- **Persistent header** — application title, the authenticated username and role,
  and a **Log out** button; all remain visible while navigating.
- **Backend-health state** — a status area continues to show
  **Backend connected** / **Backend unavailable**.
- **Sidebar navigation** — seven sections: **Dashboard**, **Vehicle Categories**,
  **Vehicles**, **Customers**, **Rentals**, **Payments**, **Maintenance**. Both
  USER and ADMIN see the same sections (visible navigation is not authorization;
  write permissions are enforced by the backend and future screens).
- **Routing** — clicking a section replaces only the central content; the header
  and sidebar stay in place and no new window is opened. **Dashboard** is selected
  by default, and exactly one section stays selected. **Vehicle Categories**,
  **Vehicles**, **Customers**, **Rentals**, **Payments**, and **Maintenance** load
  their real views; **Dashboard** loads a placeholder that states it is prepared for
  future functionality.

## Customers

The Customers screen is connected to the backend `/api/customers` API and opens
inside the central content area (header and sidebar stay visible).

- **Loading, empty, and error states** — customers load asynchronously (the
  JavaFX Application Thread is never blocked); the table shows the real backend
  data (id, first name, last name, email, phone, licence number, licence expiry).
  An empty list
  and API/connection failures are shown with a safe message and no stack traces.
- **Refresh** — reloads the current backend state; only one load runs at a time.
- **Role-aware writes** — reading is available to USER and ADMIN. Per the backend
  security rules, **only ADMIN may create, update, or delete** a customer. A USER
  sees a read-only screen (a read-only notice is shown and the write controls are
  hidden); the backend remains authoritative.
- **Create / edit** — an in-view form validates input against the backend
  constraints before sending (first and last name required, ≤ 60; a valid email,
  ≤ 120; a phone matching the backend pattern; address required, ≤ 255; licence
  number required, ≤ 40; a licence expiry date that is not in the past). Backend
  validation messages are displayed safely. **Email and driving-licence number are
  unique**; a duplicate is reported as a conflict and the entered values are kept.
- **Delete** — requires confirmation (identifying the customer by name only).
  Deleting a customer that rentals still reference is rejected by the backend and
  reported safely; the customer is kept.

Personal data is treated carefully: only the fields the screen needs are shown,
customer records are never logged, and no real customer data is committed.

## Rentals

The Rentals screen is connected to the backend `/api/rentals` API and opens inside
the central content area (header and sidebar stay visible).

- **Loading, empty, and error states** — rentals load asynchronously (the JavaFX
  Application Thread is never blocked); the table shows the real backend data (id,
  customer name, vehicle, start date, end date, status, and the backend-calculated
  total price). An empty list and API/connection failures are shown with a safe
  message and no stack traces.
- **Refresh** — reloads the current backend state; only one load runs at a time,
  and the selection is preserved by id.
- **Customer and vehicle selection** — the editor loads customers from
  `/api/customers` and vehicles from `/api/vehicles`, showing a readable label
  (customer first and last name; vehicle `registration — brand model`) while
  sending only the selected **id**. In create mode, when both dates form a valid
  period, the vehicle choices are narrowed to those the backend reports as
  available for that period via `/api/vehicles/available`; the backend still
  rejects a conflict on save if a vehicle becomes unavailable in the meantime.
- **Dates and price** — the start and end dates are required and the end date must
  be strictly after the start date (a same-day rental is rejected, matching the
  backend); past dates are accepted because the backend accepts them. The status
  and the total price are **backend-managed** and shown read-only; the editor shows
  only a clearly-labelled, non-authoritative estimate and never submits a price or
  a status.
- **Status workflow** — the backend lifecycle is `PLANNED → ACTIVE → COMPLETED`
  with `PLANNED → CANCELLED`. The screen offers only the transitions the selected
  status allows, each through its dedicated backend endpoint: **Start**
  (`PLANNED → ACTIVE`), **Complete** (`ACTIVE → COMPLETED`), and **Cancel rental**
  (`PLANNED → CANCELLED`). The status changes only after backend confirmation.
- **Role-aware controls** — per the backend security rules, USER and ADMIN may
  read, book, update, and run the lifecycle transitions; **only ADMIN may delete**
  a rental, so the Delete control is shown only to ADMIN. A role with no rental
  permission sees a read-only screen; the backend remains authoritative.
- **Create / edit** — an in-view form validates the selections and dates locally
  (customer, vehicle, start date, and end date required; end after start) before
  sending; only a PLANNED rental can be edited. Backend validation errors and
  booking conflicts (overlap, unavailable vehicle, licence expiry) are shown below
  the form, while list, delete, and transition feedback is shown above the table so
  a stale form error never lingers there.
- **Overlap and conflicts** — a vehicle with a PLANNED or ACTIVE rental overlapping
  the requested period is rejected by the backend; the editor and entered data are
  kept, no second forced request is sent, and the safe conflict message is shown.
- **Delete** — requires confirmation (identifying the rental by id, vehicle, and
  period). Only a PLANNED or CANCELLED rental can be deleted; deleting an active or
  completed rental, or one a payment references, is rejected by the backend and
  reported safely, and the rental is kept.

Monetary values use `BigDecimal` end to end. Only the customer name is shown (no
other personal data), rentals are never logged, and no fake rental data is
displayed.

## Payments

The Payments screen is connected to the backend `/api/payments` API and opens
inside the central content area (header and sidebar stay visible).

- **Loading, empty, and error states** — payments load asynchronously (the JavaFX
  Application Thread is never blocked); the table shows the real backend data (id,
  rental, amount, method, status, created and paid timestamps). An empty list and
  API/connection failures are shown with a safe message and no stack traces.
- **Refresh** — reloads the current backend state; only one load runs at a time,
  and the selection is preserved by id.
- **Rental selection** — the editor loads rentals from `/api/rentals` and offers,
  as create choices, only the rentals eligible for a new payment: those that are
  **active or completed** and do not already have a payment (the backend allows at
  most one payment per rental). Each option shows a readable label (`Rental #id —
  registration brand model — customer name (status)`) while submitting only the
  rental **id**. The backend remains authoritative and still rejects a duplicate or
  non-payable rental.
- **Amount, status, and dates are backend-managed** — the request carries only the
  rental id and the chosen method. The **amount is derived by the backend** from the
  rental total (the editor shows it read-only, from the selected rental's total),
  the status starts at `PENDING`, and the created/paid timestamps are set by the
  backend; none of them is editable or submitted by the client.
- **Payment method** — a fixed `CASH` / `CARD` / `BANK_TRANSFER` choice (no card,
  bank, or provider data is entered or stored).
- **Status workflow** — the backend lifecycle is `PENDING → PAID` (Mark paid),
  `PENDING → FAILED` (Mark failed), `FAILED → PENDING` (Retry), and `PAID →
  REFUNDED` (Refund), each through its dedicated endpoint. The screen offers only
  the transitions the selected status allows, and the status changes only after
  backend confirmation.
- **Role-aware controls** — per the backend security rules, USER and ADMIN may
  read, create, and run the pay/fail/retry transitions; **only ADMIN may refund or
  delete** a payment, so those controls are shown only to ADMIN. A role with no
  payment permission sees a read-only screen; the backend remains authoritative.
- **Create** — an in-view form validates the selections locally (rental and method
  required) before sending; backend validation errors and conflicts (duplicate
  payment, non-payable rental) are shown below the form, while list, delete, and
  transition feedback is shown above the table so a stale form error never lingers
  there.
- **Delete** — ADMIN only, with a confirmation identifying the payment by id,
  rental, and amount. Only a `PENDING` or `FAILED` payment can be deleted; deleting
  a paid or refunded payment is rejected by the backend and reported safely, and the
  payment is kept. There is no payment update on the backend, so the screen offers
  no edit.

Monetary values use `BigDecimal` end to end. Only the rental reference and (when
the rental is loaded) the customer name are shown, payments are never logged, and
no fake payment data is displayed.

## Maintenance

The Maintenance screen is connected to the backend `/api/maintenance-records` API
and opens inside the central content area (header and sidebar stay visible).

- **Loading, empty, and error states** — maintenance records load asynchronously
  (the JavaFX Application Thread is never blocked); the table shows the real backend
  data (id, vehicle, description, start date, end date, cost, status). An empty list
  and API/connection failures are shown with a safe message and no stack traces.
- **Refresh** — reloads the current backend state; only one load runs at a time,
  and the selection is preserved by id.
- **Vehicle selection** — the editor loads vehicles from `/api/vehicles` and offers,
  as create choices, only the vehicles that may have maintenance scheduled (any
  vehicle that is not `INACTIVE`; the backend rejects an inactive vehicle). Each
  option shows a readable label (`registration — brand model`) while submitting only
  the vehicle **id**; the table's vehicle column is enriched from the same lookup.
- **Dates, cost, and status** — the request carries the vehicle, a description
  (required, at most 500 characters), the start and end dates (the end date must be
  on or after the start date; same-day is allowed and past dates are accepted), and
  the cost (`BigDecimal`, zero or positive, at most ten digits before the decimal
  point and two decimals). The status is **backend-managed** (a new record starts
  `PLANNED`) and shown read-only; there is no maintenance type or mileage in the
  backend contract, so the screen has neither.
- **Status workflow** — the backend lifecycle is `PLANNED → IN_PROGRESS →
  COMPLETED`, each step through its dedicated endpoint: **Start** (`PLANNED →
  IN_PROGRESS`, which the backend moves the vehicle to maintenance for) and
  **Complete** (`IN_PROGRESS → COMPLETED`, which returns the vehicle to available).
  The screen offers only the transition the selected status allows, and the status
  changes only after backend confirmation.
- **Role-aware controls** — per the backend security rules, reading is available to
  USER and ADMIN, while **creating, starting, completing, and deleting all require
  ADMIN**. A USER sees a read-only screen (a read-only notice is shown and the write
  controls are hidden); the backend remains authoritative.
- **Create** — an in-view form validates the selection and inputs locally before
  sending; backend validation errors and conflicts (overlapping maintenance, an
  overlapping rental, an inactive vehicle) are shown below the form, while list,
  delete, and transition feedback is shown above the table so a stale form error
  never lingers there.
- **Delete** — ADMIN only, with a confirmation identifying the record by id,
  vehicle, and period. Only a `PLANNED` record can be deleted; deleting an
  in-progress or completed record is rejected by the backend and reported safely,
  and the record is kept. There is no maintenance update on the backend, so the
  screen offers no edit.

Monetary values use `BigDecimal` end to end. Only the vehicle reference is shown
(no customer data), maintenance records are never logged, and no fake maintenance
data is displayed.

## Vehicles

The Vehicles screen is connected to the backend `/api/vehicles` API and opens
inside the central content area (header and sidebar stay visible).

- **Loading, empty, and error states** — vehicles load asynchronously (the JavaFX
  Application Thread is never blocked); the table shows the real backend data (id,
  registration, brand, model, year, category, status, daily price). An empty list
  and API/connection failures are shown with a safe message and no stack traces.
- **Refresh** — reloads the current backend state; only one load runs at a time.
- **Status is read-only** — the vehicle status (`AVAILABLE`, `RENTED`,
  `MAINTENANCE`, `INACTIVE`) is managed by the backend and shown in the table; it
  is not part of the create/update request and is not edited by the client.
- **Categories for the editor** — the editor loads categories from
  `/api/categories` and selects one in a combo box that displays category names
  while sending the category **id**. Editing matches the vehicle's category by id;
  if no categories are available, creating and editing are disabled with a clear
  note.
- **Role-aware writes** — reading is available to USER and ADMIN. Per the backend
  security rules, **only ADMIN may create, update, or delete** a vehicle. A USER
  sees a read-only screen (a read-only notice is shown and the write controls are
  hidden); the backend remains authoritative.
- **Create / edit** — an in-view form validates input against the backend
  constraints before sending (registration required, ≤ 20 chars; brand and model
  required, ≤ 60; optional year between 1900 and the current year; optional color
  ≤ 40; positive daily price with at most two decimals; optional non-negative
  mileage; a category is required). Backend validation messages are displayed
  safely, and a duplicate registration is reported as a conflict.
- **Delete** — requires confirmation. Deleting a vehicle that rentals still
  reference is rejected by the backend and reported safely; the vehicle is kept.

Monetary values use `BigDecimal` end to end; the daily price is shown with two
decimals. No fake vehicle data is displayed.

## Vehicle Categories

The Vehicle Categories screen is connected to the backend `/api/categories` API
and opens inside the central content area (header and sidebar stay visible).

- **Loading** — categories are loaded asynchronously; the JavaFX Application
  Thread is never blocked. A loading indicator is shown while a request runs.
- **List, empty, and error states** — the table shows the actual backend
  categories (id, name, description); an empty result and API/connection failures
  are shown with a safe message and no stack traces.
- **Refresh** — reloads the current backend state; only one load runs at a time.
- **Role-aware writes** — reading is available to USER and ADMIN. Per the backend
  security rules, **only ADMIN may create, update, or delete** a category. A USER
  sees a read-only screen (a read-only notice is shown and the write controls are
  hidden); the backend remains authoritative.
- **Create / edit** — an in-view form validates input against the backend
  constraints (name required, at most 100 characters; description optional, at
  most 500) before sending, and displays the backend's validation messages when a
  request is rejected. Duplicate names are reported as a conflict.
- **Delete** — requires confirmation. Deleting a category that vehicles still
  reference is rejected by the backend and reported safely; the category is kept.

No fake category data is displayed, and no other domain screen is implemented in
this milestone.

## Login and authentication

- **Login screen** — the application opens on a login view with username and
  password fields; pressing Enter submits.
- **HTTP Basic** — credentials are verified asynchronously against a protected
  read endpoint (`GET /api/vehicles`); a `2xx` confirms the credentials, `401`
  means invalid username or password, and a connection failure reports the
  backend as unavailable. Wrong credentials keep the user on the login view.
- **Role resolution** — the backend exposes no authenticated-identity endpoint,
  so after successful authentication the role is mapped from the fixed
  development accounts defined in the backend security configuration
  (`user` → USER, `admin` → ADMIN). This mapping is **development-only** and
  would be replaced by a backend identity endpoint if the accounts became
  dynamic.
- **Session** — the authenticated username, role, and credentials are held only
  in application memory for the lifetime of the process. **Credentials are never
  persisted**, never written to configuration, never logged, and the password is
  never displayed after login.
- **Logout** — clears the in-memory session and returns to the login screen; the
  previous credentials become unreachable afterwards.

This uses development Basic authentication for the course. Tokens, JWT,
remember-me, and persistent login are intentionally **not** implemented.

## Prerequisites

- Java 25 (JDK)
- Apache Maven 3.9+
- The backend REST API is expected at `http://localhost:8080` (see below).

## Backend configuration

The backend base URL is read from a classpath properties file:

```text
src/main/resources/be/condorcet/easycarrent/desktop/config/desktop.properties
```

```properties
api.base-url=http://localhost:8080
```

No credentials are stored in the client or in this file; the connectivity check
uses the public `/api/ping` endpoint, and login credentials are entered at
runtime and held only in memory.

## HTTP and JSON foundation

- **Asynchronous only** — all calls use `java.net.http.HttpClient.sendAsync` and
  return `CompletableFuture`; no request blocks the JavaFX Application Thread.
- **JSON** — a single shared Jackson `ObjectMapper` (ISO-8601 date/time via the
  JSR-310 module) serializes and deserializes payloads.
- **Errors** — a non-2xx response becomes an `ApiRequestException` (carrying the
  parsed backend `ApiError` when available); a transport failure (offline,
  refused, timed out) becomes an `ApiConnectionException`.
- **Connectivity indicator** — `BackendHealthService` calls `/api/ping` and the
  main view shows a pending, connected, or unavailable state. UI updates are
  marshalled back onto the JavaFX Application Thread with `Platform.runLater`.

## Project structure

```text
desktop-client/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── be/condorcet/easycarrent/desktop/
    │   │       ├── App.java
    │   │       ├── auth/{BasicCredentials, DesktopUserRole, AuthenticatedUser,
    │   │       │        VehicleCategoryPermissions, VehiclePermissions, CustomerPermissions,
    │   │       │        RentalPermissions, PaymentPermissions, MaintenancePermissions}.java
    │   │       ├── config/ApiConfiguration.java
    │   │       ├── dto/{ApiErrorDto, VehicleCategoryResponseDto, VehicleCategoryRequestDto,
    │   │       │       VehicleResponseDto, VehicleRequestDto, VehicleStatus,
    │   │       │       CustomerResponseDto, CustomerRequestDto,
    │   │       │       RentalResponseDto, RentalRequestDto, RentalStatus,
    │   │       │       PaymentResponseDto, PaymentRequestDto, PaymentStatus, PaymentMethod,
    │   │       │       MaintenanceResponseDto, MaintenanceRequestDto, MaintenanceStatus}.java
    │   │       ├── http/{ApiClient, JsonMapperFactory}.java
    │   │       ├── http/{ApiRequestException, ApiConnectionException}.java
    │   │       ├── navigation/{MainSection, NavigationState, MainContentRouter}.java
    │   │       ├── service/{BackendHealthService, BackendHealthResult}.java
    │   │       ├── service/{AuthenticationService, AuthenticationResult}.java
    │   │       ├── service/{VehicleCategoryService, VehicleCategoryValidator}.java
    │   │       ├── service/{VehicleService, VehicleValidator}.java
    │   │       ├── service/{CustomerService, CustomerValidator, CustomerDateFormatter}.java
    │   │       ├── service/{RentalService, RentalValidator, RentalMessages, RentalFormatter}.java
    │   │       ├── service/{PaymentService, PaymentValidator, PaymentMessages, PaymentFormatter}.java
    │   │       ├── service/{MaintenanceService, MaintenanceValidator, MaintenanceMessages,
    │   │       │            MaintenanceFormatter}.java
    │   │       ├── session/{SessionManager, UserSession}.java
    │   │       └── view/{ViewManager, LoginController, MainViewController,
    │   │       │         SectionPlaceholderController, VehicleCategoryController,
    │   │       │         VehicleCategoryViewState, VehicleController, VehicleViewState,
    │   │       │         CustomerController, CustomerViewState, CustomerMessageState,
    │   │       │         RentalController, RentalViewState, RentalMessageState,
    │   │       │         PaymentController, PaymentViewState, PaymentMessageState,
    │   │       │         MaintenanceController, MaintenanceViewState, MaintenanceMessageState}.java
    │   └── resources/be/condorcet/easycarrent/desktop/
    │       ├── config/desktop.properties
    │       └── view/{login-view.fxml, main-view.fxml, section-placeholder.fxml,
    │                 vehicle-categories-view.fxml, vehicles-view.fxml, customers-view.fxml,
    │                 rentals-view.fxml, payments-view.fxml, maintenance-view.fxml, app.css}
    └── test/
        └── java/be/condorcet/easycarrent/desktop/...
```

### Responsibilities

- **App.java** — application bootstrap: assembles the shared services and shows
  the login view through the `ViewManager`. No view logic.
- **ViewManager** — authentication-level router that swaps between the login and
  main views only.
- **MainContentRouter** — routes the central content of the main shell between
  sections (loading `vehicle-categories-view.fxml` for Vehicle Categories,
  `vehicles-view.fxml` for Vehicles, `customers-view.fxml` for Customers,
  `rentals-view.fxml` for Rentals, `payments-view.fxml` for Payments,
  `maintenance-view.fxml` for Maintenance, and the placeholder for Dashboard); it
  injects the shared domain services into each loaded controller but owns no Stage
  and performs no authentication or HTTP.
- **MainSection / NavigationState** — the available sections and the current
  selection (pure, JavaFX-free).
- **MainViewController** — main-shell UI events and state.
- **LoginController** — login UI events and state.
- **SectionPlaceholderController / section-placeholder.fxml** — the reusable
  temporary section content.
- **VehicleCategoryService** — the category API workflow over `/api/categories`.
- **VehicleCategoryController / vehicle-categories-view.fxml** — the category
  screen's UI events and structure.
- **VehicleCategoryViewState** — pure, JavaFX-free presentation state for the
  category screen (loading, selection, editor mode, permissions).
- **VehicleCategoryValidator** — client-side category validation mirroring the
  backend constraints.
- **VehicleCategoryPermissions** — the role-based read/write rules for categories.
- **VehicleCategoryResponseDto / VehicleCategoryRequestDto** — category API
  contracts.
- **VehicleService** — the vehicle API workflow over `/api/vehicles`.
- **VehicleController / vehicles-view.fxml** — the vehicle screen's UI events and
  structure (reusing `VehicleCategoryService` to populate the category selector).
- **VehicleViewState** — pure, JavaFX-free presentation state for the vehicle
  screen (loading, selection, editor mode, permissions, category availability).
- **VehicleValidator** — client-side vehicle validation mirroring the backend
  constraints (the current year is injected so tests stay deterministic).
- **VehiclePermissions** — the role-based read/write rules for vehicles.
- **VehicleResponseDto / VehicleRequestDto / VehicleStatus** — vehicle API
  contracts; the status is backend-managed and read-only in the client.
- **CustomerService** — the customer API workflow over `/api/customers`.
- **CustomerController / customers-view.fxml** — the customer screen's UI events
  and structure.
- **CustomerViewState** — pure, JavaFX-free presentation state for the customer
  screen (loading, selection, editor mode, permissions).
- **CustomerValidator** — client-side customer validation mirroring the backend
  constraints (the reference "today" is injected so the licence-expiry rule stays
  deterministic in tests).
- **CustomerPermissions** — the role-based read/write rules for customers.
- **CustomerResponseDto / CustomerRequestDto** — customer API contracts (the
  driving-licence expiry is a `LocalDate`).
- **RentalService** — the rental API workflow over `/api/rentals`, including the
  dedicated `PATCH` lifecycle transitions (`start`, `complete`, `cancel`) so the
  backend's transition rules are never bypassed by a generic update.
- **RentalController / rentals-view.fxml** — the rental screen's UI events and
  structure (reusing `CustomerService` and `VehicleService` to populate the editor,
  and `VehicleService.findAvailable` for available vehicles in create mode).
- **RentalViewState** — pure, JavaFX-free presentation state for the rental screen
  (loading, selection, editor mode, per-status transition gating, permissions, and
  lookup availability).
- **RentalValidator** — client-side rental validation mirroring the backend rules
  (customer, vehicle, and both dates required; end strictly after start; past dates
  accepted).
- **RentalMessages / RentalMessageState** — JavaFX-free safe-message formatting and
  the form-versus-status message placement model (a stale form error never lingers
  above the table).
- **RentalFormatter** — JavaFX-free date, price, and status formatting for display.
- **RentalPermissions** — the role-based rules for rentals (USER and ADMIN may read,
  book, update, and transition; only ADMIN may delete).
- **RentalResponseDto / RentalRequestDto / RentalStatus** — rental API contracts;
  the status and total price are backend-managed and read-only in the client.
- **PaymentService** — the payment API workflow over `/api/payments`, including the
  dedicated body-less `PATCH` lifecycle transitions (`pay`, `fail`, `retry`,
  `refund`); there is no payment update on the backend.
- **PaymentController / payments-view.fxml** — the payment screen's UI events and
  structure (reusing `RentalService` to populate the rental selector and to enrich
  the table label).
- **PaymentViewState** — pure, JavaFX-free presentation state for the payment
  screen (loading, selection, create mode, per-status transition gating,
  permissions, and eligible-rental availability).
- **PaymentValidator** — client-side payment validation mirroring the backend rules
  (rental and method required; amount and status are backend-owned).
- **PaymentMessages / PaymentMessageState** — JavaFX-free safe-message formatting
  and the form-versus-status message placement model (a stale form error never
  lingers above the table).
- **PaymentFormatter** — JavaFX-free amount, timestamp, status, and method
  formatting for display.
- **PaymentPermissions** — the role-based rules for payments (USER and ADMIN may
  read, create, and pay/fail/retry; only ADMIN may refund or delete).
- **PaymentResponseDto / PaymentRequestDto / PaymentStatus / PaymentMethod** —
  payment API contracts; the amount, status, and timestamps are backend-managed and
  read-only in the client, and the request carries only the rental id and method.
- **MaintenanceService** — the maintenance API workflow over
  `/api/maintenance-records`, including the dedicated body-less `PATCH` start and
  complete transitions; there is no maintenance update on the backend.
- **MaintenanceController / maintenance-view.fxml** — the maintenance screen's UI
  events and structure (reusing `VehicleService` to populate the vehicle selector
  and to enrich the table label).
- **MaintenanceViewState** — pure, JavaFX-free presentation state for the
  maintenance screen (loading, selection, create mode, per-status transition
  gating, the ADMIN-only write permission, and vehicle availability).
- **MaintenanceValidator** — client-side maintenance validation mirroring the
  backend rules (vehicle, description ≤ 500, dates with end on or after start, and a
  zero-or-positive cost with at most ten integer digits and two decimals; past dates
  accepted).
- **MaintenanceMessages / MaintenanceMessageState** — JavaFX-free safe-message
  formatting and the form-versus-status message placement model (a stale form error
  never lingers above the table).
- **MaintenanceFormatter** — JavaFX-free date, cost, and status formatting for
  display.
- **MaintenancePermissions** — the role-based rules for maintenance (USER and ADMIN
  may read; creating, starting, completing, and deleting require ADMIN).
- **MaintenanceResponseDto / MaintenanceRequestDto / MaintenanceStatus** —
  maintenance API contracts; the status is backend-managed and read-only in the
  client, and there is no maintenance type or mileage in the contract.
- **ApiConfiguration** — loads and normalizes the backend base URL.
- **ApiClient** — reusable asynchronous HTTP layer (anonymous and Basic-auth GET,
  and authenticated JSON list/POST/PUT/PATCH/DELETE).
- **JsonMapperFactory** — shared JSON mapper configuration.
- **AuthenticationService / AuthenticationResult** — the login use case.
- **SessionManager / UserSession** — in-memory authenticated session.
- **BasicCredentials / DesktopUserRole / AuthenticatedUser** — auth models.
- **BackendHealthService / BackendHealthResult** — the connectivity use case.
- **login-view.fxml, main-view.fxml, app.css** — view structure and appearance.

## Build

```bash
mvn clean verify
```

## Test

```bash
mvn clean test
```

## Launch

```bash
mvn javafx:run
```

## Running the backend

Login and the connectivity check need the backend running. Start it using the
established repository instructions:

1. Start PostgreSQL from `database/`:

   ```bash
   docker compose up -d
   ```

2. Start the Spring Boot API from `backend/`:

   ```bash
   ./mvnw spring-boot:run
   ```

The API listens on `http://localhost:8080`. `/api/ping` is public; `GET
/api/vehicles` (used to validate login) requires an authenticated USER or ADMIN.

## Expected screens

A resizable window titled **Easy Car Rent**:

- **Login** — username and password fields with a **Sign in** button. Invalid
  credentials or an unavailable backend keep the user on this screen with a safe
  message; the password field is cleared after each attempt.
- **Main shell** — after a successful login: a header with the application title,
  the authenticated username and role (**USER** or **ADMIN**), and a **Log out**
  button; a sidebar listing the seven sections; a central content area showing the
  selected section's placeholder (**Dashboard** by default); and a status area
  with the initialization message and the backend connectivity indicator
  (**Backend connected** / **Backend unavailable**). Clicking a section changes
  only the central content. **Log out** returns to the login screen.

The window can be resized (the sidebar keeps a stable width and the content area
grows) and closes normally. The **Vehicle Categories**, **Vehicles**,
**Customers**, **Rentals**, **Payments**, and **Maintenance** sections show real
backend-connected tables and editors; only **Dashboard** remains a placeholder.

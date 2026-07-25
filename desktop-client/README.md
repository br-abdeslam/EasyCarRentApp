# Easy Car Rent — Desktop Client

JavaFX desktop client for the Easy Car Rent application.

## Purpose

Provides the graphical desktop front end for the Easy Car Rent system. This
module is a standalone Maven project, independent from the backend.

## Current scope

The client has an MVC/FXML/CSS architecture, a reusable non-blocking HTTP and
JSON foundation, a login workflow, the authenticated application shell, and the
first backend-connected domain screen (**Vehicle Categories**). It starts on a
login screen, authenticates against the backend using HTTP Basic, keeps the
session in memory, and then shows the main shell: a persistent header (username,
role, logout), a sidebar to navigate between sections, and a routed central
content area. **Vehicle Categories** is a real screen backed by the API; the
other six sections remain routed **placeholder** views.

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
  by default, and exactly one section stays selected. **Vehicle Categories** loads
  its real view; the remaining sections load placeholders that state they are
  prepared for future functionality.

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
    │   │       │        VehicleCategoryPermissions}.java
    │   │       ├── config/ApiConfiguration.java
    │   │       ├── dto/{ApiErrorDto, VehicleCategoryResponseDto, VehicleCategoryRequestDto}.java
    │   │       ├── http/{ApiClient, JsonMapperFactory}.java
    │   │       ├── http/{ApiRequestException, ApiConnectionException}.java
    │   │       ├── navigation/{MainSection, NavigationState, MainContentRouter}.java
    │   │       ├── service/{BackendHealthService, BackendHealthResult}.java
    │   │       ├── service/{AuthenticationService, AuthenticationResult}.java
    │   │       ├── service/{VehicleCategoryService, VehicleCategoryValidator}.java
    │   │       ├── session/{SessionManager, UserSession}.java
    │   │       └── view/{ViewManager, LoginController, MainViewController,
    │   │       │         SectionPlaceholderController, VehicleCategoryController,
    │   │       │         VehicleCategoryViewState}.java
    │   └── resources/be/condorcet/easycarrent/desktop/
    │       ├── config/desktop.properties
    │       └── view/{login-view.fxml, main-view.fxml, section-placeholder.fxml,
    │                 vehicle-categories-view.fxml, app.css}
    └── test/
        └── java/be/condorcet/easycarrent/desktop/...
```

### Responsibilities

- **App.java** — application bootstrap: assembles the shared services and shows
  the login view through the `ViewManager`. No view logic.
- **ViewManager** — authentication-level router that swaps between the login and
  main views only.
- **MainContentRouter** — routes the central content of the main shell between
  sections (loading `vehicle-categories-view.fxml` for Vehicle Categories and the
  placeholder otherwise); it owns no Stage and performs no authentication or HTTP.
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
- **ApiConfiguration** — loads and normalizes the backend base URL.
- **ApiClient** — reusable asynchronous HTTP layer (anonymous and Basic-auth GET,
  and authenticated JSON list/POST/PUT/DELETE).
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
grows) and closes normally. Domain data, tables, and CRUD are not yet
implemented.

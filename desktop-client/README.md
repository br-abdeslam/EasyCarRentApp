# Easy Car Rent — Desktop Client

JavaFX desktop client for the Easy Car Rent application.

## Purpose

Provides the graphical desktop front end for the Easy Car Rent system. This
module is a standalone Maven project, independent from the backend.

## Current scope

The client has an MVC/FXML/CSS architecture, a reusable non-blocking HTTP and
JSON foundation, and a login workflow. It starts on a login screen, authenticates
against the backend using HTTP Basic, keeps the session in memory, and then shows
the main view with the authenticated username and role and a logout action. It
does **not** yet include the full application navigation or any domain screens
(vehicles, customers, rentals, payments, maintenance).

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
    │   │       ├── auth/{BasicCredentials, DesktopUserRole, AuthenticatedUser}.java
    │   │       ├── config/ApiConfiguration.java
    │   │       ├── dto/ApiErrorDto.java
    │   │       ├── http/{ApiClient, JsonMapperFactory}.java
    │   │       ├── http/{ApiRequestException, ApiConnectionException}.java
    │   │       ├── service/{BackendHealthService, BackendHealthResult}.java
    │   │       ├── service/{AuthenticationService, AuthenticationResult}.java
    │   │       ├── session/{SessionManager, UserSession}.java
    │   │       └── view/{ViewManager, LoginController, MainViewController}.java
    │   └── resources/be/condorcet/easycarrent/desktop/
    │       ├── config/desktop.properties
    │       └── view/{login-view.fxml, main-view.fxml, app.css}
    └── test/
        └── java/be/condorcet/easycarrent/desktop/...
```

### Responsibilities

- **App.java** — application bootstrap: assembles the shared services and shows
  the login view through the `ViewManager`. No view logic.
- **ViewManager** — minimal router that swaps between the login and main views.
- **LoginController / MainViewController** — UI events and state only.
- **ApiConfiguration** — loads and normalizes the backend base URL.
- **ApiClient** — reusable asynchronous HTTP layer (anonymous and Basic-auth).
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

The API listens on `http://localhost:8080`, and `/api/ping` is publicly
accessible.

The API listens on `http://localhost:8080`. `/api/ping` is public; `GET
/api/vehicles` (used to validate login) requires an authenticated USER or ADMIN.

## Expected screens

A resizable window titled **Easy Car Rent**:

- **Login** — username and password fields with a **Sign in** button. Invalid
  credentials or an unavailable backend keep the user on this screen with a safe
  message; the password field is cleared after each attempt.
- **Main** — after a successful login, shows the heading **Easy Car Rent**, the
  message **Desktop client initialized successfully**, a backend connectivity
  indicator (**Backend connected** / **Backend unavailable**), and a session bar
  with the authenticated username, the role (**USER** or **ADMIN**), and a
  **Log out** button that returns to the login screen.

The window can be resized and closes normally. Complete navigation and domain
screens are not yet implemented.

# Easy Car Rent — Desktop Client

JavaFX desktop client for the Easy Car Rent application.

## Purpose

Provides the graphical desktop front end for the Easy Car Rent system. This
module is a standalone Maven project, independent from the backend.

## Current scope

The client has an MVC/FXML/CSS architecture and a reusable, non-blocking HTTP
and JSON foundation. On startup it checks backend connectivity against the
public `/api/ping` endpoint and shows the result. It does **not** yet include
authentication, navigation, or any domain screens (vehicles, customers, rentals,
payments, maintenance).

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

No credentials are stored in the client; the connectivity check uses the public
`/api/ping` endpoint, which requires no authentication.

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
    │   │       ├── config/ApiConfiguration.java
    │   │       ├── dto/ApiErrorDto.java
    │   │       ├── http/ApiClient.java
    │   │       ├── http/JsonMapperFactory.java
    │   │       ├── http/ApiRequestException.java
    │   │       ├── http/ApiConnectionException.java
    │   │       ├── service/BackendHealthService.java
    │   │       ├── service/BackendHealthResult.java
    │   │       └── view/MainViewController.java
    │   └── resources/be/condorcet/easycarrent/desktop/
    │       ├── config/desktop.properties
    │       └── view/{main-view.fxml, app.css}
    └── test/
        └── java/be/condorcet/easycarrent/desktop/...
```

### Responsibilities

- **App.java** — application bootstrap: loads the FXML through `FXMLLoader`,
  applies `app.css`, and shows the primary stage. No view logic.
- **MainViewController.java** — initial view logic and UI state, including the
  asynchronous backend connectivity check.
- **ApiConfiguration** — loads and normalizes the backend base URL.
- **ApiClient** — reusable asynchronous HTTP layer.
- **JsonMapperFactory** — shared JSON mapper configuration.
- **BackendHealthService / BackendHealthResult** — the connectivity use case.
- **main-view.fxml** — defines the view layout.
- **app.css** — defines the appearance.

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

The connectivity check needs the backend running. Start it using the
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

## Expected initial screen

A resizable window titled **Easy Car Rent** displaying:

- the heading **Easy Car Rent**;
- the status message **Desktop client initialized successfully**;
- a backend connectivity indicator showing **Backend connected** when the API is
  reachable, or **Backend unavailable** when it is not.

The window can be resized and closes normally.

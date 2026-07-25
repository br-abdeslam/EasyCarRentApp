# Easy Car Rent — Desktop Client

JavaFX desktop client for the Easy Car Rent application.

## Purpose

Provides the graphical desktop front end for the Easy Car Rent system. This
module is a standalone Maven project, independent from the backend.

## Current scope

This is the initial architecture milestone. It sets up a modular JavaFX
application with an MVC/FXML/CSS structure and a single initial view. It does
**not** yet include backend HTTP calls, authentication, navigation, or any
domain screens (vehicles, customers, rentals, payments, maintenance).

## Prerequisites

- Java 25 (JDK)
- Apache Maven 3.9+

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
    │   │       └── view/MainViewController.java
    │   └── resources/
    │       └── be/condorcet/easycarrent/desktop/view/
    │           ├── main-view.fxml
    │           └── app.css
    └── test/
        └── java/be/condorcet/easycarrent/desktop/
            └── AppResourcesTest.java
```

### Responsibilities

- **App.java** — application bootstrap: loads the FXML through `FXMLLoader`,
  applies `app.css`, and shows the primary stage. No view logic.
- **MainViewController.java** — controller for the initial view logic; sets the
  status text once the view is loaded.
- **main-view.fxml** — defines the view layout (title and status labels).
- **app.css** — defines the appearance (`.app-root`, `.app-title`,
  `.app-status`).

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

## Expected initial screen

A resizable window titled **Easy Car Rent** displaying:

- the heading **Easy Car Rent**;
- the status message **Desktop client initialized successfully**.

The window can be resized and closes normally.

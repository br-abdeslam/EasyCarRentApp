# Demonstration guide

A reproducible walkthrough for demonstrating Easy Car Rent. It uses only fictional,
disposable data and cleans up afterwards. Never enter real personal data. Sign in with
the predefined local development accounts documented below; these are intentionally
public local development/demonstration credentials, not production secrets.

## 1. Prepare the environment

1. Start the database. The named Docker volume is local to each Docker host and is
   not transferred through Git, so on a fresh clone this starts an empty database:
   ```powershell
   docker compose -f database/docker-compose.yml up -d
   ```
2. Start the backend (leave it running). Choose one of:
   - **Normal startup** (does not insert any demo data):
     ```powershell
     cd backend
     .\mvnw.cmd spring-boot:run
     ```
   - **Demo startup** (inserts the fictional dataset, only when the database is
     empty) — recommended for a fresh database so every screen has content:
     ```powershell
     cd backend
     .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
     ```
   Wait until it logs that it started on port 8080. Under the demo profile the log
   also reports either that the demo data was initialized or that initialization was
   skipped because the database is not empty.
3. In a second terminal, start the desktop client:
   ```powershell
   cd desktop-client
   mvn javafx:run
   ```

### What the demo profile creates

Started against an empty database, the demo profile inserts a compact, coherent and
entirely fictional dataset through the normal business rules:

- **4 vehicle categories** (Economy, Compact, SUV, Premium);
- **8 vehicles** with `DEMO-###` registrations — several remain **AVAILABLE**, one is
  **RENTED** and one is under **MAINTENANCE**;
- **6 customers** with `@example.invalid` emails and `DEMO-LIC-###` licences (one
  customer is left free of any rental so it stays fully disposable);
- **6 rentals** covering PLANNED, ACTIVE, COMPLETED and CANCELLED, with the total
  price calculated by the backend;
- **4 payments** covering PENDING, PAID, FAILED and REFUNDED, with amounts derived by
  the backend;
- **3 maintenance records** covering PLANNED, IN_PROGRESS and COMPLETED.

The dataset intentionally leaves several vehicles available, one disposable planned
rental and one disposable planned maintenance record, so the running application
stays interactive. The demo profile only ever inserts into an empty database: if any
business data already exists it skips completely and changes nothing, and starting the
backend again with the demo profile against the same database creates no duplicates.

## 2. Authentication and shell

Two predefined local development accounts are available (HTTP Basic):

| Username | Password | Role |
| --- | --- | --- |
| `user` | `user123` | USER |
| `admin` | `admin123` | ADMIN |

These are intentionally public local development/demonstration credentials, defined in
the backend security configuration; they must never be used in production. `user` shows
the standard role and `admin` shows administrative operations.

1. On the login screen, sign in with the **USER** development account (`user`).
2. Show the header (username, role, backend-connected indicator) and the sidebar.
3. Note that the **Dashboard** opens by default.
4. Demonstrate **Log out**, then sign back in — this shows the session is cleared
   and the shell is inaccessible until authenticated.

## 3. Dashboard (read-only)

1. Point out that it loads asynchronously and has only a **Refresh** control — no
   create/edit/delete.
2. Show the headline metrics, the per-status breakdowns (every status appears,
   including zero counts), and the paid/pending/refunded payment totals.
3. Press **Refresh** and note the updated "Last updated" time.
4. Explain the partial-failure design: if one source is unavailable it shows
   **Unavailable** (not zero) while the other sections still display.

## 4. Read-only role (USER)

Sign in as **USER** and show that:

- every section lists real data;
- for Vehicle Categories, Vehicles, Customers, and Maintenance the write controls
  are hidden (a read-only notice is shown) — these are ADMIN-only;
- Rentals and Payments allow booking/creating and the shared lifecycle actions, but
  delete (and payment refund) are ADMIN-only and hidden.

## 5. Administrative role (ADMIN)

Sign out and sign in as **ADMIN**. Use only disposable fictional fixtures and follow
the safe order below.

### Categories and Vehicles

1. Create a disposable **Category** (for example name `DEMO-CATEGORY`).
2. Create a disposable **Vehicle** in that category (for example registration
   `DEMO-REG-01`, a positive daily price). Note the backend-managed status.
3. Show a validation error (for example a blank required field) and a duplicate
   conflict (a second vehicle with the same registration).

### Customers

1. Create a disposable **Customer** using clearly fictional values (for example
   email `demo.customer@example.invalid`, phone `+0000000000`, a future licence
   expiry). Do not use real personal data.
2. Show that multiple validation errors appear together below the form, and that a
   duplicate email or licence is reported as a conflict with the entered values
   kept.

### Rentals

1. Create a disposable **PLANNED** rental for the demo vehicle and customer with a
   short future period. Show that the total price is calculated by the backend.
2. Attempt a second overlapping rental on the same vehicle — show the `409` overlap
   message below the form.
3. Show that only the valid lifecycle buttons are enabled for the selected status.

### Payments (read-only-safe scenarios)

Demonstrate without leaving a permanent record:

1. With the PLANNED rental selected in the Payments editor, show that creating a
   payment is rejected (`409`) because only ACTIVE/COMPLETED rentals are payable —
   the message appears below the form and no payment is created.
2. Point out that **Refund** and **Delete** are ADMIN-only and that there is no
   payment edit (the backend has no update).

### Maintenance

1. Create a disposable **PLANNED** maintenance record for the demo vehicle (a short
   period, a non-negative cost, a description).
2. Attempt an overlapping maintenance record — show the `409` conflict.
3. Point out that **Start**/**Complete** are the only transitions and that a record
   can be deleted only while `PLANNED`.

## 6. Error and degraded states

- Show a safe validation message (`400`), an authorization message where a role
  lacks permission (`403`), a not-found message after refreshing a removed record
  (`404`), and a conflict message (`409`).
- Optionally stop the backend and press **Refresh** on a screen: the client shows a
  concise "backend unavailable" message, stays responsive, and never shows a stack
  trace. Restart the backend and refresh again.

## 7. Clean up

Remove the disposable data in dependency-safe order (only records that are still
deletable):

1. Payments (if any deletable were created);
2. Maintenance records while `PLANNED`;
3. Rentals while `PLANNED`/`CANCELLED` (cancel a PLANNED rental first if needed);
4. Customers;
5. Vehicles;
6. Categories.

A rental or maintenance record that has been started (and so cannot be deleted)
should not be created for a live demonstration; the corresponding lifecycle is
covered by the automated tests instead.

When demonstrating with the seeded dataset, prefer the disposable resources — the
available vehicles, the planned rental and the planned maintenance record — and leave
the completed and refunded history as-is; those records are retained by design and
cannot be removed.

## 8. Stop the environment

Stop the desktop client and the backend (Ctrl+C in their terminals), then stop the
database while **keeping** its data:

```powershell
docker compose -f database/docker-compose.yml down
```

`down` on its own preserves the named database volume, so the next startup keeps the
same data.

> **Warning — destructive.** Adding `-v` (`docker compose ... down -v`) deletes the
> named database volume and therefore all local data. Only use it when you
> deliberately want a clean, empty database (for example to reproduce the demo
> dataset from scratch), and understand that it cannot be undone.

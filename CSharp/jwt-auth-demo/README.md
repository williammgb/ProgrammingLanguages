# dotNetApp

A small JWT auth demo, split across three projects: a .NET Web API with login/refresh/logout endpoints, an EF Core data layer backed by PostgreSQL, and a console client that plays the part of a real user.

Logging in returns a short-lived access token (30 seconds, deliberately) plus a longer-lived refresh token (7 days). The console client logs in, calls a protected endpoint on a timer, and the moment the access token expires it trades the refresh token for a new one and carries on; the whole exchange is visible in timestamped logs as it happens.

## Tools / stack

- **.NET 10 / C#** — all three projects
- **ASP.NET Core Web API** — the `MyApp.Api` service
- **Entity Framework Core** + **Npgsql** — ORM / PostgreSQL provider
- **PostgreSQL** (via Docker) — the database
- **JWT** (`Microsoft.AspNetCore.Authentication.JwtBearer`) — access/refresh token auth

## Components

- **MyApp.Data** — class library: `AppDbContext`, EF models (`User`, `Customer`, `RefreshToken`), seed data. No entry point.
- **MyApp.Api** — the web API.
  - `AuthController` — `POST /api/auth/login|refresh|logout`
  - `ServiceController` — `GET /api/service`, `[Authorize]`-protected, the resource an access token unlocks
  - `TestController` — `GET /api/test`, public, dumps some seeded data for sanity-checking the DB
  - `SeedController` — `POST /api/seed`, inserts test users/customers (idempotent)
- **MyApp.Console** — simulates a full client lifecycle: log in → poll the protected service → on expiry, refresh → keep going. Logs everything with timestamps.

## Setup

**1. Install the .NET 10 SDK** for your OS, then verify:
```bash
dotnet --version
```

**2. Get PostgreSQL running via Docker** (install Docker Desktop first):
```bash
docker run --name postgres-db \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=myapp \
  -p 5433:5432 \
  -d postgres
```
Next time, just: `docker start postgres-db`

The connection string lives in `MyApp.Api/appsettings.json` under `ConnectionStrings:Default` — edit it there if you change the port/password/db name.

**3. Run everything:**
```powershell
./run-simulation.ps1
```
One command does the whole run: starts the `postgres-db` container, builds and starts the API on `http://localhost:5163` (its output goes to log files, not this terminal), waits for it to be reachable, POSTs `/api/seed` to insert the test users (`alice` / `bob` / `charlie`, password `Password123!`), then runs the console simulation in the foreground so its timestamped log lines print live. The API is stopped again when the simulation ends.

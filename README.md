# Narayan Staffing Services

Recruitment, staffing, candidate management, company placement and payroll for Narayan Staffing Services. The public site presents the company. The admin desk runs jobs, applications, interviews, placements, employees and monthly payroll, including PDF salary slips.

## Architecture

A modular monolith:

- `backend` — Java 21, Spring Boot 3, Spring Security, JPA, Flyway, JWT, OpenPDF
- `frontend` — React, TypeScript, Vite, Tailwind CSS
- PostgreSQL 16

Feature packages sit under `com.narayan.staffing`: identity, company, job, recruitment, workforce, payroll, cms, document, audit, dashboard, report. REST controllers return DTOs. Files go through `StorageService` (local disk now, replaceable with object storage later). Mail is optional; when it is off, notifications are stored and logged.

Sensitive values (Aadhaar, PAN, bank account) are encrypted at rest and masked in API responses unless the caller has `SENSITIVE_READ`.

## Stack

Frontend: React, TypeScript, Vite, React Router, Tailwind CSS, TanStack Query, React Hook Form, Zod, Axios, Lucide, Recharts, Framer Motion.

Backend: Java 21, Spring Boot 3.4, Spring Web, Spring Security, Spring Data JPA, Bean Validation, PostgreSQL, Flyway, JWT, Maven, springdoc OpenAPI.

## Prerequisites

- Java 21 (`JAVA_HOME` must point at JDK 21)
- Maven 3.9+
- Node.js 22+
- Docker, for PostgreSQL

## PostgreSQL

```bash
docker compose up -d postgres
```

Database `narayan_staffing`, user `narayan`, password `narayan`, port `5432`.

## Environment

Copy `.env.example`. Do not commit real secrets. The values in `.env.example` and the accounts below are **development only**.

Backend reads `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `FILE_STORAGE_PATH`, `SENSITIVE_DATA_KEY`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_ENABLED`, `CORS_ORIGINS`.

Frontend reads `VITE_API_BASE_URL`. In development the Vite server also proxies `/api` to `http://localhost:8080`, so an empty base URL works if you open the site on port 5173.

## Backend

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # adjust to your JDK 21 path
cd backend
mvn spring-boot:run
```

The `dev` profile is the default. Flyway applies `V1`–`V11` on an empty database, then Hibernate validates the schema. Swagger UI: http://localhost:8080/swagger-ui.html

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Public site: http://localhost:5173  
Admin sign-in: http://localhost:5173/admin/login

## Development accounts

Created only by the `dev` profile. **Do not use these passwords in production.**

| Email | Password | Role |
| --- | --- | --- |
| admin@narayanstaffingservices.com | Admin@12345 | SUPER_ADMIN |
| hr@narayanstaffingservices.com | Hr@12345 | HR_MANAGER |
| payroll@narayanstaffingservices.com | Payroll@12345 | PAYROLL_ADMIN |
| content@narayanstaffingservices.com | Content@12345 | CONTENT_ADMIN |

Sample companies, published jobs, one candidate and one application are inserted when the jobs table is empty. Production bootstrap creates a single admin only when `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` are set and the users table is empty. It does not load sample business data.

## Migrations

SQL lives in `backend/src/main/resources/db/migration`. On startup Flyway runs anything not yet applied. Do not edit a migration that has already run; add the next version instead.

## Commands

```bash
# backend tests
cd backend && mvn test

# frontend unit tests and production build
cd frontend && npm test && npm run build
```

## Production build

```bash
docker compose up --build
```

Compose starts Postgres, the API on port 8080, and the site on port 8088. The frontend container proxies `/api`, `/sitemap.xml` and `/robots.txt` to the API. Set `JWT_SECRET`, `SENSITIVE_DATA_KEY`, `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` before the first production start. The compose file uses the `prod` profile, which does not seed sample users.

## Business flow

Sign in, create a company, create and publish a job, apply from the careers page, move the application through shortlist and interview, select the candidate, create an employee profile, record a placement, save a salary structure, generate the monthly payroll run, then download the salary slip PDF. Placements and salary revisions are historical. A later assignment or pay change does not overwrite the earlier record.

# FinBank Teller Console (v2)

A role-aware React (Vite) frontend for the FinBank microservices, styled as a banking teller console.

## Setup
```bash
npm install
npm run dev
```
Runs at **http://localhost:5173**.

## Requirements before running
Start in order:
1. Eureka Server (8761)
2. Customer Service (8082)
3. Account Service (8081)
4. Transaction Service (8083)
5. Auth Service (8084)
6. API Gateway (8080)

Make sure API Gateway's CORS config (`CorsConfig.java`) allows `http://localhost:5173`.

## Login
Sign in with a teller or manager account registered via:
```
POST http://localhost:8080/auth/register
{ "username": "teller1", "password": "pass123", "role": "TELLER" }
```

- **TELLER** — onboard customers, verify KYC, open accounts, deposit/withdraw, reactivate accounts.
- **MANAGER** — everything above, plus freeze/close accounts.

The console hides/disables manager-only actions when signed in as a teller, and the backend also enforces this via JWT role checks — so it's not just a UI restriction.

## Design notes
Built as a "ledger" aesthetic: deep navy structure, teal action accent, monospace figures for account numbers/amounts (Space Grotesk + IBM Plex Mono), and colored left-border cards + stamp-style status pills for ACTIVE / FROZEN / CLOSED / PENDING / VERIFIED / REJECTED states.

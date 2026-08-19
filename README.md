# FinCore Nexus - Digital Banking Platform

FinCore Nexus is an event-driven Spring Boot microservices banking platform with a React customer portal and teller console. It implements the project brief's account, transaction, loan, payment, KYC, audit, and role-based access-control workflows.

## Services

| Service | Port | Responsibility |
| --- | ---: | --- |
| Eureka Server | 8761 | Service discovery |
| API Gateway | 8080 | Single client entry point and CORS |
| Account Service | 8081 | Accounts, lifecycle, applications, audit, notifications |
| Customer Service | 8082 | Customer onboarding and identity records |
| Transaction Service | 8083 | Deposits, withdrawals, statements, transaction sagas |
| Auth Service | 8084 | JWT authentication and staff administration |
| Loan Service | 8085 | Loan origination, approval, EMI schedule, collections, NPA |
| Payment Service | 8086 | Beneficiaries, IMPS/NEFT/UPI transfers, fraud and settlement |
| KYC Service | 8087 | KYC document workflow, risk review, compliance audit |
| Frontend | 5173 | Customer portal and teller/manager/admin console |

## Run locally

Prerequisites: Java 17+, Node.js 18+, Docker, and Docker Compose.

1. Start MySQL and Kafka:

   ```bash
   docker compose up -d
   ```

2. Start the backend services, each in a separate terminal, in this order:

   ```bash
   cd eureka-server && ./mvnw spring-boot:run
   cd customer-service && ./mvnw spring-boot:run
   cd auth-service && ./mvnw spring-boot:run
   cd account-service && ./mvnw spring-boot:run
   cd transaction-service && ./mvnw spring-boot:run
   cd loan-service && ./mvnw spring-boot:run
   cd payment-service && ./mvnw spring-boot:run
   cd kyc-service && ./mvnw spring-boot:run
   cd api-gateway && ./mvnw spring-boot:run
   ```

3. Start the frontend:

   ```bash
   cd finbank-frontend-v2
   npm install
   npm run dev
   ```

Open http://localhost:5173. The frontend calls the gateway at http://localhost:8080.

## First administrator

Create the one-time initial administrator before signing in:

```bash
curl -X POST http://localhost:8080/auth/bootstrap-admin \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"change-me","role":"ADMIN"}'
```

Sign in as that administrator and create teller and manager accounts in **Staff Management**. Customers can self-register from the sign-in screen.

## Configuration

The checked-in defaults work with `docker compose`. To override them, export values from `.env.example` in your shell before starting the services. Do not use the included demo passwords in a deployed environment; set a strong `JWT_SECRET` and database credentials.

## Verification

```bash
cd finbank-frontend-v2 && npm run build
for service in auth-service customer-service account-service transaction-service loan-service payment-service kyc-service api-gateway eureka-server; do
  (cd "$service" && ./mvnw test)
done
```

The platform enforces JWT-backed customer/teller/manager/admin permissions in each service. Account and loan decisions are audited; transaction, payment, and loan flows persist saga logs and publish Kafka events.

## KYC demo boundary

The KYC screen captures a document selection and a live browser selfie with consent, then records that both evidence steps were completed. The OCR, face-match, liveness, and risk scores are explicitly simulated for this project. A production KYC implementation requires encrypted object storage, retention/deletion controls, and an approved identity-verification provider; it must not treat the demo scores as real identity proof.

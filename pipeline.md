# MedSys – Testing & CI/CD Overview (Milestone 5)

This document describes how we **plan to test** the MedSys microservices and how a simple **CI/CD pipeline** can automatically build, test, and run them in Docker.

Scope:

- **Services**: `auth-service`, `appointments-service`, `notification-service`
- **Infrastructure**: `medsys-mariadb`, `medsys-rabbitmq` (via `docker-compose.yml`)
- **Messaging**: RabbitMQ events from `appointments-service` → `notification-service`

---

## 1. Testing Strategy

### 1.1 Goals

- Verify the **core business logic** of each service.
- Verify **REST APIs** end-to-end (including DB and security where needed).
- Verify the **RabbitMQ messaging flow** (appointments → notifications) using the Docker environment.

### 1.2 Types of Tests

#### 1.2.1 Unit tests (inside each microservice)

**What to test**

- `auth-service`
  - Registration & login logic in the service layer (password hashing, unique email/username).
  - JWT generation / validation helper logic (if any).

- `appointments-service`
  - Creation / cancellation / decision logic in `AppointmentServiceImpl`.
  - Status transitions: `PENDING → ACCEPTED / DENIED / CANCELLED`.
  - Validation rules (doctor/patient usernames, dates in the future, etc.).

- `notification-service`
  - Notification creation in `NotificationServiceImpl`.
  - Mapping from RabbitMQ `AppointmentEvent` → `Notification` entities.
  - Marking notifications as READ.

**How**

- Use **JUnit 5** + **Mockito**:
  - Mock repositories (`UserRepository`, `AppointmentRepository`, `NotificationRepository`).
  - Focus on service methods without touching the database.

**Example commands**

Run inside each service folder:

```bash
cd auth-service
mvn test

cd ../appointments-service
mvn test

cd ../notification-service
mvn test
````

---

#### 1.2.2 Integration tests (REST + DB)

**What to test**

* Spring Boot integration tests that hit:

    * REST controllers → services → JPA repositories → MariaDB (or Testcontainers).
* Typical scenarios:

    * `auth-service`: `POST /auth/register`, `POST /auth/login`
    * `appointments-service`: `POST /appointments`, `POST /appointments/{id}/decision`, `POST /appointments/{id}/cancel`
    * `notification-service`: fetch notifications for a user (`GET /notifications/mine` or similar)

**How**

* Use `@SpringBootTest` + `@AutoConfigureMockMvc`.
* Use a separate `application-test.yml`:

    * either with an in-memory DB (H2) using a similar schema,
    * or with a **Testcontainers MariaDB** instance 

**Commands**

Same as above, but now integration tests are included:

```bash
cd auth-service
mvn verify

cd ../appointments-service
mvn verify

cd ../notification-service
mvn verify
```

---

#### 1.2.3 Manual end-to-end / messaging testing (Postman + Docker)

We used the **Docker Compose environment** and **Postman** to test the full flow including RabbitMQ:

1. Start the stack:

   ```bash
   docker compose up
   ```

   This starts:

    * `auth-service` on `8081`
    * `appointments-service` on `8082`
    * `notification-service` on `8083`
    * `medsys-mariadb`
    * `medsys-rabbitmq` (RabbitMQ UI on `localhost:15672`)

2. **Register & login patient**

    * `POST http://localhost:8081/auth/register` (role = `PATIENT`)
    * `POST http://localhost:8081/auth/login` → copy JWT token (`token` field)

3. **Register doctor**

    * `POST http://localhost:8081/auth/register` (role = `DOCTOR`), e.g. `doctor_mq`

4. **Create appointment (triggers APPOINTMENT_CREATED event)**

    * `POST http://localhost:8082/appointments`
    * Headers: `Authorization: Bearer <patient_token>`, `Content-Type: application/json`
    * Body example:

      ```json
      {
        "doctorUsername": "doctor_mq",
        "startTime": "2025-12-10T10:00:00",
        "endTime": "2025-12-10T10:30:00",
        "scheduleReason": "RabbitMQ demo appointment"
      }
      ```

   **Expected logs**

    * In `appointments-service`:

      ```text
      AppointmentEventPublisher : Publishing appointment event: type=APPOINTMENT_CREATED, id=..., patient=..., doctor=...
      ```
    * In `notification-service`:

      ```text
      AppointmentEventsListener : Received appointment event: type=APPOINTMENT_CREATED, appointmentId=..., patient=..., doctor=...
      Notification persisted for patient ...
      ```

5. **Doctor accepts appointment (triggers APPOINTMENT_ACCEPTED event)**

    * Login as doctor → get JWT.
    * `POST http://localhost:8082/appointments/{id}/decision`
    * Headers: `Authorization: Bearer <doctor_token>`
    * Body example:

      ```json
      { "decision": "ACCEPT" }
      ```

   **Expected logs**

    * `appointments-service` publishes `APPOINTMENT_ACCEPTED`.
    * `notification-service` logs a new “Notification persisted…” line for that patient.

6. (Optional) **Fetch notifications**

    * Call the notification REST endpoint (e.g. `GET /notifications/mine`) with the patient token and verify new notifications are present.

This proves that **two services communicate asynchronously** via the RabbitMQ queue and that the system is **decoupled** at the integration level.

---

## 2. CI/CD Pipeline Overview

### 2.1 Goals 

* On each push / PR, automatically:

    * **Build** all three microservices.
    * **Run tests** (at least unit + basic integration tests).
* Optionally:

    * **Build Docker images** for each service.
    * **Run `docker compose`** (on a runner or local dev machine) to simulate a deployment to a local Docker environment.

---

### 2.2 Local “mini pipeline”

For developers, we already have a manual pipeline:

```bash
# 1. Build & test each service
cd auth-service && mvn clean verify
cd ../appointments-service && mvn clean verify
cd ../notification-service && mvn clean verify

# 2. Build Docker images (example tags)
docker build -t medsys/auth-service:local ./auth-service
docker build -t medsys/appointments-service:local ./appointments-service
docker build -t medsys/notification-service:local ./notification-service

# 3. Run the whole system
docker compose up
```

This is essentially **CD on a laptop** using Docker Compose.

---

### 2.3 GitHub Actions CI (example)

We can define a simple CI pipeline in `.github/workflows/medsys-ci.yml`:

```yaml
name: CI - MedSys Microservices

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout sources
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build & test auth-service
        run: |
          cd auth-service
          mvn -B clean verify

      - name: Build & test appointments-service
        run: |
          cd appointments-service
          mvn -B clean verify

      - name: Build & test notification-service
        run: |
          cd notification-service
          mvn -B clean verify

      - name: Build Docker images (optional, for CD)
        run: |
          docker build -t medsys/auth-service:${{ github.sha }} ./auth-service
          docker build -t medsys/appointments-service:${{ github.sha }} ./appointments-service
          docker build -t medsys/notification-service:${{ github.sha }} ./notification-service
```



* **Optional**:

    * We can also build Docker images in CI (proving the services are containerizable).

    * With a self-hosted runner or dedicated dev VM, we could add a second job:

      ```yaml
      deploy-local:
        needs: build-and-test
        runs-on: self-hosted
        steps:
          - uses: actions/checkout@v4
          - name: Pull images and run docker compose
            run: |
              docker compose down || true
              docker compose up -d --build
      ```

    * This would **deploy to a local Docker environment** after tests pass.



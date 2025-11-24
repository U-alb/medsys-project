# Milestone 3 — Architecture Options & Recommendation (MedSys)

**Project:** MedSys — clinic management system (roles: Patient, Doctor, Admin; features: auth, appointments with conflict policies, medical records, prescriptions, notifications, optional billing)  
**Goal:** Investigate, describe, and evaluate **three different architecture styles** for MedSys, then **select the most suitable** and justify.

---

## Context & Quality Attributes

**Key flows**
- **Book/modify/cancel appointment** with policy checks (doctor availability, room clashes, cooldowns, telemedicine vs. in-person).
- **Manage patient record** (encounters, notes, attachments), **prescriptions** .
- **Notify** users via email/SMS/push (booking confirmations, reminders, cancellations).
- **Admin**: user onboarding, roles, audit, configuration.

**Quality attributes we care about (priority)**
1. **Data integrity & consistency** (appointments/records must not conflict)
2. **Security & compliance** (least privilege, auditing, PII protection)
3. **Evolvability** (we will iterate features frequently)
4. **Availability & resilience** (don’t lose appointments; graceful degradation)
5. **Performance & scalability** (clinic-load spikes: 09:00 booking wave, seasonal peaks)
6. **Cost & operational simplicity** (student team; limited ops time/budget)

We evaluate each architecture against these attributes and the concrete MedSys flows.

---

## 1) Monolithic Architecture (Layered, Modular)

### 1.1 Structure (components, interactions, data flow)
- **Single deployable** (e.g., Spring Boot) with **modular packages**:
    - `auth & users`, `patients`, `doctors`, `appointments`, `records`, `prescriptions`, `notifications`, `admin`.
- **Interaction**: HTTP → Controller → Service → Repository → **one relational DB** 
- **Data flow (example: booking)**:  
  `POST /appointments` → AppointmentService (policy checks & transaction) → write rows (appointments, locks/holds) → commit → send email via an internal NotificationService call.

### 1.2 Deployment (textual view)
- **1 x App** container/VM/pod (stateless) + **1 x DB** (MariaDB).

### 1.3 Pros
- **Simplicity**: one codebase, one process, **one ACID transaction** covers cross-module changes (great for **conflict-free booking** and **record + prescription** updates).
- **Low operational overhead**: minimal infra, easy to run locally (helpful for a small team).
- **Uniform cross-cutting**: logging, auth, validation in one place.
- **Fast initial velocity**: fewer moving parts, fewer integration points.

### 1.4 Cons
- **Limited independent scaling**: notifications spike can’t scale without scaling everything.
- **Change coupling**: broad redeploys; long build/test times as codebase grows.
- **Resilience**: a bad GC pause or memory leak can affect **all** features.
- **Security blast radius**: app process is an all-access gateway to the DB; harder to enforce **least-privilege** per bounded context.
- **Team friction** once multiple people modify the same module boundaries.

### 1.5 Fit for MedSys
- **Good** for early prototypes and labs; **excellent transactional integrity** for appointments.
- **Less ideal long-term**: notifications, records, and booking will grow unevenly; different performance profiles and compliance boundaries argue for stronger isolation than a monolith provides.

---

## 2) Microservices Architecture

### 2.1 Structure (components, interactions, data flow)
Split into **bounded contexts**, each with its **own database** (“DB-per-service”):
- **Identity & Access** (Auth,  roles) — _MariaDB A_
- **Patient Registry** (demographics) — _MariaDB B_
- **Doctor & Resource Management** (schedules, rooms) — _MariaDB C_
- **Appointments** (booking engine, conflict policies) — _MariaDB D_
- **Medical Records** (encounters, notes, attachments) — _MariaDB E_
- **Prescriptions** — _MariaDB F_
- **Notifications** (email/SMS/push) — _MariaDB + templates_
- **API Gateway / (single entry for web), **Observability** (logs/metrics/traces)

**Interactions**
- **Synchronous**: REST/gRPC for “command” paths (e.g., Gateway → Appointments).
- **Asynchronous**: publish **domain events** (e.g., `AppointmentBooked`) to a broker; consumers (Notifications, Records projections) react independently.

**Data flow (example: booking)**
1) Client → **Gateway** → **Appointments Service** (`POST /book`)
2) Appointments checks doctor/room slots, creates booking (ACID within service DB).
3) Emits **`AppointmentBooked`** → broker.
4) **Notifications** consumes the event and sends email/SMS; **Records** may project it into a patient timeline.

### 2.2 Deployment (textual view)
- **N services** (Docker/Kubernetes), **service discovery**, **API gateway**, **message broker** (Kafka/RabbitMQ), **per-service DBs**, central **observability** (Grafana/Tempo/Loki or ELK), **secrets** manager.

### 2.3 Pros
- **Independent scaling & deployments**: scale **Notifications** during peaks without touching **Records**.
- **Fault isolation**: Records outage shouldn’t block booking; events replay later.
- **Finer-grained security**: **least-privilege** DB access per service; easier compliance/audit boundaries.
- **Tech heterogeneity** where needed (e.g., Java for booking, Python worker for NLP on notes).
- **Team autonomy**: smaller repos, clearer ownership.

### 2.4 Cons
- **Operational complexity**: service discovery, gateway, CI/CD, secrets, tracing, **more infra cost**.
- **Distributed data & consistency**: cross-service transactions become **sagas**; need **idempotency**, **outbox**, **retries**. Some workflows are **eventually consistent**.
- **Testing complexity**: contract tests, test environments, local dev ergonomics.
- **Data duplication**: read models/projections must be kept in sync.

### 2.5 Fit for MedSys
- **Strong fit long-term**: booking engine is a natural core service; **Notifications** and **Records** have independent scaling. Compliance benefits from **per-service data ownership**. We can keep **strict consistency inside Appointments** and use **events** for everything else (reminders, read models), minimizing cross-service transactions.

---

## 3) Event-Driven Architecture (EDA, CQRS-first)

> Distinct from #2 by making **events the primary integration mechanism** and leaning heavily on **CQRS** and **materialized views**. Services are **loosely coupled** and communicate predominantly through **immutable events**; synchronous calls are minimized.

### 3.1 Structure (components, interactions, data flow)
- **Command services**: Appointments, Records, Prescriptions, etc. publish events like `AppointmentRequested`, `AppointmentBooked`, `RecordUpdated` after local commits.
- **Event bus** (Kafka/RabbitMQ) as the backbone.
- **Query services / read models** build **materialized views** (e.g., “Patient Dashboard View” combining records + upcoming appointments).
- **Schedulers/Policies** may be modeled as event processors (e.g., **conflict detection** outputting `BookingRejected`).

**Data flow (example: booking)**
- Client sends **command** → Booking service validates locally; emits `AppointmentBooked` or `BookingRejected`.
- **Every consumer** updates its read model. **UI queries** fast, denormalized views.

### 3.2 Deployment (textual view)
- Multiple command/read services + **event bus**, **schema registry** for event contracts, **storage per read model** (often NoSQL).

### 3.3 Pros
- **High decoupling**: teams ship independently; adding new reactions is just “new consumer + new view”.
- **Read performance**: materialized views are tailor-made for screens (e.g., patient timeline).
- **Auditability** with event streams; **time-travel** possible when combined with event sourcing.
- **Resilience** via replay & backpressure controls on the bus.

### 3.4 Cons
- **Modeling complexity**: designing clear **event contracts** and **versioning** is hard.
- **Strong eventual consistency** everywhere; **user-visible delays** if consumers lag.
- **Data explosion** (streams + multiple read models) and **operational cost** (brokers at scale).
- **Debuggability**: tracing cause→effect across topics requires solid tooling & discipline.

### 3.5 Fit for MedSys
- Great for **analytics & dashboards** and loosely coupled features (notifications, reminders, “what’s new” feeds).
- **But** core **booking conflict resolution** and **sensitive medical record edits** benefit from immediate consistency and simpler reasoning. A **pure EDA** approach would over-complicate critical paths for our current scope.

---

## 4) Side-by-side Comparison

| Criterion | Monolith | Microservices | Event-Driven (CQRS-first) |
|---|---|---|---|
| **Dev speed (initial)** | **High** | Medium | Low-Medium (event design first) |
| **Operational complexity** | **Low** | High | **Highest** (broker, schema registry, consumers) |
| **Data consistency (single flow)** | **Strong (ACID)** | Strong inside a service | Strong locally; **eventual** across views |
| **Cross-feature consistency** | High (single DB) | **Challenging** (sagas/outbox) | **Challenging** (eventual) |
| **Scalability** | Medium (scale whole app) | **High** (scale hot services) | **High** (scale producers/consumers) |
| **Fault isolation** | Low | **Good** | **Good**, replayable |
| **Security & compliance** | Coarse-grained | **Fine-grained** per service | Fine-grained but complex |
| **Team autonomy** | Low-Medium | **High** | High |
| **Cost (small deployment)** | **Lowest** | Medium-High | High |
| **UX freshness / read perf** | Good | Good | **Great** (materialized views) |

---

## 5) Recommendation (and why microservices for MedSys)

**Recommendation:** adopt **Microservices (#2)** as the **target architecture** for MedSys.

**Why** (tailored to our project):
1. **Clear bounded contexts** exist (Appointments, Records, Notifications, Identity). Each has different **load shape** and **compliance boundary**, benefiting from **separate scaling** and **least-privilege data access**.
2. **Critical consistency** can remain **strict** _inside_ the **Appointments** service (single-service ACID), while **non-critical fan-out** (emails, reminders, dashboards) uses **asynchronous events**. This keeps the **booking path simple and reliable** without a web of cross-service transactions.
3. **Team flow & evolvability**: services can be owned by sub-teams; independent deploys reduce coordination cost as the codebase grows.
4. **Observability & resilience**: faults in Notifications shouldn’t block booking; events are retryable.
5. **Future features** (e.g., external e-prescription API, payments) can be integrated as **new services** without destabilizing the core booking/records services.

**Why not Monolith (#1):** excellent to start, but medium-term it **limits scaling and compliance isolation**. A single DB with broad access is at odds with **least-privilege** and **audit** goals.

**Why not pure EDA (#3):** overkill for the **critical transactional core**; designing everything as events increases cognitive/ops load and may **degrade UX** due to eventual consistency in essential flows (booking edits, record updates). We’ll **use events selectively**, not as the only integration style.

---

## 6) Pragmatic Rollout Plan

1. **Gateway + Core Services first**: API Gateway/BFF, Identity, **Appointments**, **Notifications** (async). Keep **Records** as a module or a service depending on scope.
2. **Event infrastructure**: message broker, outbox pattern, idempotent consumers, basic schema governance.
3. **Observability**: tracing (propagate request/trace IDs), structured logs, metrics & alerts.
4. **Data**: DB-per-service, migrations per service, nightly backups, retention policies for PII.
5. **Security**: per-service credentials, least privilege, audit logging, encrypted secrets.
6. **Incremental decomposition**: if starting from a monolith, carve out Notifications → Appointments → Records using the **strangler** pattern.

---
.

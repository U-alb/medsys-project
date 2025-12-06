# Milestone 5 – Message Queue Integration (Appointments ↔ Notifications)

## 1. Overview

For **Milestone 5** we integrated a **RabbitMQ** message queue to enable **asynchronous communication** between:

* `appointments-service` – publishes appointment domain events.
* `notification-service` – consumes those events and stores user notifications in its own database.

This satisfies the requirement of “using a message queue for asynchronous communication between at least two services” and demonstrates **decoupling, scalability and basic fault tolerance**.

Currently we handle these appointment lifecycle events:

* `APPOINTMENT_CREATED`
* `APPOINTMENT_ACCEPTED`
* `APPOINTMENT_REJECTED`
* `APPOINTMENT_CANCELLED`

We explicitly tested flows for **created** and **accepted** events end-to-end.

---

## 2. Architecture & Routing

### 2.1 Components

* **RabbitMQ broker** (Docker service `rabbitmq`).
* **appointments-service**

    * Owns the `Appointment` aggregate and business rules.
    * Publishes `AppointmentEvent` messages to RabbitMQ.
* **notification-service**

    * Listens to appointment events.
    * Creates `Notification` entities in its own DB.

### 2.2 Exchange, Queue & Routing Key

Both services share the same RabbitMQ topology (configured via Spring AMQP):

* **Exchange** (topic): `medsys.appointments.exchange`
* **Queue**: `medsys.notifications.queue`
* **Routing key**: `appointments.notifications`

`appointments-service` **publishes** events to the exchange with that routing key, and `notification-service` **binds** its queue to the same exchange + routing key.

This means the appointments service never calls the notifications service directly; RabbitMQ sits in between them.

---

## 3. Event Model

### 3.1 Payload: `AppointmentEvent`

Publisher and consumer communicate using a strongly-typed JSON payload:

```java
public class AppointmentEvent {
    private AppointmentEventType type;
    private Long appointmentId;
    private String patientUsername;
    private String doctorUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String scheduleReason;
}
```

This contains just enough data for downstream services to build user-friendly messages, without leaking internal appointment implementation details.

### 3.2 Event Types

`AppointmentEventType` models the lifecycle of an appointment:

* `APPOINTMENT_CREATED` – emitted when a patient successfully books a new appointment.
* `APPOINTMENT_ACCEPTED` – emitted when a doctor accepts a pending appointment.
* `APPOINTMENT_REJECTED` – emitted when a doctor rejects a pending appointment.
* `APPOINTMENT_CANCELLED` – emitted when a patient cancels a pending/accepted future appointment.

On the notification side we use a parallel enum `NotificationType` (`APPOINTMENT_CREATED`, `APPOINTMENT_ACCEPTED`, `APPOINTMENT_DENIED`, `APPOINTMENT_CANCELLED`) to classify notifications.

---

## 4. Producer Side – `appointments-service`

### 4.1 RabbitMQ Configuration

In `appointments-service` we define:

* **Durable topic exchange** `medsys.appointments.exchange`
* **Durable queue** `medsys.notifications.queue`
* Binding with routing key `appointments.notifications`
* A `Jackson2JsonMessageConverter` and a `RabbitTemplate` that uses it.

This ensures:

* Messages are **JSON**.
* Queue/exchange are **durable** (survive broker restarts).

### 4.2 Publisher: `AppointmentEventPublisher`

`AppointmentEventPublisher` is a small service that translates domain objects into events and sends them to RabbitMQ:

* Builds an `AppointmentEvent` from an `Appointment`.
* Logs the action.
* Calls `rabbitTemplate.convertAndSend(APPOINTMENTS_EXCHANGE, NOTIFICATIONS_ROUTING_KEY, event);`

It exposes clear semantic methods:

* `publishCreated(Appointment appointment)`
* `publishAccepted(Appointment appointment)`
* `publishRejected(Appointment appointment)`
* `publishCancelled(Appointment appointment)`

### 4.3 Where Events Are Emitted

Events are emitted from the **application service layer**:

* `AppointmentServiceImpl.create(...)`

    * Validates time constraints and business rules.
    * Persists the new `Appointment`.
    * Calls `eventPublisher.publishCreated(saved);`
* `AppointmentServiceImpl.decideStatus(...)`

    * Doctor accepts or rejects a pending appointment.
    * Persists the new status.
    * If `ACCEPTED` → `publishAccepted(saved);`
    * If `DENIED` → `publishRejected(saved);`
* `AppointmentServiceImpl.cancel(...)`

    * Patient cancels their own future appointment.
    * Persists status `CANCELLED`.
    * Calls `publishCancelled(saved);`

This keeps domain logic and integration logic separate, and ensures every important state change emits an event *once the transaction succeeds*.

---

## 5. Consumer Side – `notification-service`

### 5.1 RabbitMQ Configuration

`notification-service` defines the **same** exchange, queue and routing key in its own `RabbitConfig`, so it can be started independently and still declare/bind the necessary infrastructure:

* Topic exchange `medsys.appointments.exchange`
* Queue `medsys.notifications.queue`
* Binding with `appointments.notifications`
* JSON message converter + RabbitTemplate (for potential outgoing messages later).

### 5.2 Notification Domain

The notification domain is completely local to `notification-service`:

* `Notification` JPA entity with:

    * `recipientUsername`
    * `title`
    * `message`
    * `type` (`NotificationType`)
    * `relatedAppointmentId`
    * `status` (`UNREAD` / `READ`)
    * `createdAt`, `readAt`
* `NotificationRepository` – Spring Data JPA (queries by recipient + status, count unread, etc.).
* `NotificationService` / `NotificationServiceImpl` – application layer to create and manage notifications.

The **Rabbit listener** (e.g. `AppointmentEventsListener`) receives `AppointmentEvent` objects and maps them to a `NotificationCreateDTO`, then calls `notificationService.create(dto)`.

For example (conceptually):

* `APPOINTMENT_CREATED` → notification to the **patient**:
  “Your appointment with doctor X on Y was created (status: PENDING).”
* `APPOINTMENT_ACCEPTED` → notification to the **patient**:
  “Your appointment with doctor X was accepted.”
* `APPOINTMENT_REJECTED` → notification to the **patient** explaining rejection.
* `APPOINTMENT_CANCELLED` → notification to the **doctor** (or patient) that the appointment was cancelled.

---

## 6. Why This Improves Scalability, Decoupling & Fault Tolerance

### 6.1 Decoupling

* `appointments-service` never calls `notification-service` directly (no REST call, no hard dependency).
* It only depends on RabbitMQ and the **event contract** (`AppointmentEvent` + `AppointmentEventType`).
* `notification-service` can evolve independently (own DB schema, business rules, and even be replaced) as long as it still understands the events.

### 6.2 Scalability

* When many appointments are created (e.g., peak hours), events are **buffered** in RabbitMQ.
* `notification-service` can scale horizontally by adding more instances consuming from the same queue.
* Producers and consumers can scale independently based on their own load.

### 6.3 Fault Tolerance

* If `notification-service` is **down**:

    * `appointments-service` still works and continues emitting events.
    * Messages stay in the durable queue until the consumer comes back.
* If RabbitMQ briefly disconnects:

    * Spring’s `CachingConnectionFactory` automatically reconnects once the broker is available.
* If notification processing fails, messages can be re-delivered based on Spring AMQP’s error handling / retry strategies (configurable).

---

## 7. How We Tested the Two Messaging Flows

Below is the **end-to-end test scenario** we ran using Postman and `docker-compose logs` to prove the integration works.

### Prerequisites

* All services are running via Docker Compose:

    * `auth-service`
    * `appointments-service`
    * `notification-service`
    * `rabbitmq`
* Databases are migrated and reachable.

### 7.1 Flow 1 – “Appointment created” → Patient notification

1. **Register a patient** (auth-service)

    * `POST http://localhost:8081/auth/register`
    * Body:

      ```json
      {
        "username": "patient_mq",
        "email": "patient_mq@example.com",
        "password": "Test1234!",
        "dateOfBirth": "2000-01-01",
        "role": "PATIENT"
      }
      ```

2. **Login as patient** and copy the JWT (`token` field)

    * `POST http://localhost:8081/auth/login`
    * Body:

      ```json
      {
        "username": "patient_mq",
        "password": "Test1234!"
      }
      ```
    * Copy `token` and use it as `Authorization: Bearer <token>` in the next steps.

3. **Register a doctor** (once)

    * `POST http://localhost:8081/auth/register`
    * Body:

      ```json
      {
        "username": "doctor_mq",
        "email": "doctor_mq@example.com",
        "password": "Test1234!",
        "dateOfBirth": "1980-01-01",
        "role": "DOCTOR",
        "spec": "Cardiology",
        "licenseNumber": "DOC-MQ-123"
      }
      ```

4. **Create an appointment as the patient** (this publishes `APPOINTMENT_CREATED`)

    * `POST http://localhost:8082/appointments`
    * Headers:

        * `Authorization: Bearer <patient_token>`
        * `Content-Type: application/json`
    * Body:

      ```json
      {
        "doctorUsername": "doctor_mq",
        "startTime": "2025-12-10T10:00:00",
        "endTime": "2025-12-10T10:30:00",
        "scheduleReason": "RabbitMQ demo appointment"
      }
      ```

5. **Check logs**

    * In `appointments-service` logs you should see something like:

      > Publishing appointment event: type=APPOINTMENT_CREATED, id=…, patient=patient_mq, doctor=doctor_mq
    * In `notification-service` logs:

      > Received appointment event: type=APPOINTMENT_CREATED, appointmentId=…, patient=patient_mq, doctor=doctor_mq
      > Notification persisted for patient patient_mq

6. **Verify notifications via API (optional)**

    * `GET http://localhost:<notifications-port>/notifications/mine`
    * `Authorization: Bearer <patient_token>`
    * You should see at least one notification related to the created appointment.

### 7.2 Flow 2 – “Appointment accepted” → Patient notification

Assuming you already created an appointment with ID `2` (for example):

1. **Login as doctor**

    * `POST http://localhost:8081/auth/login` with:

      ```json
      {
        "username": "doctor_mq",
        "password": "Test1234!"
      }
      ```
    * Copy the `token`.

2. **Doctor accepts the appointment** (this publishes `APPOINTMENT_ACCEPTED`)

    * `POST http://localhost:8082/appointments/2/decision`
    * Headers:

        * `Authorization: Bearer <doctor_token>`
        * `Content-Type: application/json`
    * Body:

      ```json
      {
        "decision": "ACCEPT"
      }
      ```

3. **Check logs**

    * In `appointments-service`:

      > Publishing appointment event: type=APPOINTMENT_ACCEPTED, id=2, patient=patient_full, doctor=doctor_mq
    * In `notification-service`:

      > Received appointment event: type=APPOINTMENT_ACCEPTED, appointmentId=2, patient=patient_full, doctor=doctor_mq
      > Notification persisted for patient patient_full

4. **Check notifications**

    * `GET /notifications/mine` as the patient or doctor (depending on how you configured the recipient) to see the stored notification.

These two flows demonstrate **both** directions we care about:

* **Booking** → `APPOINTMENT_CREATED` → notification.
* **Doctor decision** → `APPOINTMENT_ACCEPTED` → notification.


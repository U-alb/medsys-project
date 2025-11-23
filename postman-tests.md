# Postman Tests — MedSys Microservices

This document describes how we tested the **auth-service**, **appointments-service**, and **notification-service** using Postman.


---

## 0. Setup

### Services and ports

Make sure all three services are running:

* **auth-service** → `http://localhost:8081`
* **appointments-service** → `http://localhost:8082`
* **notification-service** → `http://localhost:8083`

Each service is a separate Spring Boot app with its own database schema.

### Postman basic conventions

In Postman:

* Always set `Content-Type: application/json` for JSON requests with a body.
* For protected endpoints, set:

  ```text
  Authorization: Bearer <JWT_TOKEN_HERE>
  ```

We’ll call the tokens:

* `PATIENT_TOKEN` → JWT obtained by logging in as `patient1`
* `DOCTOR_TOKEN` → JWT obtained by logging in as `doctor1`
* (`ADMIN_TOKEN` optional, if you have an admin user)

---

## 1. Auth-Service Tests

**Base URL:** `http://localhost:8081`

### 1.1. Register a new user

**Request**

* Method: `POST`
* URL: `http://localhost:8081/auth/register`
* Headers:

    * `Content-Type: application/json`
* Body (JSON example):

  ```json
  {
    "username": "patient1",
    "email": "patient1@example.test",
    "password": "password",
    "role": "PATIENT"
  }
  ```

**Expected result**

* Status: `201 Created` (or `200 OK`, depending on implementation)
* Body: basic user info (id, username, email, role, without password).

---

### 1.2. Login and get JWT token

We do this for both `patient1` and `doctor1`.

**Request (patient1)**

* Method: `POST`
* URL: `http://localhost:8081/auth/login`
* Headers:

    * `Content-Type: application/json`
* Body:

  ```json
  {
    "username": "patient1",
    "password": "password"
  }
  ```

**Expected result**

```json
{
  "id": 1,
  "username": "patient1",
  "email": "patient1@example.test",
  "role": "PATIENT",
  "token": "<PATIENT_TOKEN>"
}
```

Copy the `token` value (without quotes) and save it as **PATIENT_TOKEN**.

**Request (doctor1)**

Same as above, but:

```json
{
  "username": "doctor1",
  "password": "password"
}
```

You get a similar response with `"role": "DOCTOR"` → copy that token as **DOCTOR_TOKEN**.

---

### 1.3. Check current user (`/auth/me`)

**Request**

* Method: `GET`
* URL: `http://localhost:8081/auth/me`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`

**Expected result** (example)

```json
{
  "authenticated": true,
  "username": "patient1",
  "role": "PATIENT"
}
```

Same works with `DOCTOR_TOKEN`, returning `"doctor1"` and `"DOCTOR"`.

---

## 2. Appointments-Service Tests

**Base URL:** `http://localhost:8082`
Requires valid JWT from auth-service.

### 2.1. Create an appointment (patient)

**Request**

* Method: `POST`
* URL: `http://localhost:8082/appointments`
* Headers:

    * `Content-Type: application/json`
    * `Authorization: Bearer <PATIENT_TOKEN>`
* Body (example in the future):

  ```json
  {
    "doctorUsername": "doctor1",
    "startTime": "2025-12-01T10:00:00",
    "endTime": "2025-12-01T10:30:00",
    "scheduleReason": "Routine check"
  }
  ```

**Expected result**

```json
{
  "id": 5,
  "patientUsername": "patient1",
  "doctorUsername": "doctor1",
  "startTime": "2025-12-01T10:00:00",
  "endTime": "2025-12-01T10:30:00",
  "status": "PENDING",
  "scheduleReason": "Routine check"
}
```

Save the `id` → we call it `APPOINTMENT_ID`.

---

### 2.2. List my appointments (patient)

**Request**

* Method: `GET`
* URL: `http://localhost:8082/appointments/mine`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`

**Expected result**

JSON array of appointments for `patient1`, including the one you just created.

---

### 2.3. List appointments for doctor

**Request**

* Method: `GET`
* URL: `http://localhost:8082/appointments/doctor/me`
* Headers:

    * `Authorization: Bearer <DOCTOR_TOKEN>`

**Expected result**

All appointments where `doctorUsername = "doctor1"` (may be empty at the very beginning).

---

### 2.4. Doctor decides (accept / reject)

#### Accept example

**Request**

* Method: `POST`
* URL: `http://localhost:8082/appointments/{id}/decision`
  Example: `http://localhost:8082/appointments/5/decision`
* Headers:

    * `Content-Type: application/json`
    * `Authorization: Bearer <DOCTOR_TOKEN>`
* Body:

  ```json
  {
    "decision": "ACCEPT"
  }
  ```

**Expected result**

Appointment with updated status:

```json
{
  "id": 5,
  "patientUsername": "patient1",
  "doctorUsername": "doctor1",
  "status": "ACCEPTED",
  ...
}
```

#### Reject example

Same endpoint, but with:

```json
{
  "decision": "REJECT"
}
```

Result: `"status": "DENIED"`.

---

### 2.5. Cancel appointment (patient)

Patient can cancel their **own** appointment if it is `PENDING` or `ACCEPTED` and in the future.

**Request**

* Method: `POST`
* URL: `http://localhost:8082/appointments/{id}/cancel`
  Example: `http://localhost:8082/appointments/5/cancel`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`
* Body: none

**Expected result**

```json
{
  "id": 5,
  "status": "CANCELLED",
  ...
}
```

If the rules are violated (e.g., past appointment), you get `409 Conflict` with a message like:

```json
{
  "message": "Cannot cancel past appointments.",
  "status": 409,
  "error": "Conflict"
}
```

---

### 2.6. Filter by status

You can filter the list of appointments for a patient by status.

**Request**

* Method: `GET`
* URL: `http://localhost:8082/appointments/mine?status=ACCEPTED`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`

**Expected result**

Only appointments with `"status": "ACCEPTED"` for that user.

Notes:

* `status` must match one of the enum values (`PENDING`, `ACCEPTED`, `DENIED`, `CANCELLED`).
* If the value is invalid, you get `400 Bad Request` with an error message.

---

## 3. Notification-Service Tests

**Base URL:** `http://localhost:8083`
Also protected by JWT via the same secret.

### 3.1. View my notifications

**Request**

* Method: `GET`
* URL: `http://localhost:8083/notifications/mine`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`
      (or `<DOCTOR_TOKEN>` to see doctor notifications)

**Expected result**

```json
[
  {
    "id": 1,
    "recipientUsername": "patient1",
    "title": "Appointment accepted",
    "message": "Your appointment with doctor doctor1 on 2025-12-01T10:00 was accepted.",
    "type": "APPOINTMENT_ACCEPTED",
    "status": "UNREAD",
    "relatedAppointmentId": 5,
    "createdAt": "2025-11-23T...",
    "readAt": null
  }
]
```

---

### 3.2. Filter my notifications by status

**Request**

* Method: `GET`
* URL: `http://localhost:8083/notifications/mine?status=UNREAD`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`

**Expected result**

Only notifications where `"status": "UNREAD"`.

---

### 3.3. Unread count

**Request**

* Method: `GET`
* URL: `http://localhost:8083/notifications/mine/unread-count`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`

**Expected result**

```json
{
  "unreadCount": 1
}
```

(or another integer depending on your data).

---

### 3.4. Mark one notification as read

Pick a notification id from `GET /notifications/mine`, e.g. `id = 1`.

**Request**

* Method: `POST`
* URL: `http://localhost:8083/notifications/1/mark-read`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`
      (must be the same user as `recipientUsername`)
* Body: none

**Expected result**

```json
{
  "id": 1,
  "recipientUsername": "patient1",
  "status": "READ",
  "readAt": "2025-11-23T...",
  ...
}
```

If you then call:

* `GET /notifications/mine?status=UNREAD` → this one disappears
* `GET /notifications/mine/unread-count` → count goes down

---

### 3.5. Mark all as read

**Request**

* Method: `POST`
* URL: `http://localhost:8083/notifications/mark-all-read`
* Headers:

    * `Authorization: Bearer <PATIENT_TOKEN>`
* Body: none

**Expected result**

```json
{
  "updatedCount": 3
}
```

(or another number = how many notifications were UNREAD and got marked READ).

After that, `GET /notifications/mine?status=UNREAD` should return an empty list for that user.

---

## 4. End-to-End Flows

### 4.1. Flow A: Doctor accepts appointment → patient gets notification

1. **Login** as `patient1` and `doctor1` → get `PATIENT_TOKEN` and `DOCTOR_TOKEN`.
2. **Create appointment** as patient:

    * `POST /appointments` with `Authorization: Bearer PATIENT_TOKEN`.
3. **Accept appointment** as doctor:

    * `POST /appointments/{id}/decision` with body `{"decision": "ACCEPT"}` and `Authorization: Bearer DOCTOR_TOKEN`.
4. **Check patient notifications**:

    * `GET /notifications/mine` with `Authorization: Bearer PATIENT_TOKEN`.
    * You should see a new `APPOINTMENT_ACCEPTED` notification.

---

### 4.2. Flow B: Patient cancels accepted appointment → doctor gets notification

1. Use an appointment that is `ACCEPTED` (or create and accept a new one as in Flow A).
2. **Cancel appointment** as patient:

    * `POST /appointments/{id}/cancel` with `Authorization: Bearer PATIENT_TOKEN`.
3. **Check doctor notifications**:

    * `GET /notifications/mine` with `Authorization: Bearer DOCTOR_TOKEN`.
    * You should see a new `APPOINTMENT_CANCELLED` notification with `recipientUsername: "doctor1"`.

---

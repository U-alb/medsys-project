# MedSys — SDT Project (Milestone 1)

## Team
- Dragota Andrei — <anditu2003@gmail.com>
- Udrea Alberto-George — <albertoudrea4@gmail.com>
- Vulturescu Vlad — <vlad.vulturescu@yahoo.com>

> **Branching per requirements:** Each milestone lives on a public branch named  
> `N-milestone-name` (e.g., `1-teams-and-project-description`). The repository must remain public for grading.

---

## Project Theme & Description

**MedSys** is a small clinic web app where **patients** can book and manage appointments and view their medical history, while **doctors** can review patient data, add medical records, and issue prescriptions. Authentication and authorization gate all flows.

### Core user roles
- **Patient:** register/login, book/cancel appointments, view medical records & prescriptions.
- **Doctor:** login, see today’s schedule, accept/deny appointments, write records, issue prescriptions.
- **(Optional) Admin:** manage users and global settings (can be added later).

### Key features & flows (what we will implement next)
1. **Auth & Roles:** credential login, role-based pages.
2. **Appointments:** patient books; system validates conflicts; doctor accepts/denies; notifications sent.
3. **Medical Records:** doctor adds records tied to visits; patient can view only their own.
4. **Prescriptions:** doctor issues prescriptions linked to a record; patient can view/download.

### Data (high level)
- `User(id, username, passHash, role)`
- `Patient(User)`, `Doctor(User)`
- `Appointment(id, doctor, patient, start, end, status)`
- `MedicalRecord(id, patient, doctor, text, date)`
- `Prescription(id, patient, doctor, text, issuedAt)`

---

## Design Patterns (GoF only)

We commit to **four GoF patterns**. One is already present in our codebase; the other three we will implement in the next milestone.

### 1) Adapter — *already used*
**Problem:** Our domain `User` model does not match Spring Security’s `UserDetails` interface, but we need to plug our users into the framework’s auth pipeline.  
**Solution:** Use **Adapter** to wrap domain objects into the interface the framework expects.  
**Where:** `CustomUserDetails` adapts `User` → `UserDetails`; `CustomUserDetailsService` adapts a username to a `UserDetails` instance.  
**Why not a simpler alternative (manual field copying in controllers)?** That would tightly couple controllers to security APIs, spreading framework concerns across the codebase. Adapter keeps the boundary clean and isolated.

### 2) Factory Method — *to implement*
**Problem:** Registration must create different subclasses (`Patient` or `Doctor`) based on the selected `Role`, with consistent defaults (e.g., profile fields) and future extensibility (adding `ADMIN` later).  
**Solution:** A **Factory Method** (`RegistrationFactory`) that converts `RegisterDTO` + `Role` into the correct concrete `User` subtype.  
**Why not `if/else` in the controller?** Controller logic would grow and duplicate initialization details. Factory centralizes creation, improves testability, and makes adding a new role a single-point change.

### 3) Strategy — *to implement*
**Problem:** We will need variations both in **how** notifications are sent and **how** appointment booking policies behave (strict vs buffered overbooking).  
**Solution:** Define **Strategy** interfaces:
- `Notifier` with concrete `EmailNotifier`, `SmsNotifier`, `NoopNotifier`;
- `BookingPolicy` with `StrictPolicy`, `BufferedOverbookPolicy`, etc.  
  **Why not flags and branches in services?** Nested conditionals become hard to read and extend; Strategy lets us swap behaviors at runtime and unit-test them independently.

### 4) Chain of Responsibility — *to implement*
**Problem:** Appointment creation requires multiple checks (auth, role, doctor availability, patient conflict, business rules). We want to add/reorder checks without rewriting the flow.  
**Solution:** Use **Chain of Responsibility**: a pipeline of `Handler` objects where each performs one validation and either forwards or stops with an error.  
**Why not one “god” validator method?** It becomes brittle and hard to evolve. CoR keeps each rule isolated, composable, and testable.

---

## Why these four (brief)
- They **directly** map to real problems we have (auth integration, user creation, booking rules, multi-step validation).
- They promote **extensibility** (adding roles/channels/rules), **testability** (small units), and **separation of concerns**.

---

## Implementation sketch (next milestone)
- **Factory Method:** `RegistrationFactory#create(dto, role): User` used by the registration flow.
- **Strategy:** inject `Notifier` and `BookingPolicy` into `AppointmentService`; select impls via config.
- **Chain of Responsibility:** validators implementing `Handler` (`DoctorAvailability`, `PatientConflict`, `BusinessRules`, …) chained inside `AppointmentService.create()`.
- **Adapter:** already in place for authentication.

---

## Milestone branches
- `1-teams-and-project-description` — this README and team info (public).
- `2-auth-and-core-entities` — finish registration factory + baseline flows.
- `3-appointments-and-policies` — Strategy + CoR for scheduling.
- `4-records-and-prescriptions` — doctor features + notifications.

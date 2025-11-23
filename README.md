# MedSys – Microservices Edition

MedSys is a small **clinic management** system designed as a **microservices architecture** for coursework.

It is split into three Spring Boot services:

* **`auth-service`** – user registration, login, and JWT authentication
* **`appointments-service`** – appointment creation, listing, acceptance/denial, cancellation
* **`notification-service`** – storing and retrieving notifications for users

All services use **MariaDB** and talk to each other via **REST** and **JWT**.
A **Docker Compose** setup runs everything together.

---

## 1. Project Structure (High-Level)

From the repository root:

* `auth-service/` – authentication microservice (users, login, JWT)
* `appointments-service/` – appointments microservice (business rules, lifecycle)
* `notification-service/` – notifications microservice (UNREAD / READ, counts)
* `db/` – database init scripts (creates the three databases)
* `docker-compose.yml` – definition for MariaDB + all three services

### Microservices – Responsibilities

**Auth Service**

* Handles:

    * user registration
    * login
    * issuing JWT tokens (with username + role)
* Roles: `PATIENT`, `DOCTOR`, `ADMIN`

**Appointments Service**

* Handles:

    * patients scheduling appointments with doctors
    * listing appointments (for patient/doctor)
    * doctor accepting/denying
    * patient cancelling
* Enforces basic rules:

    * no overlapping appointments for patient/doctor
    * configurable daily limit for patient
    * cannot cancel past appointments
* Calls notification-service to create notifications when status changes.

**Notification Service**

* Handles:

    * storing notifications per user
    * listing notifications for the logged-in user
    * counting unread notifications
    * marking one / all notifications as read
* Provides an internal endpoint for other services to create notifications.

---

## 2. Prerequisites

To run everything with Docker:

* Docker
* Docker Compose

To build/run services locally with Maven (optional):

* JDK **17 or newer**
* Maven 3.x
* (Optional) Postman or similar tool for testing APIs

---

## 3. How to Run Everything with Docker

### 3.1 Build the JARs (once per code change)

From the root folder of the project:

```bash
cd auth-service
mvn clean package -DskipTests

cd ../appointments-service
mvn clean package -DskipTests

cd ../notification-service
mvn clean package -DskipTests

cd ..
```

This compiles each service and creates a JAR in its `target/` directory.
Docker images will use these JARs.

---

### 3.2 Build the Docker images

From the **project root**:

```bash
docker compose build
```

This:

* pulls the MariaDB image (if needed)
* builds images for:

    * `auth-service`
    * `appointments-service`
    * `notification-service`

---

### 3.3 Start the full system

From the **project root**:

```bash
docker compose up
```

or in detached mode:

```bash
docker compose up -d
```

What this does:

* Starts a MariaDB container
* Applies the DB init script (creates the three databases)
* Starts:

    * auth-service on port **8081**
    * appointments-service on port **8082**
    * notification-service on port **8083**

Inside Docker, services connect via a shared network, and the appointments-service calls the notification-service using its internal service name.

Wait until the logs show each service has fully started.

---

### 3.4 Accessing the services

From the **host machine** (e.g. Postman / browser):

* Auth service:
  `http://localhost:8081`
* Appointments service:
  `http://localhost:8082`
* Notification service:
  `http://localhost:8083`

Database (from the host, e.g. IntelliJ Database tool, if configured that way):

* Host: `localhost`
* Port: `3307` (if the Compose file maps 3307 → 3306)
* User: `root`
* Password: `root`

---

### 3.5 Stopping the system

From the project root:

```bash
docker compose down
```

This stops the containers but keeps the database volume.

To also delete the stored data (start from a clean DB next time):

```bash
docker compose down -v
```

---

## 4. Running Services Without Docker (Optional)

It is also possible to run everything directly with Maven and a local MariaDB installation.

High-level steps:

1. Start a local MariaDB server.
2. Create three databases:

    * `medsys_auth`
    * `medsys_appointments`
    * `medsys_notifications`
3. Make sure the connection details in each service’s `application.properties` match your local setup
   (host, port, user, password).
4. For each service, from its directory:

   ```bash
   mvn spring-boot:run
   ```

Then the services will be available at the same ports:

* `http://localhost:8081` – auth
* `http://localhost:8082` – appointments
* `http://localhost:8083` – notifications

---

## 5. Common Issue: “release version 17 not supported”

When building with Maven, it is possible to encounter:

```text
Fatal error compiling: error: release version 17 not supported
```

### 5.1 What this means

Maven is using an older Java version (e.g. Java 8 or 11), while the project is configured to compile with Java 17.

Spring Boot 3.x requires a modern Java version, so both `java` and `mvn` must use **Java 17 or newer**.

### 5.2 How to check

Run:

```bash
java -version
mvn -version
```

If `mvn -version` shows an older Java than `java -version`, Maven is using a different JDK.

### 5.3 Quick fix (for the current terminal session)

Set `JAVA_HOME` to a JDK 17+ installation and update `PATH`:

```bash
export JAVA_HOME=/path/to/jdk-17-or-newer
export PATH="$JAVA_HOME/bin:$PATH"

java -version
mvn -version
```

After this, re-run the build commands, for example:

```bash
cd auth-service
mvn clean package -DskipTests
```

Repeat for the other services if needed.
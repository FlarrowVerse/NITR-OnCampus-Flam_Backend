# 🧩 QueueCTL — Background Job Queue CLI System

`queuectl` is a **CLI-based background job queue system** built in **Java**.  
It supports **persistent jobs**, **retry with exponential backoff**, **multiple worker processes**, and a **Dead Letter Queue (DLQ)** for failed jobs — all configurable via command line.

The working CLI demo is upload in my [Google Drive](https://drive.google.com/file/d/1L1VG3RAzbOL48sjlW27RoFNiahu1B0hy/view?usp=sharing).

---

## 🚀 1. Setup Instructions

### 🧰 Prerequisites

- **Java 21+**
- **Gradle 8+**
- **PostgreSQL** (for production)
- *(Optional)* **Docker** (if you want to run Postgres via container)
- *(Optional)* **H2** is used automatically for testing — no setup required

### 📦 Clone & Build

```bash
git clone https://github.com/<your-username>/queuectl.git
cd queuectl
./gradlew clean build
```

### 🧩 Database Setup

Create a local PostgreSQL database:

```bash
psql -U postgres
CREATE DATABASE queuectl;
CREATE USER queuectl WITH PASSWORD 'queuectl_password';
GRANT ALL PRIVILEGES ON DATABASE queuectl TO queuectl;
```

Then update your `src/main/resources/application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/queuectl
db.user=queuectl
db.password=queuectl_password
```

### ⚙️ Run Migrations

Run Flyway migrations automatically when the app starts, or trigger manually:

```bash
./gradlew run
```

### 🧠 Install CLI Binary

After building:
```bash
./gradlew installDist
```

The CLI executable is created at:
```
build/install/queuectl/bin/queuectl
```

Add it to your PATH for convenience:
```bash
export PATH="$PATH:$(pwd)/build/install/queuectl/bin"
```

---

## 🧮 2. Usage Examples

### 🟢 Enqueue a Job
Add a new background job:
```bash
queuectl enqueue '{"id":"job1","command":"echo Hello World"}'
```

Output:
```
✅ Job job1 enqueued successfully
```

---

### ⚙️ Start Workers
Start 3 worker threads to process pending jobs:
```bash
queuectl worker start --count 3
```

Output:
```
Started 3 workers. Press Ctrl+C to stop.
Worker 1 (abc12) started.
Worker 2 (def34) started.
Worker 3 (ghi56) started.
```

Gracefully stop with **Ctrl+C** — it finishes active jobs before exiting.

---

### 📊 Check Status
See all job states and active workers:
```bash
queuectl status
```

Example output:
```
Jobs:
  pending: 1
  processing: 0
  completed: 3
  failed: 0
  dead: 1

Active workers: 3
```

---

### 📋 List Jobs by State
```bash
queuectl list --state pending
```

Output:
```
ID       COMMAND           ATTEMPTS   STATE
job2     echo "Hi there"   0          pending
```

---

### ☠️ Dead Letter Queue (DLQ)
List failed jobs:
```bash
queuectl dlq list
```

Retry a DLQ job:
```bash
queuectl dlq retry job5
```

---

### ⚙️ Configuration Management
Set or view system parameters:

```bash
queuectl config get
queuectl config set max_retries 4
queuectl config set backoff_base 3
```

Example output:
```
max_retries=4
backoff_base=3
```

---

## 🏗️ 3. Architecture Overview

### 🧩 Components

| Component | Responsibility |
|------------|----------------|
| **CLI (Picocli)** | Parses and executes commands (`enqueue`, `worker start`, `dlq`, etc.) |
| **Database Layer (HikariCP + Flyway)** | Manages persistent job and configuration tables |
| **Worker Service** | Executes commands, tracks retries, moves failed jobs to DLQ |
| **Config Repository** | Stores dynamic runtime settings (`max_retries`, `backoff_base`) |
| **Job Repository** | Handles enqueueing, fetching, updating, and deleting jobs |

---

### ⚙️ Job Lifecycle

```mermaid
flowchart LR
    A[pending] -->|Picked by worker| B[processing]
    B -->|Exit code 0| C[completed]
    B -->|Non-zero exit code| D[failed]
    D -->|Retries remaining| A
    D -->|Retries exhausted| E[dead (DLQ)]
```

| State | Description |
|--------|-------------|
| `pending` | Job is waiting to be picked up by a worker |
| `processing` | Job is being executed |
| `completed` | Job finished successfully (exit code 0) |
| `failed` | Job failed but is retryable |
| `dead` | Moved to DLQ after exceeding retries |

---

### 🔁 Retry & Backoff Logic

Formula:
```
delay = base ^ attempts
```

Example:
- base = 2, attempts = 3 → delay = 8 seconds  
- Jobs move to `dead` after `max_retries`.

Both `base` and `max_retries` are configurable via:
```bash
queuectl config set backoff_base 3
queuectl config set max_retries 5
```

---

### 🧵 Worker Design

- Each worker picks one pending job (row-level lock).  
- Executes the command using `ProcessBuilder`.  
- On failure:
  - Increments attempts.
  - Re-enqueues job with exponential backoff delay.
  - Moves to DLQ after retries exhausted.  
- Supports graceful shutdown via **Ctrl+C** (SIGINT).

---

### 💾 Persistence

All data (jobs, DLQ, config) are persisted in PostgreSQL (or H2 for tests).  
Jobs survive restarts — workers resume processing automatically.

---

## ⚖️ 4. Assumptions & Trade-offs

| Area | Decision | Rationale |
|-------|-----------|-----------|
| **Storage** | PostgreSQL (prod) / H2 (tests) | Reliable + easy to test |
| **Job execution** | System shell via `ProcessBuilder` | Simplicity and portability |
| **Concurrency** | Database locking (one worker per job) | Prevents duplicates |
| **Retry logic** | Exponential backoff (`base^attempts`) | Standard and predictable |
| **Configuration** | DB-backed config (`config` table) | Hot-reload without code changes |
| **Graceful shutdown** | Catches SIGINT | Clean termination with no job loss |
| **Error handling** | DLQ after `max_retries` | Ensures failed jobs are retained |

### Simplifications
- No REST API (CLI-only by design).
- Workers poll DB every few seconds instead of pub/sub.
- Commands are shell-based, not JVM tasks.
- No distributed worker coordination (single-node concurrency only).

---

## 🧪 5. Testing Instructions

### 🧩 Unit & Integration Tests

Run all tests:
```bash
./gradlew clean test
```

### ✅ Included Test Suites

| Test File | Description |
|------------|--------------|
| `AppTest.java` | Verifies DB connection + Flyway migration |
| `JobRepositoryTest.java` | Ensures enqueue/list logic works |
| `WorkerRepositoryTest.java` | Validates retry & DLQ transition |
| `ConfigRepositoryTest.java` | Confirms config persistence |
| `WorkerServiceTest.java` | Full job execution cycle |
| `BaseIntegrationTest.java` | Initializes H2 DB + Flyway |

### 💡 Local Test Output

```
✅ H2 Test Database ready: jdbc:h2:mem:queuectl
BUILD SUCCESSFUL in 7s
```

All tests run against an **in-memory H2** database (no Docker required).

---

### 🧩 Manual CLI Testing

You can also manually test behavior:

```bash
# Enqueue failing command
queuectl enqueue '{"id":"fail1","command":"false"}'

# Start worker to trigger retries and DLQ
queuectl worker start --count 1
```

Observe:
```
[worker-123] ❌ Job fail1 failed (attempt 1)
[worker-123] Retrying in 2 seconds
[worker-123] ❌ Job fail1 failed (attempt 2)
[worker-123] ⛔ Job moved to DLQ after retries
```

---


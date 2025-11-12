-- ============================
-- QueueCTL Schema Migration
-- Works in PostgreSQL and H2 (MODE=PostgreSQL)
-- ============================

-- ENUM definition (skip for H2)
-- CREATE TYPE job_state AS ENUM ('pending','processing','completed','failed','dead');

-- JOBS TABLE
CREATE TABLE IF NOT EXISTS jobs (
    id TEXT PRIMARY KEY,
    command TEXT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'pending',
    attempts INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_run_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,
    worker_id TEXT
);

CREATE INDEX IF NOT EXISTS idx_jobs_state_next ON jobs(state, next_run_at);

-- DLQ TABLE
CREATE TABLE IF NOT EXISTS dlq (
    id TEXT PRIMARY KEY,
    command TEXT NOT NULL,
    attempts INT NOT NULL,
    max_retries INT NOT NULL,
    failed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT
);

-- CONFIG TABLE
CREATE TABLE IF NOT EXISTS config (
    "key" TEXT PRIMARY KEY,
    "value" TEXT NOT NULL
);

-- CONTROL TABLE
CREATE TABLE IF NOT EXISTS control (
    "key" TEXT PRIMARY KEY,
    "value" TEXT NOT NULL
);

-- Default configuration values
MERGE INTO config ("key", "value") KEY("key") VALUES ('max_retries', '3');
MERGE INTO config ("key", "value") KEY("key") VALUES ('backoff_base', '2');
MERGE INTO control ("key", "value") KEY("key") VALUES ('shutdown', '0');

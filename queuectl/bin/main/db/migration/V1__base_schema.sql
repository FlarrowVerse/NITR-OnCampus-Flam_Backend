CREATE TYPE job_state AS ENUM ('pending','processing','completed','failed','dead');

CREATE TABLE jobs (
    id TEXT PRIMARY KEY,
    command TEXT NOT NULL,
    state job_state NOT NULL DEFAULT 'pending',
    attempts INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    next_run_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error TEXT,
    worker_id TEXT
);

CREATE INDEX idx_jobs_state_next ON jobs(state, next_run_at);

CREATE TABLE dlq (
    id TEXT PRIMARY KEY,
    command TEXT NOT NULL,
    attempts INT NOT NULL,
    max_retries INT NOT NULL,
    failed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error TEXT
);

CREATE TABLE config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE control (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

INSERT INTO config(key,value) VALUES ('max_retries','3') ON CONFLICT DO NOTHING;
INSERT INTO config(key,value) VALUES ('backoff_base','2') ON CONFLICT DO NOTHING;
INSERT INTO control(key,value) VALUES ('shutdown','0') ON CONFLICT DO NOTHING;

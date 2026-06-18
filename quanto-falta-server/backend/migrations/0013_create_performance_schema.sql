CREATE TABLE IF NOT EXISTS performance_runs (
    id TEXT PRIMARY KEY,
    source TEXT NOT NULL, -- MACROBENCHMARK, JANKSTATS
    app_version TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    commit_sha TEXT,
    branch TEXT,
    build_type TEXT NOT NULL,
    benchmark_name TEXT,
    compilation_mode TEXT,
    iterations INTEGER,
    device_model TEXT NOT NULL,
    device_manufacturer TEXT NOT NULL,
    android_version TEXT NOT NULL,
    api_level INTEGER NOT NULL,
    refresh_rate INTEGER,
    memory_available_mb INTEGER,
    battery_level INTEGER,
    thermal_status INTEGER,
    status TEXT NOT NULL, -- PROCESSING, COMPLETED, FAILED, INVALID, REGRESSION
    payload_hash TEXT UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS performance_run_metrics (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    metric_name TEXT NOT NULL,
    percentile TEXT, -- p50, p90, p95, p99, min, max, median
    value REAL NOT NULL,
    unit TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(run_id) REFERENCES performance_runs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS performance_artifacts (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    artifact_type TEXT NOT NULL, -- PERFETTO_TRACE, RAW_JSON, LOG, SCREENSHOT
    r2_key TEXT NOT NULL,
    file_name TEXT NOT NULL,
    size_bytes INTEGER NOT NULL,
    sha256 TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(run_id) REFERENCES performance_runs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS performance_regressions (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    baseline_run_id TEXT NOT NULL,
    metric_name TEXT NOT NULL,
    previous_value REAL NOT NULL,
    current_value REAL NOT NULL,
    variation_percent REAL NOT NULL,
    severity TEXT NOT NULL, -- CRITICAL, WARNING, INFO
    status TEXT NOT NULL, -- OPEN, RESOLVED, IGNORED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(run_id) REFERENCES performance_runs(id) ON DELETE CASCADE,
    FOREIGN KEY(baseline_run_id) REFERENCES performance_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_perf_runs_source ON performance_runs(source);
CREATE INDEX IF NOT EXISTS idx_perf_runs_version ON performance_runs(app_version, version_code);
CREATE INDEX IF NOT EXISTS idx_perf_runs_benchmark ON performance_runs(benchmark_name);
CREATE INDEX IF NOT EXISTS idx_perf_run_metrics_run_id ON performance_run_metrics(run_id);
CREATE INDEX IF NOT EXISTS idx_perf_artifacts_run_id ON performance_artifacts(run_id);
CREATE INDEX IF NOT EXISTS idx_perf_regressions_run_id ON performance_regressions(run_id);

DROP TABLE IF EXISTS notification_diagnostics;

CREATE TABLE notification_diagnostics (
    installation_id TEXT PRIMARY KEY REFERENCES installations(installation_id) ON DELETE CASCADE,
    notifications_allowed INTEGER NOT NULL DEFAULT 0,
    exact_alarms_allowed INTEGER NOT NULL DEFAULT 0,
    active_schedules INTEGER NOT NULL DEFAULT 0,
    next_trigger_at INTEGER,
    last_reconciliation_at INTEGER,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notif_diag_exact ON notification_diagnostics(exact_alarms_allowed);

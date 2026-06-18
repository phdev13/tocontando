-- Create notification_diagnostics table

CREATE TABLE IF NOT EXISTS notification_diagnostics (
    installation_id TEXT PRIMARY KEY REFERENCES installations(id) ON DELETE CASCADE,
    notifications_allowed INTEGER NOT NULL DEFAULT 0,
    exact_alarms_allowed INTEGER NOT NULL DEFAULT 0,
    active_schedules INTEGER NOT NULL DEFAULT 0,
    next_trigger_at INTEGER,
    last_reconciliation_at INTEGER,
    updated_at INTEGER NOT NULL
);

-- Index to query devices without exact alarms quickly
CREATE INDEX IF NOT EXISTS idx_notif_diag_exact ON notification_diagnostics(exact_alarms_allowed);

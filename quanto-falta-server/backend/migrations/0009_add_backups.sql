-- Migration 0009: Add backups table
CREATE TABLE IF NOT EXISTS user_backups (
  installation_id TEXT PRIMARY KEY,
  data            TEXT NOT NULL, -- JSON payload of the entire database
  updated_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE TRIGGER IF NOT EXISTS trg_user_backups_updated_at
AFTER UPDATE ON user_backups
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
  UPDATE user_backups SET updated_at = (unixepoch() * 1000) WHERE installation_id = NEW.installation_id;
END;

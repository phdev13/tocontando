-- Migration 0001: Initial schema for Quanto Falta? backend
-- All timestamps stored as Unix milliseconds (INTEGER)

-- ============================================================
-- App Versions
-- ============================================================
CREATE TABLE IF NOT EXISTS app_versions (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  version_code    INTEGER NOT NULL UNIQUE,
  version_name    TEXT NOT NULL,
  release_channel TEXT NOT NULL DEFAULT 'stable',  -- stable | beta | internal
  status          TEXT NOT NULL DEFAULT 'draft',   -- draft | active | paused | retired
  mandatory       INTEGER NOT NULL DEFAULT 0 CHECK (mandatory IN (0, 1)),  -- 0 = false, 1 = true
  -- Piso POR RELEASE: version_code mínimo que um cliente precisa ter para
  -- receber esta atualização (diferente do piso global em system_settings).
  min_supported_version_code INTEGER NOT NULL DEFAULT 1,
  title           TEXT NOT NULL DEFAULT 'Nova atualização disponível',
  summary         TEXT NOT NULL DEFAULT '',
  changelog       TEXT NOT NULL DEFAULT '[]',       -- JSON array of strings
  apk_r2_key      TEXT,                             -- R2 object key (never exposed publicly)
  apk_size_bytes  INTEGER,
  sha256          TEXT,
  signature_fingerprint TEXT,
  rollout_percentage INTEGER NOT NULL DEFAULT 0 CHECK (rollout_percentage BETWEEN 0 AND 100),
  published_at    INTEGER,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  updated_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE INDEX IF NOT EXISTS idx_versions_channel_status
  ON app_versions(release_channel, status);
-- idx_versions_version_code removido: version_code já é UNIQUE, então o SQLite
-- já mantém um índice automático (sqlite_autoindex_*) sobre essa coluna.

CREATE TRIGGER IF NOT EXISTS trg_app_versions_updated_at
AFTER UPDATE ON app_versions
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
  UPDATE app_versions SET updated_at = (unixepoch() * 1000) WHERE id = NEW.id;
END;

-- ============================================================
-- OTA Attempts
-- ============================================================
CREATE TABLE IF NOT EXISTS ota_attempts (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  installation_id TEXT NOT NULL,
  version_code    INTEGER NOT NULL,
  event_type      TEXT NOT NULL,  -- check | download_started | download_completed | install_started | adopted | failed
  error_reason    TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE INDEX IF NOT EXISTS idx_ota_installation_id ON ota_attempts(installation_id);
CREATE INDEX IF NOT EXISTS idx_ota_version_code ON ota_attempts(version_code);
CREATE INDEX IF NOT EXISTS idx_ota_event_type ON ota_attempts(event_type);
CREATE INDEX IF NOT EXISTS idx_ota_created_at ON ota_attempts(created_at);

-- ============================================================
-- Installations
-- ============================================================
CREATE TABLE IF NOT EXISTS installations (
  installation_id   TEXT PRIMARY KEY,
  version_code      INTEGER NOT NULL,
  version_name      TEXT NOT NULL,
  android_version   TEXT,
  architecture      TEXT,
  language          TEXT,
  manufacturer      TEXT,
  model             TEXT,
  theme             TEXT,
  release_channel   TEXT NOT NULL DEFAULT 'stable',
  first_seen_at     INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  last_seen_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  is_active         INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_installations_version_code ON installations(version_code);
CREATE INDEX IF NOT EXISTS idx_installations_last_seen ON installations(last_seen_at);
CREATE INDEX IF NOT EXISTS idx_installations_channel ON installations(release_channel);

-- ============================================================
-- Analytics Events
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_events (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  installation_id TEXT NOT NULL,
  event_name      TEXT NOT NULL,
  properties      TEXT NOT NULL DEFAULT '{}',  -- JSON object (sanitized)
  version_code    INTEGER,
  session_id      TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE INDEX IF NOT EXISTS idx_analytics_installation_id ON analytics_events(installation_id);
CREATE INDEX IF NOT EXISTS idx_analytics_event_name ON analytics_events(event_name);
CREATE INDEX IF NOT EXISTS idx_analytics_created_at ON analytics_events(created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_version_code ON analytics_events(version_code);

-- ============================================================
-- Daily Metrics (pre-aggregated for dashboard performance)
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_metrics (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  date            TEXT NOT NULL,  -- YYYY-MM-DD
  metric_name     TEXT NOT NULL,
  metric_value    REAL NOT NULL DEFAULT 0,
  dimension_key   TEXT,           -- optional: version_code, channel, etc
  dimension_value TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

-- Índice de expressão substitui o UNIQUE(date, metric_name, dimension_key, dimension_value)
-- original: em SQL, NULL <> NULL, então duas linhas com dimension_key/value = NULL
-- (métricas sem dimensão) não eram bloqueadas pelo UNIQUE simples. COALESCE trata
-- NULL como '' para fins de unicidade, cobrindo também métricas globais.
CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_metrics_unique
  ON daily_metrics(date, metric_name, COALESCE(dimension_key, ''), COALESCE(dimension_value, ''));

CREATE INDEX IF NOT EXISTS idx_daily_metrics_date ON daily_metrics(date);
CREATE INDEX IF NOT EXISTS idx_daily_metrics_name ON daily_metrics(metric_name);

-- ============================================================
-- Performance Metrics
-- ============================================================
CREATE TABLE IF NOT EXISTS performance_metrics (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  installation_id TEXT NOT NULL,
  metric_type     TEXT NOT NULL,  -- cold_start | warm_start | query_duration | render_time | slow_frame
  value_ms        REAL NOT NULL,
  screen          TEXT,
  version_code    INTEGER,
  android_version TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE INDEX IF NOT EXISTS idx_perf_type ON performance_metrics(metric_type);
CREATE INDEX IF NOT EXISTS idx_perf_created_at ON performance_metrics(created_at);
CREATE INDEX IF NOT EXISTS idx_perf_version_code ON performance_metrics(version_code);

-- ============================================================
-- Feedback
-- ============================================================
CREATE TABLE IF NOT EXISTS feedback (
  id              TEXT PRIMARY KEY,  -- UUID
  installation_id TEXT NOT NULL,
  rating          INTEGER CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),  -- 1-5
  category        TEXT NOT NULL,     -- suggestion | bug | compliment | question | other
  message         TEXT NOT NULL,
  include_tech    INTEGER NOT NULL DEFAULT 0 CHECK (include_tech IN (0, 1)),
  tech_data       TEXT,              -- JSON (sanitized device info)
  version_code    INTEGER,
  android_version TEXT,
  model           TEXT,
  language        TEXT,
  theme           TEXT,
  source_screen   TEXT,
  status          TEXT NOT NULL DEFAULT 'new',  -- new | reviewing | planned | resolved | ignored
  priority        TEXT NOT NULL DEFAULT 'normal',
  admin_notes     TEXT,
  tags            TEXT NOT NULL DEFAULT '[]',   -- JSON array
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  updated_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(status);
CREATE INDEX IF NOT EXISTS idx_feedback_category ON feedback(category);
CREATE INDEX IF NOT EXISTS idx_feedback_rating ON feedback(rating);
CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON feedback(created_at);
CREATE INDEX IF NOT EXISTS idx_feedback_version_code ON feedback(version_code);

CREATE TRIGGER IF NOT EXISTS trg_feedback_updated_at
AFTER UPDATE ON feedback
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
  UPDATE feedback SET updated_at = (unixepoch() * 1000) WHERE id = NEW.id;
END;

-- ============================================================
-- Feedback Attachments (screenshots stored in R2)
-- ============================================================
CREATE TABLE IF NOT EXISTS feedback_attachments (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  feedback_id     TEXT NOT NULL REFERENCES feedback(id) ON DELETE CASCADE,
  r2_key          TEXT NOT NULL,    -- R2 object key (never exposed; signed URL generated on demand)
  file_size_bytes INTEGER,
  mime_type       TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

-- D1 enforça FK por padrão (equivalente a PRAGMA foreign_keys = ON em toda
-- transação, sem opção de desligar permanentemente). Sem este índice, um
-- DELETE em feedback varre feedback_attachments inteira para resolver o CASCADE.
CREATE INDEX IF NOT EXISTS idx_feedback_attachments_feedback_id
  ON feedback_attachments(feedback_id);

-- ============================================================
-- Crash Reports
-- ============================================================
CREATE TABLE IF NOT EXISTS crash_reports (
  id              TEXT PRIMARY KEY,  -- UUID
  installation_id TEXT NOT NULL,
  error_type      TEXT NOT NULL,
  error_message   TEXT NOT NULL,     -- sanitized, no personal data
  screen          TEXT,
  version_code    INTEGER,
  android_version TEXT,
  resolved        INTEGER NOT NULL DEFAULT 0 CHECK (resolved IN (0, 1)),
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE INDEX IF NOT EXISTS idx_crash_error_type ON crash_reports(error_type);
CREATE INDEX IF NOT EXISTS idx_crash_version_code ON crash_reports(version_code);
CREATE INDEX IF NOT EXISTS idx_crash_created_at ON crash_reports(created_at);

-- ============================================================
-- Admin Users
-- ============================================================
CREATE TABLE IF NOT EXISTS admin_users (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  username        TEXT NOT NULL UNIQUE,
  password_hash   TEXT NOT NULL,   -- bcrypt hash (never stored in plain)
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  last_login_at   INTEGER
);

-- ============================================================
-- Admin Sessions (HttpOnly cookies)
-- ============================================================
CREATE TABLE IF NOT EXISTS admin_sessions (
  id              TEXT PRIMARY KEY,  -- random session token (stored as hash)
  admin_user_id   INTEGER NOT NULL REFERENCES admin_users(id),
  expires_at      INTEGER NOT NULL,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  ip_hash         TEXT              -- hashed IP for audit (not raw IP)
);

CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON admin_sessions(expires_at);
-- Útil para "logout em todos os dispositivos" / revogar sessões de um admin.
CREATE INDEX IF NOT EXISTS idx_sessions_admin_user_id ON admin_sessions(admin_user_id);

-- ============================================================
-- Audit Logs
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  admin_user_id   INTEGER,
  action          TEXT NOT NULL,
  target_type     TEXT,
  target_id       TEXT,
  details         TEXT,            -- JSON
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_admin_user_id ON audit_logs(admin_user_id);

-- ============================================================
-- System Settings (remote config)
-- ============================================================
CREATE TABLE IF NOT EXISTS system_settings (
  key             TEXT PRIMARY KEY,
  value           TEXT NOT NULL,
  description     TEXT,
  updated_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);

CREATE TRIGGER IF NOT EXISTS trg_system_settings_updated_at
AFTER UPDATE ON system_settings
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
  UPDATE system_settings SET updated_at = (unixepoch() * 1000) WHERE key = NEW.key;
END;

-- Default settings
INSERT OR IGNORE INTO system_settings(key, value, description) VALUES
  ('ota_check_interval_hours', '6', 'Horas entre verificações OTA no app'),
  ('ota_modal_cooldown_hours', '24', 'Horas entre exibições do modal OTA'),
  ('feedback_contextual_enabled', 'true', 'Habilitar solicitação contextual de feedback'),
  ('feedback_contextual_cooldown_days', '14', 'Dias entre solicitações contextuais'),
  ('maintenance_mode', 'false', 'Modo de manutenção ativo'),
  ('maintenance_message', '', 'Mensagem exibida em modo de manutenção'),
  ('telemetry_max_queue_size', '500', 'Tamanho máximo da fila de telemetria local'),
  -- Piso GLOBAL: cliente com version_code abaixo deste valor é forçado a
  -- atualizar, independente da release ativa (ver também
  -- app_versions.min_supported_version_code, que é o piso por-release).
  ('min_supported_version_code', '1', 'Versão mínima suportada (abaixo = atualização obrigatória)');
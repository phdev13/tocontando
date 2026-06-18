-- Arquivo para criação do schema inicial no Cloudflare D1 (SQLite)

CREATE TABLE IF NOT EXISTS events (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  slug        TEXT UNIQUE NOT NULL,
  title       TEXT NOT NULL,
  date        TEXT NOT NULL,
  color       TEXT NOT NULL,
  icon        TEXT NOT NULL,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Indice para buscas rapidas por slug
CREATE INDEX IF NOT EXISTS idx_events_slug ON events(slug);

-- Migration number: 0005 	 2026-06-14T12:00:00.000Z

CREATE TABLE IF NOT EXISTS testers (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  nickname TEXT,
  avatar_key TEXT,
  badge_key TEXT,
  message TEXT,
  participation_version TEXT,
  participation_period TEXT,
  display_order INTEGER DEFAULT 0,
  is_active INTEGER DEFAULT 1,
  is_featured INTEGER DEFAULT 0,
  consent_confirmed INTEGER DEFAULT 0,
  published_at INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_testers_active_order ON testers(is_active, display_order);
CREATE INDEX IF NOT EXISTS idx_testers_published ON testers(published_at);

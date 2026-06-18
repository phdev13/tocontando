CREATE INDEX IF NOT EXISTS idx_analytics_events_name_time ON analytics_events(event_name, created_at);
CREATE INDEX IF NOT EXISTS idx_installations_last_seen ON installations(last_seen_at);
CREATE INDEX IF NOT EXISTS idx_installations_first_seen ON installations(first_seen_at);

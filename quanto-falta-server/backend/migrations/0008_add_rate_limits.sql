-- Migration: 0008_add_rate_limits
-- Purpose: Store rate limits in D1 for robust protection across Cloudflare Worker isolates.

CREATE TABLE IF NOT EXISTS rate_limits (
    hash_key TEXT PRIMARY KEY,
    request_count INTEGER NOT NULL DEFAULT 1,
    expires_at INTEGER NOT NULL
);

-- Index to quickly delete expired rows
CREATE INDEX IF NOT EXISTS idx_rate_limits_expires ON rate_limits (expires_at);

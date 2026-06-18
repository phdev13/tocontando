-- 0002_free_tier.sql
-- Add columns for the free tier architecture (GitHub Releases and D1 Blobs)

ALTER TABLE feedback ADD COLUMN screenshot_base64 TEXT;
ALTER TABLE app_versions ADD COLUMN github_release_tag TEXT;

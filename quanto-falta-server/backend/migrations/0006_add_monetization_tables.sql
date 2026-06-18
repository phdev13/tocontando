-- Migration number: 0006 	 2026-06-15T04:41:07.411Z

CREATE TABLE IF NOT EXISTS monetization_products (
  id TEXT PRIMARY KEY,
  play_product_id TEXT NOT NULL,
  product_type TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  features TEXT, -- JSON array of features
  display_order INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE IF NOT EXISTS monetization_purchases (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  installation_id TEXT,
  platform TEXT NOT NULL,
  product_id TEXT NOT NULL,
  base_plan_id TEXT,
  offer_id TEXT,
  purchase_token_hash TEXT NOT NULL UNIQUE,
  order_id TEXT,
  purchase_state TEXT NOT NULL,
  purchased_at INTEGER NOT NULL,
  expires_at INTEGER,
  auto_renewing BOOLEAN DEFAULT 0,
  country_code TEXT,
  currency_code TEXT,
  amount REAL,
  environment TEXT NOT NULL DEFAULT 'PRODUCTION',
  last_verified_at INTEGER,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  FOREIGN KEY (product_id) REFERENCES monetization_products(id)
);

CREATE TABLE IF NOT EXISTS monetization_campaigns (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  starts_at INTEGER,
  ends_at INTEGER,
  target_audience TEXT,
  eligible_products TEXT, -- JSON array of product IDs
  features TEXT, -- JSON array of features
  paywall_variant TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE IF NOT EXISTS premium_codes (
  id TEXT PRIMARY KEY,
  code_hash TEXT NOT NULL UNIQUE,
  code_prefix TEXT NOT NULL,
  internal_name TEXT NOT NULL,
  description TEXT,
  campaign_id TEXT,
  benefit_type TEXT NOT NULL,
  features TEXT, -- JSON array of features
  duration_type TEXT NOT NULL,
  duration_value INTEGER,
  valid_from INTEGER,
  valid_until INTEGER,
  max_redemptions INTEGER NOT NULL DEFAULT 1,
  redemption_count INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_by TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  revoked_at INTEGER,
  revoked_by TEXT,
  revocation_reason TEXT,
  FOREIGN KEY (campaign_id) REFERENCES monetization_campaigns(id)
);

CREATE TABLE IF NOT EXISTS monetization_entitlements (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  type TEXT NOT NULL,
  source TEXT NOT NULL,
  product_id TEXT,
  campaign_id TEXT,
  code_id TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  features TEXT, -- JSON array of features
  starts_at INTEGER,
  expires_at INTEGER,
  offline_valid_until INTEGER,
  last_synced_at INTEGER,
  granted_by_admin BOOLEAN DEFAULT 0,
  grant_reason TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  FOREIGN KEY (product_id) REFERENCES monetization_products(id),
  FOREIGN KEY (campaign_id) REFERENCES monetization_campaigns(id),
  FOREIGN KEY (code_id) REFERENCES premium_codes(id)
);

CREATE TABLE IF NOT EXISTS premium_code_redemptions (
  id TEXT PRIMARY KEY,
  code_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  installation_id TEXT,
  entitlement_id TEXT NOT NULL,
  redeemed_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  benefit_starts_at INTEGER,
  benefit_expires_at INTEGER,
  ip_hash TEXT,
  app_version TEXT,
  platform TEXT,
  status TEXT NOT NULL DEFAULT 'SUCCESS',
  FOREIGN KEY (code_id) REFERENCES premium_codes(id),
  FOREIGN KEY (entitlement_id) REFERENCES monetization_entitlements(id)
);

CREATE TABLE IF NOT EXISTS monetization_audit_logs (
  id TEXT PRIMARY KEY,
  administrator_id TEXT NOT NULL,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  previous_state TEXT, -- JSON snapshot
  new_state TEXT, -- JSON snapshot
  reason TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

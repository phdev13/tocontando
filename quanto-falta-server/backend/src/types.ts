// Cloudflare Workers environment bindings
export interface Env {
  DB: D1Database;
  APK_BUCKET: R2Bucket;
  GITHUB_PAT: string;
  GITHUB_REPO?: string;
  ENVIRONMENT: string;
  APP_PACKAGE_NAME: string;
  // Secrets (set via wrangler secret put)
  API_SIGNING_SECRET: string;
  FEEDBACK_ENCRYPTION_KEY: string;
  RATE_LIMIT_SALT: string;
  GOOGLE_PLAY_CREDENTIALS?: string;
  CF_ACCESS_TEAM_DOMAIN: string;
  CF_ACCESS_AUDIENCE: string;
  PERFORMANCE_TRACES: R2Bucket;
  PERFORMANCE_INGEST_TOKEN: string;
  JWT_SECRET: string;
}

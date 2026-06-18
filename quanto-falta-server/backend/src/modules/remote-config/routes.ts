import { Hono } from 'hono';
import type { Env } from '../../types';
import { getSetting } from '../../services/configService';
import { ok } from '../../contracts/apiResponse';

export const remoteConfigPublicRoutes = new Hono<{ Bindings: Env }>();

remoteConfigPublicRoutes.get('/app-config', async (c) => {
  const [
    maintenanceMode,
    minSupportedVersionCode,
    telemetryMaxQueue,
    feedbackContextualEnabled,
    activeIconMode,
    premium_event_cards_enabled,
    advanced_counts_enabled,
    default_count_format,
    latestVersion,
  ] = await Promise.all([
    getSetting(c.env.DB, 'maintenance_mode'),
    getSetting(c.env.DB, 'min_supported_version_code'),
    getSetting(c.env.DB, 'telemetry_max_queue_size'),
    getSetting(c.env.DB, 'feedback_contextual_enabled'),
    getSetting(c.env.DB, 'active_icon_mode'),
    getSetting(c.env.DB, 'premium_event_cards_enabled'),
    getSetting(c.env.DB, 'advanced_counts_enabled'),
    getSetting(c.env.DB, 'default_count_format'),
    c.env.DB.prepare(`
      SELECT version_code, version_name
      FROM app_versions
      WHERE release_channel = 'stable' AND status = 'active'
      ORDER BY version_code DESC
      LIMIT 1
    `).first<{ version_code: number; version_name: string }>().catch(() => null),
  ]);

  c.header('Cache-Control', 'public, max-age=300, stale-while-revalidate=3600');

  return ok(c, {
    maintenanceMode: maintenanceMode === 'true',
    minimumAppVersion: minSupportedVersionCode ?? '1',
    latestAppVersion: latestVersion?.version_name ?? null,
    latestAppVersionCode: latestVersion?.version_code ?? null,
    freeEventLimit: 5,
    premiumEnabled: true,
    feedbackEnabled: feedbackContextualEnabled !== 'false',
    telemetryEnabled: true,
    telemetryMaxQueueSize: parseInt(telemetryMaxQueue ?? '500', 10),
    otaEnabled: true,
    notificationsEnabled: true,
    activeIconMode: activeIconMode ?? 'auto',
    premiumEventCardsEnabled: premium_event_cards_enabled !== 'false',
    advancedCountsEnabled: advanced_counts_enabled !== 'false',
    defaultCountFormat: default_count_format ?? 'DAYS',
    fetchedAt: new Date().toISOString(),
  });
});

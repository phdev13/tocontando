import { Hono } from 'hono';
import type { Env } from '../types';
import { getSetting } from '../services/configService';

export const configRoutes = new Hono<{ Bindings: Env }>();

/**
 * GET /api/v1/config
 * Returns app-facing remote configuration.
 * Never exposes admin settings or internal values.
 */
configRoutes.get('/config', async (c) => {
  const [
    otaIntervalHours,
    modalCooldownHours,
    feedbackContextualEnabled,
    feedbackCooldownDays,
    maintenanceMode,
    maintenanceMessage,
    telemetryMaxQueue,
    minSupportedVersionCode,
    activeIconMode,
  ] = await Promise.all([
    getSetting(c.env.DB, 'ota_check_interval_hours'),
    getSetting(c.env.DB, 'ota_modal_cooldown_hours'),
    getSetting(c.env.DB, 'feedback_contextual_enabled'),
    getSetting(c.env.DB, 'feedback_contextual_cooldown_days'),
    getSetting(c.env.DB, 'maintenance_mode'),
    getSetting(c.env.DB, 'maintenance_message'),
    getSetting(c.env.DB, 'telemetry_max_queue_size'),
    getSetting(c.env.DB, 'min_supported_version_code'),
    getSetting(c.env.DB, 'active_icon_mode'),
  ]);

  return c.json({
    ota: {
      checkIntervalHours: parseInt(otaIntervalHours ?? '6', 10),
      modalCooldownHours: parseInt(modalCooldownHours ?? '24', 10),
    },
    feedback: {
      contextualEnabled: feedbackContextualEnabled === 'true',
      cooldownDays: parseInt(feedbackCooldownDays ?? '14', 10),
    },
    maintenance: {
      active: maintenanceMode === 'true',
      message: maintenanceMessage ?? '',
    },
    telemetry: {
      maxQueueSize: parseInt(telemetryMaxQueue ?? '500', 10),
    },
    minSupportedVersionCode: parseInt(minSupportedVersionCode ?? '1', 10),
    activeIconMode: activeIconMode ?? 'auto',
    fetchedAt: new Date().toISOString(),
    cacheTtlSeconds: 3600,
  });
});

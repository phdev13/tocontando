import { Hono } from 'hono';
import type { Env } from '../../types';
import { adminAuthMiddleware } from '../../middleware/auth';
import { adminVersionsRoutes } from './versions';
import { adminFeedbackRoutes } from './feedback';
import { adminMetricsRoutes } from './metrics';
import { adminSettingsRoutes } from './settings';
import { adminTestersRoutes } from './testers';
import { adminMonetizationRoutes } from './monetization';

export const adminRoutes = new Hono<{ Bindings: Env }>();

adminRoutes.use('*', adminAuthMiddleware);

adminRoutes.route('/versions', adminVersionsRoutes);
adminRoutes.route('/feedback', adminFeedbackRoutes);
adminRoutes.route('/metrics', adminMetricsRoutes);
adminRoutes.route('/settings', adminSettingsRoutes);
adminRoutes.route('/testers', adminTestersRoutes);
adminRoutes.route('/monetization', adminMonetizationRoutes);

import { Hono } from 'hono';
import type { Env } from '../types';
import { fail } from '../contracts/apiResponse';
import { remoteConfigPublicRoutes } from './remote-config/routes';
import { websitePublicRoutes } from './website/routes';
import { testersPublicRoutes } from './testers/routes';
import { installationsAppRoutes } from './installations/routes';
import { sessionsAppRoutes } from './sessions/routes';
import { telemetryAppRoutes } from './telemetry/routes';
import { feedbackAppRoutes } from './feedback/routes';
import { premiumAppRoutes } from './premium/routes';
import { otaAppRoutes } from './ota/routes';
import { notificationsAppRoutes } from './notifications/routes';
import { adminV1Routes } from './admin/routes';
import { shareAppRoutes, sharePublicRoutes } from './share/routes';
import { syncAppRoutes } from './sync/routes';

export const apiV1Routes = new Hono<{ Bindings: Env }>();

apiV1Routes.route('/public', websitePublicRoutes);
apiV1Routes.route('/public', remoteConfigPublicRoutes);
apiV1Routes.route('/public', testersPublicRoutes);
apiV1Routes.route('/public', sharePublicRoutes);

apiV1Routes.route('/app', installationsAppRoutes);
apiV1Routes.route('/app', sessionsAppRoutes);
apiV1Routes.route('/app', telemetryAppRoutes);
apiV1Routes.route('/app', feedbackAppRoutes);
apiV1Routes.route('/app', premiumAppRoutes);
apiV1Routes.route('/app', otaAppRoutes);
apiV1Routes.route('/app', notificationsAppRoutes);
apiV1Routes.route('/app', shareAppRoutes);
apiV1Routes.route('/app', syncAppRoutes);

apiV1Routes.route('/admin', adminV1Routes);

apiV1Routes.notFound((c) => fail(c, 'NOT_FOUND', 'Rota da API nao encontrada.', 404));

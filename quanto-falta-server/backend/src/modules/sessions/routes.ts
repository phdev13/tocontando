import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';

export const sessionsAppRoutes = new Hono<{ Bindings: Env }>();

const SessionSchema = z.object({
  installationId: z.string().uuid(),
  appVersion: z.string().max(20).optional(),
  platform: z.literal('android').default('android'),
});

sessionsAppRoutes.post('/sessions', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = SessionSchema.safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Payload de sessao invalido.', 400);

  return ok(c, {
    sessionId: crypto.randomUUID(),
    expiresInSeconds: 3600,
    issuedAt: new Date().toISOString(),
  }, 201);
});

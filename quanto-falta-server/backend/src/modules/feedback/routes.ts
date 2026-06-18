import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';

export const feedbackAppRoutes = new Hono<{ Bindings: Env }>();

const FeedbackSchema = z.object({
  installationId: z.string().uuid(),
  rating: z.number().int().min(1).max(5).optional(),
  category: z.enum(['suggestion', 'bug', 'compliment', 'question', 'other']).default('other'),
  message: z.string().min(1).max(2000),
  includeTechData: z.boolean().default(false),
  techData: z.object({
    versionCode: z.number().int().optional(),
    androidVersion: z.string().max(10).optional(),
    model: z.string().max(50).optional(),
    language: z.string().max(10).optional(),
    theme: z.enum(['light', 'dark', 'system']).optional(),
    sourceScreen: z.string().max(64).optional(),
  }).optional(),
});

feedbackAppRoutes.post('/feedback', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = FeedbackSchema.safeParse(body);
  if (!parsed.success) {
    console.error('Feedback validation failed:', parsed.error.format(), 'Body:', body);
    return fail(c, 'INVALID_REQUEST', 'Payload de feedback invalido.', 400);
  }

  const item = parsed.data;
  const tech = item.includeTechData ? item.techData : null;
  const id = crypto.randomUUID();

  await c.env.DB.prepare(
    `INSERT INTO feedback(id, installation_id, rating, category, message, include_tech, tech_data,
       version_code, android_version, model, language, theme, source_screen)
     VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    id,
    item.installationId,
    item.rating ?? null,
    item.category,
    item.message,
    item.includeTechData ? 1 : 0,
    tech ? JSON.stringify(tech) : null,
    tech?.versionCode ?? null,
    tech?.androidVersion ?? null,
    tech?.model ?? null,
    tech?.language ?? null,
    tech?.theme ?? null,
    tech?.sourceScreen ?? null,
  ).run();

  return ok(c, { id, status: 'received' }, 201);
});

feedbackAppRoutes.post('/feedback/offline', async (c) => {
  const body = await c.req.json().catch(() => null);
  const BatchSchema = z.object({
    items: z.array(FeedbackSchema.extend({ clientId: z.string().uuid().optional() })).max(20),
  });
  const parsed = BatchSchema.safeParse(body);
  if (!parsed.success) {
    console.error('Offline Feedback validation failed:', parsed.error.format(), 'Body:', body);
    return fail(c, 'INVALID_REQUEST', 'Payload de feedback offline invalido.', 400);
  }

  const results: { clientId: string; serverId: string }[] = [];

  for (const item of parsed.data.items) {
    const id = crypto.randomUUID();
    const tech = item.includeTechData ? item.techData : null;
    try {
      await c.env.DB.prepare(
        `INSERT OR IGNORE INTO feedback(id, installation_id, rating, category, message, include_tech, tech_data,
           version_code, android_version, model, language, theme, source_screen)
         VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      ).bind(
        id,
        item.installationId,
        item.rating ?? null,
        item.category,
        item.message,
        item.includeTechData ? 1 : 0,
        tech ? JSON.stringify(tech) : null,
        tech?.versionCode ?? null,
        tech?.androidVersion ?? null,
        tech?.model ?? null,
        tech?.language ?? null,
        tech?.theme ?? null,
        tech?.sourceScreen ?? null,
      ).run();
      results.push({ clientId: item.clientId ?? id, serverId: id });
    } catch {
      // Keep processing the rest of the offline batch.
    }
  }

  return ok(c, { accepted: results.length, results });
});

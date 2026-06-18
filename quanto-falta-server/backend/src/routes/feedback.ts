import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../types';

export const feedbackRoutes = new Hono<{ Bindings: Env }>();

const FeedbackSchema = z.object({
  installationId: z.string().uuid(),
  rating: z.number().int().min(1).max(5).optional(),
  category: z.enum(['suggestion', 'bug', 'compliment', 'question', 'other']),
  // Message is kept but length-limited to prevent abuse
  message: z.string().min(1).max(2000),
  includeTechData: z.boolean().default(false),
  // Tech data is optional and only trusted if includeTechData=true
  techData: z.object({
    versionCode: z.number().int().optional(),
    androidVersion: z.string().max(10).optional(),
    model: z.string().max(50).optional(),
    language: z.string().max(10).optional(),
    theme: z.enum(['light', 'dark', 'system']).optional(),
    sourceScreen: z.string().max(64).optional(),
    relatedErrorId: z.string().max(64).optional(),
  }).optional(),
  screenshotBase64: z.string().max(1048576).optional(), // Max ~1MB
});

/**
 * POST /api/v1/feedback
 * Accepts user feedback. Never stores personal event data.
 */
feedbackRoutes.post('/feedback', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body) return c.json({ error: 'Invalid body', code: 'PARSE_ERROR' }, 400);

  const parsed = FeedbackSchema.safeParse(body);
  if (!parsed.success) {
    return c.json({ error: 'Invalid payload', code: 'VALIDATION_ERROR' }, 400);
  }

  const { installationId, rating, category, message, includeTechData, techData, screenshotBase64 } = parsed.data;

  const id = crypto.randomUUID();
  const tech = includeTechData ? techData : null;

  await c.env.DB.prepare(
    `INSERT INTO feedback(id, installation_id, rating, category, message, include_tech, tech_data,
       version_code, android_version, model, language, theme, source_screen, screenshot_base64)
     VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    id, installationId, rating ?? null, category,
    message,
    includeTechData ? 1 : 0,
    tech ? JSON.stringify(tech) : null,
    tech?.versionCode ?? null,
    tech?.androidVersion ?? null,
    tech?.model ?? null,
    tech?.language ?? null,
    tech?.theme ?? null,
    tech?.sourceScreen ?? null,
    screenshotBase64 ?? null,
  ).run();

  return c.json({ id, status: 'received' }, 201);
});

/**
 * POST /api/v1/feedback/offline
 * Bulk endpoint for offline-queued feedback submissions.
 */
feedbackRoutes.post('/feedback/offline', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body) return c.json({ error: 'Invalid body' }, 400);

  const BatchSchema = z.object({
    items: z.array(FeedbackSchema.extend({ clientId: z.string().uuid() })).max(20),
  });

  const parsed = BatchSchema.safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  const results: { clientId: string; serverId: string }[] = [];

  for (const item of parsed.data.items) {
    const id = crypto.randomUUID();
    try {
      await c.env.DB.prepare(
        `INSERT OR IGNORE INTO feedback(id, installation_id, rating, category, message, include_tech, tech_data,
           version_code, android_version, model, language, theme, source_screen, screenshot_base64)
         VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      ).bind(
        id, item.installationId, item.rating ?? null, item.category,
        item.message,
        item.includeTechData ? 1 : 0,
        item.includeTechData && item.techData ? JSON.stringify(item.techData) : null,
        item.techData?.versionCode ?? null,
        item.techData?.androidVersion ?? null,
        item.techData?.model ?? null,
        item.techData?.language ?? null,
        item.techData?.theme ?? null,
        item.techData?.sourceScreen ?? null,
        item.screenshotBase64 ?? null,
      ).run();
      results.push({ clientId: item.clientId ?? id, serverId: id });
    } catch {/* skip duplicates */}
  }

  return c.json({ accepted: results.length, results }, 200);
});

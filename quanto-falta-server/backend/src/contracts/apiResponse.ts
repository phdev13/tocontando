import type { Context } from 'hono';
import type { ContentfulStatusCode } from 'hono/utils/http-status';
import type { Env } from '../types';

type ApiContext = Context<{ Bindings: Env }>;

export type ApiErrorCode =
  | 'INVALID_REQUEST'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'RATE_LIMITED'
  | 'INVALID_TOKEN'
  | 'TOKEN_EXPIRED'
  | 'TOKEN_ALREADY_USED'
  | 'PURCHASE_INVALID'
  | 'PREMIUM_EXPIRED'
  | 'APP_UPDATE_REQUIRED'
  | 'FEATURE_DISABLED'
  | 'INTERNAL_ERROR';

function requestId(c: ApiContext): string {
  return (c.get('requestId' as never) as string | undefined) ?? crypto.randomUUID();
}

export function ok<T>(c: ApiContext, data: T, status: ContentfulStatusCode = 200) {
  return c.json({
    success: true,
    data,
    error: null,
    meta: {
      requestId: requestId(c),
    },
  }, status);
}

export function fail(
  c: ApiContext,
  code: ApiErrorCode,
  message: string,
  status: ContentfulStatusCode = 400,
) {
  return c.json({
    success: false,
    data: null,
    error: {
      code,
      message,
    },
    meta: {
      requestId: requestId(c),
    },
  }, status);
}

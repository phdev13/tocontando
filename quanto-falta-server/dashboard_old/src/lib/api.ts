/** Typed API client for the Tô Contando admin backend */
const BASE = (import.meta as any).env.DEV ? 'http://localhost:8787' : '';
const ADMIN_BASE = '/api/v1/admin';

function unwrap<T>(payload: any): T {
  if (payload && typeof payload === 'object' && 'success' in payload && 'data' in payload) {
    return payload.data as T;
  }
  return payload as T;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Network error' }));
    const apiError = (err as any).error;
    throw new ApiError(
      res.status,
      apiError?.message ?? apiError ?? 'Unknown error',
      apiError?.code ?? (err as any).code,
    );
  }
  const payload = await res.json();
  return unwrap<T>(payload);
}

export class ApiError extends Error {
  constructor(public status: number, message: string, public code?: string) {
    super(message);
  }
}


// ── Metrics ───────────────────────────────────────────────
export const metrics = {
  overview: (days = 30) => request<any>(`${ADMIN_BASE}/metrics/overview?days=${days}`),
  events: (days = 7, event?: string) =>
    request<any>(`${ADMIN_BASE}/metrics/events?days=${days}${event ? `&event=${event}` : ''}`),
  performance: (days = 14) => request<any>(`${ADMIN_BASE}/metrics/performance?days=${days}`),
};

// ── Versions ──────────────────────────────────────────────
export const versions = {
  list: (params?: { channel?: string; status?: string; limit?: number }) => {
    const q = new URLSearchParams(params as any).toString();
    return request<any>(`${ADMIN_BASE}/ota/releases${q ? '?' + q : ''}`);
  },
  create: (data: any) =>
    request<any>(`${ADMIN_BASE}/ota/releases`, { method: 'POST', body: JSON.stringify(data) }),
  update: (versionCode: number, data: any) =>
    request<any>(`${ADMIN_BASE}/ota/releases/${versionCode}`, { method: 'PATCH', body: JSON.stringify(data) }),
  uploadRelease: (formData: FormData) => {
    // Note: don't set Content-Type header when sending FormData, fetch will set it with boundary automatically
    return fetch(BASE + `${ADMIN_BASE}/ota/releases/github-release`, {
      method: 'POST',
      body: formData,
      credentials: 'include',
    }).then(async res => {
      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: 'Upload failed' }));
        const apiError = (err as any).error;
        throw new ApiError(res.status, apiError?.message ?? apiError ?? 'Upload error', apiError?.code ?? (err as any).code);
      }
      const payload = await res.json();
      return unwrap<any>(payload);
    });
  },
};

// ── Feedback ──────────────────────────────────────────────
export const feedback = {
  list: (params?: Record<string, string>) => {
    const q = new URLSearchParams(params).toString();
    return request<any>(`${ADMIN_BASE}/feedback${q ? '?' + q : ''}`);
  },
  update: (id: string, data: any) =>
    request<any>(`${ADMIN_BASE}/feedback/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),
  stats: () => request<any>(`${ADMIN_BASE}/feedback/stats`),
};

// ── Settings ──────────────────────────────────────────────
export const settings = {
  list: () => request<any>(`${ADMIN_BASE}/settings`),
  update: (key: string, value: string) =>
    request<any>(`${ADMIN_BASE}/settings/${key}`, { method: 'PUT', body: JSON.stringify({ value }) }),
};

// ── Testers ───────────────────────────────────────────────
export const testers = {
  list: () => request<any>(`${ADMIN_BASE}/testers`),
  create: (data: any) => request<any>(`${ADMIN_BASE}/testers`, { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: any) => request<any>(`${ADMIN_BASE}/testers/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: string) => request<any>(`${ADMIN_BASE}/testers/${id}`, { method: 'DELETE' }),
};

// ── App Config ────────────────────────────────────────────
export const config = {
  get: () => request<any>('/api/v1/public/app-config', { credentials: 'omit' }),
};

// ── Monetization ──────────────────────────────────────────
export const monetization = {
  metrics: () => request<any>(`${ADMIN_BASE}/premium/metrics`),
  purchases: () => request<any>(`${ADMIN_BASE}/premium/purchases`),
  users: () => request<any>(`${ADMIN_BASE}/premium/users`),
  codes: () => request<any>(`${ADMIN_BASE}/premium/codes`),
  createCode: (data: any) => request<any>(`${ADMIN_BASE}/premium/codes`, { method: 'POST', body: JSON.stringify(data) }),
};

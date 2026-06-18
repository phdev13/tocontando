const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? '';
const ADMIN_BASE = `${API_BASE}/api/v1/admin`;

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public code?: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const isFormData = init?.body instanceof FormData;
  const headers: any = {
    Accept: 'application/json',
    ...init?.headers,
  };
  
  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${ADMIN_BASE}${path}`, {
    credentials: 'include',
    headers,
    ...init,
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const error = payload?.error;
    throw new ApiError(
      response.status,
      error?.message ?? error ?? `HTTP ${response.status}`,
      error?.code,
    );
  }

  if (payload && typeof payload === 'object' && 'success' in payload && 'data' in payload) {
    return payload.data as T;
  }

  return payload as T;
}

export type DashboardData = {
  period: { days: number; since: string };
  installations: { total: number; active: number; previousNew: number; changePercent: number | null };
  feedback: { recent: number; previousRecent: number; changePercent: number | null; averageRating: number | null };
  eventsCreated: number;
  retention: { d1: number; d7: number; d30: number; base: number };
  charts: {
    installations: Array<{ date: string; label: string; value: number }>;
    events: Array<Record<string, string | number>>;
  };
  versionDistribution: Array<{ versionCode: number; versionName: string; count: number; percent: number }>;
  appHealth: {
    score: number;
    status: 'healthy' | 'attention' | 'critical';
    ota: Record<string, number | null>;
    performance: Record<string, number | null>;
  };
  recentActivity: Array<{ type: string; title: string; description: string; createdAt: number | null }>;
  alerts: Array<{ severity: string; title: string; description: string }>;
  generatedAt: string;
};

export type MetricsOverview = {
  period: { days: number; since: string };
  installations: { total: number; active: number; new: number };
  eventsCreated: number;
  versionDistribution: Array<{ version_code?: number; versionCode?: number; version_name?: string; versionName?: string; count: number }>;
  performance: { avgColdStartMs: number | null };
  ota: Record<string, number | null>;
  feedback: { count: number; averageRating: number | null };
  retention: { d1: number; d7: number; d30: number; baseInstallations: number };
  generatedAt: string;
};

export type FeedbackItem = {
  id: string;
  installation_id?: string;
  installationId?: string;
  rating: number | null;
  category: string;
  message: string;
  status: string;
  priority?: string;
  admin_notes?: string | null;
  version_code?: number | null;
  versionCode?: number | null;
  created_at?: number;
  createdAt?: number;
  updated_at?: number;
  updatedAt?: number;
};

export type TesterItem = Record<string, any>;
export type OtaRelease = Record<string, any>;
export type PremiumCode = Record<string, any>;
export type Purchase = Record<string, any>;
export type PremiumUser = Record<string, any>;
export type Setting = { key: string; value: string; description?: string; updated_at?: number };
export type AuditLog = Record<string, any>;

export const adminApi = {
  dashboard: (days = 30, init?: RequestInit) => request<DashboardData>(`/dashboard?days=${days}`, init),
  metricsOverview: (days = 30, init?: RequestInit) => request<MetricsOverview>(`/metrics/overview?days=${days}`, init),
  telemetryEvents: (days = 30, init?: RequestInit) => request<any>(`/telemetry/events?days=${days}`, init),
  performance: (days = 30, init?: RequestInit) => request<any>(`/metrics/performance?days=${days}`, init),
  devices: (limit = 100) => request<{ devices: any[]; pagination: { limit: number } }>(`/devices?limit=${limit}`),
  deviceDiagnostics: (id: string) => request<{ diagnostics: any }>(`/devices/${id}/diagnostics`),
  feedback: (params: Record<string, string | number | undefined> = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') query.set(key, String(value));
    });
    return request<{ feedback: FeedbackItem[]; total?: number; limit?: number; offset?: number }>(`/feedback${query.size ? `?${query}` : ''}`);
  },
  feedbackStats: () => request<any>('/feedback/stats'),
  updateFeedback: (id: string, data: any) => request<any>(`/feedback/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),
  testers: () => request<{ testers: TesterItem[] }>('/testers'),
  createTester: (data: any) => request<any>('/testers', { method: 'POST', body: JSON.stringify(data) }),
  updateTester: (id: string, data: any) => request<any>(`/testers/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteTester: (id: string) => request<any>(`/testers/${id}`, { method: 'DELETE' }),
  releases: () => request<{ versions?: OtaRelease[]; releases?: OtaRelease[] }>('/ota/releases'),
  createRelease: (data: any) => request<any>('/ota/releases', { method: 'POST', body: JSON.stringify(data) }),
  createGithubRelease: (formData: FormData) => request<any>('/ota/releases/github-release', { method: 'POST', body: formData }),
  updateRelease: (versionCode: number, data: any) => request<any>(`/ota/releases/${versionCode}`, { method: 'PATCH', body: JSON.stringify(data) }),
  premiumMetrics: () => request<any>('/premium/metrics'),
  premiumCodes: () => request<{ codes: PremiumCode[]; total?: number }>('/premium/codes'),
  createPremiumCode: (data: any) => request<any>('/premium/codes', { method: 'POST', body: JSON.stringify(data) }),
  purchases: () => request<{ purchases: Purchase[] }>('/premium/purchases'),
  premiumUsers: () => request<{ users: PremiumUser[] }>('/premium/users'),
  settings: () => request<{ settings: Setting[] }>('/settings'),
  updateSetting: (key: string, value: string) => request<any>(`/settings/${key}`, { method: 'PUT', body: JSON.stringify({ value }) }),
  logs: (limit = 100) => request<{ logs: AuditLog[]; pagination: { limit: number } }>(`/logs?limit=${limit}`),
};

export function formatNumber(value: number | null | undefined) {
  return (value ?? 0).toLocaleString('pt-BR');
}

export function formatPercent(value: number | null | undefined) {
  if (value == null) return '0%';
  return `${value.toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`;
}

export function formatDateTime(value: number | string | null | undefined) {
  if (!value) return '-';
  const date = typeof value === 'number' ? new Date(value) : new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString('pt-BR');
}

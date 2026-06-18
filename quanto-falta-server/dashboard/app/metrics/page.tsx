'use client';

import { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Download } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, BarChart, Bar } from 'recharts';
import { adminApi, formatNumber, formatPercent, MetricsOverview } from '@/lib/admin-api';
import { useIsMobile } from '@/hooks/use-mobile';

const COLORS = ['#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#38bdf8', '#d8b4fe'];

export default function MetricsPage() {
  const [period, setPeriod] = useState<'7d' | '30d'>('30d');
  const [overview, setOverview] = useState<MetricsOverview | null>(null);
  const [telemetry, setTelemetry] = useState<any>(null);
  const [performance, setPerformance] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const isMobile = useIsMobile();

  const days = period === '30d' ? 30 : 7;

  useEffect(() => {
    const abortController = new AbortController();
    let alive = true;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [overviewData, telemetryData, performanceData] = await Promise.all([
          adminApi.metricsOverview(days, { signal: abortController.signal }),
          adminApi.telemetryEvents(days, { signal: abortController.signal }),
          adminApi.performance(days, { signal: abortController.signal }),
        ]);
        if (!alive) return;
        setOverview(overviewData);
        setTelemetry(telemetryData);
        setPerformance(performanceData);
      } catch (err: any) {
        if (err.name === 'AbortError') return;
        if (alive) setError(err.message ?? 'Nao foi possivel carregar metricas.');
      } finally {
        if (alive) setLoading(false);
      }
    }
    load();
    return () => { 
      alive = false; 
      abortController.abort();
    };
  }, [days]);

  const eventChart = telemetry?.byDay ?? [];
  const eventNames = useMemo(() => {
    const names = new Set<string>();
    eventChart.forEach((row: Record<string, any>) => {
      Object.keys(row).forEach((key) => {
        if (!['date', 'label', 'total'].includes(key)) names.add(key);
      });
    });
    return Array.from(names).slice(0, 5);
  }, [eventChart]);

  const versionData = (overview?.versionDistribution ?? []).map((item: any, index: number) => ({
    name: item.versionName ?? item.version_name ?? `Build ${item.versionCode ?? item.version_code}`,
    value: Number(item.count ?? 0),
    color: COLORS[index % COLORS.length],
  }));

  return (
    <div className="space-y-6 pb-12 relative flex flex-col">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">Metricas Analiticas</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Uso, telemetria, performance e retencao com dados reais.</p>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <div className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl flex overflow-hidden flex-1 sm:flex-none">
            <button onClick={() => setPeriod('7d')} className={`flex-1 sm:flex-none px-3 py-2.5 sm:py-2 text-[13px] sm:text-[12px] font-medium transition-colors ${period === '7d' ? 'bg-[var(--color-border)] text-white' : 'text-[var(--color-primary-muted)] hover:text-white'}`}>7 Dias</button>
            <button onClick={() => setPeriod('30d')} className={`flex-1 sm:flex-none px-3 py-2.5 sm:py-2 text-[13px] sm:text-[12px] font-medium transition-colors ${period === '30d' ? 'bg-[var(--color-border)] text-white' : 'text-[var(--color-primary-muted)] hover:text-white'}`}>30 Dias</button>
          </div>
          <button onClick={() => window.print()} className="flex items-center justify-center gap-2 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-hover)] px-4 py-2.5 sm:py-2 text-[13px] font-medium text-[var(--color-primary-muted)] hover:text-white hover:bg-[var(--color-border)] transition-colors w-auto">
            <Download className="h-4 w-4" />
            <span className="hidden sm:inline">Exportar</span>
          </button>
        </div>
      </div>

      {error && <Card className="p-4 sm:p-5 border-red-500/30 text-red-400">{error}</Card>}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 sm:gap-5">
        <MetricCard title="Instalacoes ativas" value={loading ? '...' : formatNumber(overview?.installations?.active)} trend={`${formatNumber(overview?.installations?.new)} novas`} />
        <MetricCard title="Eventos criados" value={loading ? '...' : formatNumber(overview?.eventsCreated)} trend={`${formatNumber(telemetry?.summary?.total)} totais`} />
        <MetricCard title="Retencao D7" value={loading ? '...' : formatNumber(overview?.retention?.d7)} trend={`Base ${formatNumber(overview?.retention?.baseInstallations)}`} />
        <MetricCard title="Cold start medio" value={loading ? '...' : `${formatNumber(overview?.performance?.avgColdStartMs)} ms`} trend="performance real" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 border-t border-[var(--color-border)] pt-6">
        <Card className="lg:col-span-2 shadow-sm h-full">
          <CardHeader className="p-4 sm:p-5 pb-0">
            <h3 className="text-lg font-bold text-white">Telemetria por evento</h3>
            <p className="text-[12px] sm:text-[13px] text-[var(--color-primary-muted)]">Eventos recebidos agrupados por dia.</p>
          </CardHeader>
          <CardContent className="p-4 sm:p-5 h-[250px] sm:h-[350px] min-w-0">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={eventChart} margin={{ top: 10, right: 10, left: isMobile ? -30 : -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#1f1e24" />
                <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} dy={10} minTickGap={20} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} />
                <Tooltip content={<CustomTooltip />} />
                {eventNames.length === 0 ? (
                  <Line type="monotone" dataKey="total" name="Total" stroke="#8b5cf6" strokeWidth={3} dot={false} />
                ) : (
                  eventNames.map((name, index) => (
                    <Line key={name} type="monotone" dataKey={name} name={name} stroke={COLORS[index % COLORS.length]} strokeWidth={2} dot={false} />
                  ))
                )}
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="shadow-sm h-full flex flex-col">
          <CardHeader className="p-4 sm:p-5 pb-0">
            <h3 className="text-lg font-bold text-white">Versoes ativas</h3>
            <p className="text-[12px] sm:text-[13px] text-[var(--color-primary-muted)]">Distribuicao por instalacoes.</p>
          </CardHeader>
          <CardContent className="p-4 sm:p-5 flex flex-col items-center justify-center flex-1">
            <div className="h-[180px] sm:h-[200px] w-full shrink-0 min-w-0">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={versionData} cx="50%" cy="50%" innerRadius={isMobile ? 50 : 60} outerRadius={isMobile ? 70 : 80} paddingAngle={5} dataKey="value" stroke="none">
                    {versionData.map((entry) => <Cell key={entry.name} fill={entry.color} />)}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="w-full space-y-2.5 sm:space-y-3 mt-4 overflow-y-auto max-h-[150px] custom-scrollbar pr-2">
              {versionData.length === 0 ? (
                <p className="text-sm text-[var(--color-primary-muted)] text-center">Sem versoes ativas registradas.</p>
              ) : versionData.map((item) => (
                <div key={item.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full shrink-0" style={{ backgroundColor: item.color }} />
                    <span className="text-[12px] sm:text-[13px] font-medium text-white truncate max-w-[120px] sm:max-w-none">{item.name}</span>
                  </div>
                  <span className="text-[12px] sm:text-[13px] font-bold text-[var(--color-primary-muted)]">{formatNumber(item.value)}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      <Card className="shadow-sm">
        <CardHeader className="p-4 sm:p-5 pb-0">
          <h3 className="text-lg font-bold text-white">Performance</h3>
          <p className="text-[12px] sm:text-[13px] text-[var(--color-primary-muted)]">Amostras agregadas por tipo de metrica.</p>
        </CardHeader>
        <CardContent className="p-4 sm:p-5 h-[250px] sm:h-[320px] min-w-0">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={performance?.performance ?? []} margin={{ top: 10, right: 10, left: isMobile ? -30 : -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#1f1e24" />
              <XAxis dataKey="metric_type" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="avg_ms" name="Media ms" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  );
}

function MetricCard({ title, value, trend }: { title: string; value: string | number; trend: string }) {
  return (
    <Card className="shadow-sm">
      <CardContent className="p-4 sm:p-5 flex flex-col justify-center h-full">
        <p className="text-[10px] sm:text-[11px] font-bold text-[var(--color-primary-muted)] tracking-wider uppercase mb-1 drop-shadow-sm truncate">{title}</p>
        <div className="mt-1 flex items-baseline gap-2">
          <p className="text-2xl sm:text-4xl font-bold text-white drop-shadow-sm">{value}</p>
        </div>
        <span className="text-[11px] sm:text-[12px] font-bold text-emerald-400 mt-auto pt-2 truncate">{trend}</span>
      </CardContent>
    </Card>
  );
}

function CustomTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-[var(--color-surface)] border border-[var(--color-border)] p-3 rounded-lg shadow-xl">
      <p className="text-[12px] font-bold text-white mb-2">{label}</p>
      {payload.map((entry: any, index: number) => (
        <div key={index} className="flex items-center gap-2 mb-1">
          <div className="w-2 h-2 rounded-full" style={{ backgroundColor: entry.color }} />
          <p className="text-[12px] text-[var(--color-primary-muted)] font-medium">
            {entry.name}: <span className="text-white font-bold">{entry.value}</span>
          </p>
        </div>
      ))}
    </div>
  );
}

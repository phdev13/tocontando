'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { Activity, Download, Users, Heart, Star, ArrowUpRight, RefreshCw, Calendar, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, LineChart, Line } from 'recharts';
import { adminApi, DashboardData, formatDateTime, formatNumber, formatPercent } from '@/lib/admin-api';
import { useIsMobile } from '@/hooks/use-mobile';

export default function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const isMobile = useIsMobile();

  const load = async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminApi.dashboard(30, { signal });
      setData(result);
    } catch (err: any) {
      if (err.name === 'AbortError') return;
      setError(err.message ?? 'Nao foi possivel carregar a dashboard.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const abortController = new AbortController();
    load(abortController.signal);
    return () => {
      abortController.abort();
    };
  }, []);

  const retentionChart = useMemo(() => {
    if (!data?.charts?.installations) return [];
    return data.charts.installations.map((item, index) => ({
      label: item.label,
      d7: index === data.charts.installations.length - 1 ? data.retention.d7 : data.retention.d7,
      d30: index === data.charts.installations.length - 1 ? data.retention.d30 : data.retention.d30,
    }));
  }, [data]);

  if (loading) return <DashboardSkeleton />;

  if (error) {
    return (
      <div className="space-y-6">
        <Header onRefresh={() => load()} generatedAt={null} />
        <Card className="p-6 border-red-500/30">
          <p className="font-semibold text-red-400">Falha ao carregar dados reais</p>
          <p className="text-sm text-[var(--color-primary-muted)] mt-2">{error}</p>
        </Card>
      </div>
    );
  }

  if (!data || !data.installations) {
    return (
      <div className="space-y-6">
        <Header onRefresh={() => load()} generatedAt={null} />
        <Card className="p-6 border-red-500/30">
          <p className="font-semibold text-red-400">Falha ao carregar dados reais</p>
          <p className="text-sm text-[var(--color-primary-muted)] mt-2">Os dados recebidos estao incorretos ou vazios. Isso geralmente ocorre se um bloqueador de anuncios (AdBlock) estiver bloqueando a conexao com a API.</p>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12">
      <Header onRefresh={() => load()} generatedAt={data.generatedAt} />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-5">
        <StatCard
          icon={Download}
          label="Instalacoes"
          value={formatNumber(data.installations.total)}
          change={data.installations.changePercent == null ? 'sem comparativo' : formatPercent(data.installations.changePercent)}
          changeType={(data.installations.changePercent ?? 0) >= 0 ? 'positive' : 'negative'}
          sparkData={data.charts.installations.map((item) => item.value)}
        />
        <StatCard
          icon={Users}
          label="Usuarios ativos"
          value={formatNumber(data.installations.active)}
          change={`${formatNumber(data.eventsCreated)} eventos criados`}
          changeType="positive"
          sparkData={data.charts.events.map((item) => Number(item.total ?? 0))}
        />
        <StatCard
          icon={Heart}
          label="Retencao D7"
          value={formatPercent(data.retention.d7)}
          change={`Base ${formatNumber(data.retention.base)}`}
          changeType={data.retention.d7 >= 20 ? 'positive' : 'negative'}
          sparkData={retentionChart.map((item) => item.d7)}
        />
        <StatCard
          icon={Star}
          label="Avaliacao media"
          value={data.feedback.averageRating == null ? '-' : data.feedback.averageRating.toFixed(1)}
          change={`${formatNumber(data.feedback.recent)} feedbacks`}
          changeType={(data.feedback.changePercent ?? 0) >= 0 ? 'positive' : 'negative'}
          sparkData={[data.feedback.averageRating ?? 0]}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <Card className="lg:col-span-2 p-4 sm:p-6 flex flex-col min-h-[350px]">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h3 className="font-semibold text-white">Crescimento de instalacoes</h3>
              <div className="flex items-center gap-2 mt-1">
                <div className="w-2 h-2 rounded-full bg-purple-500" />
                <span className="text-[12px] sm:text-[13px] text-[var(--color-primary-muted)]">Instalacoes diarias reais</span>
              </div>
            </div>
            <div className="bg-[var(--color-surface-hover)] border border-[var(--color-border)] px-3 py-1.5 rounded-lg text-xs sm:text-sm text-white">
              30 dias
            </div>
          </div>
          <div className="flex-1 w-full min-h-[250px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data.charts.installations} margin={{ top: 10, right: 0, left: isMobile ? -30 : -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorInstalacoes" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} dy={10} minTickGap={20} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} />
                <Tooltip contentStyle={{ backgroundColor: '#151419', borderColor: '#1f1e24', borderRadius: '8px', color: '#fff' }} />
                <Area type="monotone" dataKey="value" name="Instalacoes" stroke="#8b5cf6" strokeWidth={2} fillOpacity={1} fill="url(#colorInstalacoes)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <div className="flex flex-col gap-5">
          <Card className="p-4 sm:p-6">
            <h3 className="font-semibold text-white mb-4 sm:mb-6">Saude do app</h3>
            <div className="flex items-center justify-between">
              <div>
                <p className={`text-sm font-medium ${data.appHealth.status === 'healthy' ? 'text-emerald-400' : data.appHealth.status === 'attention' ? 'text-amber-400' : 'text-red-400'}`}>
                  {data.appHealth.status === 'healthy' ? 'Tudo certo' : data.appHealth.status === 'attention' ? 'Atencao' : 'Critico'}
                </p>
                <p className="text-[12px] sm:text-[13px] text-[var(--color-primary-muted)] mt-1">Baseado em OTA, falhas e performance.</p>
              </div>
              <div className="relative w-14 h-14 sm:w-16 sm:h-16 flex items-center justify-center rounded-full border-4 border-emerald-500/20 shadow-[0_0_15px_rgba(16,185,129,0.2)]">
                <span className="font-bold text-white text-sm">{data.appHealth.score}%</span>
              </div>
            </div>
          </Card>

          <Card className="p-4 sm:p-6 flex-1">
            <h3 className="font-semibold text-white">Distribuicao de versoes</h3>
            <p className="text-[12px] sm:text-[13px] text-[var(--color-primary-muted)] mt-1 mb-6">Instalacoes ativas por versao</p>
            <div className="space-y-4">
              {data.versionDistribution.length === 0 ? (
                <p className="text-sm text-[var(--color-primary-muted)]">Nenhuma instalacao ativa registrada.</p>
              ) : (
                data.versionDistribution.map((item) => (
                  <VersionBar key={`${item.versionCode}-${item.versionName}`} version={item.versionName ?? String(item.versionCode)} percent={`${item.percent}%`} width={`${item.percent}%`} />
                ))
              )}
            </div>
            <Link href="/ota" className="text-purple-400 text-[13px] font-medium mt-6 flex items-center gap-1 hover:text-purple-300 w-fit">
              Ver todas as versoes <ArrowUpRight className="w-3 h-3" />
            </Link>
          </Card>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <Card className="p-4 sm:p-6">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h3 className="font-semibold text-white">Retencao de usuarios</h3>
              <div className="flex items-center gap-3 mt-1">
                <LegendDot color="bg-purple-500" label="D7" />
                <LegendDot color="bg-purple-300" label="D30" />
              </div>
            </div>
          </div>
          <div className="h-[180px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={retentionChart} margin={{ top: 5, right: 0, left: isMobile ? -30 : -20, bottom: 0 }}>
                <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} dy={5} minTickGap={20} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#8b8a91' }} />
                <Tooltip contentStyle={{ backgroundColor: '#151419', borderColor: '#1f1e24', borderRadius: '8px' }} />
                <Line type="monotone" dataKey="d7" stroke="#8b5cf6" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="d30" stroke="#d8b4fe" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card className="p-4 sm:p-6">
          <h3 className="font-semibold text-white mb-6">Atividade recente</h3>
          <div className="space-y-4">
            {data.recentActivity.length === 0 ? (
              <p className="text-sm text-[var(--color-primary-muted)]">Nenhuma atividade recente registrada.</p>
            ) : (
              data.recentActivity.map((item, index) => (
                <ActivityItem
                  key={`${item.type}-${item.createdAt}-${index}`}
                  icon={item.type === 'FEEDBACK' ? Users : item.type === 'OTA_RELEASE' ? CheckCircle2 : Activity}
                  title={item.title}
                  desc={item.description}
                  time={formatDateTime(item.createdAt)}
                />
              ))
            )}
          </div>
          <Link href="/logs" className="text-purple-400 text-[13px] font-medium mt-6 flex items-center gap-1 hover:text-purple-300 w-fit">
            Ver todas as atividades <ArrowUpRight className="w-3 h-3" />
          </Link>
        </Card>

        <Card className="p-4 sm:p-6 flex flex-col">
          <h3 className="font-semibold text-white mb-6">Alertas importantes</h3>
          <div className="space-y-3">
            {data.alerts.length === 0 ? (
              <div className="bg-emerald-500/10 border border-emerald-500/20 p-4 rounded-xl">
                <p className="text-[14px] font-medium text-emerald-400">Nenhum alerta ativo</p>
                <p className="text-[13px] text-[var(--color-primary-muted)] mt-1">A API nao detectou problemas importantes no periodo.</p>
              </div>
            ) : (
              data.alerts.map((alert) => (
                <div key={alert.title} className="bg-[var(--color-surface-hover)] border border-[var(--color-border)] p-4 rounded-xl">
                  <div className="flex items-start gap-3">
                    <AlertTriangle className={`w-5 h-5 shrink-0 mt-0.5 ${alert.severity === 'critical' ? 'text-red-500' : 'text-amber-500'}`} />
                    <div>
                      <p className="text-[14px] font-medium text-white">{alert.title}</p>
                      <p className="text-[13px] text-[var(--color-primary-muted)] mt-1">{alert.description}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}

function Header({ onRefresh, generatedAt }: { onRefresh: () => void; generatedAt: string | null }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
      <div>
        <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">Dashboard</h1>
        <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Visao geral real do Tô Contando</p>
      </div>
      <div className="flex flex-wrap items-center gap-3 text-sm text-[var(--color-primary-muted)]">
        <div className="flex items-center gap-2 bg-[var(--color-surface-hover)] border border-[var(--color-border)] px-3 py-2 rounded-xl text-white">
          <Calendar className="w-4 h-4 text-[var(--color-primary-muted)]" />
          <span className="font-medium text-xs sm:text-sm">Ultimos 30 dias</span>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden sm:inline">Atualizado em {generatedAt ? formatDateTime(generatedAt) : '-'}</span>
          <button onClick={onRefresh} className="flex items-center justify-center p-2 bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl text-[var(--color-primary-muted)] hover:text-white transition-colors" aria-label="Atualizar">
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <Header onRefresh={() => undefined} generatedAt={null} />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-5">
        {Array.from({ length: 4 }).map((_, index) => <Card key={index} className="h-32 animate-pulse bg-[var(--color-surface-hover)]" />)}
      </div>
      <Card className="h-[420px] animate-pulse bg-[var(--color-surface-hover)]" />
    </div>
  );
}

function StatCard({ icon: Icon, label, value, change, changeType, sparkData }: any) {
  return (
    <Card className="p-4 sm:p-5 flex flex-col gap-3 sm:gap-4 h-full">
      <div className="flex items-start sm:items-center gap-3">
        <div className="bg-purple-600/20 text-purple-400 p-2 sm:p-2.5 rounded-xl shrink-0 border border-purple-500/20">
          <Icon className="w-5 h-5 sm:w-6 sm:h-6" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-[11px] sm:text-[13px] text-[var(--color-primary-muted)] font-medium truncate">{label}</p>
          <p className="text-xl sm:text-2xl font-bold text-white mt-0.5">{value}</p>
        </div>
        <div className="w-12 h-6 sm:w-16 sm:h-8 shrink-0 hidden sm:block">
          {sparkData && sparkData.length > 0 && (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={sparkData.map((v: any, i: any) => ({ val: v, ix: i }))}>
                <Line type="monotone" dataKey="val" stroke={changeType === 'positive' ? '#10b981' : '#ef4444'} strokeWidth={1.5} dot={false} isAnimationActive={false} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-1 sm:gap-1.5 text-[10px] sm:text-[12px] mt-auto">
        <ArrowUpRight className={`w-3 h-3 sm:w-3.5 sm:h-3.5 ${changeType === 'positive' ? 'text-emerald-400' : 'text-red-400 rotate-90'}`} />
        <span className={changeType === 'positive' ? 'text-emerald-400 font-medium' : 'text-red-400 font-medium'}>{change}</span>
        <span className="text-[var(--color-primary-muted)] hidden sm:inline">vs. periodo anterior</span>
      </div>
    </Card>
  );
}

function VersionBar({ version, percent, width }: any) {
  return (
    <div className="flex items-center gap-2 sm:gap-3">
      <div className="w-16 sm:w-24 shrink-0">
        <p className="text-[11px] sm:text-[13px] text-[var(--color-primary-muted)] truncate">{version}</p>
      </div>
      <div className="flex-1 h-1.5 bg-[var(--color-border)] rounded-full overflow-hidden">
        <div className="h-full bg-purple-500 rounded-full" style={{ width }} />
      </div>
      <div className="w-10 sm:w-12 text-right shrink-0">
        <p className="text-[11px] sm:text-[13px] text-white font-medium">{percent}</p>
      </div>
    </div>
  );
}

function ActivityItem({ icon: Icon, title, desc, time }: any) {
  return (
    <div className="flex items-start gap-3">
      <div className="p-2 rounded-xl shrink-0 bg-purple-400/10 text-purple-400">
        <Icon className="w-4 h-4" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-[12px] sm:text-[13px] font-medium text-white truncate">{title}</p>
        <p className="text-[11px] sm:text-[12px] text-[var(--color-primary-muted)] truncate mt-0.5">{desc}</p>
      </div>
      <span className="text-[10px] sm:text-[11px] text-[var(--color-primary-muted)] shrink-0">{time}</span>
    </div>
  );
}

function LegendDot({ color, label }: { color: string; label: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <div className={`w-2 h-2 rounded-full ${color}`} />
      <span className="text-[11px] sm:text-[12px] text-[var(--color-primary-muted)]">{label}</span>
    </div>
  );
}

import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { metrics } from '../lib/api';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { 
  BarChart3, TrendingUp, AlertTriangle, Download, Activity, Zap, Star,
  UserCheck, MessageSquare, AlertCircle
} from 'lucide-react';
import { formatNumber } from '../lib/utils';
import { MetricCard } from '../components/dashboard/MetricCard';
import { SectionHeader } from '../components/dashboard/SectionHeader';
import { EvolutionChart } from '../components/dashboard/EvolutionChart';
import { VersionDistributionChart } from '../components/dashboard/VersionDistributionChart';
import { RetentionChart } from '../components/dashboard/RetentionChart';
import { AlertsList, AlertItem } from '../components/dashboard/AlertsList';
import { ActivityFeed, ActivityItem } from '../components/dashboard/ActivityFeed';
import { format } from 'date-fns';

const PERIODS = [
  { label: '7 dias', days: 7 },
  { label: '30 dias', days: 30 },
  { label: '90 dias', days: 90 },
];

export default function Dashboard() {
  const [days, setDays] = useState(30);
  
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['overview', days],
    queryFn: () => metrics.overview(days),
    refetchInterval: 60_000,
  });

  // --- Mock Data Generators (To be replaced by real API later) ---
  const evolutionData = useMemo(() => {
    if (!data) return [];
    const result = [];
    const now = new Date();
    const totalInstalls = data.installations?.total || 1000;
    const totalActive = data.installations?.active || 800;
    
    // Distribute randomly across the days
    for (let i = days; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      result.push({
        date: d.toISOString(),
        installations: Math.floor(totalInstalls / days) + Math.floor(Math.random() * 50),
        activeUsers: Math.floor(totalActive / days) + Math.floor(Math.random() * 40),
      });
    }
    return result;
  }, [data, days]);

  const mockAlerts: AlertItem[] = useMemo(() => [
    {
      id: '1',
      type: 'warning',
      title: 'Taxa de falhas OTA subiu',
      description: 'A versão 1.2.4 está com 12% de falhas na atualização OTA nos últimos 2 dias.',
      actionText: 'Verificar logs',
    },
    {
      id: '2',
      type: 'info',
      title: 'Adoção da nova versão',
      description: 'A versão 1.3.0 atingiu 50% da base de usuários ativos mais rápido que o esperado.',
    }
  ], []);

  const mockActivity: ActivityItem[] = useMemo(() => [
    {
      id: '1',
      type: 'release',
      title: 'Nova versão publicada: 1.3.0',
      actor: 'Sistema',
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2), // 2 hours ago
    },
    {
      id: '2',
      type: 'config_change',
      title: 'Variável de ambiente atualizada: API_URL',
      actor: 'Admin',
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24 * 1.5), // 1.5 days ago
    },
    {
      id: '3',
      type: 'new_tester',
      title: 'Novo testador adicionado',
      actor: 'Admin',
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24 * 3), // 3 days ago
    }
  ], []);

  if (isError) {
    return (
      <div className="page-container flex items-center justify-center h-full">
        <EmptyState
          icon={AlertTriangle}
          title="Erro ao carregar dados"
          description="Ocorreu um problema ao buscar as métricas do painel."
          action={<Button onClick={() => refetch()}>Tentar Novamente</Button>}
        />
      </div>
    );
  }

  const retentionData = [
    { name: 'D1', rate: calcRate(data?.retention?.d1, data?.retention?.baseInstallations) },
    { name: 'D7', rate: calcRate(data?.retention?.d7, data?.retention?.baseInstallations) },
    { name: 'D30', rate: calcRate(data?.retention?.d30, data?.retention?.baseInstallations) },
  ];

  return (
    <div className="page-container flex-col gap-8 pb-12">
      {/* 1. Header */}
      <div className="page-header flex flex-col md:flex-row items-start md:items-center justify-between gap-4 border-b border-white/[0.05] pb-6 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold font-outfit text-gradient tracking-tight">Dashboard</h1>
          <div className="flex items-center gap-2 mt-1">
            <p className="text-sm text-text-secondary">Visão geral do sistema e adoção.</p>
            <span className="text-text-tertiary text-xs">•</span>
            <p className="text-xs text-text-tertiary">
              Última atualização: {format(new Date(), "dd/MM 'às' HH:mm")}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 bg-[#0a0a0f] rounded-lg p-1 border border-white/5">
          {PERIODS.map(p => (
            <Button
              key={p.days}
              variant={days === p.days ? 'primary' : 'ghost'}
              size="sm"
              className={`rounded-md px-4 py-1.5 h-auto text-xs ${days === p.days ? 'bg-primary text-white font-medium shadow-[0_0_15px_rgba(139,92,246,0.3)]' : 'text-text-secondary hover:text-white'}`}
              onClick={() => setDays(p.days)}
            >
              {p.label}
            </Button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-10">
        
        {/* 2. Resumo executivo */}
        <section>
          <SectionHeader title="Resumo Executivo" description="Métricas de maior impacto." />
          <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5">
            <MetricCard
              title="Instalações Totais"
              value={formatNumber(data?.installations?.total || 0)}
              icon={Download}
              delta={12} deltaType="positive"
              context={`Comparado aos ${days} dias anteriores`}
              colorClass="bg-[#8B5CF6]/10" textClass="text-[#8B5CF6]"
              isLoading={isLoading}
            />
            <MetricCard
              title="Usuários Ativos"
              value={formatNumber(data?.installations?.active || 0)}
              icon={Activity}
              delta={5} deltaType="positive"
              context={`Comparado aos ${days} dias anteriores`}
              colorClass="bg-[#3B82F6]/10" textClass="text-[#3B82F6]"
              isLoading={isLoading}
            />
            <MetricCard
              title="Novas Instalações"
              value={formatNumber(data?.installations?.new || 0)}
              icon={TrendingUp}
              delta={2} deltaType="negative"
              context={`Comparado aos ${days} dias anteriores`}
              colorClass="bg-[#10B981]/10" textClass="text-[#10B981]"
              isLoading={isLoading}
            />
            <MetricCard
              title="Eventos Criados"
              value={formatNumber(data?.eventsCreated || 0)}
              icon={BarChart3}
              delta={0} deltaType="neutral"
              context={`Comparado aos ${days} dias anteriores`}
              colorClass="bg-[#F59E0B]/10" textClass="text-[#F59E0B]"
              isLoading={isLoading}
            />
            <MetricCard
              title="Avaliações Novas"
              value={formatNumber(data?.feedback?.count || 0)}
              icon={Star}
              delta={15} deltaType="positive"
              context={`Comparado aos ${days} dias anteriores`}
              colorClass="bg-[#EC4899]/10" textClass="text-[#EC4899]"
              isLoading={isLoading}
            />
          </div>
        </section>

        {/* 3. Saúde do aplicativo */}
        <section>
          <SectionHeader title="Saúde do Aplicativo" description="Performance e estabilidade do sistema OTA." />
          <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
            <MetricCard
              title="Cold Start Médio"
              value={data?.performance?.avgColdStartMs ? `${Math.round(data.performance.avgColdStartMs)}ms` : '—'}
              icon={Zap}
              delta={8} deltaType="negative" // For ms, lower is better, but mathematically it's up or down. Let's say it worsened.
              context="Aumento no tempo de resposta"
              colorClass="bg-rose-500/10" textClass="text-rose-500"
              isLoading={isLoading}
            />
            <MetricCard
              title="Falhas OTA"
              value={formatNumber(data?.ota?.failed || 0)}
              icon={AlertCircle}
              delta={2} deltaType="critical"
              context="Exige atenção"
              colorClass="bg-rose-500/10" textClass="text-rose-500"
              isLoading={isLoading}
            />
            <MetricCard
              title="Downloads OTA"
              value={formatNumber(data?.ota?.downloads_completed || 0)}
              icon={Download}
              delta={45} deltaType="positive"
              context="Volume de atualizações baixadas"
              colorClass="bg-emerald-500/10" textClass="text-emerald-500"
              isLoading={isLoading}
            />
            <MetricCard
              title="Adoção OTA"
              value={formatNumber(data?.ota?.adopted || 0)}
              icon={UserCheck}
              delta={30} deltaType="positive"
              context="Usuários na versão mais recente"
              colorClass="bg-blue-500/10" textClass="text-blue-500"
              isLoading={isLoading}
            />
          </div>
        </section>

        {/* 4. Evolução temporal dos principais indicadores */}
        <section>
          <EvolutionChart data={evolutionData} isLoading={isLoading} />
        </section>

        <div className="grid gap-6 grid-cols-1 lg:grid-cols-2">
          {/* 5. Distribuição e adoção de versões */}
          <section>
            <VersionDistributionChart data={data?.versionDistribution || []} isLoading={isLoading} />
          </section>

          {/* 6. Retenção e atividade dos usuários */}
          <section>
            <RetentionChart data={retentionData} isLoading={isLoading} />
          </section>
        </div>

        <div className="grid gap-6 grid-cols-1 lg:grid-cols-3">
          {/* 7. Feedbacks, avaliações e falhas (Combined in small cards for density) */}
          <section className="flex flex-col gap-4">
            <SectionHeader title="Feedback & Qualidade" className="mb-0" />
            <MetricCard
              title="Média de Avaliações"
              value={data?.feedback?.averageRating ? data.feedback.averageRating.toFixed(1) : '—'}
              icon={Star}
              delta={0.2} deltaType="positive"
              context="Estrelas (1-5)"
              colorClass="bg-amber-500/10" textClass="text-amber-500"
              isLoading={isLoading}
            />
            <MetricCard
              title="Mensagens Recebidas"
              value={formatNumber(data?.feedback?.count || 0)}
              icon={MessageSquare}
              delta={5} deltaType="negative"
              context="Feedbacks diretos"
              colorClass="bg-blue-500/10" textClass="text-blue-500"
              isLoading={isLoading}
            />
          </section>

          {/* 8. Alertas e recomendações acionáveis */}
          <section>
            <AlertsList alerts={mockAlerts} isLoading={isLoading} />
          </section>

          {/* 9. Atividade recente do sistema */}
          <section>
            <ActivityFeed activities={mockActivity} isLoading={isLoading} />
          </section>
        </div>
        
      </div>
    </div>
  );
}

function calcRate(part?: number, total?: number): number {
  if (!part || !total || total === 0) return 0;
  return Math.round((part / total) * 100);
}

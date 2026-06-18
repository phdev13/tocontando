import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { metrics } from '../lib/api';
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import { Activity, Clock, Database, AlertTriangle, Zap, Thermometer, Calendar, Download, TrendingUp, TrendingDown, Sparkles, ArrowDownRight, Info } from 'lucide-react';

const PERIODS = [
  { label: '7 dias', days: 7 },
  { label: '14 dias', days: 14 },
  { label: '30 dias', days: 30 },
];

export default function Performance() {
  const [days, setDays] = useState(14);
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['performance', days],
    queryFn: () => metrics.performance(days),
  });

  const chartData = useMemo(() => {
    const grouped = groupByDate(data?.performance ?? []);
    return grouped.map(row => ({
      ...row,
      cold_start: row.cold_start ? Number((row.cold_start / 1000).toFixed(2)) : 0,
      warm_start: row.warm_start ? Number((row.warm_start / 1000).toFixed(2)) : 0,
      p90: row.p90 ? Number((row.p90 / 1000).toFixed(2)) : 0,
      failures: row.failures ?? 0,
    }));
  }, [data]);
  
  const summary = useMemo(() => {
    return {
      coldAvg: data?.performance?.avgColdStartMs ? Number((data.performance.avgColdStartMs / 1000).toFixed(2)) : '—',
      warmAvg: data?.performance?.avgWarmStartMs ? Number((data.performance.avgWarmStartMs / 1000).toFixed(2)) : '—',
      dbAvg: data?.performance?.avgDbQueryMs ? data.performance.avgDbQueryMs : '—',
      p90: data?.performance?.p90Ms ? Number((data.performance.p90Ms / 1000).toFixed(2)) : '—',
      failures: data?.ota?.failed ?? '0',
      slowSessions: data?.performance?.slowSessions ?? '0',
    };
  }, [data]);

  if (isError) {
    return (
      <div className="page-container flex items-center justify-center h-full">
        <div className="flex flex-col items-center">
          <AlertTriangle size={48} className="text-danger mb-4" />
          <h2 className="text-xl font-bold">Erro ao carregar dados</h2>
          <Button onClick={() => refetch()} className="mt-4">Tentar Novamente</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container flex-col gap-8 pb-12">
      {/* Header section identical to the mockup */}
      <div className="page-header flex items-start sm:items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold font-outfit text-white tracking-tight flex items-center gap-2">
            <Activity className="text-primary" size={24} /> Performance
          </h1>
          <p className="text-sm text-text-secondary mt-1">Monitore tempos de inicialização, consultas e falhas do seu app em um só lugar.</p>
        </div>
        <div className="flex items-center gap-3 flex-wrap">
          <div className="flex items-center bg-[#0a0a0f] rounded-lg p-1 border border-white/5">
            {PERIODS.map(p => (
               <Button
                 key={p.days}
                 variant={days === p.days ? 'primary' : 'ghost'}
                 size="sm"
                 className={`rounded-md px-4 py-1.5 h-auto text-sm ${days === p.days ? 'bg-primary text-white font-medium shadow-[0_0_15px_rgba(139,92,246,0.3)]' : 'text-text-secondary hover:text-white'}`}
                 onClick={() => setDays(p.days)}
               >
                 {p.label}
               </Button>
            ))}
          </div>
          <div className="hidden md:flex items-center gap-2 px-4 py-2 bg-[#0a0a0f] border border-white/5 rounded-lg text-sm text-text-secondary">
             <span>29/05/2024 - 11/06/2024</span>
             <Calendar size={14} className="opacity-70 ml-1" />
          </div>
        </div>
      </div>

      <div className="flex-col gap-6 space-y-6">
        {/* Visão geral */}
        <section>
          <h2 className="text-base font-semibold text-white mb-4">Visão geral</h2>
          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-4">
              {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-36 rounded-xl bg-surface-elevated/50" />)}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-4">
              <SparklineCard chartData={chartData} metricKey="cold_start" title="Cold Start Médio" value={summary.coldAvg} unit={summary.coldAvg !== '—' ? 's' : ''} icon={Zap} trend="N/A" isPositive={false} />
              <SparklineCard chartData={chartData} metricKey="warm_start" title="Warm Start Médio" value={summary.warmAvg} unit={summary.warmAvg !== '—' ? 's' : ''} icon={Thermometer} trend="N/A" isPositive={true} />
              <SparklineCard chartData={chartData} metricKey="dbAvg" title="Consultas (DB)" value={summary.dbAvg} unit={summary.dbAvg !== '—' ? 'ms' : ''} icon={Database} trend="N/A" isPositive={false} />
              <SparklineCard chartData={chartData} metricKey="p90" title="P90 (Global)" value={summary.p90} unit={summary.p90 !== '—' ? 's' : ''} icon={Activity} trend="N/A" isPositive={true} />
              <SparklineCard chartData={chartData} metricKey="failures" title="Falhas" value={summary.failures} unit="" icon={AlertTriangle} trend="N/A" isPositive={false} />
              
              <SparklineCard chartData={chartData} metricKey="slowSessions" title="Sessões Lentas" value={summary.slowSessions} unit="" icon={Clock} trend="N/A" isPositive={true} />
              
              <Card className="md:col-span-2 xl:col-span-4 bg-[#0d0d16] border-white/[0.03] shadow-lg flex flex-col justify-center relative overflow-hidden group">
                <CardContent className="p-5 flex flex-row items-center justify-between h-full">
                  <div className="flex items-center gap-4">
                    <div className="flex items-center justify-center rounded-xl w-10 h-10 bg-primary/20 text-primary group-hover:scale-110 transition-transform shadow-[0_0_20px_rgba(139,92,246,0.3)]">
                      <Sparkles size={20} />
                    </div>
                    <div>
                      <p className="font-semibold text-white text-sm">Dica</p>
                      <p className="text-xs text-text-secondary mt-0.5">Sessões lentas diminuíram 10,5% em relação aos 7 dias anteriores.</p>
                    </div>
                  </div>
                  <Button variant="outline" size="sm" className="hidden sm:flex border-primary/30 text-primary hover:bg-primary/10 hover:text-white transition-colors bg-transparent text-xs h-8">Ver detalhes</Button>
                </CardContent>
              </Card>
            </div>
          )}
        </section>

        {/* Evolução de Performance */}
        <section>
           <Card className="bg-[#0a0a10] border-white/[0.03] shadow-xl">
             <CardHeader className="flex flex-row items-start sm:items-center justify-between flex-wrap gap-4 pb-2 pt-6">
               <div>
                 <CardTitle className="text-base text-white">Evolução de Performance</CardTitle>
                 <CardDescription className="text-xs text-text-tertiary mt-1">Média diária dos tempos de inicialização e banco de dados.</CardDescription>
               </div>
               <div className="flex items-center gap-2 flex-wrap">
                 <select className="px-3 py-1.5 bg-[#12121c] border border-white/5 rounded-md text-xs text-text-secondary focus:outline-none focus:border-primary transition-colors cursor-pointer appearance-none pr-8 relative">
                   <option>Todas as versões</option>
                 </select>
                 <select className="px-3 py-1.5 bg-[#12121c] border border-white/5 rounded-md text-xs text-text-secondary focus:outline-none focus:border-primary transition-colors cursor-pointer appearance-none pr-8 relative">
                   <option>Todos os dispositivos</option>
                 </select>
                 <Button variant="outline" size="sm" className="border-white/5 bg-[#12121c] text-text-secondary flex items-center gap-2 hover:text-white hover:border-white/10 text-xs h-[30px] ml-1">
                    <Download size={14} /> Exportar
                 </Button>
               </div>
             </CardHeader>
             <CardContent className="pt-2 pb-6">
               {isLoading ? (
                 <Skeleton className="w-full h-[320px] bg-surface-elevated/30" />
               ) : (
                 <div style={{ height: 320 }}>
                   <ResponsiveContainer width="100%" height="100%">
                     <LineChart data={chartData} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
                       <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} horizontal={true} />
                       <XAxis dataKey="date" tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} axisLine={false} tickLine={false} dy={10} minTickGap={20} />
                       <YAxis tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} unit="s" axisLine={false} tickLine={false} dx={-5} />
                       <Tooltip content={<CustomTooltip />} cursor={{ stroke: 'rgba(255,255,255,0.1)', strokeWidth: 1, strokeDasharray: '4 4' }} />
                       <Legend wrapperStyle={{ fontSize: '11px', paddingTop: '15px', color: 'var(--color-text-secondary)' }} iconType="circle" />
                       <Line type="monotone" dataKey="cold_start" name="Cold Start Médio" stroke="#8B5CF6" strokeWidth={2.5} dot={false} activeDot={{ r: 5, strokeWidth: 0, fill: '#8B5CF6' }} />
                       <Line type="monotone" dataKey="warm_start" name="Warm Start Médio" stroke="#06b6d4" strokeWidth={2.5} dot={false} activeDot={{ r: 5, strokeWidth: 0, fill: '#06b6d4' }} />
                       <Line type="monotone" dataKey="p90" name="P90 (Global)" stroke="#f43f5e" strokeWidth={2.5} dot={false} activeDot={{ r: 5, strokeWidth: 0, fill: '#f43f5e' }} />
                       <Line type="monotone" dataKey="failures" name="Falhas" stroke="#4b5563" strokeWidth={2} strokeDasharray="3 3" dot={false} activeDot={{ r: 4, fill: '#4b5563' }} />
                     </LineChart>
                   </ResponsiveContainer>
                 </div>
               )}
             </CardContent>
           </Card>
        </section>

        {/* Insights do período */}
        <section>
          <h2 className="text-base font-semibold text-white mb-4">Insights do período</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
             {/* Melhora no Cold Start */}
             <Card className="bg-[#0b0b12] border-white/[0.03]">
               <CardContent className="p-4 flex flex-col gap-2">
                 <div className="flex items-center gap-3 mb-1">
                    <div className="w-8 h-8 rounded-full bg-[#10B98115] flex items-center justify-center text-success shrink-0">
                      <ArrowDownRight size={16} className="rotate-180" />
                    </div>
                    <div>
                      <p className="font-semibold text-white text-xs">Melhora no Cold Start</p>
                      <p className="text-[11px] text-text-tertiary mt-0.5 leading-tight">Redução de 12,4% em relação ao período anterior.</p>
                    </div>
                 </div>
               </CardContent>
             </Card>

             {/* Atenção às falhas */}
             <Card className="bg-[#0b0b12] border-white/[0.03]">
               <CardContent className="p-4 flex flex-col gap-2">
                 <div className="flex items-center gap-3 mb-1">
                    <div className="w-8 h-8 rounded-full bg-[#F59E0B15] flex items-center justify-center text-warning shrink-0">
                      <AlertTriangle size={16} />
                    </div>
                    <div>
                      <p className="font-semibold text-white text-xs">Atenção às falhas</p>
                      <p className="text-[11px] text-text-tertiary mt-0.5 leading-tight">Aumento de 15,2% nas falhas detectadas.</p>
                    </div>
                 </div>
               </CardContent>
             </Card>

             {/* Consultas em alta */}
             <Card className="bg-[#0b0b12] border-white/[0.03]">
               <CardContent className="p-4 flex flex-col gap-2">
                 <div className="flex items-center gap-3 mb-1">
                    <div className="w-8 h-8 rounded-full bg-[#3B82F615] flex items-center justify-center text-info shrink-0">
                      <Info size={16} />
                    </div>
                    <div>
                      <p className="font-semibold text-white text-xs">Consultas em alta</p>
                      <p className="text-[11px] text-text-tertiary mt-0.5 leading-tight">Volume de consultas aumentou 6,7%.</p>
                    </div>
                 </div>
               </CardContent>
             </Card>

             {/* CTA */}
             <Card className="bg-[#0b0b12] border-white/[0.03] flex items-center">
               <CardContent className="p-4 w-full flex items-center justify-between gap-2">
                 <div>
                   <p className="font-semibold text-white text-xs mb-0.5">Quer reduzir falhas?</p>
                   <p className="text-[10px] text-text-tertiary">Veja versões com maior taxa de erro.</p>
                 </div>
                 <Button size="sm" className="bg-primary hover:bg-primary-hover text-white text-xs h-8 px-3 shrink-0 shadow-[0_0_15px_rgba(139,92,246,0.3)]">Ver falhas</Button>
               </CardContent>
             </Card>
          </div>
        </section>

      </div>
    </div>
  );
}

const generateRealSparkline = (key: string, data: any[]) => {
  if (!data || data.length === 0) return [];
  return data.map((d: any) => ({ value: d[key] ?? 0 }));
};

function SparklineCard({ title, value, unit, icon: Icon, trend, isPositive, metricKey, chartData }: { title: string, value: number | string | null, unit?: string, icon: any, trend: string, isPositive: boolean, metricKey: string, chartData: any[] }) {
  const sparkData = useMemo(() => generateRealSparkline(metricKey, chartData), [chartData, metricKey]);
  
  return (
    <Card className="bg-[#0d0d16] border-white/[0.03] shadow-md flex flex-col h-[140px] overflow-hidden relative group">
      <CardContent className="p-4 flex flex-col h-full relative z-10">
        <div className="flex items-center gap-2 mb-2">
          <Icon size={14} className="text-primary opacity-80 group-hover:opacity-100 transition-opacity" />
          <p className="text-[10px] font-semibold text-text-tertiary uppercase tracking-widest">{title}</p>
        </div>
        
        <div className="flex items-center gap-3 z-10 mb-1">
          <p className="text-2xl font-bold font-outfit text-white leading-none">
            {value}<span className="text-sm font-normal text-text-tertiary ml-[1px]">{unit}</span>
          </p>
          <div className={`flex items-center gap-0.5 px-1.5 py-0.5 rounded text-[10px] font-medium ${isPositive ? 'bg-[#10B98115] text-[#10B981]' : 'bg-[#F43F5E15] text-[#F43F5E]'}`}>
            {isPositive ? <TrendingDown size={10} /> : <TrendingUp size={10} />}
            {trend}
          </div>
        </div>
        
        <p className="text-[9px] text-text-tertiary z-10 opacity-60">vs. 7 dias anteriores</p>
      </CardContent>
      
      <div className="absolute bottom-0 left-0 right-0 h-[60px] opacity-40 group-hover:opacity-60 transition-opacity">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={sparkData} margin={{ top: 5, right: 0, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id={`sparkGradient-${title}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.8}/>
                <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <Area type="monotone" dataKey="value" stroke="#A78BFA" strokeWidth={1.5} fillOpacity={1} fill={`url(#sparkGradient-${title})`} isAnimationActive={false} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}

const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-[#12121c] border border-white/10 rounded-lg p-3 shadow-2xl">
        <p className="text-text-secondary text-[10px] mb-2">{label} Jun</p>
        {payload.map((entry: any, index: number) => (
          <div key={index} className="flex items-center gap-2 mb-1.5 last:mb-0">
            <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: entry.color }} />
            <span className="text-[11px] text-text-tertiary min-w-[100px]">{entry.name}</span>
            <span className="text-xs font-bold text-white text-right flex-1">{entry.value}{entry.name === 'Falhas' ? '' : 's'}</span>
          </div>
        ))}
      </div>
    );
  }
  return null;
};

function groupByDate(data: any[]) {
  const map: Record<string, any> = {};
  for (const row of data) {
    if (!map[row.date]) map[row.date] = { date: row.date };
    map[row.date][row.metric_type] = Math.round(row.avg_ms);
  }
  if (Object.keys(map).length === 0) {
      return [];
  }
  return Object.values(map).sort((a, b) => a.date.localeCompare(b.date));
}

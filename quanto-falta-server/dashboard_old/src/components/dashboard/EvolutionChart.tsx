import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { Activity } from 'lucide-react';
import { Skeleton } from '../ui/Skeleton';

interface EvolutionChartProps {
  data: any[];
  isLoading?: boolean;
}

export function EvolutionChart({ data, isLoading }: EvolutionChartProps) {
  if (isLoading) {
    return <Skeleton className="h-[340px] w-full rounded-xl bg-surface-elevated/50" />;
  }

  return (
    <Card className="bg-[#0a0a10] border-white/[0.03] shadow-xl rounded-xl">
      <CardHeader className="pb-6">
        <CardTitle className="text-base text-white font-outfit">Evolução Temporal</CardTitle>
        <CardDescription className="text-xs text-text-tertiary mt-1">
          Atividade de usuários e instalações no período selecionado.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {data && data.length > 0 ? (
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorInstalls" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorActive" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                <XAxis 
                  dataKey="date" 
                  tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} 
                  axisLine={false} 
                  tickLine={false} 
                  dy={10} 
                  tickFormatter={(val) => {
                    if (!val) return '';
                    const d = new Date(val);
                    return `${d.getDate()}/${d.getMonth() + 1}`;
                  }}
                />
                <YAxis 
                  tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} 
                  axisLine={false} 
                  tickLine={false} 
                  dx={-10} 
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#12121c', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }}
                  labelFormatter={(label) => new Date(label).toLocaleDateString('pt-BR')}
                />
                <Area type="monotone" dataKey="installations" stroke="#8B5CF6" strokeWidth={2} fillOpacity={1} fill="url(#colorInstalls)" name="Instalações" />
                <Area type="monotone" dataKey="activeUsers" stroke="#3B82F6" strokeWidth={2} fillOpacity={1} fill="url(#colorActive)" name="Usuários Ativos" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div className="h-[260px] flex items-center justify-center">
             <EmptyState 
               icon={Activity} 
               title="Sem dados suficientes" 
               description="Não há histórico suficiente no período para montar a evolução." 
             />
          </div>
        )}
      </CardContent>
    </Card>
  );
}

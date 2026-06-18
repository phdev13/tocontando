import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { BarChart3 } from 'lucide-react';
import { Skeleton } from '../ui/Skeleton';

interface VersionDistributionChartProps {
  data: any[];
  isLoading?: boolean;
}

export function VersionDistributionChart({ data, isLoading }: VersionDistributionChartProps) {
  if (isLoading) {
    return <Skeleton className="h-[340px] w-full rounded-xl bg-surface-elevated/50" />;
  }

  return (
    <Card className="bg-[#0a0a10] border-white/[0.03] shadow-xl rounded-xl">
      <CardHeader className="pb-6">
        <CardTitle className="text-base text-white font-outfit">Distribuição de Versões</CardTitle>
        <CardDescription className="text-xs text-text-tertiary mt-1">Adoção das versões ativas no período.</CardDescription>
      </CardHeader>
      <CardContent>
        {data && data.length > 0 ? (
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} horizontal={true} />
                <XAxis dataKey="version_name" tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} axisLine={false} tickLine={false} dy={10} />
                <YAxis tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} axisLine={false} tickLine={false} dx={-10} />
                <Tooltip 
                  cursor={{ fill: 'rgba(255,255,255,0.05)' }} 
                  contentStyle={{ backgroundColor: '#12121c', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }} 
                />
                <Bar dataKey="count" name="Dispositivos" fill="url(#colorVersionDashboard)" radius={[4, 4, 0, 0]} maxBarSize={40} />
              </BarChart>
            </ResponsiveContainer>
            <svg width="0" height="0">
              <defs>
                <linearGradient id="colorVersionDashboard" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#8B5CF6" stopOpacity={1}/>
                  <stop offset="100%" stopColor="#8B5CF6" stopOpacity={0.2}/>
                </linearGradient>
              </defs>
            </svg>
          </div>
        ) : (
          <div className="h-[260px] flex items-center justify-center text-text-tertiary">
             <EmptyState icon={BarChart3} title="Sem dados" description="Nenhuma versão ativa no período." />
          </div>
        )}
      </CardContent>
    </Card>
  );
}

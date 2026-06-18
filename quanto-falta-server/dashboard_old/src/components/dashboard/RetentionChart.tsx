import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../ui/Card';
import { Skeleton } from '../ui/Skeleton';

interface RetentionChartProps {
  data: any[];
  isLoading?: boolean;
}

export function RetentionChart({ data, isLoading }: RetentionChartProps) {
  if (isLoading) {
    return <Skeleton className="h-[340px] w-full rounded-xl bg-surface-elevated/50" />;
  }

  const hasData = data && data.some(d => d.rate > 0);

  return (
    <Card className="bg-[#0a0a10] border-white/[0.03] shadow-xl rounded-xl">
      <CardHeader className="pb-6">
        <CardTitle className="text-base text-white font-outfit">Retenção de Usuários</CardTitle>
        <CardDescription className="text-xs text-text-tertiary mt-1">Taxa de usuários que retornaram após instalação.</CardDescription>
      </CardHeader>
      <CardContent>
        {hasData ? (
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} horizontal={true} />
                <XAxis dataKey="name" tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} axisLine={false} tickLine={false} dy={10} />
                <YAxis tick={{ fill: 'var(--color-text-tertiary)', fontSize: 10 }} tickFormatter={(v) => `${v}%`} axisLine={false} tickLine={false} dx={-10} />
                <Tooltip 
                  cursor={{ fill: 'rgba(255,255,255,0.05)' }} 
                  formatter={(v: number) => [`${v}%`, 'Taxa']} 
                  contentStyle={{ backgroundColor: '#12121c', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }} 
                />
                <Bar dataKey="rate" name="Retenção" fill="url(#colorRetentionDashboard)" radius={[4, 4, 0, 0]} maxBarSize={40} />
              </BarChart>
            </ResponsiveContainer>
            <svg width="0" height="0">
              <defs>
                <linearGradient id="colorRetentionDashboard" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#3B82F6" stopOpacity={1}/>
                  <stop offset="100%" stopColor="#3B82F6" stopOpacity={0.2}/>
                </linearGradient>
              </defs>
            </svg>
          </div>
        ) : (
          <div className="h-[260px] flex items-center justify-center flex-col">
            <div className="w-full h-full relative">
               <div className="absolute inset-0 flex flex-col justify-between pt-2 pb-[24px]">
                  {[4, 3, 2, 1, 0].map(val => (
                    <div key={val} className="flex items-center w-full">
                      <span className="text-[10px] text-text-tertiary w-8 text-right pr-2">{val}%</span>
                      <div className="flex-1 border-t border-dashed border-white/[0.05]"></div>
                    </div>
                  ))}
               </div>
               <div className="absolute bottom-0 left-8 right-0 flex justify-around">
                 <span className="text-[10px] text-text-tertiary">D1</span>
                 <span className="text-[10px] text-text-tertiary">D7</span>
                 <span className="text-[10px] text-text-tertiary">D30</span>
               </div>
            </div>
            <p className="text-xs text-text-tertiary mt-4">Sem dados suficientes para calcular retenção.</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

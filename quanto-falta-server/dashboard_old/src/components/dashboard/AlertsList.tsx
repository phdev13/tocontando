import { AlertTriangle, Info, ShieldAlert, CheckCircle2 } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../ui/Card';
import { Skeleton } from '../ui/Skeleton';
import { cn } from '../../lib/utils';

export interface AlertItem {
  id: string;
  type: 'critical' | 'warning' | 'info' | 'success';
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
}

interface AlertsListProps {
  alerts: AlertItem[];
  isLoading?: boolean;
}

const icons = {
  critical: ShieldAlert,
  warning: AlertTriangle,
  info: Info,
  success: CheckCircle2,
};

const styles = {
  critical: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
  warning: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  info: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  success: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
};

export function AlertsList({ alerts, isLoading }: AlertsListProps) {
  if (isLoading) {
    return <Skeleton className="h-[340px] w-full rounded-xl bg-surface-elevated/50" />;
  }

  return (
    <Card className="bg-[#0a0a10] border-white/[0.03] shadow-xl rounded-xl h-full">
      <CardHeader className="pb-4">
        <CardTitle className="text-base text-white font-outfit">Alertas e Recomendações</CardTitle>
      </CardHeader>
      <CardContent>
        {alerts.length > 0 ? (
          <div className="space-y-3">
            {alerts.map((alert) => {
              const Icon = icons[alert.type];
              return (
                <div key={alert.id} className={cn("flex items-start gap-3 p-3 rounded-lg border", styles[alert.type])}>
                  <Icon size={18} className="mt-0.5 shrink-0" />
                  <div className="flex-1">
                    <p className="text-sm font-semibold">{alert.title}</p>
                    <p className="text-xs opacity-80 mt-1">{alert.description}</p>
                    {alert.actionText && (
                      <button 
                        onClick={alert.onAction}
                        className="text-xs font-medium mt-2 hover:underline opacity-90"
                      >
                        {alert.actionText} &rarr;
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <div className="w-12 h-12 rounded-full bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-3">
              <CheckCircle2 size={24} />
            </div>
            <p className="text-sm font-medium text-white">Tudo normal por aqui!</p>
            <p className="text-xs text-text-tertiary mt-1">Nenhum alerta crítico ou recomendação pendente.</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

import { Activity, Settings, UserPlus, UploadCloud, Smartphone } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../ui/Card';
import { Skeleton } from '../ui/Skeleton';
import { formatDistanceToNow } from 'date-fns';
import { ptBR } from 'date-fns/locale';

export interface ActivityItem {
  id: string;
  type: 'config_change' | 'new_tester' | 'release' | 'system';
  title: string;
  timestamp: Date | number | string;
  actor?: string;
}

interface ActivityFeedProps {
  activities: ActivityItem[];
  isLoading?: boolean;
}

const icons = {
  config_change: Settings,
  new_tester: UserPlus,
  release: UploadCloud,
  system: Smartphone,
};

export function ActivityFeed({ activities, isLoading }: ActivityFeedProps) {
  if (isLoading) {
    return <Skeleton className="h-[340px] w-full rounded-xl bg-surface-elevated/50" />;
  }

  return (
    <Card className="bg-[#0a0a10] border-white/[0.03] shadow-xl rounded-xl h-full">
      <CardHeader className="pb-4">
        <CardTitle className="text-base text-white font-outfit">Atividade Recente</CardTitle>
      </CardHeader>
      <CardContent>
        {activities.length > 0 ? (
          <div className="relative pl-3 space-y-6 before:absolute before:inset-y-0 before:left-[19px] before:w-px before:bg-white/[0.05]">
            {activities.map((activity) => {
              const Icon = icons[activity.type] || Activity;
              return (
                <div key={activity.id} className="relative pl-6">
                  <div className="absolute left-[-11px] top-0.5 w-6 h-6 rounded-full bg-[#12121c] border border-white/10 flex items-center justify-center text-text-tertiary">
                    <Icon size={12} />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-white">{activity.title}</p>
                    <div className="flex items-center gap-2 mt-1">
                      {activity.actor && (
                        <span className="text-xs text-primary">{activity.actor}</span>
                      )}
                      {activity.actor && <span className="text-xs text-text-tertiary">•</span>}
                      <span className="text-xs text-text-tertiary">
                        {formatDistanceToNow(new Date(activity.timestamp), { addSuffix: true, locale: ptBR })}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Activity size={24} className="text-text-tertiary mb-3 opacity-50" />
            <p className="text-sm font-medium text-white">Nenhuma atividade recente</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

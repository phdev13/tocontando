import { LucideIcon, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { Card } from '../ui/Card';
import { Skeleton } from '../ui/Skeleton';
import { cn } from '../../lib/utils';

export type DeltaType = 'positive' | 'negative' | 'neutral' | 'critical';

export interface MetricCardProps {
  title: string;
  value: string | number | null | undefined;
  icon: LucideIcon;
  delta?: number;
  deltaType?: DeltaType;
  context?: string;
  colorClass: string;
  textClass: string;
  isLoading?: boolean;
}

export function MetricCard({
  title,
  value,
  icon: Icon,
  delta,
  deltaType = 'neutral',
  context = 'Comparado aos 30 dias anteriores',
  colorClass,
  textClass,
  isLoading,
}: MetricCardProps) {
  if (isLoading) {
    return <Skeleton className="h-[140px] rounded-[14px] bg-surface-elevated/50" />;
  }

  const displayValue = value == null ? '—' : value;
  const isZero = value === 0 || value === '0';

  let DeltaIcon = Minus;
  let deltaColorClass = 'text-text-tertiary bg-white/[0.03]';

  if (deltaType === 'positive') {
    DeltaIcon = TrendingUp;
    deltaColorClass = 'text-emerald-400 bg-emerald-400/10';
  } else if (deltaType === 'negative') {
    DeltaIcon = TrendingDown;
    deltaColorClass = 'text-rose-400 bg-rose-400/10';
  } else if (deltaType === 'critical') {
    DeltaIcon = TrendingUp;
    deltaColorClass = 'text-rose-400 bg-rose-400/10';
  }

  return (
    <Card className="bg-[#0b0b12] border-white/[0.03] shadow-md p-5 rounded-[14px] relative overflow-hidden group flex flex-col justify-between h-[140px]">
      {/* Subtle background glow */}
      <div
        className={cn(
          "absolute -top-10 -right-10 w-32 h-32 rounded-full blur-[40px] opacity-10 group-hover:opacity-20 transition-opacity duration-300 pointer-events-none",
          colorClass
        )}
      />

      <div className="flex items-start justify-between relative z-10">
        <p className="text-xs font-semibold text-text-tertiary uppercase tracking-wider">{title}</p>
        <div className={cn("flex items-center justify-center w-10 h-10 rounded-xl shrink-0 backdrop-blur-md transition-transform group-hover:scale-105", colorClass, textClass)}>
          <Icon size={18} />
        </div>
      </div>

      <div className="relative z-10 mt-1">
        <div className="flex items-baseline gap-3">
          <p className={cn("text-3xl font-extrabold font-outfit tracking-tight", isZero ? 'text-text-secondary' : 'text-white')}>
            {displayValue}
          </p>
          
          {delta !== undefined && (
            <div className={cn("flex items-center gap-1 px-1.5 py-0.5 rounded-md text-xs font-semibold", deltaColorClass)}>
              <DeltaIcon size={12} strokeWidth={3} />
              <span>{Math.abs(delta)}%</span>
            </div>
          )}
        </div>
        
        {context && (
          <p className="text-[10px] text-text-tertiary mt-1.5 truncate">
            {context}
          </p>
        )}
      </div>
    </Card>
  );
}

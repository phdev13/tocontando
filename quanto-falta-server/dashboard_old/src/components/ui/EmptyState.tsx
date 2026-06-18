import React from 'react';
import { LucideIcon } from 'lucide-react';
import { cn } from '../../lib/utils';

interface EmptyStateProps extends React.HTMLAttributes<HTMLDivElement> {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: React.ReactNode;
}

export function EmptyState({ icon: Icon, title, description, action, className, ...props }: EmptyStateProps) {
  return (
    <div
      className={cn("flex flex-col items-center justify-center p-8 text-center min-h-[300px] bg-surface rounded-xl border border-dashed border-white/20", className)}
      {...props}
    >
      <div className="p-4 bg-surface-elevated rounded-full mb-4 ring-8 ring-surface shadow-inner">
        <Icon size={32} className="text-text-tertiary" />
      </div>
      <h3 className="text-lg font-semibold text-text-primary mb-2 font-outfit">
        {title}
      </h3>
      <p className="text-sm text-text-secondary max-w-sm mb-6 leading-relaxed">
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
}

import { cn } from '../../lib/utils';
import React from 'react';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'default';
}

export function Badge({ className, variant = 'default', ...props }: BadgeProps) {
  const variantClasses = {
    success: 'bg-success/15 text-success border-success/30 shadow-[0_0_10px_rgba(16,185,129,0.2)]',
    warning: 'bg-warning/15 text-warning border-warning/30 shadow-[0_0_10px_rgba(245,158,11,0.2)]',
    danger: 'bg-danger/15 text-danger border-danger/30 shadow-[0_0_10px_rgba(239,68,68,0.2)]',
    info: 'bg-info/15 text-info border-info/30 shadow-[0_0_10px_rgba(59,130,246,0.2)]',
    default: 'bg-white/10 text-white border-white/20',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border backdrop-blur-sm transition-all',
        variantClasses[variant],
        className
      )}
      {...props}
    />
  );
}

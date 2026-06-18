'use client';

import * as React from 'react';
import { useIsMobile } from '@/hooks/use-mobile';

/**
 * ResponsiveTable: renders children (a desktop table) on ≥768px.
 * On mobile, renders the mobileRender prop instead.
 * Desktop table is completely untouched.
 */
export function ResponsiveTable({
  children,
  mobileRender,
}: {
  children: React.ReactNode;
  mobileRender: React.ReactNode;
}) {
  const isMobile = useIsMobile();

  if (isMobile) {
    return <>{mobileRender}</>;
  }

  return <>{children}</>;
}

/**
 * MobileCard: A card representation of a table row for mobile views.
 */
export function MobileCard({
  title,
  subtitle,
  badge,
  fields,
  actions,
  className,
}: {
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  badge?: React.ReactNode;
  fields?: { label: string; value: React.ReactNode }[];
  actions?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl p-4 space-y-3 ${className ?? ''}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="font-bold text-[14px] text-white truncate">{title}</div>
          {subtitle && (
            <div className="text-[12px] text-[var(--color-primary-muted)] mt-0.5 truncate">{subtitle}</div>
          )}
        </div>
        {badge && <div className="shrink-0">{badge}</div>}
      </div>
      
      {fields && fields.length > 0 && (
        <div className="grid grid-cols-2 gap-x-4 gap-y-2">
          {fields.map((field, i) => (
            <div key={i}>
              <div className="text-[11px] font-medium text-[var(--color-primary-muted)] uppercase tracking-wider">{field.label}</div>
              <div className="text-[13px] text-white mt-0.5">{field.value}</div>
            </div>
          ))}
        </div>
      )}
      
      {actions && (
        <div className="flex items-center gap-3 pt-2 border-t border-[var(--color-border)]">
          {actions}
        </div>
      )}
    </div>
  );
}

/**
 * MobileCardList: wraps MobileCards with consistent spacing
 */
export function MobileCardList({ children }: { children: React.ReactNode }) {
  return <div className="space-y-3">{children}</div>;
}

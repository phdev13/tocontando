'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useRef } from 'react';
import { 
  LayoutDashboard, 
  TrendingUp, 
  DollarSign, 
  GitBranch, 
  Users, 
  MessageSquare, 
  Settings,
  LogOut,
  Timer,
  Activity,
  X
} from 'lucide-react';
import { cn } from '@/lib/utils';

const navigationGroups = [
  {
    title: 'PRINCIPAL',
    items: [
      { name: 'Dashboard', href: '/', icon: LayoutDashboard },
      { name: 'Métricas App', href: '/metrics', icon: TrendingUp },
      { name: 'Lab Performance', href: '/performance', icon: Activity },
      { name: 'Monetização', href: '/monetization', icon: DollarSign },
    ]
  },
  {
    title: 'GESTÃO',
    items: [
      { name: 'Versões', href: '/ota', icon: GitBranch },
      { name: 'Testers', href: '/devices', icon: Users },
    ]
  },
  {
    title: 'SUPORTE',
    items: [
      { name: 'Feedbacks', href: '/feedbacks', icon: MessageSquare },
    ]
  },
  {
    title: 'SISTEMA',
    items: [
      { name: 'Diagnósticos', href: '/devices/diagnostics', icon: Activity },
      { name: 'Configurações', href: '/settings', icon: Settings },
    ]
  }
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed inset-y-0 left-0 z-50 w-64 flex flex-col border-r border-[var(--color-border)] bg-[var(--color-surface)] max-lg:hidden">
      <SidebarContent pathname={pathname} />
    </aside>
  );
}

export function MobileDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const pathname = usePathname();
  const drawerRef = useRef<HTMLDivElement>(null);

  // Close on route change
  useEffect(() => {
    onClose();
  }, [pathname, onClose]);

  // Lock body scroll when open
  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => { document.body.style.overflow = ''; };
  }, [open]);

  // Close on Escape key
  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    if (open) {
      document.addEventListener('keydown', handleKey);
      return () => document.removeEventListener('keydown', handleKey);
    }
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />
      {/* Drawer panel */}
      <div
        ref={drawerRef}
        className="absolute inset-y-0 left-0 w-72 max-w-[85vw] flex flex-col bg-[var(--color-surface)] border-r border-[var(--color-border)] shadow-2xl animate-in slide-in-from-left duration-200"
      >
        {/* Close button */}
        <div className="flex items-center justify-end px-4 pt-4">
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-[var(--color-primary-muted)] hover:text-white hover:bg-[var(--color-surface-hover)] transition-colors"
            aria-label="Fechar menu"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <SidebarContent pathname={pathname} />
      </div>
    </div>
  );
}

function SidebarContent({ pathname }: { pathname: string }) {
  return (
    <>
      <div className="flex h-20 shrink-0 items-center px-6 gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-black">
          <Timer className="h-6 w-6" />
        </div>
        <div className="flex flex-col">
          <span className="font-bold text-lg leading-tight text-white tracking-tight">Quanto Falta?</span>
          <span className="text-xs font-medium text-purple-400">Admin Premium</span>
        </div>
      </div>
      
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-8">
        {navigationGroups.map((group) => (
          <div key={group.title}>
            <div className="mb-3 px-2 text-[11px] font-semibold text-[var(--color-primary-muted)] tracking-wider">
              {group.title}
            </div>
            <nav className="space-y-1.5">
              {group.items.map((item) => {
                const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={cn(
                      "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200",
                      isActive 
                        ? "bg-purple-600/10 text-purple-400" 
                        : "text-[var(--color-primary-muted)] hover:bg-[var(--color-surface-hover)] hover:text-white"
                    )}
                  >
                    <item.icon className={cn("h-4 w-4", isActive ? "text-purple-400" : "text-[var(--color-primary-muted)]")} />
                    {item.name}
                  </Link>
                );
              })}
            </nav>
          </div>
        ))}
      </div>
      
      <div className="mt-auto p-4 border-t border-[var(--color-border)]">
        <button className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[var(--color-primary-muted)] hover:bg-[var(--color-surface-hover)] hover:text-white transition-colors">
          <LogOut className="h-4 w-4" />
          Sair do sistema
        </button>
      </div>
    </>
  );
}

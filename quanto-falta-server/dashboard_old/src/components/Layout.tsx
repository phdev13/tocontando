import { Outlet, NavLink, useLocation } from 'react-router-dom';
import { LayoutDashboard, GitBranch, MessageSquare, Zap, Settings, LogOut, Users, DollarSign, Menu, X } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { config } from '../lib/api';
import { cn } from '../lib/utils';
import { Button } from './ui/Button';

const NAV_GROUPS = [
  {
    title: 'Métricas',
    items: [
      { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
      { to: '/performance', icon: Zap, label: 'Performance' },
      { to: '/monetization', icon: DollarSign, label: 'Monetização' },
    ]
  },
  {
    title: 'Gestão',
    items: [
      { to: '/versions', icon: GitBranch, label: 'Versões' },
      { to: '/testers', icon: Users, label: 'Testers' },
    ]
  },
  {
    title: 'Conteúdo',
    items: [
      { to: '/feedbacks', icon: MessageSquare, label: 'Feedbacks' },
    ]
  },
  {
    title: 'Sistema',
    items: [
      { to: '/settings', icon: Settings, label: 'Configurações' },
    ]
  }
];

export default function Layout() {
  const location = useLocation();
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const { data: configData } = useQuery({ queryKey: ['config'], queryFn: config.get });

  const mode = configData?.activeIconMode ?? 'auto';
  let isCopa = false;
  if (mode === 'force_copa') isCopa = true;
  else if (mode === 'force_padrao') isCopa = false;
  else isCopa = new Date() < new Date('2026-07-20T00:00:00Z');

  async function handleLogout() {
    window.location.href = '/cdn-cgi/access/logout';
  }

  // Close mobile menu on route change
  useEffect(() => {
    setIsMobileOpen(false);
  }, [location.pathname]);

  const SidebarContent = () => (
    <>
      <div className="flex items-center gap-3 pb-6 border-b border-white/10 mb-6">
        <div className="w-10 h-10 flex items-center justify-center shrink-0">
          <img src={isCopa ? '/favicon-copa.png' : '/favicon.png'} alt="Logo" width="32" height="32" className="rounded-lg" />
        </div>
        <div>
          <div className="font-outfit font-bold text-base text-text-primary leading-tight">Tô Contando</div>
          <div className="text-xs text-primary font-semibold tracking-wider uppercase">Admin Premium</div>
        </div>
      </div>

      <nav className="flex-1 flex flex-col gap-6 overflow-y-auto custom-scrollbar">
        {NAV_GROUPS.map((group, i) => (
          <div key={i} className="flex flex-col gap-1">
            <div className="text-xs font-semibold text-text-tertiary uppercase tracking-wider px-2 mb-1">{group.title}</div>
            {group.items.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) => cn(
                  "flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-300",
                  isActive 
                    ? "bg-primary/20 text-primary border border-primary/30 shadow-[0_0_10px_rgba(139,92,246,0.15)] translate-x-1" 
                    : "text-text-secondary hover:bg-white/5 hover:text-text-primary hover:translate-x-1"
                )}
              >
                <Icon size={18} className="transition-transform duration-300 group-hover:scale-110" />
                <span>{label}</span>
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <button 
        className="flex items-center justify-center gap-2 px-3 py-2 mt-4 w-full rounded-lg text-text-tertiary text-sm font-medium transition-colors hover:bg-surface-hover hover:text-text-secondary border border-transparent"
        onClick={handleLogout}
      >
        <LogOut size={16} />
        <span>Sair do sistema</span>
      </button>
    </>
  );

  return (
    <div className="flex flex-col md:flex-row h-screen overflow-hidden bg-bg-base">
      
      {/* Mobile Header */}
      <header className="md:hidden flex items-center justify-between p-4 glass-panel border-b border-white/5 sticky top-0 z-30">
        <div className="font-outfit font-bold text-lg text-text-primary">Tô Contando</div>
        <Button variant="ghost" size="icon" onClick={() => setIsMobileOpen(true)}>
          <Menu size={24} />
        </Button>
      </header>

      {/* Desktop Sidebar */}
      <aside className="hidden md:flex flex-col w-64 h-full shrink-0 p-6 z-20">
        <div className="glass-panel w-full h-full rounded-2xl flex flex-col p-6 shadow-2xl border-white/5 relative overflow-hidden">
          {/* Subtle glow behind sidebar content */}
          <div className="absolute top-0 left-0 w-full h-32 bg-gradient-to-b from-primary/10 to-transparent pointer-events-none" />
          <SidebarContent />
        </div>
      </aside>

      {/* Mobile Sidebar (Drawer) */}
      {isMobileOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex md:hidden animate-in fade-in duration-200" onClick={() => setIsMobileOpen(false)}>
          <div className={`fixed inset-y-0 left-0 w-64 glass-panel-elevated shadow-2xl z-40 transform transition-transform duration-300 ease-in-out flex flex-col p-6 ${isMobileOpen ? 'translate-x-0' : '-translate-x-full'}`}>
            <div className="flex justify-end mb-6">
              <Button variant="ghost" size="icon" onClick={() => setIsMobileOpen(false)}>
                <X size={24} />
              </Button>
            </div>
            <SidebarContent />
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className="flex-1 min-w-0 overflow-y-auto custom-scrollbar relative">
        <div className="absolute inset-0 bg-gradient-to-b from-primary/5 to-transparent h-[400px] pointer-events-none -z-10" />
        <Outlet />
      </main>
    </div>
  );
}

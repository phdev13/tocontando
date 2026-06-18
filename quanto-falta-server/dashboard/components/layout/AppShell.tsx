'use client';

import { useCallback, useState } from 'react';
import { Sidebar, MobileDrawer } from '@/components/layout/Sidebar';
import { Topbar } from '@/components/layout/Topbar';

export function AppShell({ children }: { children: React.ReactNode }) {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const closeDrawer = useCallback(() => setDrawerOpen(false), []);

  return (
    <>
      <Sidebar />
      <MobileDrawer open={drawerOpen} onClose={closeDrawer} />
      <div className="lg:pl-64 flex flex-col min-h-screen">
        <div className="lg:hidden">
          <Topbar onMenuClick={() => setDrawerOpen(true)} />
        </div>
        <main className="flex-1 py-6 px-4 sm:px-6 lg:py-10 lg:px-10 max-w-7xl mx-auto w-full">
          {children}
        </main>
      </div>
    </>
  );
}

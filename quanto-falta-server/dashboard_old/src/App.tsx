import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEffect } from 'react';
import { config } from './lib/api';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Versions from './pages/Versions';
import Feedbacks from './pages/Feedbacks';
import Performance from './pages/Performance';
import Settings from './pages/Settings';
import Testers from './pages/Testers';
import Monetization from './pages/Monetization';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

function DynamicFavicon() {
  useEffect(() => {
    config.get().then((c) => {
      const mode = c.activeIconMode ?? 'auto';
      let isCopa = false;
      if (mode === 'force_copa') isCopa = true;
      else if (mode === 'force_padrao') isCopa = false;
      else isCopa = new Date() < new Date('2026-07-20T00:00:00Z');

      const link = document.querySelector("link[rel~='icon']") as HTMLLinkElement;
      if (link) {
        link.href = isCopa ? '/favicon-copa.png' : '/favicon.svg'; // TODO: Provide images
      }
    }).catch(() => {});
  }, []);
  return null;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <DynamicFavicon />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="versions" element={<Versions />} />
            <Route path="feedbacks" element={<Feedbacks />} />
            <Route path="performance" element={<Performance />} />
            <Route path="testers" element={<Testers />} />
            <Route path="monetization" element={<Monetization />} />
            <Route path="settings" element={<Settings />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

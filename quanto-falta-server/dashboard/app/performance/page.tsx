'use client';

import React, { useState, useEffect } from 'react';
import { Activity, Download, Layers, Smartphone, AlertCircle, LayoutDashboard, Database, RefreshCw, BarChart2 } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { useIsMobile } from '@/hooks/use-mobile';

export default function PerformancePage() {
  const [activeTab, setActiveTab] = useState<'overview' | 'runs' | 'errors'>('overview');
  const [runs, setRuns] = useState<any[]>([]);
  const [screens, setScreens] = useState<any[]>([]);
  const [devices, setDevices] = useState<any[]>([]);
  const [releases, setReleases] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const isMobile = useIsMobile();

  useEffect(() => {
    const abortController = new AbortController();
    fetchData(abortController.signal);
    return () => { abortController.abort(); };
  }, []);

  const fetchData = async (signal?: AbortSignal) => {
    setLoading(true);
    try {
      const [runsRes, screensRes, devicesRes, releasesRes] = await Promise.all([
        fetch('/api/v1/admin/performance/runs', { signal }),
        fetch('/api/v1/admin/performance/screens?days=14', { signal }),
        fetch('/api/v1/admin/performance/devices', { signal }),
        fetch('/api/v1/admin/performance/releases', { signal })
      ]);

      if (runsRes.ok) setRuns((await runsRes.json()).runs || []);
      if (screensRes.ok) setScreens((await screensRes.json()).screens || []);
      if (devicesRes.ok) setDevices((await devicesRes.json()).devices || []);
      if (releasesRes.ok) setReleases((await releasesRes.json()).releases || []);
    } catch (e: any) {
      if (e.name === 'AbortError') return;
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="pb-20 animate-in fade-in duration-500">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
        <div>
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight bg-gradient-to-r from-emerald-400 to-cyan-400 bg-clip-text text-transparent">
            Central de Performance
          </h1>
          <p className="text-[var(--color-primary-muted)] mt-2">Métricas de lentidão, inicialização e traces de Jank.</p>
        </div>
        <button 
          onClick={() => fetchData()}
          className="flex items-center justify-center gap-2 px-4 py-2.5 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg hover:bg-[var(--color-surface-hover)] transition-all w-full sm:w-auto"
        >
          <RefreshCw className={loading ? "animate-spin text-emerald-400" : "text-emerald-400"} size={18} />
          {loading ? 'Atualizando...' : 'Atualizar Dados'}
        </button>
      </div>

      {/* Tabs - scrollable on mobile */}
      <div className="flex gap-1 sm:gap-4 border-b border-[var(--color-border)] mb-8 overflow-x-auto -mx-4 px-4 sm:mx-0 sm:px-0">
        <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} icon={<LayoutDashboard size={18} />} label={isMobile ? "Geral" : "Visão Geral"} />
        <TabButton active={activeTab === 'runs'} onClick={() => setActiveTab('runs')} icon={<Activity size={18} />} label={isMobile ? "Traces" : "Relatórios (Jank & Traces)"} />
        <TabButton active={activeTab === 'errors'} onClick={() => setActiveTab('errors')} icon={<AlertCircle size={18} />} label={isMobile ? "Erros" : "Erros e Falhas"} />
      </div>

      {loading && runs.length === 0 ? (
        <div className="h-64 flex items-center justify-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-500"></div>
        </div>
      ) : (
        <div className="space-y-8">
          {activeTab === 'overview' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              
              {/* Tempo de Inicialização por Versão */}
              <div className="bg-[var(--color-surface)] border border-[var(--color-border)] p-4 sm:p-6 rounded-2xl shadow-sm">
                <div className="flex items-center gap-3 mb-6">
                  <div className="p-3 bg-blue-500/10 rounded-xl text-blue-400"><Layers size={24} /></div>
                  <h2 className="text-lg sm:text-xl font-semibold">Startup (Últimas Versões)</h2>
                </div>
                <div className="h-48 sm:h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={releases.map(r => ({ name: 'v' + r.version_code, ms: Math.round(r.avg_startup || 0) }))}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" vertical={false} />
                      <XAxis dataKey="name" stroke="#888" fontSize={12} tickLine={false} axisLine={false} />
                      <YAxis stroke="#888" fontSize={12} tickLine={false} axisLine={false} unit="ms" />
                      <Tooltip cursor={{fill: 'rgba(255,255,255,0.05)'}} contentStyle={{backgroundColor: '#1a1a1a', border: '1px solid #333', borderRadius: '8px'}} />
                      <Bar dataKey="ms" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* Piores Telas (Jank/Lentidão) */}
              <div className="bg-[var(--color-surface)] border border-[var(--color-border)] p-4 sm:p-6 rounded-2xl shadow-sm">
                <div className="flex items-center gap-3 mb-6">
                  <div className="p-3 bg-red-500/10 rounded-xl text-red-400"><Activity size={24} /></div>
                  <h2 className="text-lg sm:text-xl font-semibold">Telas Mais Lentas</h2>
                </div>
                <div className="space-y-4">
                  {screens.map((screen, idx) => (
                    <div key={idx} className="flex justify-between items-center p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors">
                      <div className="min-w-0 flex-1 mr-3">
                        <p className="font-medium truncate">{screen.screen}</p>
                        <p className="text-xs text-[var(--color-primary-muted)]">{screen.affected_sessions} sessões afetadas</p>
                      </div>
                      <div className="text-right shrink-0">
                        <p className="text-red-400 font-bold">{Math.round(screen.avg_startup || screen.slow_frames_sum || 0)}ms</p>
                        <p className="text-xs text-[var(--color-primary-muted)]">média</p>
                      </div>
                    </div>
                  ))}
                  {screens.length === 0 && <p className="text-[var(--color-primary-muted)]">Nenhum dado de tela lento registrado.</p>}
                </div>
              </div>

              {/* Aparelhos Mais Lentos */}
              <div className="md:col-span-2 bg-[var(--color-surface)] border border-[var(--color-border)] p-4 sm:p-6 rounded-2xl shadow-sm">
                <div className="flex items-center gap-3 mb-6">
                  <div className="p-3 bg-emerald-500/10 rounded-xl text-emerald-400"><Smartphone size={24} /></div>
                  <h2 className="text-lg sm:text-xl font-semibold">Impacto por Dispositivo (Cold Start)</h2>
                </div>
                
                {/* Mobile: card list / Desktop: table */}
                {isMobile ? (
                  <div className="space-y-3">
                    {devices.map((device, idx) => (
                      <div key={idx} className="flex items-center justify-between p-3 rounded-xl bg-white/5">
                        <div className="min-w-0 flex-1">
                          <p className="font-medium text-[14px]">{device.model}</p>
                          <p className="text-xs text-[var(--color-primary-muted)]">Android {device.android_version} • {device.sessions} sessões</p>
                        </div>
                        <p className={`font-bold shrink-0 ml-3 ${device.avg_cold_start > 2000 ? 'text-red-400' : 'text-emerald-400'}`}>
                          {Math.round(device.avg_cold_start)}ms
                        </p>
                      </div>
                    ))}
                    {devices.length === 0 && <p className="text-[var(--color-primary-muted)]">Nenhum dado de dispositivo registrado.</p>}
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left">
                      <thead className="text-[var(--color-primary-muted)] border-b border-[var(--color-border)]">
                        <tr>
                          <th className="pb-3 font-medium">Modelo</th>
                          <th className="pb-3 font-medium">Android OS</th>
                          <th className="pb-3 font-medium">Amostras</th>
                          <th className="pb-3 font-medium text-right">Tempo Médio (ms)</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[var(--color-border)]">
                        {devices.map((device, idx) => (
                          <tr key={idx} className="hover:bg-white/5 transition-colors">
                            <td className="py-3 font-medium">{device.model}</td>
                            <td className="py-3">{device.android_version}</td>
                            <td className="py-3">{device.sessions}</td>
                            <td className={`py-3 text-right font-semibold ${device.avg_cold_start > 2000 ? 'text-red-400' : 'text-emerald-400'}`}>
                              {Math.round(device.avg_cold_start)}ms
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {devices.length === 0 && <p className="text-[var(--color-primary-muted)] mt-4">Nenhum dado de dispositivo registrado.</p>}
                  </div>
                )}
              </div>

            </div>
          )}

          {activeTab === 'runs' && (
             <div className="bg-[var(--color-surface)] p-4 sm:p-6 rounded-2xl border border-[var(--color-border)] shadow-sm">
             <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                  <div className="p-3 bg-purple-500/10 rounded-xl text-purple-400"><Database size={24} /></div>
                  <h2 className="text-lg sm:text-xl font-semibold">Traces e Macrobenchmarks</h2>
                </div>
             </div>
             
             {/* Mobile: card list / Desktop: table */}
             {isMobile ? (
               <div className="space-y-3">
                 {runs.map(run => (
                   <div key={run.id} className="bg-white/5 rounded-xl p-4 space-y-2">
                     <div className="flex items-center justify-between">
                       <span className={`px-2 py-1 rounded text-xs font-bold ${run.source === 'MACROBENCHMARK' ? 'bg-purple-600/10 text-purple-400' : 'bg-emerald-600/10 text-emerald-400'}`}>
                         {run.source}
                       </span>
                       <span className={`px-2 py-1 rounded text-xs font-medium ${run.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-white/5 text-[var(--color-primary-muted)]'}`}>
                         {run.status}
                       </span>
                     </div>
                     <p className="font-medium text-[14px]">{run.benchmark_name || 'Agregado Geral'}</p>
                     <div className="flex items-center justify-between text-[12px] text-[var(--color-primary-muted)]">
                       <span>{run.app_version} • {run.device_model}</span>
                       <span>{new Date(run.created_at).toLocaleDateString()}</span>
                     </div>
                     <a 
                       href={`/api/v1/admin/performance/runs/${run.id}/artifacts/trace.perfetto-trace`}
                       className="inline-flex items-center gap-1.5 px-3 py-2 bg-blue-500/10 text-blue-400 hover:bg-blue-500/20 rounded-md transition-colors text-sm font-medium w-full justify-center mt-1"
                       target="_blank" rel="noreferrer"
                       onClick={(e) => {
                         if (run.source === 'JANKSTATS') {
                           e.preventDefault();
                           alert('Traces completos não são gerados no JankStats de produção, apenas as métricas de quadro foram computadas.');
                         }
                       }}
                     >
                       <Download size={16} /> Baixar Trace
                     </a>
                   </div>
                 ))}
                 {runs.length === 0 && (
                   <div className="text-center py-12 text-[var(--color-primary-muted)]">
                     Nenhum relatório de trace/jank encontrado.
                   </div>
                 )}
               </div>
             ) : (
               <div className="overflow-x-auto">
                 <table className="w-full text-left text-sm whitespace-nowrap">
                   <thead className="uppercase tracking-wider border-b border-[var(--color-border)] text-[var(--color-primary-muted)] text-xs font-semibold">
                     <tr>
                       <th className="px-4 py-3">Data</th>
                       <th className="px-4 py-3">Origem</th>
                       <th className="px-4 py-3">Nome / Tela</th>
                       <th className="px-4 py-3">Versão</th>
                       <th className="px-4 py-3">Aparelho</th>
                       <th className="px-4 py-3">Status</th>
                       <th className="px-4 py-3 text-right">Ação</th>
                     </tr>
                   </thead>
                   <tbody className="divide-y divide-[var(--color-border)]">
                     {runs.map(run => (
                       <tr key={run.id} className="hover:bg-white/5 transition-colors group">
                         <td className="px-4 py-4">{new Date(run.created_at).toLocaleString()}</td>
                         <td className="px-4 py-4">
                           <span className={`px-2 py-1 rounded text-xs font-bold ${run.source === 'MACROBENCHMARK' ? 'bg-purple-600/10 text-purple-400' : 'bg-emerald-600/10 text-emerald-400'}`}>
                             {run.source}
                           </span>
                         </td>
                         <td className="px-4 py-4 font-medium">{run.benchmark_name || 'Agregado Geral'}</td>
                         <td className="px-4 py-4 text-[var(--color-primary-muted)]">{run.app_version}</td>
                         <td className="px-4 py-4 text-[var(--color-primary-muted)]">{run.device_model}</td>
                         <td className="px-4 py-4">
                           <span className={`px-2 py-1 rounded text-xs font-medium ${run.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-white/5 text-[var(--color-primary-muted)]'}`}>
                             {run.status}
                           </span>
                         </td>
                         <td className="px-4 py-4 text-right">
                           <a 
                              href={`/api/v1/admin/performance/runs/${run.id}/artifacts/trace.perfetto-trace`}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-500/10 text-blue-400 hover:bg-blue-500/20 rounded-md transition-colors text-sm font-medium"
                              target="_blank" rel="noreferrer"
                              onClick={(e) => {
                                if (run.source === 'JANKSTATS') {
                                  e.preventDefault();
                                  alert('Traces completos não são gerados no JankStats de produção, apenas as métricas de quadro foram computadas.');
                                }
                              }}
                           >
                              <Download size={16} /> Trace
                           </a>
                         </td>
                       </tr>
                     ))}
                     {runs.length === 0 && (
                       <tr>
                         <td colSpan={7} className="px-4 py-12 text-center text-[var(--color-primary-muted)]">
                           Nenhum relatório de trace/jank encontrado.
                         </td>
                       </tr>
                     )}
                   </tbody>
                 </table>
               </div>
             )}
           </div>
          )}

          {activeTab === 'errors' && (
             <div className="bg-[var(--color-surface)] p-4 sm:p-6 rounded-2xl border border-[var(--color-border)] flex items-center justify-center h-64 shadow-sm">
                <div className="text-center px-4">
                  <AlertCircle size={48} className="mx-auto text-yellow-500 mb-4 opacity-50" />
                  <h3 className="text-xl font-medium mb-2">Painel de Erros</h3>
                  <p className="text-[var(--color-primary-muted)] max-w-md mx-auto">Em breve. Os relatórios de erro (Crash e ANR) unificados estarão disponíveis nesta aba em futuras atualizações.</p>
                </div>
             </div>
          )}
        </div>
      )}
    </div>
  );
}

function TabButton({ active, onClick, icon, label }: { active: boolean, onClick: () => void, icon: React.ReactNode, label: string }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-2 px-3 sm:px-4 py-3 font-medium transition-all border-b-2 whitespace-nowrap text-sm ${
        active 
          ? 'border-emerald-400 text-emerald-400' 
          : 'border-transparent text-[var(--color-primary-muted)] hover:text-white hover:bg-white/5'
      }`}
    >
      {icon}
      {label}
    </button>
  );
}

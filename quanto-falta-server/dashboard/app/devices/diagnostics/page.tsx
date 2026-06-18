'use client';

import { useEffect, useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { adminApi, formatDateTime } from '@/lib/admin-api';
import { Bell, Clock, CalendarClock, ShieldAlert, CheckCircle2, XCircle, Search, Laptop } from 'lucide-react';

function DiagnosticsContent() {
  const searchParams = useSearchParams();
  const id = searchParams.get('id');
  
  const [diagnostics, setDiagnostics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [inputId, setInputId] = useState('');

  const [recentDevices, setRecentDevices] = useState<any[]>([]);

  useEffect(() => {
    if (!id) {
      adminApi.devices(5).then(res => setRecentDevices(res.devices)).catch(() => {});
      setLoading(false);
      return;
    }
    
    async function load() {
      try {
        setLoading(true);
        const data = await adminApi.deviceDiagnostics(id as string);
        setDiagnostics(data.diagnostics);
      } catch (err: any) {
        setError(err.message || 'Erro ao carregar diagnóstico do dispositivo');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  if (loading) {
    return <div className="text-center py-12 text-[var(--color-primary-muted)]">Carregando diagnósticos...</div>;
  }

  if (!id) {
    return (
      <div className="space-y-6 pb-12">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white leading-none">Diagnóstico de Notificações</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Busque pelo ID de instalação de um dispositivo para inspecionar a saúde dos lembretes.</p>
        </div>
        
        <Card className="max-w-2xl">
          <CardHeader className="p-4 sm:p-6">
            <CardTitle className="text-lg">Inspecionar Dispositivo</CardTitle>
          </CardHeader>
          <CardContent className="p-4 sm:p-6 pt-0 space-y-6">
            <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 items-end">
              <div className="flex-1 w-full relative">
                <Search className="absolute left-3 top-[12px] h-4 w-4 text-[var(--color-primary-muted)]" />
                <input 
                  value={inputId} 
                  onChange={(e) => setInputId(e.target.value)} 
                  placeholder="Cole aqui a Installation ID (ex: a1b2c3d4-...)" 
                  className="w-full pl-9 pr-3 py-2.5 bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl text-white text-[14px] focus:outline-none focus:border-purple-500/50" 
                  onKeyDown={(e) => e.key === 'Enter' && inputId.trim() && (window.location.href = `/devices/diagnostics?id=${inputId.trim()}`)}
                />
              </div>
              <button 
                onClick={() => inputId.trim() && (window.location.href = `/devices/diagnostics?id=${inputId.trim()}`)}
                disabled={!inputId.trim()}
                className="w-full sm:w-auto px-6 py-2.5 sm:py-2 text-[14px] font-medium text-white bg-purple-600 hover:bg-purple-700 disabled:opacity-50 rounded-xl transition-colors"
              >
                Inspecionar
              </button>
            </div>

            {recentDevices.length > 0 && (
              <div>
                <h3 className="text-[13px] font-semibold text-[var(--color-primary-muted)] mb-3 uppercase tracking-wider">Acessos Recentes</h3>
                <div className="space-y-2">
                  {recentDevices.map((dev: any) => (
                    <button
                      key={dev.installationId}
                      onClick={() => { window.location.href = `/devices/diagnostics?id=${dev.installationId}` }}
                      className="w-full flex items-center justify-between p-3 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-hover)] hover:border-purple-500/30 hover:bg-purple-500/5 transition-all text-left"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-full bg-[var(--color-surface)] flex items-center justify-center border border-[var(--color-border)] shrink-0">
                          <Laptop className="h-4 w-4 text-[var(--color-primary-muted)]" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-[13px] font-medium text-white truncate">{dev.installationId}</div>
                          <div className="text-[11px] text-[var(--color-primary-muted)] truncate">{dev.manufacturer} {dev.model} • Android {dev.androidVersion}</div>
                        </div>
                      </div>
                      <div className="text-[11px] font-semibold px-2 py-1 bg-[var(--color-surface)] rounded-md border border-[var(--color-border)] text-[var(--color-primary-muted)] shrink-0 ml-2">
                        v{dev.versionName}
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <h1 className="text-[28px] font-bold tracking-tight text-white leading-none">Diagnóstico de Notificações</h1>
        <Card className="border-red-500/30 bg-red-500/10">
          <CardContent className="p-6 text-red-400 font-medium">
            {error}
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!diagnostics) {
    return <div className="text-center py-12 text-[var(--color-primary-muted)]">Nenhum diagnóstico recebido deste dispositivo ainda.</div>;
  }

  const { notificationsAllowed, exactAlarmsAllowed, activeSchedules, nextTriggerAt, lastReconciliationAt, updatedAt } = diagnostics;

  return (
    <div className="space-y-6 pb-12">
      <div>
        <h1 className="text-[28px] font-bold tracking-tight text-white leading-none">Diagnóstico de Notificações</h1>
        <p className="text-[14px] text-[var(--color-primary-muted)] mt-2 break-all">Dispositivo: {id}</p>
        <p className="text-[12px] text-[var(--color-primary-muted)] mt-1">
          Última atualização: {formatDateTime(updatedAt)}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 p-4 sm:p-6 pb-2">
            <CardTitle className="text-sm font-medium text-[var(--color-primary-muted)]">
              Permissão de Notificação
            </CardTitle>
            <Bell className="h-4 w-4 text-[var(--color-primary-muted)]" />
          </CardHeader>
          <CardContent className="p-4 sm:p-6 pt-0">
            <div className="text-2xl font-bold flex items-center gap-2">
              {notificationsAllowed ? (
                <><CheckCircle2 className="h-6 w-6 text-emerald-500" /> Permitido</>
              ) : (
                <><XCircle className="h-6 w-6 text-red-500" /> Bloqueado</>
              )}
            </div>
            {!notificationsAllowed && (
              <p className="text-xs text-red-400 mt-2">
                O usuário desativou as notificações do aplicativo nas configurações do Android.
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 p-4 sm:p-6 pb-2">
            <CardTitle className="text-sm font-medium text-[var(--color-primary-muted)]">
              Alarmes Exatos
            </CardTitle>
            <Clock className="h-4 w-4 text-[var(--color-primary-muted)]" />
          </CardHeader>
          <CardContent className="p-4 sm:p-6 pt-0">
            <div className="text-2xl font-bold flex items-center gap-2">
              {exactAlarmsAllowed ? (
                <><CheckCircle2 className="h-6 w-6 text-emerald-500" /> Permitido</>
              ) : (
                <><ShieldAlert className="h-6 w-6 text-amber-500" /> Aproximado</>
              )}
            </div>
            {!exactAlarmsAllowed && (
              <p className="text-xs text-amber-400 mt-2">
                O aplicativo está operando em fallback. Atrasos de até 10 minutos podem ocorrer.
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 p-4 sm:p-6 pb-2">
            <CardTitle className="text-sm font-medium text-[var(--color-primary-muted)]">
              Lembretes Ativos
            </CardTitle>
            <CalendarClock className="h-4 w-4 text-[var(--color-primary-muted)]" />
          </CardHeader>
          <CardContent className="p-4 sm:p-6 pt-0">
            <div className="text-2xl font-bold text-white">
              {activeSchedules} agendamento{activeSchedules !== 1 ? 's' : ''}
            </div>
            {nextTriggerAt && (
              <p className="text-xs text-[var(--color-primary-muted)] mt-2">
                Próximo: {formatDateTime(nextTriggerAt)}
              </p>
            )}
            <p className="text-xs text-[var(--color-primary-muted)] mt-1">
              Última reconciliação: {formatDateTime(lastReconciliationAt)}
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function DeviceDiagnosticsPage() {
  return (
    <Suspense fallback={<div className="text-center py-12 text-[var(--color-primary-muted)]">Carregando...</div>}>
      <DiagnosticsContent />
    </Suspense>
  );
}

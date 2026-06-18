'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card';
import { FileText, X, Info } from 'lucide-react';
import { useIsMobile } from '@/hooks/use-mobile';

const initialLogs = [
  { time: '15:34:12', type: 'ADMIN_LOGIN', typeColor: 'text-blue-400', message: 'philippeboechat1@gmail.com authenticated via CF Access', details: 'User: philippeboechat1@gmail.com\nIP: 192.168.1.1\nAuth Method: Cloudflare Access\nStatus: Success' },
  { time: '14:20:00', type: 'OTA_PUBLISH_SUCCESS', typeColor: 'text-emerald-400', message: "Version 1.2.0 deployed to channel 'produção'", details: 'Version: 1.2.0\nChannel: produção\nTriggered By: System\nRollout: 100%' },
  { time: '09:12:44', type: 'WARN_API_LATENCY', typeColor: 'text-amber-400', message: 'D1 Query /feedbacks exceeded 300ms (542ms)', details: 'Endpoint: /api/feedbacks\nQuery Time: 542ms\nThreshold: 300ms\nDatabase Node: us-east-4' },
];

export default function LogsPage() {
  const [selectedLog, setSelectedLog] = useState<typeof initialLogs[0] | null>(null);
  const isMobile = useIsMobile();

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">Trilhas de Auditoria (Logs)</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Registros imutáveis de operações no backend Cloudflare Workers.</p>
        </div>
      </div>

      <Card className="shadow-sm">
        <CardHeader className="p-5 pb-0">
          <CardTitle className="text-lg font-bold text-white flex items-center gap-2">Histórico de Eventos</CardTitle>
          <CardDescription className="text-[13px] text-[var(--color-primary-muted)] mt-1">Visualização dos últimos eventos críticos no sistema</CardDescription>
        </CardHeader>
        <CardContent className="p-4 sm:p-5">
          <div className="space-y-4">
            {initialLogs.map((log, i) => (
              isMobile ? (
                /* Mobile: stacked layout */
                <div key={i} className="border-b border-[var(--color-border)] pb-4 last:border-0 last:pb-0 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className={`${log.typeColor} font-bold text-[12px]`}>{log.type}</span>
                    <span className="text-[var(--color-primary-muted)] font-mono text-[12px]">{log.time}</span>
                  </div>
                  <p className="text-white text-[13px] leading-relaxed">{log.message}</p>
                  <button onClick={() => setSelectedLog(log)} className="text-blue-500 hover:text-blue-400 text-[12px] font-medium flex items-center gap-1">
                    <FileText className="h-3.5 w-3.5" /> Ver detalhes
                  </button>
                </div>
              ) : (
                /* Desktop: horizontal layout */
                <div key={i} className="flex items-center gap-4 text-sm border-b border-[var(--color-border)] pb-4 last:border-0 last:pb-0">
                  <div className="text-[var(--color-primary-muted)] font-mono text-[13px]">{log.time}</div>
                  <div className={`${log.typeColor} font-bold text-[13px] w-48 shrink-0`}>{log.type}</div>
                  <div className="text-white flex-1 truncate text-[13px]">{log.message}</div>
                  <button onClick={() => setSelectedLog(log)} className="text-blue-500 hover:text-blue-400 transition-colors ml-auto" title="Ver Detalhes">
                    <FileText className="h-4 w-4" />
                  </button>
                </div>
              )
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Detail Modal */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4 bg-black/50 backdrop-blur-sm">
          <div className={`bg-[var(--color-surface)] border border-[var(--color-border)] shadow-2xl w-full overflow-hidden flex flex-col ${isMobile ? 'rounded-t-2xl max-h-[90vh]' : 'rounded-2xl max-w-lg'}`}>
            <div className="flex items-center justify-between p-5 border-b border-[var(--color-border)] shrink-0">
               <h3 className="text-lg font-bold text-white flex items-center gap-2">
                 <Info className="w-5 h-5 text-purple-400" /> Detalhes do Evento
               </h3>
               <button onClick={() => setSelectedLog(null)} className="text-[var(--color-primary-muted)] hover:text-white transition-colors p-1">
                 <X className="w-5 h-5" />
               </button>
            </div>
            
            <div className="p-5 flex-1 overflow-y-auto space-y-4">
               <div>
                  <span className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Timestamp</span>
                  <span className="text-[14px] font-bold text-white">{selectedLog.time}</span>
               </div>
               <div>
                  <span className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Tipo de Evento</span>
                  <span className={`text-[14px] font-bold ${selectedLog.typeColor}`}>{selectedLog.type}</span>
               </div>
               <div>
                  <span className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Mensagem</span>
                  <span className="text-[14px] font-medium text-white">{selectedLog.message}</span>
               </div>
               <div>
                  <span className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Payload / Metadados</span>
                  <pre className="p-3 bg-[var(--color-surface-hover)] rounded-xl border border-[var(--color-border)] text-[13px] text-[var(--color-primary-muted)] leading-relaxed font-mono whitespace-pre-wrap break-all">
                    {selectedLog.details}
                  </pre>
               </div>
            </div>

            <div className="p-5 border-t border-[var(--color-border)] flex items-center justify-end gap-3 bg-[var(--color-surface-hover)] shrink-0">
               <button onClick={() => setSelectedLog(null)} className="px-4 py-2.5 text-[14px] font-medium text-white bg-[var(--color-border)] hover:bg-[var(--color-border-hover)] rounded-xl transition-colors w-full sm:w-auto">
                 Fechar
               </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

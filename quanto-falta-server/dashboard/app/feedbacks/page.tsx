'use client';

import { useEffect, useMemo, useState } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { Badge } from '@/components/ui/Badge';
import { Star, MessageSquareText, X, Reply } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';
import { adminApi, FeedbackItem, formatDateTime, formatNumber } from '@/lib/admin-api';
import { useIsMobile } from '@/hooks/use-mobile';
import { MobileCard, MobileCardList } from '@/components/ui/MobileComponents';

export default function FeedbacksPage() {
  const [feedbacks, setFeedbacks] = useState<FeedbackItem[]>([]);
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedFeedback, setSelectedFeedback] = useState<FeedbackItem | null>(null);
  const [replyText, setReplyText] = useState('');
  const isMobile = useIsMobile();

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const [list, statsData] = await Promise.all([
        adminApi.feedback({ limit: 100 }),
        adminApi.feedbackStats(),
      ]);
      setFeedbacks(list.feedback ?? []);
      setStats(statsData);
    } catch (err: any) {
      setError(err.message ?? 'Nao foi possivel carregar feedbacks.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const averageRating = useMemo(() => {
    if (stats?.averageRating != null) return Number(stats.averageRating).toFixed(1);
    const rated = feedbacks.filter((item) => item.rating != null);
    if (!rated.length) return '-';
    return (rated.reduce((sum, item) => sum + Number(item.rating), 0) / rated.length).toFixed(1);
  }, [feedbacks, stats]);

  async function submitReply() {
    if (!selectedFeedback) return;
    await adminApi.updateFeedback(selectedFeedback.id, {
      status: 'resolved',
      adminNotes: replyText || selectedFeedback.admin_notes || 'Resolvido pelo painel administrativo.',
    });
    setSelectedFeedback(null);
    setReplyText('');
    await load();
  }

  return (
    <div className="space-y-6 pb-12 relative">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">Feedbacks</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Avaliacoes e comentarios recebidos pelo app.</p>
        </div>
      </div>

      {error && <Card className="p-4 border-red-500/30 text-red-400">{error}</Card>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <Card><CardContent className="p-5"><div className="flex items-center justify-between"><p className="text-[11px] font-bold text-[var(--color-primary-muted)] tracking-wider uppercase mb-1">Media Geral</p><Star className="h-4 w-4 text-emerald-400 fill-emerald-400" /></div><p className="text-4xl font-bold text-white mt-1">{loading ? '...' : averageRating}</p></CardContent></Card>
        <Card><CardContent className="p-5"><div className="flex items-center justify-between"><p className="text-[11px] font-bold text-[var(--color-primary-muted)] tracking-wider uppercase mb-1">Total</p><MessageSquareText className="h-4 w-4 text-purple-400" /></div><p className="text-4xl font-bold text-white mt-1">{loading ? '...' : formatNumber(stats?.total ?? feedbacks.length)}</p></CardContent></Card>
        <Card><CardContent className="p-5"><p className="text-[11px] font-bold text-[var(--color-primary-muted)] tracking-wider uppercase mb-1">Novos</p><p className="text-4xl font-bold text-purple-400 mt-1">{loading ? '...' : formatNumber(feedbacks.filter((item) => item.status === 'new').length)}</p></CardContent></Card>
      </div>

      {!isMobile ? (
        <div className="bg-[var(--color-surface-hover)] rounded-2xl overflow-hidden border border-[var(--color-border)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nota</TableHead>
                <TableHead>Comentario</TableHead>
                <TableHead>Versao</TableHead>
                <TableHead>Data</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Acao</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={6} className="text-center py-12">Carregando feedbacks...</TableCell></TableRow>
              ) : feedbacks.length === 0 ? (
                <TableRow><TableCell colSpan={6} className="text-center py-12 text-[var(--color-primary-muted)]">Nenhum feedback recebido.</TableCell></TableRow>
              ) : feedbacks.map((fb) => (
                <TableRow key={fb.id}>
                  <TableCell><div className="flex items-center gap-1.5"><span className="font-bold text-[14px] text-white">{fb.rating ?? '-'}</span>{fb.rating && <Star className={`h-3.5 w-3.5 ${fb.rating >= 4 ? 'text-emerald-400 fill-emerald-400' : fb.rating === 3 ? 'text-amber-400 fill-amber-400' : 'text-red-400 fill-red-400'}`} />}</div></TableCell>
                  <TableCell className="max-w-[460px]"><p className="text-[14px] truncate text-white">{fb.message}</p><p className="text-[12px] text-[var(--color-primary-muted)] mt-0.5">{fb.installationId ?? fb.installation_id ?? 'instalacao anonima'} - {fb.category}</p></TableCell>
                  <TableCell className="font-mono text-[13px] text-[var(--color-primary-muted)]">{fb.versionCode ?? fb.version_code ?? '-'}</TableCell>
                  <TableCell className="text-[13px] text-[var(--color-primary-muted)]">{formatDateTime(fb.createdAt ?? fb.created_at)}</TableCell>
                  <TableCell><StatusBadge status={fb.status} /></TableCell>
                  <TableCell className="text-right"><button onClick={() => { setSelectedFeedback(fb); setReplyText(fb.admin_notes ?? ''); }} className="inline-flex items-center gap-1.5 text-[13px] font-medium text-purple-400 hover:text-purple-300"><Reply className="w-3.5 h-3.5" />{fb.status === 'resolved' ? 'Ver' : 'Responder'}</button></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : (
        <MobileCardList>
          {loading ? (
            <div className="text-center py-12 text-[var(--color-primary-muted)]">Carregando feedbacks...</div>
          ) : feedbacks.length === 0 ? (
            <div className="text-center py-12 text-[var(--color-primary-muted)]">Nenhum feedback recebido.</div>
          ) : feedbacks.map((fb) => (
            <MobileCard
              key={fb.id}
              title={
                <div className="flex items-center gap-2">
                  {fb.rating != null && (
                    <div className="flex items-center gap-1">
                      <span className="font-bold text-white">{fb.rating}</span>
                      <Star className={`h-3.5 w-3.5 ${fb.rating >= 4 ? 'text-emerald-400 fill-emerald-400' : fb.rating === 3 ? 'text-amber-400 fill-amber-400' : 'text-red-400 fill-red-400'}`} />
                    </div>
                  )}
                  <StatusBadge status={fb.status} />
                </div>
              }
              subtitle={<span className="line-clamp-2">{fb.message}</span>}
              fields={[
                { label: 'Versão', value: fb.versionCode ?? fb.version_code ?? '-' },
                { label: 'Data', value: formatDateTime(fb.createdAt ?? fb.created_at) },
              ]}
              actions={
                <button
                  onClick={() => { setSelectedFeedback(fb); setReplyText(fb.admin_notes ?? ''); }}
                  className="flex items-center gap-1.5 text-[13px] font-medium text-purple-400"
                >
                  <Reply className="w-4 h-4" />
                  {fb.status === 'resolved' ? 'Ver detalhes' : 'Responder'}
                </button>
              }
            />
          ))}
        </MobileCardList>
      )}

      {selectedFeedback && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4 bg-black/50 backdrop-blur-sm">
          <div className={`bg-[var(--color-surface)] border border-[var(--color-border)] shadow-2xl w-full overflow-hidden flex flex-col ${isMobile ? 'rounded-t-2xl max-h-[90vh]' : 'rounded-2xl max-w-md'}`}>
            <div className="flex items-center justify-between p-5 border-b border-[var(--color-border)] shrink-0">
              <h3 className="text-lg font-bold text-white flex items-center gap-2"><Reply className="w-5 h-5 text-purple-400" /> Feedback</h3>
              <button onClick={() => setSelectedFeedback(null)} className="text-[var(--color-primary-muted)] hover:text-white p-1"><X className="w-5 h-5" /></button>
            </div>
            <div className="p-5 space-y-4 overflow-y-auto flex-1">
              <div className="bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl p-4">
                <p className="text-[13px] font-medium text-white">{selectedFeedback.installationId ?? selectedFeedback.installation_id ?? 'Instalacao anonima'}</p>
                <p className="text-[14px] text-[var(--color-primary-muted)] italic mt-2 break-words">"{selectedFeedback.message}"</p>
              </div>
              <div>
                <label className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Notas administrativas</label>
                <textarea value={replyText} onChange={(e) => setReplyText(e.target.value)} rows={4} className="w-full bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl px-3 py-2 text-white text-[14px] focus:outline-none focus:border-purple-500/50 resize-none" />
              </div>
            </div>
            <div className="p-5 border-t border-[var(--color-border)] flex flex-col sm:flex-row items-center justify-end gap-3 bg-[var(--color-surface-hover)] shrink-0">
              <button onClick={() => setSelectedFeedback(null)} className="px-4 py-2.5 text-[14px] font-medium text-white hover:bg-[var(--color-border)] rounded-xl w-full sm:w-auto order-2 sm:order-1">Fechar</button>
              <button onClick={submitReply} className="px-4 py-2.5 text-[14px] font-medium text-white bg-purple-600 hover:bg-purple-700 rounded-xl w-full sm:w-auto order-1 sm:order-2">Marcar como resolvido</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  if (status === 'resolved') return <Badge variant="success">Resolvido</Badge>;
  if (status === 'reviewing' || status === 'planned') return <Badge variant="warning">Em analise</Badge>;
  return <Badge variant="default" className="bg-purple-500/20 text-purple-400 border border-purple-500/20">Novo</Badge>;
}

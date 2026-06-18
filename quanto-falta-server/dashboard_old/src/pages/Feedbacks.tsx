import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { feedback } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Card, CardContent } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton } from '../components/ui/Skeleton';
import { Lightbulb, Bug, Heart, HelpCircle, MessageSquare, Search, Filter, Star, Clock, Smartphone, Info } from 'lucide-react';

const STATUS_MAP: Record<string, { label: string; variant: 'default' | 'success' | 'warning' | 'danger' | 'info' }> = {
  new: { label: 'Novo', variant: 'info' },
  reviewing: { label: 'Em análise', variant: 'warning' },
  planned: { label: 'Planejado', variant: 'default' },
  resolved: { label: 'Resolvido', variant: 'success' },
  ignored: { label: 'Ignorado', variant: 'danger' },
};

const STATUS_OPTS = ['new', 'reviewing', 'planned', 'resolved', 'ignored'];

const CAT_MAP: Record<string, { label: string; icon: any; color: string }> = {
  suggestion: { label: 'Sugestão', icon: Lightbulb, color: 'var(--warning)' },
  bug: { label: 'Problema', icon: Bug, color: 'var(--danger)' },
  compliment: { label: 'Elogio', icon: Heart, color: 'var(--success)' },
  question: { label: 'Dúvida', icon: HelpCircle, color: 'var(--info)' },
  other: { label: 'Outro', icon: MessageSquare, color: 'var(--text-secondary)' },
};

export default function Feedbacks() {
  const qc = useQueryClient();
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [search, setSearch] = useState('');
  const [selectedFeedback, setSelectedFeedback] = useState<any>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['feedback', filters, search],
    queryFn: () => feedback.list({ ...filters, ...(search ? { search } : {}) }),
  });

  const { data: statsData } = useQuery({
    queryKey: ['feedback-stats'],
    queryFn: feedback.stats,
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => feedback.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['feedback'] });
      // Update selected feedback if it's open
      if (selectedFeedback) {
        qc.invalidateQueries({ queryKey: ['feedback', filters, search] });
        // Close modal or update locally to be fast, for now we let react query refetch in background
      }
    },
  });

  const handleStatusChange = (id: string, newStatus: string) => {
    updateMut.mutate({ id, data: { status: newStatus } });
    if (selectedFeedback && selectedFeedback.id === id) {
      setSelectedFeedback({ ...selectedFeedback, status: newStatus });
    }
  };

  return (
    <div className="page-container flex-col gap-6">
      <div className="page-header">
        <div>
          <h1 className="page-title">Feedbacks</h1>
          <p className="page-subtitle">Central de atendimento e sugestões dos usuários</p>
        </div>
        <div className="flex gap-4">
          <Card className="flex items-center justify-center p-3 px-6 bg-surface-elevated">
            <div className="flex-col">
              <span className="text-xs text-secondary font-semibold uppercase">Total</span>
              <span className="text-xl font-bold font-outfit text-primary-text">{statsData?.total ?? 0}</span>
            </div>
          </Card>
          <Card className="flex items-center justify-center p-3 px-6 bg-surface-elevated">
            <div className="flex-col">
              <span className="text-xs text-secondary font-semibold uppercase">Nota Média</span>
              <span className="text-xl font-bold font-outfit flex items-center gap-1 text-warning">
                {statsData?.averageRating ?? '—'} <Star size={16} fill="currentColor" />
              </span>
            </div>
          </Card>
        </div>
      </div>

      <div className="flex items-center gap-4 flex-wrap p-4 rounded-lg border border-white/[0.03] shadow-sm mb-2" style={{ backgroundColor: '#0a0a0f' }}>
        <div className="flex-1 min-w-[250px] relative">
          <Search size={16} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-text-tertiary" />
          <input
            className="w-full pl-9 pr-4 py-1.5 bg-transparent border-none text-sm text-white outline-none focus:ring-0"
            placeholder="Buscar na mensagem..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter size={16} className="text-text-tertiary" />
          <select
            className="p-1.5 bg-transparent border border-white/[0.05] rounded-md text-xs text-text-secondary outline-none focus:border-primary transition-colors cursor-pointer appearance-none"
            value={filters.status ?? ''}
            onChange={e => setFilters(f => ({ ...f, status: e.target.value || undefined! }))}
          >
            <option value="">Todos os status</option>
            {STATUS_OPTS.map(s => <option key={s} value={s}>{STATUS_MAP[s]?.label}</option>)}
          </select>
          <select
            className="p-1.5 bg-transparent border border-white/[0.05] rounded-md text-xs text-text-secondary outline-none focus:border-primary transition-colors cursor-pointer appearance-none"
            value={filters.category ?? ''}
            onChange={e => setFilters(f => ({ ...f, category: e.target.value || undefined! }))}
          >
            <option value="">Todas as categorias</option>
            {Object.entries(CAT_MAP).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
          </select>
        </div>
      </div>

      <div className="grid gap-4 mt-2">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}><CardContent className="p-4 flex gap-4 items-center"><Skeleton className="h-16 w-full" /></CardContent></Card>
          ))
        ) : (data?.feedback?.length === 0) ? (
          <EmptyState icon={MessageSquare} title="Nenhum feedback encontrado" description="Tente ajustar os filtros ou os termos de busca." />
        ) : (
          (data?.feedback ?? []).map((fb: any) => {
            const cat = CAT_MAP[fb.category] || CAT_MAP['other'];
            const CatIcon = cat.icon;
            const statusInfo = STATUS_MAP[fb.status] || { label: fb.status, variant: 'default' };

            return (
              <Card key={fb.id} className="bg-[#0b0b12] border-white/[0.03] shadow-md cursor-pointer hover:border-white/10 transition-colors" onClick={() => setSelectedFeedback(fb)}>
                <CardContent className="p-4 flex items-start sm:items-center gap-4">
                  {/* Category Icon */}
                  <div className="flex items-center justify-center flex-shrink-0 pt-1 sm:pt-0">
                    <CatIcon size={16} className="text-text-secondary" />
                  </div>
                  
                  {/* Content */}
                  <div className="flex-1 min-w-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div className="flex flex-col">
                      <div className="flex items-center gap-3 mb-1">
                        <span className="font-bold text-sm text-white">{cat.label}</span>
                        {fb.rating && (
                          <div className="flex items-center text-[#F59E0B]">
                            {Array.from({ length: 5 }).map((_, i) => (
                              <Star key={i} size={10} fill={i < fb.rating ? 'currentColor' : 'none'} className={i >= fb.rating ? 'text-white/10' : ''} />
                            ))}
                          </div>
                        )}
                        <span className="text-[11px] text-text-tertiary flex items-center gap-1">
                          <Clock size={10} /> {new Date(fb.created_at * 1000).toLocaleDateString('pt-BR')}
                        </span>
                      </div>
                      <p className="text-sm text-text-secondary line-clamp-1">{fb.message}</p>
                    </div>
                    
                    <div className="flex items-center gap-3 sm:ml-4 flex-shrink-0">
                      <div className="flex items-center text-[10px] text-text-tertiary gap-1">
                        <Smartphone size={12} /> {fb.app_version || 'v?'}
                      </div>
                      <Badge variant={statusInfo.variant} className="text-[10px] px-2 py-0.5 rounded shadow-sm">{statusInfo.label}</Badge>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })
        )}
      </div>

      <Modal
        isOpen={!!selectedFeedback}
        onClose={() => setSelectedFeedback(null)}
        title="Detalhes do Feedback"
        footer={
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold text-secondary">Status:</span>
              <select
                className="p-1.5 bg-surface border border-default rounded-md text-sm text-primary-text outline-none"
                value={selectedFeedback?.status}
                onChange={e => handleStatusChange(selectedFeedback.id, e.target.value)}
              >
                {STATUS_OPTS.map(s => <option key={s} value={s}>{STATUS_MAP[s]?.label}</option>)}
              </select>
            </div>
            <Button variant="outline" onClick={() => setSelectedFeedback(null)}>Fechar</Button>
          </div>
        }
      >
        {selectedFeedback && (
          <div className="flex-col gap-6">
            <div className="flex items-center gap-4 bg-surface-elevated p-4 rounded-lg">
              <div className="flex items-center justify-center rounded-full flex-shrink-0" style={{ width: 48, height: 48, backgroundColor: `${(CAT_MAP[selectedFeedback.category] || CAT_MAP['other']).color}15`, color: (CAT_MAP[selectedFeedback.category] || CAT_MAP['other']).color }}>
                {(() => {
                  const Icon = (CAT_MAP[selectedFeedback.category] || CAT_MAP['other']).icon;
                  return <Icon size={24} />;
                })()}
              </div>
              <div>
                <h3 className="font-semibold text-lg">{(CAT_MAP[selectedFeedback.category] || CAT_MAP['other']).label}</h3>
                {selectedFeedback.rating && (
                  <div className="flex items-center text-warning gap-1 mt-1">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star key={i} size={14} fill={i < selectedFeedback.rating ? 'currentColor' : 'none'} className={i >= selectedFeedback.rating ? 'text-tertiary' : ''} />
                    ))}
                    <span className="text-xs font-semibold ml-1">Nota {selectedFeedback.rating}</span>
                  </div>
                )}
              </div>
              <div className="ml-auto flex flex-col items-end">
                <span className="text-sm text-secondary">{new Date(selectedFeedback.created_at).toLocaleDateString('pt-BR')}</span>
                <span className="text-xs text-tertiary">{new Date(selectedFeedback.created_at).toLocaleTimeString('pt-BR')}</span>
              </div>
            </div>

            <div className="flex-col gap-2">
              <h4 className="text-sm font-semibold text-secondary uppercase tracking-wider">Mensagem do Usuário</h4>
              <p className="text-base text-primary-text bg-surface p-4 rounded-lg border border-default whitespace-pre-wrap leading-relaxed">
                {selectedFeedback.message}
              </p>
            </div>

            <div className="flex-col gap-2">
              <h4 className="text-sm font-semibold text-secondary uppercase tracking-wider flex items-center gap-2"><Info size={14}/> Informações Técnicas</h4>
              <div className="grid grid-cols-2 gap-4 bg-surface-elevated p-4 rounded-lg border border-default text-sm">
                <div>
                  <span className="text-tertiary block mb-1">Versão do App</span>
                  <span className="font-medium text-primary-text">v{selectedFeedback.version_code}</span>
                </div>
                <div>
                  <span className="text-tertiary block mb-1">Dispositivo</span>
                  <span className="font-medium text-primary-text">{selectedFeedback.model || 'Desconhecido'}</span>
                </div>
                <div>
                  <span className="text-tertiary block mb-1">Versão do Android</span>
                  <span className="font-medium text-primary-text">{selectedFeedback.android_version ? `Android ${selectedFeedback.android_version}` : 'Desconhecido'}</span>
                </div>
                <div>
                  <span className="text-tertiary block mb-1">ID do Usuário</span>
                  <span className="font-medium text-primary-text text-xs break-all">{selectedFeedback.user_id || 'Não fornecido'}</span>
                </div>
              </div>
            </div>
            
            <div className="flex-col gap-2">
               <h4 className="text-sm font-semibold text-secondary uppercase tracking-wider">Notas Administrativas (Interno)</h4>
               <textarea 
                  className="w-full p-3 bg-surface-elevated border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary transition-colors resize-y"
                  rows={3}
                  placeholder="Adicione notas internas sobre a resolução deste feedback..."
                  value={selectedFeedback.admin_notes || ''}
                  onChange={(e) => {
                     const newNotes = e.target.value;
                     setSelectedFeedback({...selectedFeedback, admin_notes: newNotes});
                     // Optimistic quick update, real app might debounce
                     updateMut.mutate({ id: selectedFeedback.id, data: { admin_notes: newNotes } });
                  }}
               />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

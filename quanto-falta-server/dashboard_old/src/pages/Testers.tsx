import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { Plus, Edit2, Trash2, CheckCircle2, XCircle, Users, Star, StarOff, Globe, Globe2 } from 'lucide-react';
import { testers } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';

const BADGES = {
  'initial_tester': 'Tester Inicial',
  'beta_tester': 'Beta Tester',
  'bug_hunter': 'Caçador de Bugs',
  'ui_tester': 'Tester de Interface',
  'performance_tester': 'Tester de Performance',
  'contributor': 'Colaborador',
};

export default function Testers() {
  const qc = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTester, setEditingTester] = useState<any>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['testers'],
    queryFn: testers.list,
  });

  const testersList = data?.testers ?? [];

  const summary = useMemo(() => {
    let active = 0, published = 0, pendingConsent = 0;
    testersList.forEach((t: any) => {
      if (t.is_active) active++;
      if (t.published_at) published++;
      if (!t.consent_confirmed) pendingConsent++;
    });
    return { total: testersList.length, active, published, pendingConsent };
  }, [testersList]);

  const createMut = useMutation({
    mutationFn: testers.create,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['testers'] });
      setIsModalOpen(false);
    },
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => testers.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['testers'] });
      setIsModalOpen(false);
    },
  });

  const deleteMut = useMutation({
    mutationFn: testers.remove,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['testers'] }),
  });

  function handleSave(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const payload = {
      display_name: fd.get('display_name'),
      nickname: fd.get('nickname'),
      avatar_key: fd.get('avatar_key'),
      badge_key: fd.get('badge_key'),
      message: fd.get('message'),
      participation_version: fd.get('participation_version'),
      participation_period: fd.get('participation_period'),
      display_order: Number(fd.get('display_order') || 0),
      is_active: fd.get('is_active') === 'on',
      is_featured: fd.get('is_featured') === 'on',
      consent_confirmed: fd.get('consent_confirmed') === 'on',
      // Mock toggle for published state via UI since there is no explicit endpoint, 
      // but if the API allows updating published_at we can send it.
    };

    if (editingTester) {
      updateMut.mutate({ id: editingTester.id, data: payload });
    } else {
      createMut.mutate(payload);
    }
  }

  function toggleFeatured(t: any) {
    updateMut.mutate({ id: t.id, data: { is_featured: !t.is_featured } });
  }


  function togglePublished(t: any) {
    // Note: Assuming API accepts published_at explicitly to publish/unpublish, or handles it.
    // If not, this is standard optimistic update for a boolean-like toggle.
    const isPublished = !!t.published_at;
    updateMut.mutate({ id: t.id, data: { published_at: isPublished ? null : new Date().toISOString() } });
  }

  return (
    <div className="page-container flex-col gap-6">
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-extrabold font-outfit text-gradient tracking-tight">Gestão de Testers</h1>
          <p className="text-sm text-text-secondary mt-1">Agradecimentos, consentimentos e controle de publicação</p>
        </div>
        <Button onClick={() => { setEditingTester(null); setIsModalOpen(true); }} className="bg-[#8B5CF6] hover:bg-[#7C3AED] text-white">
          <Plus size={18} /> Novo Tester
        </Button>
      </div>

      <div className="grid gap-4 grid-cols-1 md:grid-cols-2 lg:grid-cols-4">
        <Card className="bg-[#0b0b12] border-white/[0.03] shadow-md flex flex-col justify-center p-5 h-[104px] rounded-[14px] relative overflow-hidden group">
          <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full blur-[40px] opacity-20 group-hover:opacity-30 transition-opacity bg-[#8B5CF6] pointer-events-none" />
          <div className="relative z-10 w-full mb-3">
            <span className="text-[10px] font-bold text-text-tertiary uppercase tracking-widest leading-none truncate block">Total de Testers</span>
          </div>
          <span className="text-[32px] font-extrabold font-outfit text-white leading-none tracking-tight truncate relative z-10">{summary.total}</span>
        </Card>
        <Card className="bg-[#0b0b12] border-white/[0.03] shadow-md flex flex-col justify-center p-5 h-[104px] rounded-[14px] relative overflow-hidden group">
          <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full blur-[40px] opacity-20 group-hover:opacity-30 transition-opacity bg-[#10B981] pointer-events-none" />
          <div className="relative z-10 w-full mb-3">
            <span className="text-[10px] font-bold text-text-tertiary uppercase tracking-widest leading-none truncate block">Ativos no App</span>
          </div>
          <span className="text-[32px] font-extrabold font-outfit text-[#10B981] leading-none tracking-tight truncate relative z-10">{summary.active}</span>
        </Card>
        <Card className="bg-[#0b0b12] border-white/[0.03] shadow-md flex flex-col justify-center p-5 h-[104px] rounded-[14px] relative overflow-hidden group">
          <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full blur-[40px] opacity-20 group-hover:opacity-30 transition-opacity bg-[#3B82F6] pointer-events-none" />
          <div className="relative z-10 w-full mb-3">
            <span className="text-[10px] font-bold text-text-tertiary uppercase tracking-widest leading-none truncate block">Publicados</span>
          </div>
          <span className="text-[32px] font-extrabold font-outfit text-[#3B82F6] leading-none tracking-tight truncate relative z-10">{summary.published}</span>
        </Card>
        <Card className="bg-[#0b0b12] border-white/[0.03] shadow-md flex flex-col justify-center p-5 h-[104px] rounded-[14px] relative overflow-hidden group">
          <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full blur-[40px] opacity-20 group-hover:opacity-30 transition-opacity bg-[#F59E0B] pointer-events-none" />
          <div className="relative z-10 w-full mb-3">
            <span className="text-[10px] font-bold text-text-tertiary uppercase tracking-widest leading-none truncate block">Pendentes (Consentimento)</span>
          </div>
          <span className="text-[32px] font-extrabold font-outfit text-[#F59E0B] leading-none tracking-tight truncate relative z-10">{summary.pendingConsent}</span>
        </Card>
      </div>

      <Card className="mt-4">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Perfil</TableHead>
              <TableHead>Versão/Selo</TableHead>
              <TableHead>Mensagem</TableHead>
              <TableHead className="text-center">Consentimento</TableHead>
              <TableHead className="text-center">Status</TableHead>
              <TableHead className="text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
               Array.from({ length: 3 }).map((_, i) => (
                 <TableRow key={i}>
                   <TableCell><Skeleton className="h-10 w-32" /></TableCell>
                   <TableCell><Skeleton className="h-6 w-20" /></TableCell>
                   <TableCell><Skeleton className="h-6 w-full" /></TableCell>
                   <TableCell><Skeleton className="h-6 w-16 mx-auto" /></TableCell>
                   <TableCell><Skeleton className="h-6 w-16 mx-auto" /></TableCell>
                   <TableCell><Skeleton className="h-8 w-24 ml-auto" /></TableCell>
                 </TableRow>
               ))
            ) : testersList.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6}>
                  <EmptyState icon={Users} title="Nenhum tester cadastrado" description="Adicione seu primeiro tester para exibir no app." />
                </TableCell>
              </TableRow>
            ) : (
              testersList.map((t: any) => (
                <TableRow key={t.id} className={t.is_featured ? 'bg-primary-bg/50' : ''}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      {t.avatar_key ? (
                        <img src={t.avatar_key} alt={t.display_name} className="w-10 h-10 rounded-full object-cover bg-surface-elevated" />
                      ) : (
                        <div className="w-10 h-10 rounded-full bg-surface-elevated flex items-center justify-center font-bold text-secondary text-xs border border-default">
                          {t.display_name.substring(0, 2).toUpperCase()}
                        </div>
                      )}
                      <div className="flex flex-col">
                        <span className="font-semibold text-sm text-primary-text flex items-center gap-1">
                          {t.display_name}
                          {t.is_featured && <span title="Em destaque"><Star size={12} fill="var(--warning)" color="var(--warning)" /></span>}
                        </span>
                        <span className="text-xs text-secondary">{t.nickname ? `@${t.nickname}` : '—'}</span>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-1 items-start">
                      {t.participation_version ? <Badge variant="info">v{t.participation_version}</Badge> : null}
                      {t.badge_key ? <span className="text-xs text-tertiary">{BADGES[t.badge_key as keyof typeof BADGES] || t.badge_key}</span> : null}
                    </div>
                  </TableCell>
                  <TableCell>
                    <p className="text-xs text-secondary max-w-[200px] truncate" title={t.message}>
                      {t.message || '—'}
                    </p>
                  </TableCell>
                  <TableCell className="text-center">
                    {t.consent_confirmed ? (
                      <Badge variant="success" className="bg-success-bg text-success border-success-border"><CheckCircle2 size={12} className="mr-1"/> OK</Badge>
                    ) : (
                      <Badge variant="warning" className="bg-warning-bg text-warning border-warning-border"><XCircle size={12} className="mr-1"/> Pendente</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-center">
                    <div className="flex flex-col items-center gap-1">
                       <Badge variant={t.is_active ? 'success' : 'default'}>{t.is_active ? 'Ativo' : 'Inativo'}</Badge>
                       <span className="text-[10px] text-tertiary">{t.published_at ? 'Publicado' : 'Rascunho'}</span>
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button variant="ghost" size="icon" onClick={() => toggleFeatured(t)} title={t.is_featured ? "Remover destaque" : "Destacar"}>
                        {t.is_featured ? <StarOff size={16} className="text-warning" /> : <Star size={16} className="text-secondary" />}
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => togglePublished(t)} title={t.published_at ? "Despublicar" : "Publicar"}>
                        {t.published_at ? <Globe2 size={16} className="text-info" /> : <Globe size={16} className="text-secondary" />}
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => { setEditingTester(t); setIsModalOpen(true); }} title="Editar">
                        <Edit2 size={16} className="text-primary" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => { if(confirm('Tem certeza que deseja excluir este tester?')) deleteMut.mutate(t.id) }} title="Excluir">
                        <Trash2 size={16} className="text-danger" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingTester ? 'Editar Tester' : 'Novo Tester'}
      >
        <form id="tester-form" onSubmit={handleSave} className="flex-col gap-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Nome (Obrigatório)</label>
              <input name="display_name" required defaultValue={editingTester?.display_name} className="w-full p-2 bg-surface border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary" />
            </div>
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Apelido (Opcional)</label>
              <input name="nickname" defaultValue={editingTester?.nickname} className="w-full p-2 bg-surface border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary" />
            </div>
          </div>
          <div className="flex-col gap-1">
            <label className="text-sm font-semibold text-secondary">URL do Avatar</label>
            <input name="avatar_key" placeholder="https://..." defaultValue={editingTester?.avatar_key} className="w-full p-2 bg-surface border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Selo de Participação</label>
              <select name="badge_key" defaultValue={editingTester?.badge_key} className="w-full p-2 bg-surface border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary">
                <option value="">Sem selo</option>
                {Object.entries(BADGES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Versão Base (Ex: 1.0)</label>
              <input name="participation_version" defaultValue={editingTester?.participation_version} className="w-full p-2 bg-surface border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary" />
            </div>
          </div>
          <div className="flex-col gap-1">
            <label className="text-sm font-semibold text-secondary">Mensagem (Máx 200 caract.)</label>
            <textarea name="message" rows={3} maxLength={200} defaultValue={editingTester?.message} className="w-full p-2 bg-surface border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-4">
             <div className="flex-col gap-1">
               <label className="text-sm font-semibold text-secondary">Ordem de Exibição</label>
               <input type="number" name="display_order" defaultValue={editingTester?.display_order || 0} className="w-full p-2 bg-surface border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary" />
             </div>
          </div>
          <div className="flex gap-6 mt-2 pt-4 border-t border-default">
            <label className="flex items-center gap-2 text-sm text-primary-text cursor-pointer">
              <input type="checkbox" name="is_active" defaultChecked={editingTester ? editingTester.is_active : true} className="accent-primary" /> Ativo
            </label>
            <label className="flex items-center gap-2 text-sm text-primary-text cursor-pointer">
              <input type="checkbox" name="is_featured" defaultChecked={editingTester?.is_featured} className="accent-primary" /> Destaque
            </label>
            <label className="flex items-center gap-2 text-sm text-primary-text cursor-pointer">
              <input type="checkbox" name="consent_confirmed" defaultChecked={editingTester?.consent_confirmed} className="accent-primary" /> Consentimento
            </label>
          </div>
          <div className="flex justify-end gap-2 mt-4">
            <Button variant="outline" type="button" onClick={() => setIsModalOpen(false)}>Cancelar</Button>
            <Button type="submit" isLoading={createMut.isPending || updateMut.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

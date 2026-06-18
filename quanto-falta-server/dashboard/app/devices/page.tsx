'use client';

import { useEffect, useState } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { Card, CardContent } from '@/components/ui/Card';
import { Plus, EyeOff, Eye, Globe, Edit2, Trash2, CheckCircle2, X } from 'lucide-react';
import { adminApi, TesterItem } from '@/lib/admin-api';
import { useIsMobile } from '@/hooks/use-mobile';
import { MobileCard, MobileCardList } from '@/components/ui/MobileComponents';

export default function TestersPage() {
  const [testers, setTesters] = useState<TesterItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTester, setEditingTester] = useState<TesterItem | null>(null);
  const [form, setForm] = useState({ name: '', role: '', version: '', badgeTitle: '', message: '' });
  const isMobile = useIsMobile();

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await adminApi.testers();
      setTesters(data.testers ?? []);
    } catch (err: any) {
      setError(err.message ?? 'Nao foi possivel carregar testers.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const mapped = testers.map(mapTester);
  const totalTesters = testers.length;
  const actives = mapped.filter((t) => t.active).length;
  const published = mapped.filter((t) => t.published).length;
  const pendents = mapped.filter((t) => !t.consent).length;

  function openModal(tester?: TesterItem) {
    setEditingTester(tester ?? null);
    const t = tester ? mapTester(tester) : null;
    setForm({
      name: t?.name ?? '',
      role: t?.role ?? '',
      version: t?.version ?? '',
      badgeTitle: t?.badgeTitle ?? '',
      message: t?.message ?? '',
    });
    setIsModalOpen(true);
  }

  async function save() {
    const payload = {
      display_name: form.name,
      nickname: form.role || null,
      participation_version: form.version || null,
      badge_key: form.badgeTitle || null,
      message: form.message || null,
      display_order: editingTester?.display_order ?? 0,
      is_active: editingTester ? Boolean(editingTester.is_active) : false,
      is_featured: Boolean(editingTester?.is_featured),
      consent_confirmed: editingTester ? Boolean(editingTester.consent_confirmed) : false,
      avatar_key: editingTester?.avatar_key ?? null,
      participation_period: editingTester?.participation_period ?? null,
    };
    if (editingTester) await adminApi.updateTester(String(editingTester.id), payload);
    else await adminApi.createTester(payload);
    setIsModalOpen(false);
    await load();
  }

  async function remove(id: string) {
    await adminApi.deleteTester(id);
    await load();
  }

  async function togglePublish(tester: TesterItem) {
    const current = mapTester(tester);
    await adminApi.updateTester(String(tester.id), {
      ...toApiTester(tester),
      is_active: !current.published,
      consent_confirmed: !current.published ? true : Boolean(tester.consent_confirmed),
    });
    await load();
  }

  async function toggleHidden(tester: TesterItem) {
    const current = mapTester(tester);
    await adminApi.updateTester(String(tester.id), {
      ...toApiTester(tester),
      is_active: current.hidden,
    });
    await load();
  }

  return (
    <div className="space-y-6 pb-12 relative">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">Gestao de Testers</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Agradecimentos, consentimentos e controle de publicacao.</p>
        </div>
        <button onClick={() => openModal()} className="flex items-center justify-center gap-2 bg-purple-600 hover:bg-purple-700 text-white border border-purple-500/50 px-4 py-2.5 sm:py-2 rounded-xl transition-colors font-medium w-full sm:w-auto">
          <Plus className="h-4 w-4" />
          Novo Tester
        </button>
      </div>

      {error && <Card className="p-4 border-red-500/30 text-red-400">{error}</Card>}

      <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-5">
        <Kpi title="Total de Testers" value={totalTesters} />
        <Kpi title="Ativos no App" value={actives} tone="text-emerald-400" />
        <Kpi title="Publicados" value={published} tone="text-blue-400" />
        <Kpi title="Pendentes" value={pendents} tone="text-amber-500" />
      </div>

      {!isMobile ? (
        <div className="bg-[var(--color-surface-hover)] rounded-2xl overflow-hidden border border-[var(--color-border)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">Perfil</TableHead>
                <TableHead>Versao/Selo</TableHead>
                <TableHead className="w-1/3">Mensagem</TableHead>
                <TableHead>Consentimento</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Acoes</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={6} className="text-center py-12">Carregando testers...</TableCell></TableRow>
              ) : mapped.length === 0 ? (
                <TableRow><TableCell colSpan={6} className="text-center py-12 text-[var(--color-primary-muted)]">Nenhum tester cadastrado.</TableCell></TableRow>
              ) : mapped.map((tester) => (
                <TableRow key={tester.id} className={tester.hidden ? 'opacity-40 grayscale' : ''}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 rounded-full border border-[var(--color-border-hover)] bg-[var(--color-surface)] flex items-center justify-center font-bold text-white text-[13px]">
                        {tester.initials}
                      </div>
                      <div>
                        <div className="font-bold text-[14px] text-white flex items-center gap-1.5">{tester.name}{tester.published && <span className="text-purple-400 text-[10px]">*</span>}</div>
                        <div className="text-[12px] text-[var(--color-primary-muted)] mt-0.5">{tester.role || '-'}</div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-1.5 items-start">
                      <span className="rounded-full bg-blue-500/20 text-blue-400 px-2 py-0.5 text-[11px] font-bold border border-blue-500/20">{tester.version || '-'}</span>
                      <span className="text-[12px] font-medium text-white">{tester.badgeTitle || '-'}</span>
                    </div>
                  </TableCell>
                  <TableCell><span className="text-[14px] font-medium text-white">{tester.message || '-'}</span></TableCell>
                  <TableCell>{tester.consent ? <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2.5 py-1 text-[12px] font-bold"><CheckCircle2 className="h-3.5 w-3.5" />OK</span> : <span className="text-[12px] font-medium text-amber-500">Pendente</span>}</TableCell>
                  <TableCell><span className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold border ${tester.active ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/20' : 'bg-amber-500/20 text-amber-400 border-amber-500/20'}`}>{tester.active ? 'Ativo' : 'Pendente'}</span></TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-5">
                      <button onClick={() => toggleHidden(tester.raw)} className="text-[var(--color-primary-muted)] hover:text-white"><EyeOff className="h-[18px] w-[18px] stroke-[1.5]" /></button>
                      <button onClick={() => togglePublish(tester.raw)} className={`${tester.published ? 'text-blue-500' : 'text-[var(--color-primary-muted)]'} hover:text-blue-400`}><Globe className="h-[18px] w-[18px] stroke-[1.5]" /></button>
                      <button onClick={() => openModal(tester.raw)} className="text-purple-500 hover:text-purple-400"><Edit2 className="h-[18px] w-[18px] stroke-[1.5]" /></button>
                      <button onClick={() => remove(tester.id)} className="text-red-500 hover:text-red-400"><Trash2 className="h-[18px] w-[18px] stroke-[1.5]" /></button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : (
        <MobileCardList>
          {loading ? (
            <div className="text-center py-12 text-[var(--color-primary-muted)]">Carregando testers...</div>
          ) : mapped.length === 0 ? (
            <div className="text-center py-12 text-[var(--color-primary-muted)]">Nenhum tester cadastrado.</div>
          ) : mapped.map((tester) => (
            <MobileCard
              key={tester.id}
              className={tester.hidden ? 'opacity-40 grayscale' : ''}
              title={
                <div className="flex items-center gap-2">
                  <div className="h-8 w-8 rounded-full border border-[var(--color-border-hover)] bg-[var(--color-surface)] flex items-center justify-center font-bold text-white text-[11px] shrink-0">
                    {tester.initials}
                  </div>
                  <span>{tester.name}{tester.published && <span className="text-purple-400 text-[10px] ml-1">*</span>}</span>
                </div>
              }
              subtitle={tester.role || undefined}
              badge={
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold border ${tester.active ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/20' : 'bg-amber-500/20 text-amber-400 border-amber-500/20'}`}>
                  {tester.active ? 'Ativo' : 'Pendente'}
                </span>
              }
              fields={[
                { label: 'Versão', value: tester.version || '-' },
                { label: 'Consentimento', value: tester.consent ? <span className="text-emerald-400 font-bold">OK</span> : <span className="text-amber-500">Pendente</span> },
              ]}
              actions={
                <div className="flex items-center gap-4 w-full">
                  <button onClick={() => toggleHidden(tester.raw)} className="text-[var(--color-primary-muted)] p-1"><EyeOff className="h-5 w-5" /></button>
                  <button onClick={() => togglePublish(tester.raw)} className={`p-1 ${tester.published ? 'text-blue-500' : 'text-[var(--color-primary-muted)]'}`}><Globe className="h-5 w-5" /></button>
                  <button onClick={() => openModal(tester.raw)} className="text-purple-500 p-1"><Edit2 className="h-5 w-5" /></button>
                  <button onClick={() => remove(tester.id)} className="text-red-500 p-1 ml-auto"><Trash2 className="h-5 w-5" /></button>
                </div>
              }
            />
          ))}
        </MobileCardList>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4 bg-black/50 backdrop-blur-sm">
          <div className={`bg-[var(--color-surface)] border border-[var(--color-border)] shadow-2xl w-full overflow-hidden flex flex-col ${isMobile ? 'rounded-t-2xl max-h-[90vh]' : 'rounded-2xl max-w-md'}`}>
            <div className="flex items-center justify-between p-5 border-b border-[var(--color-border)] shrink-0">
              <h3 className="text-lg font-bold text-white">{editingTester ? 'Editar Tester' : 'Novo Tester'}</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-[var(--color-primary-muted)] hover:text-white p-1"><X className="w-5 h-5" /></button>
            </div>
            <div className="p-5 space-y-4 overflow-y-auto flex-1">
              <Input label="Nome" value={form.name} onChange={(name) => setForm((f) => ({ ...f, name }))} />
              <Input label="Cargo / Papel" value={form.role} onChange={(role) => setForm((f) => ({ ...f, role }))} />
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input label="Versao" value={form.version} onChange={(version) => setForm((f) => ({ ...f, version }))} />
                <Input label="Selo / Titulo" value={form.badgeTitle} onChange={(badgeTitle) => setForm((f) => ({ ...f, badgeTitle }))} />
              </div>
              <label className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Mensagem</label>
              <textarea value={form.message} onChange={(e) => setForm((f) => ({ ...f, message: e.target.value }))} rows={3} className="w-full bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[14px] focus:outline-none focus:border-purple-500/50 resize-none" />
              <div className="flex flex-col sm:flex-row items-center justify-end gap-3 pt-4">
                <button onClick={() => setIsModalOpen(false)} className="px-4 py-2.5 text-[14px] font-medium text-white hover:bg-[var(--color-border)] rounded-xl w-full sm:w-auto order-2 sm:order-1">Cancelar</button>
                <button onClick={save} disabled={!form.name} className="px-4 py-2.5 text-[14px] font-medium text-white bg-purple-600 hover:bg-purple-700 disabled:opacity-50 rounded-xl w-full sm:w-auto order-1 sm:order-2">Salvar</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function mapTester(raw: TesterItem) {
  const name = raw.display_name ?? raw.displayName ?? raw.name ?? '';
  return {
    raw,
    id: String(raw.id),
    name,
    role: raw.nickname ?? raw.role ?? '',
    initials: name.slice(0, 2).toUpperCase() || 'QF',
    version: raw.participation_version ?? raw.participationVersion ?? '',
    badgeTitle: raw.badge_key ?? raw.badgeKey ?? '',
    message: raw.message ?? '',
    consent: Boolean(raw.consent_confirmed ?? raw.consentConfirmed),
    active: Boolean(raw.is_active ?? raw.isActive),
    published: Boolean(raw.published_at ?? raw.publishedAt),
    hidden: !Boolean(raw.is_active ?? raw.isActive),
  };
}

function toApiTester(raw: TesterItem) {
  return {
    display_name: raw.display_name,
    nickname: raw.nickname ?? null,
    avatar_key: raw.avatar_key ?? null,
    badge_key: raw.badge_key ?? null,
    message: raw.message ?? null,
    participation_version: raw.participation_version ?? null,
    participation_period: raw.participation_period ?? null,
    display_order: raw.display_order ?? 0,
    is_active: Boolean(raw.is_active),
    is_featured: Boolean(raw.is_featured),
    consent_confirmed: Boolean(raw.consent_confirmed),
  };
}

function Kpi({ title, value, tone = 'text-white' }: { title: string; value: number; tone?: string }) {
  return <Card><CardContent className="p-4 sm:p-5"><p className="text-[10px] sm:text-[11px] font-bold text-[var(--color-primary-muted)] tracking-wider uppercase mb-1">{title}</p><p className={`text-2xl sm:text-4xl font-bold ${tone}`}>{value}</p></CardContent></Card>;
}

function Input({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return <div><label className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">{label}</label><input value={value} onChange={(e) => onChange(e.target.value)} className="w-full bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[14px] focus:outline-none focus:border-purple-500/50" /></div>;
}

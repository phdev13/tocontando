'use client';

import { useEffect, useState, useRef } from 'react';
import AppInfoParser from 'app-info-parser';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { Badge } from '@/components/ui/Badge';
import { UploadCloud, CheckCircle2, AlertCircle, Play, Pause, X, FileText, Info } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';
import { adminApi, formatDateTime, OtaRelease } from '@/lib/admin-api';
import { useIsMobile } from '@/hooks/use-mobile';
import { MobileCard, MobileCardList } from '@/components/ui/MobileComponents';

export default function OTAPage() {
  const [releases, setReleases] = useState<OtaRelease[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedOTA, setSelectedOTA] = useState<OtaRelease | null>(null);
  const [form, setForm] = useState({ versionName: '', versionCode: '', channel: 'stable', mandatory: false, title: '', summary: '' });
  const [apkFile, setApkFile] = useState<File | null>(null);
  const [isParsing, setIsParsing] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const isMobile = useIsMobile();

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await adminApi.releases();
      setReleases(data.versions ?? data.releases ?? []);
    } catch (err: any) {
      setError(err.message ?? 'Nao foi possivel carregar releases OTA.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function toggleStatus(release: OtaRelease) {
    const status = release.status === 'active' ? 'paused' : 'active';
    await adminApi.updateRelease(Number(release.version_code ?? release.versionCode), { status });
    await load();
  }

  async function handleFile(file: File) {
    if (!file.name.endsWith('.apk')) {
      alert('Por favor, selecione um arquivo .apk');
      return;
    }
    setApkFile(file);
    setIsParsing(true);
    try {
      const parser = new AppInfoParser(file);
      const result = await parser.parse();
      setForm((f) => ({
        ...f,
        versionName: result.versionName || '',
        versionCode: String(result.versionCode || ''),
      }));
    } catch (e: any) {
      console.error('Failed to parse APK', e);
      alert('Nao foi possivel extrair os dados do APK: ' + e.message);
    } finally {
      setIsParsing(false);
    }
  }

  async function createRelease() {
    setLoading(true);
    try {
      if (apkFile) {
        const formData = new FormData();
        formData.append('apk', apkFile);
        formData.append('versionCode', form.versionCode);
        formData.append('versionName', form.versionName);
        formData.append('title', form.title || `Versao ${form.versionName}`);
        formData.append('notes', form.summary);
        await adminApi.createGithubRelease(formData);
      } else {
        await adminApi.createRelease({
          versionCode: Number(form.versionCode),
          versionName: form.versionName,
          releaseChannel: form.channel,
          mandatory: form.mandatory,
          title: form.title || `Versao ${form.versionName}`,
          summary: form.summary,
          changelog: form.summary ? [form.summary] : [],
          rolloutPercentage: 0,
        });
      }
      setIsModalOpen(false);
      setApkFile(null);
      setForm({ versionName: '', versionCode: '', channel: 'stable', mandatory: false, title: '', summary: '' });
      await load();
    } catch (e: any) {
      alert(e.message || 'Erro ao enviar a atualizacao');
    } finally {
      setLoading(false);
    }
  }

  const activeCount = releases.filter((r) => r.status === 'active').length;
  const failed = releases.reduce((sum, r) => sum + Number(r.failures_count ?? r.failuresCount ?? 0), 0);
  const completed = releases.reduce((sum, r) => sum + Number(r.downloads_completed ?? r.downloadsCompleted ?? 0), 0);
  const successRate = completed + failed === 0 ? null : Math.round((completed / (completed + failed)) * 1000) / 10;

  return (
    <div className="space-y-6 pb-12 relative">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">OTA &amp; Versoes</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Releases, rollout e telemetria de atualizacao.</p>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="flex items-center justify-center gap-2 bg-purple-600 hover:bg-purple-700 text-white border border-purple-500/50 px-4 py-2.5 sm:py-2 rounded-xl transition-colors text-sm font-medium w-full sm:w-auto">
          <UploadCloud className="h-4 w-4" />
          Nova Atualizacao
        </button>
      </div>

      {error && <Card className="p-4 border-red-500/30 text-red-400">{error}</Card>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <Kpi icon={CheckCircle2} title="Releases ativas" value={activeCount} tone="text-emerald-400" />
        <Kpi icon={AlertCircle} title="Falhas registradas" value={failed} tone="text-amber-400" />
        <Kpi icon={CheckCircle2} title="Taxa de sucesso" value={successRate == null ? '-' : `${successRate}%`} tone="text-white" />
      </div>

      {/* Desktop Table */}
      {!isMobile ? (
        <div className="bg-[var(--color-surface-hover)] rounded-2xl overflow-hidden border border-[var(--color-border)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Versao</TableHead>
                <TableHead>Canal</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Publicacao</TableHead>
                <TableHead>Adocao</TableHead>
                <TableHead>Falhas</TableHead>
                <TableHead className="text-right">Acoes</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={7} className="text-center py-12">Carregando releases...</TableCell></TableRow>
              ) : releases.length === 0 ? (
                <TableRow><TableCell colSpan={7} className="text-center py-12 text-[var(--color-primary-muted)]">Nenhuma release cadastrada.</TableCell></TableRow>
              ) : releases.map((ota) => {
                const versionCode = ota.version_code ?? ota.versionCode;
                const versionName = ota.version_name ?? ota.versionName;
                const adoption = Number(ota.adoption_count ?? ota.adoptionCount ?? 0);
                const installs = Number(ota.installations_count ?? ota.installationsCount ?? 0);
                const adoptionPercent = installs > 0 ? Math.round((adoption / installs) * 100) : 0;
                return (
                  <TableRow key={`${versionCode}-${versionName}`}>
                    <TableCell><div className="font-mono font-bold text-[14px] text-white">v{versionName}</div><div className="text-[12px] text-[var(--color-primary-muted)] mt-0.5 font-medium">Build {versionCode}{Boolean(ota.mandatory) && <span className="text-amber-400 font-bold ml-1">Obrigatoria</span>}</div></TableCell>
                    <TableCell><Badge variant={ota.release_channel === 'stable' || ota.releaseChannel === 'stable' ? 'default' : 'secondary'} className="uppercase text-[10px] font-bold px-2 py-0.5">{ota.release_channel ?? ota.releaseChannel}</Badge></TableCell>
                    <TableCell><StatusBadge status={ota.status} /></TableCell>
                    <TableCell className="text-[13px] text-[var(--color-primary-muted)] font-medium">{formatDateTime(ota.published_at ?? ota.publishedAt)}</TableCell>
                    <TableCell><div className="flex items-center gap-2"><div className="flex-1 h-1.5 w-16 bg-[var(--color-border)] rounded-full overflow-hidden"><div className="h-full bg-purple-500 rounded-full" style={{ width: `${adoptionPercent}%` }} /></div><span className="text-[12px] font-bold text-white w-10">{adoptionPercent}%</span></div></TableCell>
                    <TableCell><span className="text-[13px] font-bold text-amber-400">{Number(ota.failures_count ?? ota.failuresCount ?? 0)}</span></TableCell>
                    <TableCell className="text-right"><div className="flex justify-end gap-5 items-center">{ota.status === 'active' ? <button onClick={() => toggleStatus(ota)} className="text-amber-500 hover:text-amber-400"><Pause className="h-[18px] w-[18px]" /></button> : <button onClick={() => toggleStatus(ota)} className="text-emerald-500 hover:text-emerald-400"><Play className="h-[18px] w-[18px]" /></button>}<button onClick={() => setSelectedOTA(ota)} className="text-blue-500 hover:text-blue-400"><FileText className="h-[18px] w-[18px]" /></button></div></TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      ) : (
        /* Mobile Card List */
        <MobileCardList>
          {loading ? (
            <div className="text-center py-12 text-[var(--color-primary-muted)]">Carregando releases...</div>
          ) : releases.length === 0 ? (
            <div className="text-center py-12 text-[var(--color-primary-muted)]">Nenhuma release cadastrada.</div>
          ) : releases.map((ota) => {
            const versionCode = ota.version_code ?? ota.versionCode;
            const versionName = ota.version_name ?? ota.versionName;
            const adoption = Number(ota.adoption_count ?? ota.adoptionCount ?? 0);
            const installs = Number(ota.installations_count ?? ota.installationsCount ?? 0);
            const adoptionPercent = installs > 0 ? Math.round((adoption / installs) * 100) : 0;
            const failures = Number(ota.failures_count ?? ota.failuresCount ?? 0);
            return (
              <MobileCard
                key={`${versionCode}-${versionName}`}
                title={<span className="font-mono">v{versionName}</span>}
                subtitle={<>Build {versionCode}{Boolean(ota.mandatory) && <span className="text-amber-400 font-bold ml-1">Obrigatória</span>}</>}
                badge={<StatusBadge status={ota.status} />}
                fields={[
                  { label: 'Canal', value: <Badge variant={ota.release_channel === 'stable' || ota.releaseChannel === 'stable' ? 'default' : 'secondary'} className="uppercase text-[10px] font-bold px-2 py-0.5">{ota.release_channel ?? ota.releaseChannel}</Badge> },
                  { label: 'Publicação', value: formatDateTime(ota.published_at ?? ota.publishedAt) },
                  { label: 'Adoção', value: <div className="flex items-center gap-2"><div className="flex-1 h-1.5 w-12 bg-[var(--color-border)] rounded-full overflow-hidden"><div className="h-full bg-purple-500 rounded-full" style={{ width: `${adoptionPercent}%` }} /></div><span className="text-[12px] font-bold text-white">{adoptionPercent}%</span></div> },
                  { label: 'Falhas', value: <span className="font-bold text-amber-400">{failures}</span> },
                ]}
                actions={
                  <div className="flex items-center gap-4 w-full">
                    {ota.status === 'active' ? (
                      <button onClick={() => toggleStatus(ota)} className="flex items-center gap-1.5 text-amber-500 text-[13px] font-medium"><Pause className="h-4 w-4" /> Pausar</button>
                    ) : (
                      <button onClick={() => toggleStatus(ota)} className="flex items-center gap-1.5 text-emerald-500 text-[13px] font-medium"><Play className="h-4 w-4" /> Ativar</button>
                    )}
                    <button onClick={() => setSelectedOTA(ota)} className="flex items-center gap-1.5 text-blue-500 text-[13px] font-medium ml-auto"><FileText className="h-4 w-4" /> Detalhes</button>
                  </div>
                }
              />
            );
          })}
        </MobileCardList>
      )}

      {selectedOTA && <Details release={selectedOTA} onClose={() => setSelectedOTA(null)} isMobile={isMobile} />}
      {isModalOpen && (
        <Modal title="Nova Atualizacao" onClose={() => setIsModalOpen(false)} isMobile={isMobile}>
          <div 
            className={`border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer transition-colors ${apkFile ? 'border-purple-500/50 bg-purple-500/10' : 'border-[var(--color-border)] hover:border-purple-500/50 hover:bg-white/5'}`}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => { e.preventDefault(); if (e.dataTransfer.files?.[0]) handleFile(e.dataTransfer.files[0]); }}
            onClick={() => fileInputRef.current?.click()}
          >
            <input type="file" ref={fileInputRef} className="hidden" accept=".apk" onChange={(e) => { if (e.target.files?.[0]) handleFile(e.target.files[0]); }} />
            <UploadCloud className={`h-8 w-8 mx-auto mb-3 ${apkFile ? 'text-purple-400' : 'text-[var(--color-primary-muted)]'}`} />
            {isParsing ? (
              <p className="text-[14px] font-medium text-white">Analisando APK...</p>
            ) : apkFile ? (
              <p className="text-[14px] font-medium text-purple-400 break-all">{apkFile.name}</p>
            ) : (
              <div>
                <p className="text-[14px] font-medium text-white">Arraste o APK aqui</p>
                <p className="text-[12px] text-[var(--color-primary-muted)] mt-1">ou clique para selecionar</p>
              </div>
            )}
          </div>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-2">
            <Input label="Versao" value={form.versionName} onChange={(versionName) => setForm((f) => ({ ...f, versionName }))} />
            <Input label="Build Code" value={form.versionCode} onChange={(versionCode) => setForm((f) => ({ ...f, versionCode }))} type="number" />
          </div>
          <Input label="Titulo" value={form.title} onChange={(title) => setForm((f) => ({ ...f, title }))} />
          <Input label="Resumo / Changelog" value={form.summary} onChange={(summary) => setForm((f) => ({ ...f, summary }))} />
          <label className="flex items-center gap-3 cursor-pointer"><input type="checkbox" checked={form.mandatory} onChange={(e) => setForm((f) => ({ ...f, mandatory: e.target.checked }))} /><span className="text-[13px] font-medium text-white">Atualizacao obrigatoria</span></label>
          <div className="flex flex-col sm:flex-row justify-end gap-3 pt-4"><button onClick={() => setIsModalOpen(false)} className="px-4 py-2.5 text-white hover:bg-[var(--color-border)] rounded-xl order-2 sm:order-1">Cancelar</button><button onClick={createRelease} disabled={!form.versionName || !form.versionCode || isParsing || loading} className="px-4 py-2.5 text-white bg-purple-600 hover:bg-purple-700 disabled:opacity-50 rounded-xl order-1 sm:order-2">{loading ? 'Enviando...' : 'Criar rascunho'}</button></div>
        </Modal>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  if (status === 'active') return <Badge variant="success">Em distribuicao</Badge>;
  if (status === 'paused') return <Badge variant="warning">Pausada</Badge>;
  return <Badge variant="secondary">{status}</Badge>;
}

function Kpi({ icon: Icon, title, value, tone }: any) {
  return <Card><CardContent className="p-5 flex items-center gap-4"><div className={`rounded-xl bg-white/5 p-3 ${tone}`}><Icon className="h-6 w-6" /></div><div><p className="text-[11px] font-bold text-[var(--color-primary-muted)] tracking-wider uppercase mb-1">{title}</p><p className={`text-2xl font-bold ${tone}`}>{value}</p></div></CardContent></Card>;
}

function Details({ release, onClose, isMobile }: { release: OtaRelease; onClose: () => void; isMobile: boolean }) {
  return <Modal title="Detalhes da Versao" onClose={onClose} isMobile={isMobile}><Info className="w-5 h-5 text-purple-400" /><p className="text-white font-bold">v{release.version_name ?? release.versionName} build {release.version_code ?? release.versionCode}</p><p className="text-[13px] text-[var(--color-primary-muted)]">{release.summary ?? 'Sem resumo.'}</p><p className="text-[13px] text-[var(--color-primary-muted)]">Rollout: {release.rollout_percentage ?? release.rolloutPercentage ?? 0}%</p><div className="flex justify-end"><button onClick={onClose} className="px-4 py-2.5 text-white bg-purple-600 rounded-xl w-full sm:w-auto">Fechar</button></div></Modal>;
}

function Modal({ title, onClose, children, isMobile }: { title: string; onClose: () => void; children: React.ReactNode; isMobile?: boolean }) {
  return <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4 bg-black/50 backdrop-blur-sm"><div className={`bg-[var(--color-surface)] border border-[var(--color-border)] shadow-2xl w-full overflow-hidden ${isMobile ? 'rounded-t-2xl max-h-[90vh] flex flex-col' : 'rounded-2xl max-w-sm'}`}><div className="flex items-center justify-between p-5 border-b border-[var(--color-border)] shrink-0"><h3 className="text-lg font-bold text-white">{title}</h3><button onClick={onClose} className="text-[var(--color-primary-muted)] hover:text-white p-1"><X className="w-5 h-5" /></button></div><div className="p-5 space-y-4 overflow-y-auto flex-1">{children}</div></div></div>;
}

function Input({ label, value, onChange, type = 'text' }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return <div><label className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">{label}</label><input type={type} value={value} onChange={(e) => onChange(e.target.value)} className="w-full bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[14px] focus:outline-none focus:border-purple-500/50" /></div>;
}

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { versions } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton } from '../components/ui/Skeleton';
import { UploadCloud, GitBranch } from 'lucide-react';
import { formatNumber } from '../lib/utils';

const STATUS_MAP: Record<string, { label: string; variant: 'default' | 'success' | 'warning' | 'danger' | 'info' }> = {
  draft: { label: 'Rascunho', variant: 'default' },
  active: { label: 'Ativa', variant: 'success' },
  paused: { label: 'Pausada', variant: 'warning' },
  retired: { label: 'Encerrada', variant: 'danger' },
};

export default function Versions() {
  const qc = useQueryClient();
  const [filter, setFilter] = useState<{ channel?: string; status?: string }>({});
  
  // Upload modal state
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadData, setUploadData] = useState({
    versionCode: '',
    versionName: '',
    title: '',
    notes: '',
  });
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['versions', filter],
    queryFn: () => versions.list(filter),
  });

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!file) {
      alert('Selecione um arquivo APK');
      return;
    }
    setIsUploading(true);
    try {
      const fd = new FormData();
      fd.append('apk', file);
      fd.append('versionCode', uploadData.versionCode);
      fd.append('versionName', uploadData.versionName);
      fd.append('title', uploadData.title);
      fd.append('notes', uploadData.notes);

      await versions.uploadRelease(fd);
      alert('Versão enviada com sucesso para o GitHub!');
      setShowUploadModal(false);
      setFile(null);
      setUploadData({ versionCode: '', versionName: '', title: '', notes: '' });
      qc.invalidateQueries({ queryKey: ['versions'] });
    } catch (err: any) {
      alert('Erro ao enviar: ' + (err.message || 'Falha desconhecida'));
    } finally {
      setIsUploading(false);
    }
  }

  function openUploadModal() {
    const versionsList = data?.versions ?? [];
    let nextCode = 1;
    let nextName = '1.0';
    
    if (versionsList.length > 0) {
      const latest = versionsList[0];
      nextCode = (latest.version_code || 0) + 1;
      
      let currentName = latest.version_name || '1.0';
      let prefix = '';
      if (currentName.startsWith('v') || currentName.startsWith('V')) {
        prefix = currentName.charAt(0);
        currentName = currentName.substring(1);
      }
      
      const parts = currentName.split('.');
      if (parts.length >= 2) {
        parts[1] = String((parseInt(parts[1]) || 0) + 1);
        if (parts.length > 2) parts[2] = '0'; // reset patch if exists
        nextName = prefix + parts.join('.');
      } else {
        nextName = prefix + currentName + '.1';
      }
    }

    setUploadData({
      versionCode: String(nextCode),
      versionName: nextName,
      title: '',
      notes: '',
    });
    setShowUploadModal(true);
  }

  const handleFilterChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFilter(prev => ({ ...prev, [name]: value || undefined }));
  };

  return (
    <div className="page-container flex-col gap-6">
      <div className="page-header">
        <div>
          <h1 className="page-title">Versões e Releases</h1>
          <p className="page-subtitle">Gerenciamento de atualizações, APKs e rollout</p>
        </div>
        <div className="flex items-center gap-4 flex-wrap">
          <select
            name="channel"
            className="p-2 bg-surface-elevated border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary transition-colors"
            style={{ backgroundColor: 'var(--bg-surface-elevated)', color: 'var(--text-primary)', borderColor: 'var(--border-default)' }}
            value={filter.channel ?? ''}
            onChange={handleFilterChange}
          >
            <option value="">Todos os canais</option>
            <option value="stable">Stable</option>
            <option value="beta">Beta</option>
            <option value="internal">Internal</option>
          </select>
          <select
            name="status"
            className="p-2 bg-surface-elevated border border-default rounded-md text-sm text-primary-text outline-none focus:border-primary transition-colors"
            style={{ backgroundColor: 'var(--bg-surface-elevated)', color: 'var(--text-primary)', borderColor: 'var(--border-default)' }}
            value={filter.status ?? ''}
            onChange={handleFilterChange}
          >
            <option value="">Todos os status</option>
            <option value="draft">Rascunho</option>
            <option value="active">Ativa</option>
            <option value="paused">Pausada</option>
            <option value="retired">Encerrada</option>
          </select>
          <Button onClick={openUploadModal}>
            <UploadCloud size={18} />
            Nova Versão
          </Button>
        </div>
      </div>

      <div className="bg-surface rounded-xl border border-white/10 overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Versão</TableHead>
              <TableHead>Canal</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Data de Upload</TableHead>
              <TableHead className="text-right">Adoções</TableHead>
              <TableHead className="text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell><Skeleton style={{ width: '100px', height: '20px' }} /></TableCell>
                  <TableCell><Skeleton style={{ width: '60px', height: '20px' }} /></TableCell>
                  <TableCell><Skeleton style={{ width: '80px', height: '24px', borderRadius: '12px' }} /></TableCell>
                  <TableCell><Skeleton style={{ width: '100px', height: '20px' }} /></TableCell>
                  <TableCell className="text-right"><Skeleton style={{ width: '50px', height: '20px', marginLeft: 'auto' }} /></TableCell>
                  <TableCell className="text-right"><Skeleton style={{ width: '32px', height: '32px', marginLeft: 'auto' }} /></TableCell>
                </TableRow>
              ))
            ) : data?.versions?.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6}>
                  <EmptyState icon={GitBranch} title="Nenhuma versão encontrada" description="Não há versões correspondentes aos filtros selecionados." />
                </TableCell>
              </TableRow>
            ) : (
              (data?.versions ?? []).map((v: any) => {
                const statusInfo = STATUS_MAP[v.status] || { label: v.status, variant: 'default' };
                return (
                  <TableRow key={v.id}>
                    <TableCell>
                      <div className="flex flex-col">
                        <span className="font-semibold text-primary-text">{v.version_name}</span>
                        <span className="text-xs text-secondary">Code: {v.version_code}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <span style={{ textTransform: 'capitalize', fontSize: '0.875rem' }}>{v.channel || '—'}</span>
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                    </TableCell>
                    <TableCell>
                      <span className="text-sm text-secondary">
                        {v.published_at ? new Date(v.published_at).toLocaleDateString('pt-BR') : '—'}
                      </span>
                    </TableCell>
                    <TableCell className="text-right font-medium">
                      {formatNumber(v.adoption_count ?? 0)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm">Editar</Button>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </div>

      <Modal
        isOpen={showUploadModal}
        onClose={() => !isUploading && setShowUploadModal(false)}
        title="Nova Versão (Upload pro GitHub)"
        footer={
          <>
            <Button variant="outline" onClick={() => setShowUploadModal(false)} disabled={isUploading}>Cancelar</Button>
            <Button onClick={handleUpload} disabled={isUploading || !file} isLoading={isUploading}>
              Enviar para GitHub
            </Button>
          </>
        }
      >
        <div className="flex-col gap-4">
          <div className="flex-col gap-1">
            <label className="text-sm font-semibold text-secondary">Arquivo APK</label>
            <input 
              type="file" 
              accept=".apk" 
              onChange={e => setFile(e.target.files?.[0] || null)} 
              className="w-full p-2 border border-default rounded-md text-sm outline-none focus:border-primary transition-colors"
              style={{ backgroundColor: 'var(--bg-surface-elevated)', color: 'var(--text-primary)' }}
            />
          </div>
          <div className="grid grid-cols-2 gap-4 mt-4">
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Version Code</label>
              <input 
                type="number" 
                value={uploadData.versionCode}
                onChange={e => setUploadData({ ...uploadData, versionCode: e.target.value })}
                className="w-full p-2 border border-default rounded-md text-sm outline-none focus:border-primary transition-colors"
                style={{ backgroundColor: 'var(--bg-surface-elevated)', color: 'var(--text-primary)' }}
                placeholder="Ex: 15"
              />
            </div>
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Version Name</label>
              <input 
                type="text" 
                value={uploadData.versionName}
                onChange={e => setUploadData({ ...uploadData, versionName: e.target.value })}
                className="w-full p-2 border border-default rounded-md text-sm outline-none focus:border-primary transition-colors"
                style={{ backgroundColor: 'var(--bg-surface-elevated)', color: 'var(--text-primary)' }}
                placeholder="Ex: 1.1.0"
              />
            </div>
          </div>
          <div className="flex-col gap-1 mt-4">
            <label className="text-sm font-semibold text-secondary">Título da Atualização</label>
            <input 
              type="text" 
              value={uploadData.title}
              onChange={e => setUploadData({ ...uploadData, title: e.target.value })}
              className="w-full p-2 bg-surface-elevated text-primary-text border border-white/10 rounded-lg text-sm outline-none focus:border-primary transition-colors"
              placeholder="Novidades fantásticas..."
            />
          </div>
          <div className="flex-col gap-1 mt-4">
            <label className="text-sm font-semibold text-secondary">Notas / Changelog</label>
            <textarea 
              rows={4}
              value={uploadData.notes}
              onChange={e => setUploadData({ ...uploadData, notes: e.target.value })}
              className="w-full p-2 bg-surface-elevated text-primary-text border border-white/10 rounded-lg text-sm outline-none focus:border-primary transition-colors"
              placeholder="Descreva o que mudou nesta versão..."
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}

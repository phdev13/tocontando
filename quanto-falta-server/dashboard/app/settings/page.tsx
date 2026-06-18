'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card';
import { Database, ShieldAlert, Settings2, Save, Loader2, AlertCircle } from 'lucide-react';
import { adminApi, Setting } from '@/lib/admin-api';

export default function SettingsPage() {
  const [settings, setSettings] = useState<Setting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // State to hold unsaved changes
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState<string | null>(null);

  const SETTINGS_METADATA: Record<string, { label: string; desc: string; type: 'text' | 'number' | 'boolean' }> = {
    ota_check_interval_hours: { label: 'Intervalo de Check OTA (horas)', desc: 'Tempo em horas entre verificações automáticas de atualização em segundo plano.', type: 'number' },
    ota_modal_cooldown_hours: { label: 'Cooldown do Modal OTA (horas)', desc: 'Tempo mínimo em horas para re-exibir o alerta de atualização caso o usuário ignore.', type: 'number' },
    feedback_contextual_enabled: { label: 'Feedback Contextual Ativo', desc: 'Habilita (true) ou desabilita (false) o trigger automático de feedback dentro do app.', type: 'boolean' },
    feedback_contextual_cooldown_days: { label: 'Cooldown do Feedback (dias)', desc: 'Dias de carência antes de pedir feedback contextual para um usuário novamente.', type: 'number' },
    maintenance_mode: { label: 'Modo Manutenção', desc: 'Define se o app deve exibir tela de manutenção. Use true ou false.', type: 'boolean' },
    maintenance_message: { label: 'Mensagem de Manutenção', desc: 'Texto exibido aos usuários quando o modo manutenção está ativo.', type: 'text' },
    telemetry_max_queue_size: { label: 'Fila Máxima de Telemetria', desc: 'Quantidade máxima de eventos salvos offline antes de forçar o envio.', type: 'number' },
    min_supported_version_code: { label: 'Versão Mínima Suportada', desc: 'Version code mínimo para o app funcionar. Versões menores serão bloqueadas.', type: 'number' },
    active_icon_mode: { label: 'Ícone Dinâmico Ativo', desc: 'Nome do tema do ícone ativo (padrão, dark, premium, etc).', type: 'text' },
    premium_event_cards_enabled: { label: 'Cards de Evento Premium', desc: 'Ativa ou desativa a nova repaginação visual dos cards de evento com suporte a foto de fundo.', type: 'boolean' },
  };

  async function load() {
    setLoading(true);
    try {
      const data = await adminApi.settings();
      setSettings(data.settings ?? []);
    } catch (err: any) {
      setError(err.message ?? 'Erro ao carregar configurações remotas.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleSave(key: string) {
    const value = drafts[key];
    if (value === undefined) return;
    
    setSaving(key);
    try {
      await adminApi.updateSetting(key, value);
      // Atualiza o local state para refletir a nova config salva
      setSettings((prev) => {
        const exists = prev.find(s => s.key === key);
        if (exists) {
          return prev.map(s => s.key === key ? { ...s, value } : s);
        }
        return [...prev, { key, value }];
      });
      
      // Remove o draft salvo
      setDrafts((prev) => {
        const next = { ...prev };
        delete next[key];
        return next;
      });
    } catch (err: any) {
      alert(`Erro ao salvar: ${err.message}`);
    } finally {
      setSaving(null);
    }
  }

  // Helper para obter o valor atual ou draft
  const getValue = (key: string) => {
    if (drafts[key] !== undefined) return drafts[key];
    const found = settings.find(s => s.key === key);
    return found ? found.value : '';
  };

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">Configurações Base</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Preferências globais e Remote Config do aplicativo.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <Card className="shadow-sm">
          <CardHeader className="p-4 sm:p-5 pb-0">
            <CardTitle className="text-lg font-bold text-white flex items-center gap-2">
              <Database className="w-5 h-5 text-purple-400" /> Integração de API
            </CardTitle>
            <CardDescription className="text-[13px] text-[var(--color-primary-muted)] mt-1">Endpoints base do Cloudflare Workers</CardDescription>
          </CardHeader>
          <CardContent className="p-4 sm:p-5 space-y-4">
            <div>
              <label className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Base URL (Produção)</label>
              <input 
                type="text" 
                disabled 
                value="https://api.tocontando.com.br/v1" 
                className="w-full bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[13px] opacity-60 cursor-not-allowed"
              />
            </div>
            <div>
              <label className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Status da Camada D1</label>
              <div className="flex items-center gap-2 mt-2 text-[13px] text-emerald-400 font-bold bg-emerald-500/10 border border-emerald-500/20 px-3 py-2 rounded-xl w-fit">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                </span>
                Conectado e Saudável
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-sm">
          <CardHeader className="p-4 sm:p-5 pb-0">
            <CardTitle className="text-lg font-bold text-white flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-amber-400" /> Autenticação Root
            </CardTitle>
            <CardDescription className="text-[13px] text-[var(--color-primary-muted)] mt-1">Gerido estritamente no nível Edge (Access)</CardDescription>
          </CardHeader>
          <CardContent className="p-4 sm:p-5 space-y-4">
            <div className="bg-amber-500/5 border border-amber-500/20 rounded-xl p-4">
              <p className="text-[13px] text-amber-500 font-bold">Aviso de Segurança Zero-Trust</p>
              <p className="text-[13px] text-[var(--color-primary-muted)] mt-2 leading-relaxed">
                Este frontend <span className="text-white font-medium">não lida com tokens locais senhas de administrador</span>. Toda a proteção de rotas é imposta pelas regras do Cloudflare Access sobre a restrição de email "philippeboechat1@gmail.com".
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card className="shadow-sm mt-8 border-purple-500/20">
        <CardHeader className="p-4 sm:p-5 border-b border-[var(--color-border)] bg-[var(--color-surface-hover)]">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3">
            <div className="bg-purple-500/20 p-2 rounded-lg w-fit">
              <Settings2 className="w-5 h-5 text-purple-400" />
            </div>
            <div>
              <CardTitle className="text-lg font-bold text-white">Remote Config</CardTitle>
              <CardDescription className="text-[13px] text-[var(--color-primary-muted)] mt-0.5">Parâmetros dinâmicos que alteram o comportamento do app em tempo real, sem necessidade de atualização nas lojas.</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0 divide-y divide-[var(--color-border)]">
          {loading ? (
            <div className="p-12 flex flex-col items-center justify-center text-[var(--color-primary-muted)]">
              <Loader2 className="w-6 h-6 animate-spin mb-4" />
              <p className="text-sm">Carregando configurações do banco de dados...</p>
            </div>
          ) : error ? (
            <div className="p-4 sm:p-8">
              <div className="bg-red-500/10 border border-red-500/20 p-4 rounded-xl flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
                <div>
                  <p className="text-sm font-bold text-red-400">Falha na sincronização</p>
                  <p className="text-[13px] text-[var(--color-primary-muted)] mt-1">{error}</p>
                </div>
              </div>
            </div>
          ) : (
            Object.entries(SETTINGS_METADATA).map(([key, meta]) => {
              const val = getValue(key);
              const hasChanges = drafts[key] !== undefined && drafts[key] !== (settings.find(s => s.key === key)?.value ?? '');
              
              return (
                <div key={key} className="p-4 sm:p-5 flex flex-col lg:flex-row lg:items-center justify-between gap-4 hover:bg-[var(--color-surface-hover)] transition-colors">
                  <div className="flex-1">
                    <p className="text-[14px] font-bold text-white">{meta.label}</p>
                    <p className="text-[13px] text-[var(--color-primary-muted)] mt-1">{meta.desc}</p>
                    <p className="text-[11px] font-mono text-purple-400/70 mt-2 bg-purple-500/10 px-2 py-0.5 rounded w-fit break-all">{key}</p>
                  </div>
                  
                  <div className="flex items-center gap-3 w-full lg:w-auto">
                    <div className="flex-1 lg:w-64">
                      {meta.type === 'boolean' ? (
                        <select
                          value={val}
                          onChange={(e) => setDrafts(prev => ({ ...prev, [key]: e.target.value }))}
                          className="w-full bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[13px] focus:outline-none focus:border-purple-500 transition-colors"
                        >
                          <option value="">(Não definido)</option>
                          <option value="true">True (Ativado)</option>
                          <option value="false">False (Desativado)</option>
                        </select>
                      ) : (
                        <input
                          type={meta.type === 'number' ? 'number' : 'text'}
                          value={val}
                          onChange={(e) => setDrafts(prev => ({ ...prev, [key]: e.target.value }))}
                          placeholder="Valor vazio"
                          className="w-full bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[13px] focus:outline-none focus:border-purple-500 transition-colors"
                        />
                      )}
                    </div>
                    
                    <button 
                      onClick={() => handleSave(key)}
                      disabled={!hasChanges || saving === key}
                      className={`shrink-0 flex items-center justify-center w-11 h-11 sm:w-10 sm:h-10 rounded-xl transition-all ${
                        hasChanges 
                          ? 'bg-purple-600 hover:bg-purple-500 text-white shadow-[0_0_15px_rgba(147,51,234,0.3)]' 
                          : 'bg-[var(--color-surface)] border border-[var(--color-border)] text-[var(--color-primary-muted)] opacity-50 cursor-not-allowed'
                      }`}
                      aria-label="Salvar"
                    >
                      {saving === key ? <Loader2 className="w-5 h-5 sm:w-4 sm:h-4 animate-spin" /> : <Save className="w-5 h-5 sm:w-4 sm:h-4" />}
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </CardContent>
      </Card>
    </div>
  );
}

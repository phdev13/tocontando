import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { settings } from '../lib/api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import { Settings as SettingsIcon, Save, X, RefreshCw, Smartphone, MessageSquare, Activity, ShieldAlert } from 'lucide-react';
import { cn } from '../lib/utils';

const SETTING_GROUPS = [
  {
    id: 'ota',
    title: 'Atualizações e OTA',
    icon: Smartphone,
    description: 'Controle de atualizações obrigatórias e verificações OTA.',
    keys: ['ota_check_interval_hours', 'ota_modal_cooldown_hours', 'min_supported_version_code']
  },
  {
    id: 'app',
    title: 'Experiência do App',
    icon: ShieldAlert,
    description: 'Modo de manutenção, ícones, formatos de contagem e estados globais.',
    keys: ['maintenance_mode', 'maintenance_message', 'active_icon_mode', 'advanced_counts_enabled', 'default_count_format']
  },
  {
    id: 'feedback',
    title: 'Feedback e Engajamento',
    icon: MessageSquare,
    description: 'Regras de solicitação de avaliação e NPS.',
    keys: ['feedback_contextual_enabled', 'feedback_contextual_cooldown_days']
  },
  {
    id: 'telemetry',
    title: 'Telemetria',
    icon: Activity,
    description: 'Controle de eventos e envio de dados.',
    keys: ['telemetry_max_queue_size']
  }
];

const SETTING_META: Record<string, { label: string; description: string; type: 'number' | 'boolean' | 'text' | 'enum'; options?: {value:string, label:string}[] }> = {
  ota_check_interval_hours: { label: 'Intervalo de Verificação OTA (h)', description: 'Frequência que o app busca atualizações silentes.', type: 'number' },
  ota_modal_cooldown_hours: { label: 'Cooldown do Modal OTA (h)', description: 'Tempo mínimo para exibir o alerta de nova versão novamente.', type: 'number' },
  min_supported_version_code: { label: 'Versão Mínima Suportada', description: 'Obriga atualização se o usuário estiver abaixo desta versão.', type: 'number' },
  
  maintenance_mode: { label: 'Modo de Manutenção', description: 'Bloqueia o app e exibe a mensagem de manutenção.', type: 'boolean' },
  maintenance_message: { label: 'Mensagem de Manutenção', description: 'Texto exibido aos usuários.', type: 'text' },
  active_icon_mode: { 
    label: 'Ícone Ativo do App', 
    description: 'Define qual ícone os usuários verão (se suportado pelo SO).', 
    type: 'enum', 
    options: [{value:'auto', label:'Automático'}, {value:'force_copa', label:'Forçar Copa'}, {value:'force_padrao', label:'Forçar Padrão'}] 
  },
  
  advanced_counts_enabled: { label: 'Formatos Avançados', description: 'Habilita opções como Anos, Úteis e Idade.', type: 'boolean' },
  default_count_format: { 
    label: 'Formato de Contagem Padrão', 
    description: 'Formato selecionado por padrão na criação de evento.', 
    type: 'enum', 
    options: [{value:'DAYS', label:'Dias (Padrão)'}, {value:'WEEKS', label:'Semanas'}, {value:'MONTHS', label:'Meses'}, {value:'FULL_TIME', label:'Tempo Completo'}] 
  },
  
  feedback_contextual_enabled: { label: 'Feedback Contextual', description: 'Solicita avaliações de forma inteligente após ações de sucesso.', type: 'boolean' },
  feedback_contextual_cooldown_days: { label: 'Cooldown do Feedback (dias)', description: 'Dias de espera antes de pedir avaliação novamente.', type: 'number' },
  
  telemetry_max_queue_size: { label: 'Fila Máx. Telemetria', description: 'Limite de eventos armazenados offline antes do envio forçado.', type: 'number' },
};

export default function Settings() {
  const qc = useQueryClient();
  const { data, isLoading, isFetching } = useQuery({ queryKey: ['settings'], queryFn: settings.list });
  
  const [localValues, setLocalValues] = useState<Record<string, string>>({});
  const [isSaving, setIsSaving] = useState<Record<string, boolean>>({});

  const settingsMap: Record<string, string> = {};
  for (const s of data?.settings ?? []) settingsMap[s.key] = s.value;

  // Initialize local state when data loads
  useEffect(() => {
    if (data?.settings) {
      const newLocal = { ...localValues };
      data.settings.forEach((s: any) => {
        if (newLocal[s.key] === undefined) {
          newLocal[s.key] = s.value;
        }
      });
      setLocalValues(newLocal);
    }
  }, [data]);

  const updateMut = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) => settings.update(key, value),
    onMutate: async ({ key }) => {
      // Optimistic update
      await qc.cancelQueries({ queryKey: ['settings'] });
      const previous = qc.getQueryData(['settings']);
      setIsSaving(prev => ({ ...prev, [key]: true }));
      return { previous, key };
    },
    onSuccess: (_, { key }) => {
      qc.invalidateQueries({ queryKey: ['settings'] });
      setIsSaving(prev => ({ ...prev, [key]: false }));
    },
    onError: (_err, _variables, context: any) => {
      qc.setQueryData(['settings'], context.previous);
      setIsSaving(prev => ({ ...prev, [context.key]: false }));
      alert('Erro ao salvar configuração.');
    }
  });

  const handleSave = (key: string) => {
    const val = localValues[key];
    if (val !== undefined && val !== settingsMap[key]) {
      updateMut.mutate({ key, value: val });
    }
  };

  const handleToggle = (key: string, checked: boolean) => {
    const val = String(checked);
    setLocalValues(prev => ({ ...prev, [key]: val }));
    updateMut.mutate({ key, value: val });
  };

  const hasChanges = (key: string) => localValues[key] !== undefined && localValues[key] !== settingsMap[key];

  if (isLoading) return (
    <div className="page-container flex-col gap-6">
       <div className="page-header"><div><Skeleton className="h-8 w-64 mb-2"/><Skeleton className="h-4 w-48"/></div></div>
       {Array.from({length: 3}).map((_, i) => <Skeleton key={i} className="h-48 w-full rounded-xl"/>)}
    </div>
  );

  return (
    <div className="page-container flex-col gap-6">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2"><SettingsIcon className="text-primary"/> Configurações Remotas</h1>
          <p className="page-subtitle">Altere o comportamento do app sem precisar publicar uma nova versão.</p>
        </div>
        {isFetching && <div className="text-sm text-secondary flex items-center gap-2"><RefreshCw size={14} className="animate-spin" /> Sincronizando...</div>}
      </div>

      <div className="grid gap-6">
        {SETTING_GROUPS.map(group => {
          const GroupIcon = group.icon;
          return (
            <Card key={group.id} className="overflow-hidden">
              <CardHeader className="bg-surface-elevated border-b border-default">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-primary/10 rounded-lg text-primary"><GroupIcon size={20} /></div>
                  <div>
                    <CardTitle className="text-lg">{group.title}</CardTitle>
                    <CardDescription>{group.description}</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="p-0 divide-y divide-default">
                {group.keys.map(key => {
                  const meta = SETTING_META[key];
                  if (!meta) return null;
                  const currentVal = localValues[key] !== undefined ? localValues[key] : (settingsMap[key] ?? '');
                  const isChanged = hasChanges(key);
                  const saving = isSaving[key];

                  return (
                    <div key={key} className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-surface-elevated/50 transition-colors">
                      <div className="flex-1 min-w-0">
                        <label className="text-sm font-semibold text-primary-text block mb-1">{meta.label}</label>
                        <p className="text-xs text-secondary">{meta.description}</p>
                      </div>
                      
                      <div className="flex items-center gap-3 md:min-w-[300px] md:justify-end">
                        {meta.type === 'boolean' ? (
                          <div className="flex items-center gap-2">
                             <span className={cn("text-xs font-medium uppercase tracking-wider", currentVal === 'true' ? "text-success" : "text-secondary")}>
                               {currentVal === 'true' ? 'Ativado' : 'Desativado'}
                             </span>
                             <button 
                               role="switch" 
                               aria-checked={currentVal === 'true'}
                               onClick={() => handleToggle(key, currentVal !== 'true')}
                               className="toggle-switch"
                               disabled={saving}
                             >
                               <span className="toggle-knob" />
                             </button>
                          </div>
                        ) : (
                          <div className="flex flex-1 items-center gap-2">
                            {meta.type === 'enum' ? (
                              <select
                                value={currentVal}
                                onChange={e => setLocalValues(prev => ({ ...prev, [key]: e.target.value }))}
                                className={cn("flex-1 p-2 bg-surface border rounded-md text-sm outline-none transition-colors", isChanged ? "border-warning bg-warning/5" : "border-default focus:border-primary")}
                                disabled={saving}
                              >
                                {meta.options?.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                              </select>
                            ) : (
                              <input
                                type={meta.type === 'number' ? 'number' : 'text'}
                                value={currentVal}
                                onChange={e => setLocalValues(prev => ({ ...prev, [key]: e.target.value }))}
                                className={cn("flex-1 p-2 bg-surface border rounded-md text-sm outline-none transition-colors", isChanged ? "border-warning bg-warning/5" : "border-default focus:border-primary")}
                                disabled={saving}
                              />
                            )}
                            
                            {isChanged && (
                              <div className="flex items-center gap-1">
                                <Button size="icon" variant="primary" onClick={() => handleSave(key)} isLoading={saving} title="Salvar">
                                  <Save size={16} />
                                </Button>
                                <Button size="icon" variant="outline" onClick={() => setLocalValues(prev => ({ ...prev, [key]: settingsMap[key] }))} disabled={saving} title="Descartar alterações">
                                  <X size={16} />
                                </Button>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}

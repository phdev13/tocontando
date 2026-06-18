import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { monetization } from '../lib/api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/Card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/Table';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { Modal } from '../components/ui/Modal';
import { formatCurrency, formatNumber, cn } from '../lib/utils';
import { DollarSign, ShoppingCart, Users, Key, RefreshCw, Smartphone, Clock, CreditCard, Activity, AlertTriangle, Star } from 'lucide-react';

export default function Monetization() {
  const [activeTab, setActiveTab] = useState<'overview' | 'purchases' | 'users' | 'codes'>('overview');
  
  return (
    <div className="page-container flex-col gap-6">
      <div className="page-header">
        <div>
          <h1 className="page-title">Monetização</h1>
          <p className="page-subtitle">Central financeira: vendas, acessos vitalícios e códigos Premium.</p>
        </div>
      </div>

      <div className="flex items-center gap-2 border-b border-default pb-0 overflow-x-auto no-scrollbar">
        {[
          { id: 'overview', label: 'Visão Geral', icon: Activity },
          { id: 'purchases', label: 'Vendas', icon: ShoppingCart },
          { id: 'users', label: 'Usuários Premium', icon: Users },
          { id: 'codes', label: 'Códigos & Resgates', icon: Key },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={cn(
              "flex items-center gap-2 px-6 py-4 text-sm font-semibold rounded-t-lg transition-colors whitespace-nowrap border-b-2",
              activeTab === tab.id
                ? "border-primary text-primary"
                : "border-transparent text-text-secondary hover:text-text-primary hover:border-default"
            )}
          >
            <tab.icon size={16} />
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-2">
        {activeTab === 'overview' && <OverviewTab />}
        {activeTab === 'purchases' && <PurchasesTab />}
        {activeTab === 'users' && <UsersTab />}
        {activeTab === 'codes' && <CodesTab />}
      </div>
    </div>
  );
}

function OverviewTab() {
  const [lastSync, setLastSync] = useState(new Date());
  
  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['monetization-metrics'],
    queryFn: monetization.metrics,
  });

  useEffect(() => {
    if (!isFetching) setLastSync(new Date());
  }, [isFetching]);

  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold font-outfit mb-6 text-text-primary">Visão Geral Financeira</h2>
        <div className="flex items-center gap-3">
          <span className="text-xs text-text-secondary flex items-center gap-1">
            <Clock size={12} /> Última sincronização: {lastSync.toLocaleTimeString('pt-BR')}
          </span>
          <Button variant="ghost" size="sm" onClick={() => refetch()} isLoading={isFetching}>
            <RefreshCw size={14} className={cn("mr-2", isFetching && "animate-spin")} /> Atualizar
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="grid gap-6" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))' }}>
          {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-lg" />)}
        </div>
      ) : (
        <div className="grid gap-6" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))' }}>
          {/* Faturamento */}
          <MetricCard title="Receita Total" value={data?.totalRevenue ? formatCurrency(data.totalRevenue) : 'N/D'} icon={DollarSign} color="var(--success)" />
          <MetricCard title="Receita do Período" value={data?.periodRevenue ? formatCurrency(data.periodRevenue) : 'N/D'} icon={Activity} color="var(--info)" />
          <MetricCard title="Ticket Médio" value={data?.averageTicket ? formatCurrency(data.averageTicket) : 'N/D'} icon={CreditCard} color="var(--purple-300)" />
          
          {/* Vendas & Premium */}
          <MetricCard title="Vendas (Qtd)" value={data?.purchases} icon={ShoppingCart} color="var(--primary)" />
          <MetricCard title="Premium Ativos" value={data?.activeEntitlements} icon={Users} color="var(--warning)" />
          <MetricCard title="Vitalícios" value={data?.lifetimeUsers ?? 'N/D'} icon={Star} color="var(--warning)" />
          
          {/* Códigos */}
          <MetricCard title="Códigos Ativos" value={data?.totalCodes} icon={Key} color="var(--purple-500)" />
          <MetricCard title="Resgates Efetuados" value={data?.totalRedemptions} icon={Smartphone} color="var(--info)" />
          
          {/* Falhas */}
          <MetricCard title="Falhas de Validação" value={data?.validationFailures ?? 'N/D'} icon={AlertTriangle} color="var(--danger)" />
          <MetricCard title="Compras Restauradas" value={data?.restoredPurchases ?? 'N/D'} icon={RefreshCw} color="var(--success)" />
        </div>
      )}
    </div>
  );
}

function PurchasesTab() {
  const { data, isLoading } = useQuery({
    queryKey: ['monetization-purchases'],
    queryFn: monetization.purchases,
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Histórico de Vendas</CardTitle>
        <CardDescription>Todas as transações processadas pelas lojas de aplicativos.</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? <Skeleton className="h-64 w-full" /> : (data?.purchases?.length === 0 ? (
          <EmptyState icon={ShoppingCart} title="Nenhuma venda" description="Não há registros de vendas processadas." />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Transação ID</TableHead>
                <TableHead>Usuário</TableHead>
                <TableHead>Produto</TableHead>
                <TableHead>Plataforma</TableHead>
                <TableHead className="text-right">Data</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(data?.purchases || []).map((p: any) => (
                <TableRow key={p.id}>
                  <TableCell className="font-mono text-xs text-secondary">{p.id.split('-')[0]}...</TableCell>
                  <TableCell className="font-medium text-primary-text">{p.user_id}</TableCell>
                  <TableCell><Badge variant="info">{p.product_id}</Badge></TableCell>
                  <TableCell><span className="capitalize">{p.platform}</span></TableCell>
                  <TableCell className="text-right text-sm text-secondary">{new Date(p.purchased_at * 1000).toLocaleString('pt-BR')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ))}
      </CardContent>
    </Card>
  );
}

function UsersTab() {
  const { data, isLoading } = useQuery({
    queryKey: ['monetization-users'],
    queryFn: monetization.users,
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Usuários Premium</CardTitle>
        <CardDescription>Controle de usuários com Premium ativo, incluindo compras vitalícias e códigos temporários.</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? <Skeleton className="h-64 w-full" /> : (data?.users?.length === 0 ? (
          <EmptyState icon={Users} title="Nenhum usuário premium" description="Não há usuários com plano ativo no momento." />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID Usuário</TableHead>
                <TableHead>Tipo de Acesso</TableHead>
                <TableHead>Origem</TableHead>
                <TableHead>Expira em</TableHead>
                <TableHead className="text-right">Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(data?.users || []).map((u: any) => (
                <TableRow key={u.id}>
                  <TableCell className="font-medium text-primary-text">{u.user_id}</TableCell>
                  <TableCell><span className="capitalize">{u.type}</span></TableCell>
                  <TableCell>{u.source}</TableCell>
                  <TableCell className="text-sm text-secondary">{u.expires_at ? new Date(u.expires_at * 1000).toLocaleString('pt-BR') : 'Vitalício (Sempre ativo)'}</TableCell>
                  <TableCell className="text-right">
                    <Badge variant={u.status === 'ACTIVE' ? 'success' : 'default'}>{u.status === 'ACTIVE' ? 'Ativo' : u.status}</Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ))}
      </CardContent>
    </Card>
  );
}

function CodesTab() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  
  const { data, isLoading } = useQuery({
    queryKey: ['monetization-codes'],
    queryFn: monetization.codes,
  });

  const mutation = useMutation({
    mutationFn: monetization.createCode,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['monetization-codes'] });
      setIsModalOpen(false);
    }
  });

  const handleCreate = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const planType = fd.get('planType') as string;
    const customDays = parseInt(fd.get('customDays') as string, 10);
    if (planType === 'PERSONALIZADO' && (!Number.isFinite(customDays) || customDays <= 0)) {
      alert('Informe uma quantidade de dias maior que zero para o código temporário.');
      return;
    }
    mutation.mutate({
      internalName: fd.get('internalName'),
      planType: planType,
      customDays: planType === 'PERSONALIZADO' ? customDays : undefined,
      maxRedemptions: parseInt(fd.get('maxRedemptions') as string) || 1,
      codePrefix: fd.get('codePrefix') || 'QF',
      customCode: fd.get('customCode') as string | undefined
    });
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle>Códigos Promocionais</CardTitle>
          <CardDescription>Geração e resgate de códigos para acesso Premium.</CardDescription>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>Gerar Novo Código</Button>
      </CardHeader>
      <CardContent>
        {isLoading ? <Skeleton className="h-64 w-full" /> : (data?.codes?.length === 0 ? (
          <EmptyState icon={Key} title="Nenhum código gerado" description="Crie códigos promocionais para influenciadores ou campanhas." />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Código / Prefixo</TableHead>
                <TableHead>Campanha</TableHead>
                <TableHead>Benefício (Duração)</TableHead>
                <TableHead className="text-center">Resgates</TableHead>
                <TableHead className="text-right">Criado em</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(data?.codes || []).map((c: any) => (
                <TableRow key={c.id}>
                  <TableCell className="font-mono font-bold text-primary">{c.code_prefix}</TableCell>
                  <TableCell className="font-medium">{c.internal_name}</TableCell>
                  <TableCell><Badge variant="default">{formatCodeDuration(c)}</Badge></TableCell>
                  <TableCell className="text-center">
                    <span className={cn("font-medium", c.redemption_count >= c.max_redemptions ? "text-danger" : "text-success")}>
                      {c.redemption_count}
                    </span>
                    <span className="text-secondary text-xs"> / {c.max_redemptions}</span>
                  </TableCell>
                  <TableCell className="text-right text-sm text-secondary">{new Date(c.created_at * 1000).toLocaleString('pt-BR')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ))}
      </CardContent>

      <Modal isOpen={isModalOpen} onClose={() => !mutation.isPending && setIsModalOpen(false)} title="Gerar Novo Código">
        <form onSubmit={handleCreate} className="flex-col gap-4">
          <div className="flex-col gap-1">
            <label className="text-sm font-semibold text-secondary">Nome da Campanha (Interno)</label>
            <input name="internalName" required placeholder="Ex: Sorteio Instagram 2026" className="w-full p-2 bg-surface border border-default rounded-md text-sm outline-none focus:border-primary" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Prefixo Aleatório</label>
              <input name="codePrefix" defaultValue="QF" placeholder="QF" className="w-full p-2 bg-surface border border-default rounded-md text-sm outline-none focus:border-primary" />
            </div>
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Ou Código Exato</label>
              <input name="customCode" placeholder="Ex: PROMO2026" className="w-full p-2 bg-surface border border-default rounded-md text-sm outline-none focus:border-primary" />
            </div>
          </div>
          <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Tipo de Código</label>
              <select name="planType" className="w-full p-2 bg-surface border border-default rounded-md text-sm outline-none focus:border-primary">
              <option value="VITALICIO">Vitalício</option>
              <option value="PERSONALIZADO">Temporário personalizado (dias exatos)</option>
            </select>
            <p className="text-xs text-secondary">Códigos temporários são promocionais e não criam assinatura.</p>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Dias (temporário)</label>
              <input name="customDays" type="number" defaultValue="30" min="1" className="w-full p-2 bg-surface border border-default rounded-md text-sm outline-none focus:border-primary" />
            </div>
            <div className="flex-col gap-1">
              <label className="text-sm font-semibold text-secondary">Limite de Resgates</label>
              <input name="maxRedemptions" type="number" defaultValue="1" min="1" className="w-full p-2 bg-surface border border-default rounded-md text-sm outline-none focus:border-primary" />
            </div>
          </div>
          <div className="flex justify-end gap-2 mt-4 pt-4 border-t border-default">
            <Button variant="outline" type="button" onClick={() => setIsModalOpen(false)}>Cancelar</Button>
            <Button type="submit" isLoading={mutation.isPending}>Gerar Código</Button>
          </div>
        </form>
      </Modal>
    </Card>
  );
}

function formatCodeDuration(code: any) {
  if (code.duration_type === 'LIFETIME') return 'Vitalício';
  if (code.duration_type === 'DAYS') return `${code.duration_value} dias`;
  if (code.duration_type === 'MONTHS') return `Legado: ${code.duration_value} ${code.duration_value === 1 ? 'mês' : 'meses'}`;
  return code.duration_value ? `${code.duration_value} ${code.duration_type}` : code.duration_type;
}

function MetricCard({ title, value, icon: Icon }: { title: string, value: number | string | null | undefined, icon: any, color?: string }) {
  const displayValue = value == null ? 'N/D' : typeof value === 'number' ? formatNumber(value) : value;
  
  return (
    <Card className="bg-[#0b0b12] border-white/[0.03] shadow-md flex flex-col justify-center p-5 h-[104px] rounded-[14px]">
      <div className="flex items-center gap-2 mb-3">
        <Icon size={16} className="text-text-tertiary" />
        <p className="text-[10px] font-bold text-text-tertiary uppercase tracking-widest leading-none truncate">{title}</p>
      </div>
      <p className="text-[32px] font-extrabold font-outfit text-white leading-none tracking-tight truncate" title={String(displayValue)}>
        {displayValue}
      </p>
    </Card>
  );
}

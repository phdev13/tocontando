'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent } from '@/components/ui/Card';
import { KeyRound, TrendingUp, Users, X, Copy, CheckCircle2, ShoppingCart } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { adminApi, formatDateTime, formatNumber, PremiumCode, PremiumUser, Purchase } from '@/lib/admin-api';
import { useIsMobile } from '@/hooks/use-mobile';
import { MobileCard, MobileCardList } from '@/components/ui/MobileComponents';

export default function MonetizationPage() {
  const [metrics, setMetrics] = useState<any>(null);
  const [codes, setCodes] = useState<PremiumCode[]>([]);
  const [purchases, setPurchases] = useState<Purchase[]>([]);
  const [users, setUsers] = useState<PremiumUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newTokenPlan, setNewTokenPlan] = useState('VITALICIO');
  const [customDays, setCustomDays] = useState('30');
  const [copiedToken, setCopiedToken] = useState<string | null>(null);
  const [lastGeneratedCode, setLastGeneratedCode] = useState<string | null>(null);
  const isMobile = useIsMobile();

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const [metricData, codeData, purchaseData, userData] = await Promise.all([
        adminApi.premiumMetrics(),
        adminApi.premiumCodes(),
        adminApi.purchases(),
        adminApi.premiumUsers(),
      ]);
      setMetrics(metricData);
      setCodes(codeData.codes ?? []);
      setPurchases(purchaseData.purchases ?? []);
      setUsers(userData.users ?? []);
    } catch (err: any) {
      setError(err.message ?? 'Nao foi possivel carregar monetizacao.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function generate() {
    const result = await adminApi.createPremiumCode({
      planType: newTokenPlan,
      customDays: newTokenPlan === 'PERSONALIZADO' ? Number(customDays) : undefined,
      maxRedemptions: 1,
      codePrefix: 'QF',
      internalName: `Painel ${newTokenPlan}`,
    });
    setLastGeneratedCode(result.code);
    setIsModalOpen(false);
    await load();
  }

  async function copy(value: string) {
    await navigator.clipboard?.writeText(value);
    setCopiedToken(value);
    setTimeout(() => setCopiedToken(null), 2000);
  }

  return (
    <div className="space-y-6 pb-12 relative">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight text-white m-0 p-0 leading-none">Monetizacao e Tokens Premium</h1>
          <p className="text-[14px] text-[var(--color-primary-muted)] mt-2">Tokens, compras e usuarios Premium reais.</p>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="flex items-center justify-center gap-2 bg-emerald-500 hover:bg-emerald-600 text-white border border-emerald-400/50 px-4 py-2.5 sm:py-2 rounded-xl transition-colors text-[14px] font-medium w-full sm:w-auto">
          <KeyRound className="h-4 w-4" />
          Gerar Novo Token
        </button>
      </div>

      {error && <Card className="p-4 border-red-500/30 text-red-400">{error}</Card>}
      {lastGeneratedCode && <Card className="p-4 border-emerald-500/30"><p className="text-sm text-emerald-400 font-bold break-all">Token gerado: <span className="font-mono">{lastGeneratedCode}</span></p></Card>}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 sm:gap-5">
        <Kpi title="Usuarios Premium" value={loading ? '...' : formatNumber(metrics?.activeEntitlements)} icon={Users} />
        <Kpi title="Compras" value={loading ? '...' : formatNumber(metrics?.purchases)} icon={ShoppingCart} />
        <Kpi title="Vitalicios" value={loading ? '...' : formatNumber(metrics?.lifetimeUsers)} icon={TrendingUp} />
        <Kpi title="Tokens ativos" value={loading ? '...' : formatNumber(metrics?.totalCodes)} icon={KeyRound} />
      </div>

      {/* Códigos Premium */}
      <Section title="Codigos Premium">
        {!isMobile ? (
          <Table>
            <TableHeader><TableRow><TableHead>Referencia</TableHead><TableHead>Plano</TableHead><TableHead>Status</TableHead><TableHead>Resgates</TableHead><TableHead>Criado em</TableHead><TableHead className="text-right">Acao</TableHead></TableRow></TableHeader>
            <TableBody>
              {codes.length === 0 ? <TableRow><TableCell colSpan={6} className="text-center py-10 text-[var(--color-primary-muted)]">Nenhum codigo cadastrado.</TableCell></TableRow> : codes.map((code) => {
                const ref = code.code_prefix ?? code.id;
                return <TableRow key={code.id}><TableCell className="font-mono text-white">{ref}</TableCell><TableCell>{formatCodeDuration(code)}</TableCell><TableCell><Badge variant={code.status === 'ACTIVE' ? 'success' : 'secondary'}>{code.status}</Badge></TableCell><TableCell>{formatNumber(code.redemption_count)} / {formatNumber(code.max_redemptions)}</TableCell><TableCell>{formatDateTime((code.created_at ?? 0) * 1000)}</TableCell><TableCell className="text-right"><button onClick={() => copy(ref)} className="inline-flex items-center gap-1.5 text-purple-400 hover:text-purple-300 text-[13px]"><Copy className="w-3.5 h-3.5" />{copiedToken === ref ? 'Copiado' : 'Copiar ref'}</button></TableCell></TableRow>;
              })}
            </TableBody>
          </Table>
        ) : (
          <div className="p-3 space-y-3">
            {codes.length === 0 ? (
              <div className="text-center py-10 text-[var(--color-primary-muted)]">Nenhum codigo cadastrado.</div>
            ) : codes.map((code) => {
              const ref = code.code_prefix ?? code.id;
              return (
                <MobileCard
                  key={code.id}
                  title={<span className="font-mono">{ref}</span>}
                  badge={<Badge variant={code.status === 'ACTIVE' ? 'success' : 'secondary'}>{code.status}</Badge>}
                  fields={[
                    { label: 'Plano', value: formatCodeDuration(code) },
                    { label: 'Resgates', value: `${formatNumber(code.redemption_count)} / ${formatNumber(code.max_redemptions)}` },
                  ]}
                  actions={
                    <button onClick={() => copy(ref)} className="flex items-center gap-1.5 text-purple-400 text-[13px] font-medium">
                      <Copy className="w-4 h-4" />{copiedToken === ref ? 'Copiado!' : 'Copiar referência'}
                    </button>
                  }
                />
              );
            })}
          </div>
        )}
      </Section>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        {/* Compras */}
        <Section title="Compras">
          {!isMobile ? (
            <Table><TableHeader><TableRow><TableHead>ID</TableHead><TableHead>Produto</TableHead><TableHead>Plataforma</TableHead><TableHead>Data</TableHead></TableRow></TableHeader><TableBody>{purchases.length === 0 ? <TableRow><TableCell colSpan={4} className="text-center py-10 text-[var(--color-primary-muted)]">Nenhuma compra registrada.</TableCell></TableRow> : purchases.map((p) => <TableRow key={p.id}><TableCell className="font-mono">{String(p.id).slice(0, 8)}</TableCell><TableCell>{p.product_id}</TableCell><TableCell>{p.platform}</TableCell><TableCell>{formatDateTime((p.purchased_at ?? 0) * 1000)}</TableCell></TableRow>)}</TableBody></Table>
          ) : (
            <div className="p-3 space-y-3">
              {purchases.length === 0 ? (
                <div className="text-center py-10 text-[var(--color-primary-muted)]">Nenhuma compra registrada.</div>
              ) : purchases.map((p) => (
                <MobileCard
                  key={p.id}
                  title={<span className="font-mono text-[13px]">{String(p.id).slice(0, 12)}</span>}
                  subtitle={p.product_id}
                  fields={[
                    { label: 'Plataforma', value: p.platform },
                    { label: 'Data', value: formatDateTime((p.purchased_at ?? 0) * 1000) },
                  ]}
                />
              ))}
            </div>
          )}
        </Section>

        {/* Usuarios Premium */}
        <Section title="Usuarios Premium">
          {!isMobile ? (
            <Table><TableHeader><TableRow><TableHead>Usuario</TableHead><TableHead>Tipo</TableHead><TableHead>Origem</TableHead><TableHead>Expira</TableHead></TableRow></TableHeader><TableBody>{users.length === 0 ? <TableRow><TableCell colSpan={4} className="text-center py-10 text-[var(--color-primary-muted)]">Nenhum usuario Premium ativo.</TableCell></TableRow> : users.map((u) => <TableRow key={u.id}><TableCell className="font-mono">{u.user_id}</TableCell><TableCell>{u.type}</TableCell><TableCell>{u.source}</TableCell><TableCell>{u.expires_at ? formatDateTime(u.expires_at * 1000) : 'Vitalicio'}</TableCell></TableRow>)}</TableBody></Table>
          ) : (
            <div className="p-3 space-y-3">
              {users.length === 0 ? (
                <div className="text-center py-10 text-[var(--color-primary-muted)]">Nenhum usuario Premium ativo.</div>
              ) : users.map((u) => (
                <MobileCard
                  key={u.id}
                  title={<span className="font-mono text-[13px] break-all">{u.user_id}</span>}
                  badge={<span className="text-[11px] font-bold text-purple-400">{u.type}</span>}
                  fields={[
                    { label: 'Origem', value: u.source },
                    { label: 'Expira', value: u.expires_at ? formatDateTime(u.expires_at * 1000) : 'Vitalício' },
                  ]}
                />
              ))}
            </div>
          )}
        </Section>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4 bg-black/50 backdrop-blur-sm">
          <div className={`bg-[var(--color-surface)] border border-[var(--color-border)] shadow-2xl w-full overflow-hidden flex flex-col ${isMobile ? 'rounded-t-2xl max-h-[90vh]' : 'rounded-2xl max-w-sm'}`}>
            <div className="flex items-center justify-between p-5 border-b border-[var(--color-border)] shrink-0"><h3 className="text-lg font-bold text-white flex items-center gap-2"><KeyRound className="w-5 h-5 text-emerald-400" /> Gerar Token</h3><button onClick={() => setIsModalOpen(false)} className="text-[var(--color-primary-muted)] hover:text-white p-1"><X className="w-5 h-5" /></button></div>
            <div className="p-5 space-y-4 overflow-y-auto flex-1">
              <label className="block text-[13px] font-medium text-[var(--color-primary-muted)] mb-1.5">Plano</label>
              <select value={newTokenPlan} onChange={(e) => setNewTokenPlan(e.target.value)} className="w-full bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[14px]">
                <option value="VITALICIO">Vitalicio</option>
                <option value="PERSONALIZADO">Temporario personalizado</option>
              </select>
              {newTokenPlan === 'PERSONALIZADO' && <input type="number" min="1" value={customDays} onChange={(e) => setCustomDays(e.target.value)} className="w-full bg-[var(--color-surface-hover)] border border-[var(--color-border)] rounded-xl px-3 py-2.5 text-white text-[14px]" />}
              <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-3"><p className="text-[12px] text-amber-400/80">O token completo sera exibido uma unica vez apos a criacao.</p></div>
              <div className="flex flex-col sm:flex-row justify-end gap-3"><button onClick={() => setIsModalOpen(false)} className="px-4 py-2.5 text-white hover:bg-[var(--color-border)] rounded-xl order-2 sm:order-1">Cancelar</button><button onClick={generate} className="px-4 py-2.5 text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl order-1 sm:order-2">Gerar</button></div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Kpi({ title, value, icon: Icon }: { title: string; value: string | number; icon: any }) {
  return <Card><CardContent className="p-4 sm:p-5"><div className="flex items-center justify-between"><p className="text-[10px] sm:text-[11px] font-bold text-[var(--color-primary-muted)] tracking-wider uppercase mb-1">{title}</p><Icon className="h-4 w-4 text-purple-400 hidden sm:block" /></div><p className="text-2xl sm:text-4xl font-bold text-white mt-1">{value}</p></CardContent></Card>;
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <div className="bg-[var(--color-surface-hover)] rounded-2xl overflow-hidden border border-[var(--color-border)]"><div className="p-4 sm:p-5 border-b border-[var(--color-border)]"><h2 className="font-bold text-white">{title}</h2></div>{children}</div>;
}

function formatCodeDuration(code: any) {
  if (code.duration_type === 'LIFETIME') return 'Vitalicio';
  if (code.duration_type === 'DAYS') return `${code.duration_value} dias`;
  if (code.duration_type === 'MONTHS') return `${code.duration_value} meses`;
  return code.benefit_type ?? '-';
}

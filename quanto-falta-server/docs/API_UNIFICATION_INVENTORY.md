# Inventario inicial da unificacao da API

Backup criado antes da continuacao da implementacao:

```text
C:\Users\Philippe\Desktop\Quanto falta\backups\quanto-falta-server-backup-20260615-202157.zip
```

Backup remoto do D1 criado antes das migrations de producao:

```text
C:\Users\Philippe\Desktop\Quanto falta\backups\quanto-falta-d1-remote-20260615-204839.sql
```

Observacao: antes do backup foram adicionados apenas arquivos novos da camada modular, sem sobrescrever arquivos existentes.

## API central

Dominio alvo:

```text
https://api.quantofalta.shop/api/v1
```

Contrato central:

```text
backend/openapi.yaml
```

## Mapeamento atual

| Origem | Endpoint atual | Quem consome | Metodo | Autenticacao | Banco | Risco | Destino novo |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Worker backend | `/health` | Operacao | GET | Nenhuma | D1/R2 readiness | Baixo | `/api/v1/public/status` |
| Worker backend | `/api/v1/config` | App Android | GET | Nenhuma | `system_settings` | Baixo | `/api/v1/public/app-config` |
| Worker backend | `/api/v1/testers` | App/site | GET | Nenhuma | `testers` | Baixo | `/api/v1/public/testers` |
| Worker backend | `/api/v1/updates/check` | App Android | GET | Instalacao | `app_versions`, `ota_attempts`, `system_settings` | Medio | `/api/v1/app/ota/check` |
| Worker backend | `/api/v1/installations/register` | App Android | POST | Instalacao | `installations` | Baixo | `/api/v1/app/installations/register` |
| Worker backend | `/api/v1/analytics/events` | App Android | POST | Instalacao | `analytics_events` | Medio | `/api/v1/app/telemetry` |
| Worker backend | `/api/v1/analytics/performance` | App Android | POST | Instalacao | `performance_metrics` | Medio | `/api/v1/app/telemetry` |
| Worker backend | `/api/v1/feedback` | App Android | POST | Instalacao | `feedback` | Baixo | `/api/v1/app/feedback` |
| Worker backend | `/api/v1/monetization/entitlements` | App Android | GET | userId/installationId | `monetization_entitlements` | Medio | `/api/v1/app/premium/status` |
| Worker backend | `/api/v1/monetization/redeem` | App Android | POST | Instalacao + rate limit | `premium_codes`, `premium_code_redemptions`, `monetization_entitlements` | Alto | `/api/v1/app/premium/activate-token` |
| Worker backend | `/api/v1/monetization/verify-purchase` | App Android | POST | Google Play server-side | `monetization_purchases`, `monetization_entitlements` | Alto | `/api/v1/app/premium/verify-purchase` |
| Worker backend | `/admin/metrics/*` | Painel admin | GET | Cloudflare Access | D1 | Medio | `/api/v1/admin/metrics` |
| Worker backend | `/admin/feedback/*` | Painel admin | GET/PATCH | Cloudflare Access | `feedback`, `audit_logs` | Medio | `/api/v1/admin/feedback/*` |
| Worker backend | `/admin/settings/*` | Painel admin | GET/PUT | Cloudflare Access | `system_settings`, `audit_logs` | Medio | `/api/v1/admin/config` |
| Worker backend | `/admin/versions/*` | Painel admin | GET/POST/PATCH | Cloudflare Access | `app_versions`, `ota_attempts` | Alto | `/api/v1/admin/ota/releases` |
| Pages Function | `/api/events` | App Android/site share | GET/POST | Chave fixa no APK | `events` no D1 do site | Alto | `/api/v1/app/share/events` e `/api/v1/public/share/events/:slug` |
| Worker backend | `/api/v1/sync/backup` | App Android | POST | Instalacao | `user_backups` | Alto | `/api/v1/app/sync/backup` |

## Regras mantidas nesta primeira etapa

- Rotas antigas continuam ativas.
- Rotas novas usam envelope padronizado com `success`, `data`, `error` e `meta.requestId`.
- `/api/v1/admin/*` exige Cloudflare Access e CORS restrito.
- `/api/v1/public/*` e `/api/v1/app/*` ficam separados por tipo de acesso.
- Nenhum segredo novo foi criado ou exposto.
- Nenhum dado mockado foi introduzido para metricas ou entidades administrativas.

## Migrado nesta rodada

- Painel administrativo passou a consumir `/api/v1/admin/*` em `dashboard/src/lib/api.ts`.
- Site publico passou a consumir `/api/v1/public/stats/events/count`.
- App Android passou a consumir:
  - `/api/v1/public/app-config`
  - `/api/v1/public/testers`
  - `/api/v1/app/installations/register`
  - `/api/v1/app/telemetry`
  - `/api/v1/app/telemetry/performance`
  - `/api/v1/app/feedback`
  - `/api/v1/app/feedback/offline`
  - `/api/v1/app/ota/check`
  - `/api/v1/app/premium/activate-token`
  - `/api/v1/app/premium/verify-purchase`
  - `/api/v1/app/share/events`
  - `/api/v1/app/sync/backup`
- A chave fixa `X-API-Key` do fluxo de compartilhamento foi removida do app.
- Criada migration `0010_add_shared_events.sql` para centralizar eventos compartilhados no Worker principal.

## Validacao

- Backend: `npm run typecheck` passou.
- Painel: `npm run build` passou.
- App Android principal: `assemblePlayStoreDebug` passou usando o JBR do Android Studio.
- App Android completo: `assembleDebug` ainda falha no flavor `websiteDebug` por conflito de manifest/FileProvider preexistente, fora da migracao de API.
- Testes Worker: `npm test` segue bloqueado por resolucao do `@cloudflare/vitest-pool-workers` em caminho com espaco; `npx vitest run tests/rollout.test.ts --pool=forks` passou.

## Operacao aplicada em producao

- Migrations remotas aplicadas no D1 `quanto_falta_db`:
  - `0007_metrics_indexes.sql`
  - `0008_add_rate_limits.sql`
  - `0009_add_backups.sql`
  - `0010_add_shared_events.sql`
- API Worker publicada:
  - Worker: `quanto-falta-api`
  - Version ID: `7e530746-3a32-462a-acb9-d24c76a6afae`
  - URL workers.dev: `https://quanto-falta-api.philippeboechat1.workers.dev`
  - Dominio publico validado: `https://api.quantofalta.shop`
- Painel publicado no Cloudflare Pages:
  - Projeto: `quanto-falta-dashboard`
  - Alias: `https://production.quanto-falta-dashboard.pages.dev`
  - Dominio protegido validado: `https://admin.quantofalta.shop`
- Site publicado no Cloudflare Pages:
  - Projeto: `quanto-falta-web`
  - Alias: `https://production.quanto-falta-web.pages.dev`
  - Dominios validados: `https://quantofalta.shop`, `https://www.quantofalta.shop`, `https://share.quantofalta.shop`

## Validacao final

- Backend: `npm run typecheck` passou.
- Worker tests: `npm run test:worker-safe` passou. Esse script copia o backend para um caminho temporario sem espaco antes de rodar `vitest`, contornando a falha do `@cloudflare/vitest-pool-workers` no caminho local `Quanto falta`.
- Painel: `npm run build` passou.
- App Android: `assembleDebug` passou usando `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.
- Smoke HTTP:
  - `GET https://api.quantofalta.shop/api/v1/public/status` retornou 200.
  - `GET https://api.quantofalta.shop/api/v1/public/app-config` retornou 200.
  - `GET https://api.quantofalta.shop/api/v1/public/stats/events/count` retornou 200.
  - `https://admin.quantofalta.shop` retornou a tela do Cloudflare Access.
  - Site publico retornou 200 nos dominios principais.

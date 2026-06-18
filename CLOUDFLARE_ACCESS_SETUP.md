# Configuração do Cloudflare Access (Setup)

Este documento descreve as etapas necessárias para configurar o **Cloudflare Zero Trust Access** no painel administrativo do Tô Contando (`admin.tocontando.com.br`).

## 1. Aplicação Access

No painel do Cloudflare Zero Trust (Access -> Applications):
1. **Nome da aplicação:** Admin Tô Contando
2. **Domain:** `admin.tocontando.com.br`
3. **Session Duration:** 8 hours
4. **App Launcher Visibility:** Hide

## 2. Configuração do Identity Provider (IdP)
1. Certifique-se de que o **Google Workspace** (ou Google Login) está configurado em `Settings > Authentication`.
2. Se não estiver pronto, habilite o **One-Time PIN (OTP)** via E-mail temporariamente.

## 3. Criação da Política (Policy)
Crie uma regra com as seguintes configurações rigorosas:
- **Action:** Allow
- **Include:** `Emails` = `philippeboechat1@gmail.com`
- **Require:** (Nenhum)
- **Exclude:** (Nenhum)

> **Importante:** Jamais configure `Everyone` ou domínios curinga (wildcard domains). Nenhuma regra de Bypass público deve ser criada.

## 4. Configuração do Backend (Worker)
O backend (`quanto-falta-api`) já foi modificado para validar o token que o Cloudflare Access injeta (`Cf-Access-Jwt-Assertion`).
Para que ele saiba as chaves públicas (JWKS) corretas, defina as variáveis secretas:

```bash
npx wrangler secret put CF_ACCESS_TEAM_DOMAIN
# Exemplo de valor: https://seu-team.cloudflareaccess.com

npx wrangler secret put CF_ACCESS_AUDIENCE
# Exemplo de valor: <Audience Tag da aplicação criada no passo 1>
```

> **Atenção:** O painel administrativo `admin.tocontando.com.br` passará automaticamente pelo Access. Para que as chamadas da API (`workers.dev`) tenham o header injetado via proxy/CORS com cookies, recomendamos mapear o worker para `admin.tocontando.com.br/api/*` via painel da Cloudflare (Triggers -> Custom Domains).

## 5. Service Tokens (Opcional)
Se precisar acessar via CI/CD ou Scripts automatizados:
1. Crie um *Service Token* no painel Access.
2. Adicione uma política extra na aplicação do tipo **Service Auth**.
3. Envie os headers: `CF-Access-Client-Id` e `CF-Access-Client-Secret` nas requisições do bot.

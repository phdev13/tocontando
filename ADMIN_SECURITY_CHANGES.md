# Mudanças de Segurança Implementadas

Este documento lista formalmente todas as modificações técnicas feitas na arquitetura de autenticação.

## 1. Modificações no Backend (`quanto-falta-api`)
- **Remoção de Código:** Excluída a rota `backend/src/routes/admin/auth.ts`, que geria as antigas sessões de senha.
- **Middleware JWT (`auth.ts`):** Substituído o middleware que validava hashes D1 por uma validação nativa de JWT da Cloudflare (`Cf-Access-Jwt-Assertion`), que utiliza cache estrito das chaves criptográficas da JWKS da Cloudflare via biblioteca `jose`.
- **Injeção Centralizada (`index.ts`):** A root de rotas `/admin/*` agora está firmemente travada pelo novo `adminAuthMiddleware`.
- **Desativação de Cache:** Adicionados cabeçalhos em `/admin/*`:
  `Cache-Control: no-store, no-cache, must-revalidate, proxy-revalidate`
  `Pragma: no-cache`
- **Variáveis de Ambiente:** Eliminados segredos vazios (`ADMIN_SESSION_SECRET`, `ADMIN_PASSWORD_HASH`). Introduzidos `CF_ACCESS_TEAM_DOMAIN` e `CF_ACCESS_AUDIENCE`.

## 2. Modificações no Frontend (`dashboard`)
- **Remoção da Rota `/login`:** A página inteira e o formulário de credenciais foram excluídos.
- **Rotas Zero Trust (`App.tsx`):** O `AuthGuard` ineficiente foi abolido. O frontend confia que o Cloudflare Access bloqueou usuários não autorizados no *Edge* (CDNs da Cloudflare).
- **Botão de Logout (`Layout.tsx`):** O botão de "Sair" agora emite um redirecionamento seguro para a limpeza global do SSO: `window.location.href = '/cdn-cgi/access/logout'`.

## 3. Banco de Dados (D1)
- As tabelas `admin_users` e `admin_sessions` podem (e devem) ser truncadas e descartadas na próxima migração SQL (Drop Tables), pois nenhuma consulta a elas restou na aplicação.

# Auditoria de Segurança Administrativa

**Data da Auditoria:** 2026-06-15
**Alvo:** Backend (Worker) e Frontend (Dashboard) do projeto *Tô Contando*

## Descobertas
1. **Autenticação Obsoleta:** O sistema antigo possuía a rota `POST /admin/auth/login`, validando um formulário de usuário e senha por meio de `pbkdf2`.
2. **Sessões Internas Inseguras:** Existiam tabelas `admin_users` e `admin_sessions` no D1 gerenciando acessos via cookie `qf_admin_session`.
3. **Fallback Administrativo:** Não foram identificados fallbacks ou *bypasses* de rede duvidosos, mas a mera existência de senhas em DB representa risco de *brute force* e *credential stuffing*.
4. **Credenciais Hardcoded:** Não havia "admin/admin" literalmente colado, mas a lógica permitia o *login* com uma senha armazenada como hash nos Secrets (o que significa que havia *apenas um* usuário real mapeado com a senha na env `ADMIN_PASSWORD_HASH`).
5. **Endpoints de API:** A rota `/admin/*` não possuía cache desativado, o que poderia (embora com baixo risco devido aos Cookies) causar *leak* acidental se a CDN não interpretasse o Cache-Control padrão corretamente.

## Ação Executada
- Todo o fluxo manual foi destruído, em conformidade total com o paradigma de Zero Trust de ponta a ponta. Sessões obsoletas não podem mais ser exploradas.

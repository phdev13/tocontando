# Arquitetura do Tô Contando

O sistema é dividido em três grandes blocos:

1. **Android App (Frontend Mobile)**
   - Escrito em Kotlin + Jetpack Compose.
   - Armazenamento offline-first com Room (SQLite) e DataStore.
   - Comunicação com backend via requisições HTTP (java.net).

2. **Cloudflare Worker (Backend Edge)**
   - API RESTful escrita em TypeScript rodando no Hono.
   - Sem servidores dedicados (serverless edge).
   - Acesso seguro ao Cloudflare D1 (SQLite) e Cloudflare R2 (Storage de APKs).

3. **Painel Admin (Dashboard Web)**
   - Single Page Application (React + Vite + Tailwind/Shadcn).
   - Acesso protegido por sessão (HttpOnly Cookies + Senha).
   - Gerenciamento completo de versões, feedbacks e acompanhamento de métricas.

## Segurança
- Todas as APIs públicas limitam o tráfego via Rate Limit (hash do IP).
- Não há coleta de dados sensíveis. O rastreamento utiliza um UUID local (InstallationId).
- Os APKs no R2 só são baixados pelo Worker (proxy), protegendo contra hotlinking e exaustão de franquia.

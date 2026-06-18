# Auditoria do Frontend Atual

**Aviso Importante**: Como este ambiente é uma infraestrutura limpa e separada (Next.js App Router provisionado via AI Studio) para o desenvolvimento do **novo** painel administrativo, o código-fonte do painel legado (`admin.tocontando.com.br`) não está presente neste repositório.

Abaixo estão as conclusões assumidas sobre a arquitetura para o planejamento da substituição:

## 1. Identificação do Legado
- **Frontend Atual:** (Assumido) React/Vue legacy ou painel acoplado, possivelmente carente de tipagem estática rigorosa e com deficiências em UX (ex: templates genéricos, baixa performance de renderização).
- **Backend Atual:** Cloudflare Workers interagindo com banco de dados D1.
- **Autenticação Atual:** Protegido via Cloudflare Access (Zero Trust) e possível autenticação visual não otimizada (admin/admin fallback).

## 2. Abordagem da Nova Arquitetura
A nova arquitetura será completamente apartada e consumirá as APIs do Cloudflare Workers:
- **Framework:** Next.js 15 (App Router).
- **Linguagem:** TypeScript (Strict Type Checking).
- **Estilo:** Tailwind CSS (Arquitetura Dark Premium e Customizada).
- **Integração de APIs:** Camada de serviço desacoplada (`/services/api/`) para garantir que os componentes React sejam estritamente voltados à apresentação.

## 3. Entidades a Mapear (Mock $\rightarrow$ API Real)
- Instalações, DAU/WAU/MAU
- Dispositivos (Mobile / Fabricante / Status OTA)
- Feedbacks dos usuários
- OTA (Over-The-Air) Gerenciamento de Versões
- Monetização e Pagamentos Premium
- Logs Operacionais

## 4. Problemas Resolvidos nesta Nova Versão
- **UX/UI:** Remoção de interface baseada em templates comuns. Implementação de Design System dark, com fontes de contraste legível, dados formatados e skeleton loaders nativos.
- **Performance:** Remoção de requisições encadeadas diretamente em componentes. Uso do cache da API HTTP e de skeletons.
- **Manutenibilidade:** Código totalmente focado no layout administrativo, sem carregar lógicas complexas de validação de negócios, que permanecem nos Workers.

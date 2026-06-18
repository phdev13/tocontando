# Arquitetura do Frontend

O novo painel administrativo do "Tô Contando" será baseado na arquitetura **App Router do Next.js**.

## Estrutura de Diretórios
- **/app/ (Rotas da Aplicação):** Cada diretório representa uma página. `page.tsx` é a UI da página. Recheado de `loading.tsx` e `error.tsx` globais.
- **/components/ (Componentes):** Componentes isolados. Divididos em `ui/` para componentes primitivos (botões, inputs, cards) e `layout/` para estruturas complexas.
- **/services/ (Integração Backend):** Onde estão localizados os proxies para consumo da API Cloudflare Workers/D1. Nenhuma chamada HTTP "suja" será feita nos componentes visuais.
- **/lib/ (Utilitários):** Funções globais de manipulação visual, parse de datas ou merge de classes (ex: `cn`).
- **/types/ (Tipagens):** Todos os schemas do TypeScript e validações estáticas.

## Dados e Estados
- **Local State:** Usado estritamente para estado de UI (menus abertos, modais e tooltips).
- **Remote State:** Chamadas assíncronas ao Service, geridas via Server Components (RSCs) quando for possível ler dados de leitura inicial estática, ou Client Components com SWR/Fetch genérico se precisarem de dinamismo.

## Design System e Estilos
O painel utilizará **Tailwind CSS**. A cor base do painel será predominantemente tons escuros sofisticados, usando as primitivas `zinc` ou `slate` para neutralidade premium. Todos os espaçamentos seguirão a escala nativa (`p-4`, `p-6`).

## Proteções Aplicadas
- O token da API não precisa residir em state no cliente; os componentes servidores (App Router) podem interceptar a conexão ao endpoint com as credenciais ambientais de desenvolvimento, mas para produção com Cloudflare Access, será passado pelas diretrizes do cabeçalho Proxy.

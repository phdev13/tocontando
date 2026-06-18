# Plano de Migração do Painel

A migração requer controle estrito para garantir tolerância à falha sem impacto à operação global do aplicativo móvel ou análises atuais.

## Fases da Migração

### Fase 1: Fundação do Novo Painel (Preview)
1. Construir toda a UI e Design System com dados "mockados" baseados nos contratos assumidos da API real.
2. Definir a camada de serviço (`/services`) limpa e coesa.
3. Hospedar o App num novo subdomínio ou ambiente apartado.

### Fase 2: Conexão Progressiva 
- Ligar módulos passo-a-passo. Ex: Dashboard, Métricas. Validar retornos, respostas vazias e lidar corretamente com a repaginação ou formato de paginação do Workers existente.

### Fase 3: Validação
- O único usuário ativo (philippeboechat1@gmail.com) testará na nova versão consumindo o Cloudflare Access do ambiente de teste.
- Auditar logs e garantir que nenhuma ação administrativa é barrada por timeout ou CORS no novo Preview.

### Fase 4: O Corte
- Quando todos os componentes da Fase 3 estiverem 100% confiáveis, trocar o CNAME e Rotas DNS do legados para a Vercel/Cloudflare Pages hospedando o novo projeto Next.js.
- Deletar e limpar ambiente legado APÓS 30 dias de operação ininterrupta.

## Rollback
- O DNS pode ser revertido aos IPs da hospedagem antiga do Cloudflare Workers Legacy ao notar falhas críticas de infraestrutura em Produção.

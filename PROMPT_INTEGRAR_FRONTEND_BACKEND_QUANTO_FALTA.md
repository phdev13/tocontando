# PROMPT — INTEGRAR O FRONTEND EXISTENTE AO BACKEND DO QUANTO FALTA?

## Objetivo

Analise o frontend administrativo que já está sendo desenvolvido e faça ele se comunicar corretamente com o backend existente do Tô Contando.

Este trabalho é exclusivamente de integração.

Não crie um novo frontend.
Não refaça o projeto.
Não altere o design.
Não substitua componentes visuais que já existem.
Não recrie telas que já estão prontas.
Não altere a identidade visual.
Não reorganize o projeto sem necessidade técnica real.

A missão é conectar todas as telas, componentes, formulários, cards, métricas, tabelas, filtros e ações já existentes às APIs reais documentadas abaixo.

---

## Regra principal

O frontend atual deve ser preservado.

Antes de modificar qualquer coisa:

1. analise toda a estrutura do projeto;
2. identifique quais telas e componentes já existem;
3. identifique dados mockados, hardcoded ou temporários;
4. identifique chamadas falsas ou incompletas;
5. relacione cada tela existente com os endpoints corretos;
6. implemente apenas o necessário para a integração funcionar.

Não recrie algo que já existe.

Não troque bibliotecas, frameworks ou padrões atuais sem necessidade comprovada.

Adapte a integração à arquitetura existente do projeto.

---

## Proibições

É estritamente proibido:

- criar outro frontend;
- recriar páginas já existentes;
- alterar o design;
- mudar cores, espaçamentos, tipografia ou identidade visual;
- substituir componentes prontos por outros;
- criar outro backend;
- alterar rotas do backend sem necessidade comprovada;
- usar Firebase, Supabase ou banco local;
- criar dados mockados permanentes;
- gerar métricas falsas;
- usar números aleatórios;
- esconder erros da API com dados falsos;
- inventar endpoints;
- inventar campos;
- utilizar Bearer Token nas rotas administrativas;
- colocar segredos, tokens ou credenciais no frontend;
- criar login próprio;
- armazenar o cookie do Cloudflare Access manualmente;
- declarar uma integração como concluída sem testá-la.

---

## Backend existente

O backend já está pronto e utiliza:

- Cloudflare Workers;
- Hono;
- D1;
- Cloudflare Access nas rotas administrativas;
- APIs públicas em `/api/v1`;
- APIs administrativas em `/admin`.

A documentação fornecida ao final deste prompt é a única fonte de verdade para:

- endpoints;
- métodos HTTP;
- parâmetros;
- corpos de requisição;
- corpos de resposta;
- valores permitidos;
- autenticação;
- regras de negócio.

Não invente contratos ausentes.

Quando algum retorno não estiver completamente documentado, faça a chamada real, inspecione a resposta e tipifique com base no retorno real.

---

## URL do backend

Utilize uma variável de ambiente.

Exemplo:

```env
VITE_API_BASE_URL=https://api.tocontando.com.br
```

Crie ou ajuste:

```env
VITE_API_BASE_URL=
```

Também crie ou atualize:

```text
.env.example
```

Nunca coloque a URL real diretamente em vários arquivos.

Considere que `VITE_API_BASE_URL` representa somente a origem do backend.

Exemplos corretos:

```text
${VITE_API_BASE_URL}/admin/metrics/overview
${VITE_API_BASE_URL}/admin/feedback
${VITE_API_BASE_URL}/api/v1/config
```

Evite duplicações como:

```text
/api/v1/api/v1
/admin/admin
```

---

## Cloudflare Access

Todas as rotas `/admin/*` são protegidas pelo Cloudflare Access.

As chamadas administrativas devem enviar:

```ts
credentials: "include"
```

Não utilize Bearer Token.

Não tente ler ou manipular o cookie `CF_Authorization`.

Não armazene cookies em:

- localStorage;
- sessionStorage;
- IndexedDB;
- variáveis JavaScript;
- arquivos do projeto.

Quando uma chamada retornar `401` ou `403`:

- informe que a sessão administrativa expirou ou que o acesso não foi autorizado;
- preserve a rota atual;
- permita recarregar ou renovar a sessão;
- não redirecione para um login falso;
- não esconda o erro;
- não considere o usuário autenticado apenas por estado local.

---

## Auditoria inicial obrigatória

Antes de implementar, analise o projeto inteiro e crie:

```text
docs/BACKEND_INTEGRATION_AUDIT.md
```

Esse arquivo deve conter:

```text
Tela ou componente
Dados atuais
Endpoint correto
Método HTTP
Status atual
Problemas encontrados
Integração necessária
```

Classifique cada item como:

```text
já integrado
mockado
hardcoded
parcialmente integrado
sem integração
bloqueado por contrato
bloqueado por CORS ou autenticação
```

Não altere o visual durante essa auditoria.

---

## Mapeamento obrigatório

Relacione cada parte do frontend existente com a API correspondente.

Exemplos:

- cards do dashboard → `/admin/metrics/overview`;
- gráficos de eventos → `/admin/metrics/events`;
- gráficos de performance → `/admin/metrics/performance`;
- lista de versões → `/admin/versions`;
- criação de versão → `POST /admin/versions`;
- edição de versão → `PATCH /admin/versions/:versionCode`;
- upload de APK → `/admin/versions/github-release`;
- configurações → `/admin/settings`;
- feedbacks → `/admin/feedback`;
- estatísticas de feedback → `/admin/feedback/stats`;
- códigos promocionais → `/admin/monetization/codes`;
- métricas financeiras → `/admin/monetization/metrics`;
- compras → `/admin/monetization/purchases`;
- usuários premium → `/admin/monetization/users`;
- testers → `/admin/testers`.

Use os endpoints exatos definidos na documentação.

---

## Camada de API

Primeiro verifique se o projeto já possui:

- cliente HTTP;
- pasta de serviços;
- hooks de dados;
- tipos TypeScript;
- tratamento de erros;
- sistema de cache.

Se já existir, reutilize e corrija.

Somente crie uma nova camada se não houver nenhuma estrutura adequada.

Evite espalhar chamadas `fetch` diretamente pelos componentes.

Centralize a integração seguindo a arquitetura atual do projeto.

Uma estrutura possível, apenas se compatível com o projeto, é:

```text
src/
  api/
    client.ts
    apiError.ts
  services/
    metricsService.ts
    versionsService.ts
    feedbackService.ts
    settingsService.ts
    monetizationService.ts
    testersService.ts
  types/
    api.ts
```

Não reorganize o projeto inteiro apenas para seguir esse exemplo.

---

## Cliente HTTP

O cliente HTTP deve possuir:

- URL base via variável de ambiente;
- `credentials: "include"` nas rotas administrativas;
- timeout;
- suporte a `AbortController`;
- parsing seguro de JSON;
- suporte a respostas sem corpo;
- suporte a respostas não JSON;
- tratamento centralizado de erros;
- cabeçalho `Accept: application/json`;
- `Content-Type: application/json` somente para corpos JSON;
- suporte a FormData;
- prevenção de requisições duplicadas;
- retry somente para GET e apenas em falhas transitórias.

Nunca repita automaticamente:

- POST;
- PUT;
- PATCH;
- DELETE.

Nunca defina manualmente o `Content-Type` ao enviar `FormData`.

---

## Tipagem

Crie ou ajuste tipos TypeScript usando exatamente os contratos reais do backend.

Não invente propriedades para satisfazer componentes visuais.

Quando o componente atual espera um campo que não existe na API:

1. verifique se existe outro campo equivalente;
2. crie um mapper explícito, se fizer sentido;
3. adapte somente a camada de dados;
4. preserve o componente visual;
5. documente a divergência.

Não faça conversões silenciosas entre `snake_case` e `camelCase`.

Caso seja necessário converter, crie funções de mapeamento centralizadas e tipadas.

---

## Remoção de mocks

Substitua todos os dados mockados por respostas reais da API.

Isso inclui:

- cards;
- gráficos;
- tabelas;
- contadores;
- listas;
- detalhes;
- filtros;
- formulários;
- status;
- configurações;
- monetização;
- versões;
- feedbacks;
- testers.

Não apague os mocks imediatamente se eles forem úteis para entender o formato atual.

Primeiro:

1. identifique;
2. documente;
3. substitua pela API;
4. confirme que não são mais usados;
5. remova somente após a integração real funcionar.

Não mantenha fallback silencioso para mock quando a API falhar.

---

## Estados da interface

Aproveite os componentes visuais que já existem.

Conecte corretamente os estados:

- carregando;
- sucesso;
- vazio;
- erro;
- erro de autenticação;
- erro de validação;
- atualização;
- envio;
- exclusão;
- processamento.

Não redesenhe as telas.

Caso não exista estado de erro ou carregamento, adicione o mínimo necessário, respeitando totalmente o design atual.

Não exiba valores falsos como `0` quando a API falhar.

Não transforme erro em lista vazia sem informar o usuário.

---

## Dashboard e métricas

Conecte os componentes atuais às rotas:

```text
GET /admin/metrics/overview
GET /admin/metrics/events
GET /admin/metrics/performance
```

Regras:

- usar apenas campos reais;
- não gerar dados para preencher gráfico;
- não inventar variação percentual;
- preservar os filtros atuais;
- enviar o parâmetro `days`;
- respeitar o limite máximo de 90 dias;
- formatar datas sem alterar o visual;
- tratar ausência de dados;
- cancelar chamadas antigas ao trocar filtros;
- evitar requisições duplicadas.

Se o frontend possuir cards para dados que o backend não retorna, não invente valores.

Documente o campo ausente e mantenha o componente em estado indisponível até que o contrato seja confirmado.

---

## Versões e OTA

Conecte as telas existentes às rotas:

```text
GET /admin/versions
POST /admin/versions
PATCH /admin/versions/:versionCode
POST /admin/versions/github-release
```

Preserve os formulários e modais atuais.

Implemente:

- listagem;
- filtros;
- paginação;
- criação;
- edição;
- ativação;
- pausa;
- aposentadoria;
- rollout;
- atualização obrigatória;
- versão mínima;
- upload do APK;
- atualização da lista após sucesso.

Status válidos:

```text
draft
active
paused
retired
```

Canais válidos:

```text
stable
beta
internal
```

O rollout deve permanecer entre 0 e 100.

Não considere uma ação concluída antes da confirmação do backend.

---

## Upload de APK

A rota:

```text
POST /admin/versions/github-release
```

utiliza:

```text
multipart/form-data
```

Campos:

```text
apk
versionCode
versionName
title
notes
```

Regras:

- utilizar `FormData`;
- não definir manualmente o `Content-Type`;
- validar extensão `.apk`;
- preservar o componente de upload já existente;
- impedir envio duplicado;
- mostrar estado de envio;
- mostrar processamento;
- mostrar erro real;
- mostrar sucesso somente após confirmação;
- atualizar versões e métricas relacionadas;
- não expor GitHub PAT no frontend.

---

## Feedbacks

Conecte as telas atuais às rotas:

```text
GET /admin/feedback
PATCH /admin/feedback/:id
GET /admin/feedback/stats
```

Implemente os filtros existentes:

- status;
- categoria;
- nota mínima;
- pesquisa;
- paginação;
- limite;
- offset.

Campos editáveis:

```text
status
priority
adminNotes
tags
```

Status válidos:

```text
new
reviewing
planned
resolved
ignored
```

Prioridades válidas:

```text
low
normal
high
critical
```

Após uma alteração:

- atualizar detalhes;
- atualizar a lista;
- atualizar estatísticas;
- preservar os filtros atuais.

---

## Configurações

Conecte as telas atuais às rotas:

```text
GET /admin/settings
PUT /admin/settings/:key
```

Regras:

- carregar valores reais;
- enviar `value` sempre como string;
- editar somente chaves permitidas;
- não inventar configurações;
- preservar o formulário atual;
- restaurar o valor anterior se o salvamento falhar;
- atualizar a configuração após sucesso;
- mostrar a data real de atualização quando disponível.

---

## Monetização

Conecte as telas existentes às rotas:

```text
GET /admin/monetization/metrics
GET /admin/monetization/codes
GET /admin/monetization/codes/:id
POST /admin/monetization/codes
DELETE /admin/monetization/codes/:id
GET /admin/monetization/purchases
GET /admin/monetization/users
```

Planos válidos:

```text
VITALICIO
PERSONALIZADO
```

Para `PERSONALIZADO`, `customDays` é obrigatório.

Preserve:

- cards;
- tabelas;
- filtros;
- formulários;
- modais;
- paginação.

Não exiba compras, receita ou usuários fictícios.

Depois de criar ou revogar um código:

- atualizar lista;
- atualizar detalhes;
- atualizar métricas;
- preservar os filtros atuais.

---

## Beta Testers

Conecte as telas atuais às rotas:

```text
GET /admin/testers
POST /admin/testers
PUT /admin/testers/:id
DELETE /admin/testers/:id
```

Preserve a interface atual.

Implemente:

- listagem;
- criação;
- edição;
- ativação;
- destaque;
- ordenação;
- consentimento;
- exclusão.

Não crie upload de imagem caso o backend aceite apenas URL ou chave.

---

## Formulários existentes

Não recrie os formulários.

Conecte os campos atuais aos corpos reais da API.

Adicione somente o necessário para:

- validação;
- erro de campo;
- envio;
- bloqueio de envio duplicado;
- confirmação;
- sucesso;
- falha;
- atualização dos dados.

Quando houver divergência entre o formulário e a API:

- preserve o visual;
- ajuste o nome do campo na camada de dados;
- crie mapper;
- documente a diferença.

---

## Cache e atualização

Utilize a solução já presente no projeto.

Se o projeto já usa TanStack Query, SWR ou equivalente, mantenha a biblioteca atual.

Não instale outra biblioteca de cache sem necessidade.

Após mutações:

- invalidar somente os dados relacionados;
- refazer consultas necessárias;
- preservar filtros;
- preservar paginação;
- evitar recarregar toda a aplicação;
- não fazer atualização otimista em ações críticas;
- não assumir sucesso antes da resposta.

Exemplos:

- editar feedback atualiza lista, detalhes e stats;
- publicar versão atualiza versões, dashboard e métricas OTA;
- salvar configuração atualiza as configurações;
- criar código atualiza códigos e monetização;
- editar tester atualiza lista e detalhes.

---

## CORS

Analise se frontend e backend estão:

- na mesma origem;
- em subdomínios diferentes;
- em domínio `pages.dev`;
- em domínio `workers.dev`;
- em domínio personalizado.

Durante desenvolvimento, utilize o proxy já existente do Vite, se houver.

Caso não exista e seja necessário, configure proxy para:

```text
/admin
/api/v1
```

Preserve cookies e credenciais.

Não altere o backend automaticamente para resolver CORS.

Se houver erro de CORS, documente exatamente:

- origem do frontend;
- origem da API;
- método bloqueado;
- cabeçalho ausente;
- preflight afetado;
- ajuste necessário.

Com cookies, o backend não pode utilizar:

```text
Access-Control-Allow-Origin: *
```

A origem deve ser explícita e compatível com:

```text
Access-Control-Allow-Credentials: true
```

---

## Tratamento de erros

Padronize os erros sem alterar o design.

Trate:

```text
400
401
403
404
409
422
429
500
falha de rede
timeout
resposta inválida
```

Mensagens esperadas:

- `401`: sessão administrativa não encontrada;
- `403`: acesso não autorizado;
- `404`: recurso não encontrado;
- `409`: conflito;
- `422`: dados inválidos;
- `429`: muitas solicitações;
- `500`: erro interno;
- rede: não foi possível conectar ao servidor;
- timeout: o servidor demorou para responder.

Não exponha detalhes sensíveis em produção.

---

## Segurança

Garanta que o frontend não contenha:

- Cloudflare API Token;
- GitHub PAT;
- credenciais do R2;
- secrets do Worker;
- tokens de compra;
- chaves privadas;
- cookies copiados;
- credenciais administrativas;
- valores sensíveis em logs.

Não registre no console:

- cookies;
- tokens;
- purchase tokens;
- códigos promocionais completos;
- payloads sensíveis;
- dados pessoais desnecessários.

---

## Performance

A integração não pode prejudicar a performance atual.

Evite:

- chamadas duplicadas;
- polling desnecessário;
- refetch em excesso;
- requisições a cada render;
- loops de requisição;
- atualização global após qualquer ação;
- re-renderizações desnecessárias;
- dependências novas sem necessidade.

Use:

- cancelamento de chamadas obsoletas;
- cache existente;
- paginação;
- debounce em pesquisa;
- carregamento sob demanda;
- invalidação específica;
- memoização somente quando necessária.

---

## Processo de implementação

### Etapa 1 — Análise

- ler toda a documentação;
- analisar o frontend existente;
- localizar mocks;
- localizar serviços;
- localizar hooks;
- localizar formulários;
- localizar componentes;
- relacionar telas e endpoints;
- criar `docs/BACKEND_INTEGRATION_AUDIT.md`.

### Etapa 2 — Fundação da integração

- corrigir URL base;
- corrigir variáveis de ambiente;
- ajustar cliente HTTP;
- configurar cookies;
- configurar erros;
- configurar tipos;
- configurar CORS ou proxy quando necessário.

### Etapa 3 — Integração por módulo

Ordem recomendada:

1. health e autenticação;
2. dashboard;
3. métricas;
4. feedbacks;
5. versões e OTA;
6. configurações;
7. monetização;
8. testers.

### Etapa 4 — Testes

Para cada módulo, testar:

- carregamento;
- sucesso;
- vazio;
- erro;
- `401`;
- `403`;
- filtros;
- paginação;
- criação;
- edição;
- exclusão;
- atualização de cache;
- responsividade existente.

### Etapa 5 — Limpeza

Somente após a integração funcionar:

- remover mocks não utilizados;
- remover serviços falsos;
- remover imports mortos;
- remover TODOs resolvidos;
- remover chamadas duplicadas;
- remover logs temporários.

---

## Documento de status

Crie:

```text
docs/BACKEND_INTEGRATION_STATUS.md
```

Formato:

```text
Tela ou componente
Endpoint
Método
Arquivo de serviço
Status
Testado
Problema encontrado
Observação
```

Status permitidos:

```text
não iniciado
em andamento
integrado
bloqueado
contrato incompleto
erro de CORS
erro de autenticação
```

Não marque como integrado sem uma chamada real bem-sucedida.

---

## Critérios de conclusão

A tarefa só estará concluída quando:

- o frontend existente tiver sido preservado;
- o design não tiver sido alterado;
- nenhuma página tiver sido recriada sem necessidade;
- todos os mocks identificados estiverem substituídos ou documentados;
- as telas utilizarem dados reais;
- chamadas administrativas enviarem cookies;
- não houver Bearer Token;
- o Cloudflare Access estiver respeitado;
- erros estiverem tratados;
- filtros funcionarem;
- paginação funcionar;
- formulários enviarem dados reais;
- mutações atualizarem os dados relacionados;
- upload de APK utilizar FormData;
- não houver segredos no frontend;
- não houver números inventados;
- não houver botões sem função por causa da integração;
- a aplicação compilar sem erros;
- TypeScript não possuir erros;
- não houver imports quebrados;
- não houver chamadas duplicadas importantes;
- a documentação de status estiver atualizada.

---

## Entrega final

Ao terminar, informe:

1. quais arquivos foram alterados;
2. quais mocks foram removidos;
3. quais telas foram conectadas;
4. quais endpoints foram integrados;
5. quais endpoints foram realmente testados;
6. quais integrações ficaram bloqueadas;
7. quais divergências foram encontradas;
8. quais ajustes de CORS são necessários;
9. quais variáveis de ambiente precisam ser configuradas;
10. quais pontos dependem do Cloudflare Access.

Não diga que tudo está funcionando sem ter validado.

---

# DOCUMENTAÇÃO DA API

Cole abaixo desta linha a documentação completa da API do Tô Contando.

Ela deve ser tratada como a única fonte de verdade para a integração.

Não recrie o frontend.
Não recrie o backend.
Apenas faça o frontend já existente conversar corretamente com o backend real.

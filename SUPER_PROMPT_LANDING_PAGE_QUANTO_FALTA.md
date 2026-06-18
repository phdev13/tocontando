# SUPER PROMPT — Landing Page Premium do app “Tô Contando”

## Missão

Crie uma landing page completa, premium, moderna e altamente refinada para o aplicativo Android **Tô Contando**, um app de contagens regressivas para momentos importantes, como viagens, casamentos, formaturas, aniversários, provas, férias e metas pessoais.

A página deve seguir fielmente a linguagem visual da imagem de referência fornecida, mas não deve ser apenas uma cópia superficial. O objetivo é transformar essa referência em uma interface real, responsiva, elegante, funcional, rápida e pronta para produção.

A implementação precisa transmitir imediatamente:

- produto confiável;
- aplicativo bem acabado;
- simplicidade;
- privacidade;
- emoção;
- organização;
- valor premium;
- identidade própria;
- qualidade de produto comercial real.

O resultado não pode parecer um template genérico, uma landing page montada com componentes aleatórios ou um site simples de portfólio.

---

# 1. Direção visual

## Estilo geral

Use uma linguagem visual premium, limpa e emocional, combinando:

- fundo predominantemente branco;
- grandes áreas de respiro;
- hierarquia tipográfica forte;
- roxo como cor principal;
- tons suaves de lilás;
- pequenos destaques em verde, azul, coral, amarelo e rosa;
- cards com cantos arredondados;
- sombras muito suaves e realistas;
- superfícies brancas elevadas;
- bordas discretas;
- seções muito bem espaçadas;
- componentes com aparência polida;
- detalhes visuais suficientes para parecer um produto real.

A aparência deve ficar próxima de páginas premium de aplicativos SaaS e mobile, com qualidade comparável a produtos modernos da Apple, Linear, Notion, Arc, Framer e startups de alto nível, mas mantendo personalidade própria.

## Sensação desejada

A página precisa parecer:

- leve;
- acolhedora;
- moderna;
- confiável;
- bonita;
- organizada;
- premium;
- emocional sem ser infantil;
- detalhada sem ficar carregada.

## O que evitar

Não criar:

- visual genérico de template;
- excesso de texto;
- excesso de cards iguais;
- seções sem contraste;
- espaços vazios sem intenção;
- roxo chapado em toda a página;
- gradientes exagerados;
- glassmorphism excessivo;
- sombras fortes;
- ícones inconsistentes;
- botões pequenos;
- fontes muito finas;
- animações chamativas;
- efeitos que prejudiquem a performance;
- elementos desalinhados;
- conteúdo apertado;
- layout com largura estreita demais;
- aparência de painel administrativo;
- aparência de documento técnico.

---

# 2. Stack e implementação

Implemente a página utilizando a stack já existente no projeto.

Antes de alterar qualquer arquivo:

1. analise toda a estrutura atual;
2. identifique o framework utilizado;
3. localize os componentes existentes;
4. preserve rotas, integrações e funcionalidades atuais;
5. reutilize o que estiver bem estruturado;
6. substitua apenas o que estiver visualmente fraco, inconsistente ou incompleto.

Caso o projeto ainda esteja em HTML, CSS e JavaScript puro, mantenha essa stack.

Caso esteja em React, Next.js, Vue, Vite ou tecnologia semelhante, siga o padrão já existente.

Não recrie o projeto do zero sem necessidade.

---

# 3. Estrutura completa da página

A landing page deve conter as seguintes áreas.

---

## 3.1 Navbar premium

Crie uma navbar flutuante e refinada, centralizada dentro de um container amplo.

### Conteúdo

À esquerda:

- logotipo do app;
- nome “Tô Contando”.

No centro:

- Funcionalidades;
- Preços;
- Perguntas.

À direita:

- botão principal “Baixar grátis”.

### Aparência

- fundo branco levemente translúcido;
- blur muito sutil;
- sombra suave;
- borda quase imperceptível;
- cantos arredondados;
- altura confortável;
- aparência de barra premium flutuante;
- largura alinhada ao restante da página;
- sticky no topo;
- espaçamento interno generoso.

### Comportamento

- smooth scroll;
- estado hover discreto;
- indicador visual da seção ativa;
- menu mobile elegante;
- acessibilidade por teclado;
- boa legibilidade em telas pequenas.

---

## 3.2 Hero principal

A primeira dobra precisa causar impacto visual imediato.

Use uma composição em duas colunas no desktop.

### Coluna esquerda

Adicionar:

1. badge pequeno e elegante:
   - “O contador de momentos da sua vida”;

2. título principal:
   - “Cada contagem conta uma história”;

3. destacar visualmente a palavra:
   - “história”;

4. descrição:
   - explicar que o app cria contagens regressivas para viagens, casamentos, formaturas, aniversários e outros momentos importantes;
   - reforçar que funciona offline, sem cadastro e sem anúncios;

5. botões:
   - “Baixar grátis”;
   - “Ver funcionalidades”;

6. pequenos indicadores:
   - 100% offline;
   - sem cadastro;
   - sem anúncios;
   - privado.

### Coluna direita

Criar um mockup premium de smartphone Android, levemente inclinado, exibindo a interface real do aplicativo.

Dentro do aparelho, mostrar quatro eventos:

- Férias em Lisboa;
- Nosso Casamento;
- Formatura;
- Meu Aniversário.

Cada evento deve conter:

- imagem temática;
- nome;
- data;
- dias;
- horas;
- minutos;
- segundos;
- cores diferentes;
- boa legibilidade.

Adicionar botão flutuante circular com ícone de “+” na tela do app.

### Fundo do hero

Criar uma ambientação visual sofisticada inspirada na referência:

- paisagem suave;
- montanhas desfocadas;
- luz de fim de tarde;
- flores discretas;
- névoa;
- tons lilás, rosa e pêssego.

O fundo não pode competir com o conteúdo.

Usar overlay claro para preservar contraste.

No mobile:

- texto primeiro;
- mockup abaixo;
- botões em largura adequada;
- imagem totalmente responsiva;
- nenhum corte estranho.

---

## 3.3 Barra de benefícios rápidos

Logo abaixo do hero, criar uma barra branca elevada, sobreposta parcialmente ao final da primeira seção.

Exibir quatro benefícios:

- 100% offline;
- sem cadastro;
- sem anúncios;
- privado.

Cada item deve ter:

- ícone dentro de círculo suave;
- título;
- subtítulo curto;
- cor temática própria;
- separação visual sutil.

A barra precisa parecer parte importante do produto, não um rodapé improvisado.

---

## 3.4 Seção de funcionalidades

Adicionar label:

> FEITO PARA O QUE IMPORTA

Título:

> Tudo que você precisa, nada que você não precisa.

Criar seis cards em grid.

### Cards

1. **Privacidade total**
   - os eventos ficam apenas no aparelho;
   - sem nuvem obrigatória;
   - sem rastreamento invasivo.

2. **Personalização**
   - cores;
   - imagens;
   - ícones;
   - identidade visual individual para cada evento.

3. **Widgets**
   - visualização na tela inicial;
   - diferentes tamanhos;
   - atualização automática.

4. **Lembretes**
   - notificações antes dos eventos;
   - marcos importantes;
   - alertas discretos.

5. **Modo escuro**
   - adaptação automática ao sistema;
   - boa legibilidade;
   - conforto visual.

6. **Compartilhamento**
   - compartilhar contagens;
   - enviar momentos para amigos e familiares;
   - experiência simples.

### Aparência dos cards

- layout horizontal no desktop;
- ícone circular grande;
- título;
- descrição;
- borda sutil;
- sombra leve;
- altura equilibrada;
- hover discreto;
- microelevação;
- ícones consistentes;
- cores suaves diferentes.

---

## 3.5 Galeria de eventos

Adicionar label:

> MOMENTOS QUE VALEM A PENA CONTAR

Criar uma galeria com quatro cards visuais grandes.

### Eventos

- Férias em Lisboa;
- Nosso Casamento;
- Formatura;
- Meu Aniversário.

Cada card deve conter:

- imagem temática;
- overlay para contraste;
- título;
- data;
- contagem em dias, horas, minutos e segundos;
- estilo próprio;
- cantos arredondados;
- aparência próxima da interface real do app.

Usar:

- Lisboa ou viagem no primeiro card;
- flores e casamento no segundo;
- capelo de formatura no terceiro;
- balões ou bolo no quarto.

No desktop, exibir quatro cards lado a lado.

No tablet, dois por linha.

No mobile, usar carrossel horizontal com snap suave.

Adicionar indicadores de paginação discretos quando necessário.

---

## 3.6 Planos

Adicionar label:

> ESCOLHA SEU PLANO

Criar dois cards principais.

### Plano grátis

Mostrar:

- preço R$ 0;
- uso permanente;
- contagens limitadas;
- recursos essenciais;
- widgets;
- lembretes;
- uso offline;
- botão “Baixar grátis”.

### Premium vitalício

Mostrar:

- destaque “Mais escolhido”;
- preço em real;
- pagamento único;
- temas exclusivos;
- ícones premium;
- imagens premium;
- personalização avançada;
- recursos futuros elegíveis;
- apoio ao desenvolvimento;
- botão “Quero o Premium”.

### Aparência

O card Premium deve receber maior destaque:

- borda roxa;
- fundo com lilás muito suave;
- pequenos detalhes decorativos;
- sombra ligeiramente mais evidente;
- selo superior;
- botão roxo forte;
- hierarquia de preço clara.

Não tornar o plano grátis visualmente ruim.

Os dois precisam parecer opções legítimas.

No mobile, exibir o Premium primeiro.

---

## 3.7 Perguntas frequentes

Adicionar label:

> PERGUNTAS FREQUENTES

Criar FAQ em duas colunas no desktop e uma coluna no mobile.

Perguntas sugeridas:

- O app funciona sem internet?
- Preciso criar uma conta?
- Tem anúncios?
- Posso usar no meu computador?
- Como funciona o Premium Vitalício?
- Meus dados ficam seguros?

Usar accordion real.

### Requisitos

- animação curta e suave;
- ícone de expansão;
- navegação por teclado;
- `aria-expanded`;
- conteúdo legível;
- divisões sutis;
- sem excesso de bordas.

---

## 3.8 CTA final

Criar uma faixa larga e premium antes do footer.

### Conteúdo

Título:

> Pronto para contar o que importa?

Descrição:

> Baixe agora e transforme cada momento em uma contagem especial.

Botão:

> Baixar grátis agora

### Aparência

- fundo roxo intenso;
- detalhes suaves;
- ícone do app;
- boa hierarquia;
- composição horizontal no desktop;
- empilhada no mobile;
- sombra sutil;
- cantos arredondados;
- botão branco ou muito claro;
- contraste AA.

---

## 3.9 Footer

Criar footer completo e organizado.

### Coluna da marca

- logo;
- nome do app;
- descrição curta;
- copyright.

### Produto

- Funcionalidades;
- Preços;
- Avaliações.

### Suporte

- Perguntas frequentes;
- Fale conosco;
- Política de Privacidade.

### Redes

- Instagram;
- e-mail;
- Telegram ou outra rede configurada;
- selo da Google Play.

Utilizar o e-mail oficial:

> contato@quantofalta.shop

Manter o footer leve, limpo e alinhado.

---

# 4. Identidade visual

## Paleta principal

Use como base:

```css
--purple-50: #f7f3ff;
--purple-100: #eee5ff;
--purple-200: #ddccff;
--purple-300: #c4a5ff;
--purple-400: #a875ff;
--purple-500: #8b45f2;
--purple-600: #7429df;
--purple-700: #6020bd;
--purple-800: #4f1d99;
--purple-900: #421a7d;

--text-primary: #171326;
--text-secondary: #625d72;
--text-muted: #918b9d;

--surface: #ffffff;
--surface-soft: #fbfaff;
--border: rgba(45, 33, 72, 0.10);
--border-strong: rgba(45, 33, 72, 0.16);

--success: #20b486;
--blue: #4e91ff;
--coral: #ff7373;
--amber: #ffae38;
--pink: #ee78a9;
```

A paleta pode ser refinada, desde que preserve a identidade roxa.

## Tipografia

Usar uma combinação sofisticada:

- títulos principais: uma fonte editorial elegante, como Lora, Fraunces ou equivalente;
- interface, botões e textos: Inter, Manrope ou equivalente.

Regras:

- título hero grande;
- subtítulos claros;
- corpo confortável;
- pesos equilibrados;
- boa altura de linha;
- não usar negrito excessivo;
- nunca usar fonte cursiva;
- limitar largura dos parágrafos.

---

# 5. Responsividade

A página deve ser construída mobile first.

Testar cuidadosamente em:

- 320 px;
- 360 px;
- 390 px;
- 414 px;
- 768 px;
- 1024 px;
- 1280 px;
- 1440 px;
- telas ultrawide.

Garantir:

- nenhuma rolagem horizontal;
- nenhum texto cortado;
- nenhum card espremido;
- mockup do celular redimensionado corretamente;
- botões fáceis de tocar;
- menus funcionais;
- grids adaptáveis;
- imagens com proporção preservada;
- espaçamentos proporcionais;
- footer legível.

---

# 6. Microinterações e animações

Adicionar animações discretas, premium e leves.

Permitido:

- fade-in;
- leve translateY;
- hover com elevação de 2 a 4 px;
- mudança suave de borda;
- brilho sutil em botões;
- animação curta nos accordions;
- entrada suave do mockup;
- parallax mínimo apenas em elementos decorativos;
- feedback visual no clique;
- scroll suave.

Regras:

- duração entre 150 ms e 450 ms;
- respeitar `prefers-reduced-motion`;
- não bloquear o carregamento;
- não animar grandes áreas continuamente;
- não usar bibliotecas pesadas sem necessidade;
- não prejudicar FPS;
- evitar animações em loop.

---

# 7. Performance extrema

A página precisa ser muito rápida.

Implementar:

- imagens AVIF ou WebP;
- `srcset`;
- `sizes`;
- lazy loading;
- preload apenas do essencial;
- fontes otimizadas;
- poucas variações de fonte;
- CSS organizado;
- JavaScript mínimo;
- evitar dependências pesadas;
- evitar layout shift;
- reservar espaço das imagens;
- componentes abaixo da dobra carregados progressivamente;
- eventos com listeners passivos;
- nenhuma animação baseada em scroll pesado;
- nenhuma requisição desnecessária;
- nenhuma imagem gigantesca sem compressão;
- nenhum vídeo automático no hero.

Metas:

- Lighthouse Performance acima de 95;
- Accessibility acima de 95;
- Best Practices acima de 95;
- SEO acima de 95;
- LCP abaixo de 2,5 s;
- CLS abaixo de 0,1;
- INP abaixo de 200 ms.

---

# 8. SEO

Configurar:

- `lang="pt-BR"`;
- title;
- meta description;
- canonical;
- Open Graph;
- Twitter Cards;
- favicon;
- theme color;
- sitemap;
- robots.txt;
- JSON-LD para SoftwareApplication;
- headings semânticos;
- conteúdo indexável;
- links descritivos;
- URLs corretas;
- textos alternativos;
- dados estruturados coerentes.

Título sugerido:

> Tô Contando — Contagens regressivas para momentos especiais

Descrição sugerida:

> Crie contagens regressivas bonitas para viagens, casamentos, aniversários, formaturas e muito mais. Gratuito, privado, sem anúncios e 100% offline.

---

# 9. Acessibilidade

Garantir:

- contraste mínimo WCAG AA;
- foco visível;
- navegação completa por teclado;
- áreas de toque de no mínimo 44 px;
- labels para controles;
- `aria-label` quando necessário;
- accordions acessíveis;
- menu mobile acessível;
- hierarquia correta de headings;
- textos alternativos úteis;
- ícones decorativos ocultos de leitores de tela;
- suporte a zoom;
- suporte a `prefers-reduced-motion`;
- nenhuma informação transmitida apenas por cor.

---

# 10. Conteúdo e consistência

Use textos reais em português brasileiro.

Não use:

- Lorem Ipsum;
- textos genéricos;
- inglês misturado;
- números aleatórios sem contexto;
- métricas falsas apresentadas como reais;
- avaliações inventadas sem identificação de demonstração;
- preços diferentes em cada seção;
- nomes inconsistentes;
- informações contraditórias.

Caso uma informação ainda não esteja definida, use um placeholder claramente marcado no código, mas mantenha o visual completo.

---

# 11. Imagens e mockups

Use a imagem de referência fornecida como direção visual.

Criar ou utilizar:

- mockup realista do aplicativo;
- imagens temáticas para os eventos;
- fundos leves;
- ilustrações compatíveis com a identidade.

Não usar banco de imagens aleatório sem coerência.

As imagens precisam parecer parte do mesmo sistema visual.

Não inserir imagens com texto incorporado quando o texto puder ser HTML.

---

# 12. Qualidade de código

O código deve ser:

- limpo;
- semântico;
- modular;
- organizado;
- reutilizável;
- fácil de manter;
- livre de duplicação desnecessária;
- sem componentes gigantes;
- sem estilos inline em excesso;
- sem hacks frágeis;
- sem valores mágicos espalhados;
- sem erros no console;
- sem warnings relevantes;
- sem links quebrados;
- sem botões sem ação.

Criar tokens para:

- cores;
- espaçamento;
- radius;
- sombras;
- tipografia;
- largura dos containers;
- transições;
- z-index.

---

# 13. Funcionalidades obrigatórias

Implementar de verdade:

- navegação por âncora;
- navbar sticky;
- menu mobile;
- FAQ accordion;
- carrossel ou scroll snap dos cards no mobile;
- botões com links configuráveis;
- dark mode opcional, caso já exista no projeto;
- animações de entrada;
- estados hover, focus, active e disabled;
- links do footer;
- integração com o link da Google Play quando fornecido.

Não deixar controles apenas decorativos.

---

# 14. Processo obrigatório

Antes de finalizar:

1. analisar todo o projeto;
2. identificar problemas visuais atuais;
3. implementar a nova estrutura;
4. revisar desktop;
5. revisar tablet;
6. revisar mobile;
7. verificar acessibilidade;
8. verificar performance;
9. verificar erros no console;
10. validar links;
11. revisar ortografia;
12. revisar alinhamentos;
13. revisar espaçamentos;
14. revisar contraste;
15. revisar consistência entre componentes.

---

# 15. Critérios de aprovação

O trabalho só estará concluído quando:

- o site parecer um produto comercial premium;
- o hero estiver visualmente impactante;
- o mockup do app estiver convincente;
- as seções tiverem ritmo e hierarquia;
- o layout estiver impecável em mobile;
- nenhuma seção parecer vazia ou genérica;
- os cards tiverem variação visual sem perder consistência;
- os botões tiverem presença;
- a identidade roxa estiver bem aplicada;
- os textos estiverem corretos;
- a página estiver rápida;
- o código estiver limpo;
- nenhuma funcionalidade existente tiver sido quebrada;
- o resultado estiver claramente superior ao site atual.

---

# Instrução final

Implemente a landing page completa no projeto existente.

Não entregue apenas uma descrição, wireframe, plano ou código parcial.

Faça as alterações reais nos arquivos, crie todos os componentes necessários, integre os assets, garanta responsividade, acessibilidade, performance e acabamento visual.

Use a imagem de referência como direção estética principal, mas refine cada detalhe para que o resultado final tenha identidade própria e aparência de um produto premium real.

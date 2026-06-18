# Matriz de Testes de Segurança Obrigatórios

Assim que você configurar a aplicação Cloudflare Access e aplicar os deploys via terminal, submeta o sistema aos testes lógicos abaixo:

## Bloco 1: Acesso de Borda (Edge)
1. **[ ] Bloqueio Anônimo:** Abra uma guia anônima e acesse `admin.quantofalta.shop`. **Passou se:** A tela de login da Cloudflare interceptar o carregamento. Nenhuma página "App" carregou.
2. **[ ] Bloqueio Curinga:** Tente entrar usando OTP em um e-mail falso genérico (ex: `teste@gmail.com`). **Passou se:** O e-mail não receber código ou o Access informar que você não é autorizado pela política.
3. **[ ] Acesso Exato:** Entre com `philippeboechat1@gmail.com` recebendo OTP/Google OAuth. **Passou se:** A dashboard aparecer renderizada e os dados surgirem.
4. **[ ] Logout Universal:** Clique no botão de logout da dashboard (que envia para `/cdn-cgi/access/logout`). **Passou se:** A sessão for deslogada e ao tentar voltar for exigida a tela Cloudflare novamente.

## Bloco 2: Acesso Backend (API Workers)
Usando ferramentas como Postman ou `curl` contra a rota do worker (ex: `https://quanto-falta-api.philippeboechat1.workers.dev/admin/metrics`):
5. **[ ] Requisição sem JWT:** Faça uma requisição GET. **Passou se:** Retornar Status HTTP `401 Unauthorized` e JSON `Missing Cloudflare Access Token`.
6. **[ ] Requisição com JWT falsificado:** Envie um header `Cf-Access-Jwt-Assertion: aaa.bbb.ccc`. **Passou se:** O servidor emitir `401 Unauthorized` com `Invalid token`.
7. **[ ] E-mail de terceiro válido no JWT:** (Se você tiver controle para gerar um JWT válido por outra regra da CF e tentar forjar neste worker). **Passou se:** Status HTTP `403 Forbidden` informando `Unauthorized user`.

## Bloco 3: Segurança Complementar (Pós-login)
8. **[ ] Verificação de Cache Header:** Abra a aba *Network* no DevTools e analise a resposta de `/admin/metrics`. **Passou se:** A resposta contiver: `Cache-Control: no-store, no-cache...`.
9. **[ ] Teste de Ausência de `admin/admin`:** Teste a URL legada `/admin/auth/login`. **Passou se:** Ocorrer um Status `404 Not Found` puro emitido pelo Hono router.
10. **[ ] Não-Impacto em Tráfego Público:** Acesse os endpoints nativos usados pelo app Mobile (como `/api/v1/config`). **Passou se:** Responder Status HTTP `200` sem pedir validação do Cloudflare Access.

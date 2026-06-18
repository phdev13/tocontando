# Plano de Recuperação de Acesso Administrativo

## Cenário 1: E-mail Principal Inacessível
Se a conta `philippeboechat1@gmail.com` sofrer *lockout* ou for deletada/desativada no Google, você **não poderá** acessar o painel administrativo.

**Procedimento de Recuperação:**
1. Autentique-se no painel mestre global da **Cloudflare** (`dash.cloudflare.com`).
2. Acesse **Zero Trust -> Access -> Applications -> Admin Tô Contando**.
3. Na seção de **Policies**, edite a política *Allow*.
4. Mude ou adicione o seu **E-mail Secundário** na lista de `Emails`.
5. Salve a política. O acesso é restaurado instantaneamente para o novo e-mail (usando OTP ou nova conta Google).

## Cenário 2: Revogação Compulsória de Sessões Vativas
Se você estiver logado em um dispositivo comprometido e precisar deslogar *imediatamente*:
1. Acesse o **Zero Trust -> My Team -> Users**.
2. Busque pelo seu e-mail (`philippeboechat1@gmail.com`).
3. Clique em **Revoke User Sessions**.
4. A Cloudflare matará todos os JWTs válidos instantaneamente na CDN global.

## Cenário 3: Exclusão Acidental da Aplicação Access
1. Se a aplicação Access for deletada, a Cloudflare **passará a permitir requisições ao domínio sem interceptação**. 
2. No entanto, o **Backend Rejeitará (401)**, pois o Worker exige estritamente que um JWT válido `Cf-Access-Jwt-Assertion` assinado pela sua Audience esteja presente. O frontend exibirá erro em todos os dados e tabelas.
3. **Procedimento:** Recrie a Aplicação Access e a Política Exclusiva. Gere um novo *Audience Tag* e atualize a variável `CF_ACCESS_AUDIENCE` via CLI do Wrangler.

# Deploy do Ecossistema

O deploy do ecossistema é garantido via GitHub Actions.

## Deploy do App Android (OTA) e Dashboard
Basta você "taggear" a versão atual do app e enviar pro Github.
```bash
git tag v1.0.0
git push origin v1.0.0
```
O Github fará todo o processo: compilar, assinar o APK com a Keystore, mandar para o R2 do Cloudflare, postar a atualização na API, atualizar a Dashboard e publicar no painel de release do GitHub.

## Deploy Manual
Se preferir fazer deploy manual da API ou do Painel de Admin, certifique-se de estar logado no Cloudflare:
```bash
npx wrangler login
```

**Para a API (Worker):**
```bash
cd backend
npm run deploy:prod
```

**Para a Dashboard:**
```bash
cd dashboard
npm run build
npx wrangler pages deploy dist --project-name quanto-falta-dashboard --branch main
```

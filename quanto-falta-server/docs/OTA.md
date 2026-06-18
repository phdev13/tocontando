# Sistema OTA (Over-The-Air)

O sistema de OTA permite ao Tô Contando atualizar-se sem depender da Google Play Store.

## Fluxo:
1. **App:** O `OtaWorker` (Android WorkManager) roda a cada 6 horas (com flexibilidade).
2. **App:** Realiza um GET em `/api/v1/updates/check`, enviando `installationId` e a `versionCode` atual.
3. **Worker:** A API calcula um hash determinístico (`FNV-1a`) combinando `installationId` + `novaVersionCode`. Isso garante que se um rollout está em 10%, exatamente os mesmos 10% dos usuários receberão o update.
4. **App:** Caso elegível, baixa o arquivo APK para uma área segura (`context.filesDir/ota`).
5. **App:** Verifica o SHA-256 localmente.
6. **App:** Mostra o `OtaUpdateModal` convidando (ou forçando) o usuário a instalar.

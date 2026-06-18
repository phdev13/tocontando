# Sistema de Analytics e Métricas

## Princípio da Privacidade
O Tô Contando NÃO utiliza IDs de publicidade, Firebase Analytics ou identificadores rastreáveis de hardware (IMEI/MAC). O identificador único (`InstallationId`) é um UUID-v4 gerado localmente que se perde caso o app seja desinstalado ou limpo.

## Eventos Autorizados
O Android usa uma *sealed class* `AnalyticsEvent` garantindo que:
- Não há campos de texto livre (onde o usuário poderia acidentalmente enviar PII, como títulos de eventos).
- Todo evento passa por um "Privacy Gate" (`PrivacySettings`). Se o usuário não consentir, o evento é descartado silenciosamente.

## Envio Offline
As métricas são agrupadas na `AnalyticsQueue` (um JSON na memória interna).
O `AnalyticsWorker` roda apenas quando há Wi-Fi ou Conexão, em blocos de até 50 eventos, enviando via POST para o Worker.

# Documentação de Monetização - Tô Contando

## Visão Geral

Este documento descreve a arquitetura técnica e o fluxo de dados do sistema de monetização do aplicativo *Tô Contando*. O modelo comercial atual possui apenas:

- Grátis
- Premium Vitalício, pago uma única vez pela Google Play

Assinaturas mensais e anuais são tratadas apenas como compatibilidade legada, caso existam compradores antigos com direito ainda válido.

## 1. Catálogo de Permissões

Para manter a lógica extensível, o código do aplicativo não deve depender de preço ou plano comercial hardcoded. A fonte de autoridade é o entitlement validado pelo servidor.

## 2. Produtos do Play Console

Produto comercial ativo:

1. `premium_lifetime`: produto `INAPP`, pagamento único, sem expiração.

Produtos legados preservados apenas para restauração/validação de compradores antigos:

1. `premium_monthly`: assinatura antiga.
2. `premium_annual`: assinatura antiga.

O app não deve oferecer novas compras de `premium_monthly` ou `premium_annual`.

## 3. Estados de Entitlements

- `FREE`: sem privilégio Premium.
- `PREMIUM_LIFETIME_PURCHASE`: compra vitalícia validada pela Google Play.
- `PREMIUM_LEGACY_SUBSCRIPTION`: assinatura antiga ainda válida.
- `PREMIUM_CODE`: concessão via código vitalício ou temporário.
- `PREMIUM_ADMIN_GRANTED`: concessão manual pelo painel admin.

Compras vitalícias não têm `expires_at`. Códigos temporários e assinaturas legadas podem ter expiração.

## 4. Códigos Premium

O painel pode gerar:

- Código vitalício: sem data de expiração do benefício.
- Código temporário personalizado: quantidade exata de dias.

Códigos temporários são promocionais e não representam assinatura, mensalidade ou renovação automática.

Regras de segurança:

1. Os códigos originais nunca são armazenados em texto plano; o banco guarda apenas hash.
2. A rota de resgate valida status, janela de validade e limite de resgates.
3. A contagem de uso, o resgate e o entitlement são persistidos em lote.

## 5. Fluxo de Compra e Validação

1. O app busca somente `premium_lifetime` como `BillingClient.ProductType.INAPP`.
2. O usuário compra pela Google Play.
3. O app envia `purchaseToken`, `productId` e `installationId` ao backend.
4. O servidor valida o token na Google Play.
5. Se válido, registra `monetization_purchases` e cria `monetization_entitlements`.
6. O app salva apenas o entitlement retornado pelo servidor como cache local.

A restauração consulta produtos `INAPP` e também `SUBS` para preservar assinantes legados.

## 6. Dashboard e Auditoria

O painel não cria novos códigos Mensal ou Anual. Registros históricos podem continuar aparecendo em relatórios como legados para não quebrar auditoria.

Revogações e concessões administrativas devem ser rastreáveis por `monetization_audit_logs` quando o fluxo administrativo correspondente estiver disponível.

## 7. Funcionamento Offline

O app pode usar cache local de entitlements para experiência offline, mas não deve conceder Premium definitivo apenas por estado local. Purchase tokens continuam sendo validados pelo servidor.

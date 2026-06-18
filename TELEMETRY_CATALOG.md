# Catálogo de Telemetria - Tô Contando

Este documento cataloga todos os eventos, métricas de performance e coletas do ecossistema Tô Contando, atualizado após a unificação da infraestrutura de telemetria.

> [!IMPORTANT]
> **Privacidade:** Nenhuma destas métricas contém PII (Personally Identificable Information) como nomes, e-mails, títulos de viagens explícitos que identifiquem a pessoa.

## 1. Eventos de Analytics Gerais
Enviados pelo `AnalyticsManager.track(AnalyticsEvent(...))` e processados no endpoint `/api/v1/app/telemetry`. Todos os payloads são compactados usando **GZIP** via `TelemetrySyncWorker`.

| Nome do Evento | Propriedades Registradas | Gravidade/Tipo |
| --- | --- | --- |
| `app_opened` | source (push, launcher, link) | NORMAL |
| `screen_view` | screen_name, previous_screen | LOW |
| `event_created` | format (DAYS, WEEKS, MONTHS), has_image, has_time | NORMAL |
| `event_deleted` | was_completed, days_remaining | NORMAL |
| `paywall_opened` | trigger_source (limit_reached, banner, settings) | HIGH |
| `purchase_completed`| sku_id, price_currency, is_trial | CRITICAL |
| `api_request` (Interceptor) | endpoint, method, status, duration_ms, success, error | NORMAL |
| `widget_added` | widget_type (small, medium, large) | NORMAL |

## 2. Métricas de Performance

### 2.1 Startup & Queries
Enviadas em lotes compactados para `/api/v1/app/telemetry/performance`.
| Tipo (`metric_type`) | Valor (`value_ms`) | Tela/Contexto (`screen`) |
| --- | --- | --- |
| `cold_start` | Tempo desde o `Application.onCreate` até a primeira renderização do Compose. | N/A |
| `db_query` | Tempo gasto em consultas Room/SQLite no Android. | Nome da Tabela/Dao |
| `slow_frame` | Quadros que estouraram o budget (>16ms). | Nome do Composable |

### 2.2 JankStats (Taxa de Quadros)
O `JankStatsAggregator` coleta dados de hardware contínuos da UI Thread por tela e os salva no Room local, sendo enviados também pelo `TelemetrySyncWorker` a cada janela de 12 horas.
- **Métricas extraídas**: `TotalFrames_ScreenName` e `JankFrames_ScreenName`.
- **Traces**: Ao contrário do ambiente de testes (Macrobenchmark), a produção não coleta perfettos completos do Jank para economizar rede (GCP/Cloudflare), ela apenas agrupa e agrega.

### 2.3 Macrobenchmark (Ambiente Controlado)
Acionado primariamente em pipelines ou builds profile via `/api/v1/performance/runs`.
- Envia o .perfetto-trace bruto compactado para armazenamento em R2 (Cloudflare).
- Retenção máxima de 30 dias (limpeza automática programada por CRON no Worker Backend às 4:00 AM UTC).

## 3. Crash Reports & Erros
(Planejado)
| Tipo de Erro | Módulo Afetado | Ação de Recarga |
| --- | --- | --- |
| `NetworkTimeout` | OTA / Sync | Retry Automático via Worker |
| `DbCorruption` | Room | Deleção DB / Rebuild |

## 4. Retenção de Dados e Custos

- **Eventos Analíticos (D1)**: Retidos permanentemente, compactados periodicamente em Parquet (exportação analítica programada).
- **Traces de Performance (R2)**: Excluídos automaticamente após **30 dias** pelo cron job `scheduled` do Hono index.ts.
- **Métricas Agregadas (D1)**: As métricas brutas (performance_metrics e performance_runs) também são deletadas após 30 dias para evitar o estouro de 500 MB do D1 Free/Paid Standard, mantendo apenas visualizações consolidadas na Dashboard (agregadas periodicamente).

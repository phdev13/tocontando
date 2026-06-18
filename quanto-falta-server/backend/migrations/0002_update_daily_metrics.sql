-- Remove o índice antigo se ele existir
DROP INDEX IF EXISTS idx_daily_metrics_unique;

-- Cria o novo índice usando COALESCE para tratar dimensões nulas como ''
CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_metrics_unique
  ON daily_metrics(date, metric_name, COALESCE(dimension_key, ''), COALESCE(dimension_value, ''));

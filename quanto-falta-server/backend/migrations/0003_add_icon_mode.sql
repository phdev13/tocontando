-- Migration 0003: Insert default icon mode setting
INSERT OR IGNORE INTO system_settings (key, value, description)
VALUES ('active_icon_mode', 'auto', 'Ícone ativo do app: auto, force_copa, force_padrao');

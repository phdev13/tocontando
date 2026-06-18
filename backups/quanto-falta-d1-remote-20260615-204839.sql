PRAGMA defer_foreign_keys=TRUE;
CREATE TABLE d1_migrations(
		id         INTEGER PRIMARY KEY AUTOINCREMENT,
		name       TEXT UNIQUE,
		applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
INSERT INTO "d1_migrations" ("id","name","applied_at") VALUES(1,'0001_initial.sql','2026-06-13 00:33:50');
INSERT INTO "d1_migrations" ("id","name","applied_at") VALUES(2,'0002_free_tier.sql','2026-06-13 00:46:11');
INSERT INTO "d1_migrations" ("id","name","applied_at") VALUES(3,'0003_add_icon_mode.sql','2026-06-13 01:14:35');
INSERT INTO "d1_migrations" ("id","name","applied_at") VALUES(4,'0004_add_push_tokens.sql','2026-06-13 01:30:23');
INSERT INTO "d1_migrations" ("id","name","applied_at") VALUES(5,'0005_add_testers.sql','2026-06-14 23:21:31');
INSERT INTO "d1_migrations" ("id","name","applied_at") VALUES(6,'0002_update_daily_metrics.sql','2026-06-15 04:48:49');
INSERT INTO "d1_migrations" ("id","name","applied_at") VALUES(7,'0006_add_monetization_tables.sql','2026-06-15 04:48:49');
CREATE TABLE app_versions (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  version_code    INTEGER NOT NULL UNIQUE,
  version_name    TEXT NOT NULL,
  release_channel TEXT NOT NULL DEFAULT 'stable',  
  status          TEXT NOT NULL DEFAULT 'draft',   
  mandatory       INTEGER NOT NULL DEFAULT 0,       
  min_supported_version_code INTEGER NOT NULL DEFAULT 1,
  title           TEXT NOT NULL DEFAULT 'Nova atualização disponível',
  summary         TEXT NOT NULL DEFAULT '',
  changelog       TEXT NOT NULL DEFAULT '[]',       
  apk_r2_key      TEXT,                             
  apk_size_bytes  INTEGER,
  sha256          TEXT,
  signature_fingerprint TEXT,
  rollout_percentage INTEGER NOT NULL DEFAULT 0,
  published_at    INTEGER,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  updated_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
, github_release_tag TEXT);
INSERT INTO "app_versions" ("id","version_code","version_name","release_channel","status","mandatory","min_supported_version_code","title","summary","changelog","apk_r2_key","apk_size_bytes","sha256","signature_fingerprint","rollout_percentage","published_at","created_at","updated_at","github_release_tag") VALUES(1,2,'1.1','stable','draft',1,1,'Novidades: Linha do Tempo, Agradecimento aos Testers e Melhorias Visuais!',replace(replace('✨ NOVIDADES:\r\n• Linha do Tempo dos Eventos: Agora você pode acompanhar o histórico completo de cada evento! Veja quando ele foi criado, marcos importantes da contagem regressiva e quando foi concluído.\r\n• Agradecimento aos Testers: Criamos uma área especial (em Ajustes > Sobre) para homenagear todos que ajudaram a construir o Quanto Falta. Um muito obrigado a todos os nossos testadores!\r\n\r\n🛠 MELHORIAS E CORREÇÕES:\r\n• Textos Inteligentes: Melhoramos as mensagens de destaque e os textos dos eventos. O app agora se adapta melhor ao tema do evento (viagens, finanças, festas) de forma muito mais natural e fluida.\r\n• Correção de Placeholders: Resolvemos o problema onde mensagens técnicas (como "{prefillTitle}") apareciam na tela por engano.\r\n• Interface Limpa: Removemos botões redundantes na tela de Destaque para uma experiência mais limpa.\r\n• Nova nomenclatura: Na aba de Concluídos, o botão agora diz "Ver Eventos Ativos", deixando mais claro o próximo passo!\r\n\r\nAtualize agora e aproveite!\r\n','\r',char(13)),'\n',char(10)),'["✨ NOVIDADES:\r\n• Linha do Tempo dos Eventos: Agora você pode acompanhar o histórico completo de cada evento! Veja quando ele foi criado, marcos importantes da contagem regressiva e quando foi concluído.\r\n• Agradecimento aos Testers: Criamos uma área especial (em Ajustes > Sobre) para homenagear todos que ajudaram a construir o Quanto Falta. Um muito obrigado a todos os nossos testadores!\r\n\r\n🛠 MELHORIAS E CORREÇÕES:\r\n• Textos Inteligentes: Melhoramos as mensagens de destaque e os textos dos eventos. O app agora se adapta melhor ao tema do evento (viagens, finanças, festas) de forma muito mais natural e fluida.\r\n• Correção de Placeholders: Resolvemos o problema onde mensagens técnicas (como \"{prefillTitle}\") apareciam na tela por engano.\r\n• Interface Limpa: Removemos botões redundantes na tela de Destaque para uma experiência mais limpa.\r\n• Nova nomenclatura: Na aba de Concluídos, o botão agora diz \"Ver Eventos Ativos\", deixando mais claro o próximo passo!\r\n\r\nAtualize agora e aproveite!\r\n"]',NULL,20172787,NULL,NULL,0,NULL,1781481061000,1781481067192,'1.1');
INSERT INTO "app_versions" ("id","version_code","version_name","release_channel","status","mandatory","min_supported_version_code","title","summary","changelog","apk_r2_key","apk_size_bytes","sha256","signature_fingerprint","rollout_percentage","published_at","created_at","updated_at","github_release_tag") VALUES(2,3,'1.2','stable','paused',1,1,'Melhorias Visuais','','[""]',NULL,20172787,NULL,NULL,100,1781500547460,1781481583000,1781500554389,'1.2');
INSERT INTO "app_versions" ("id","version_code","version_name","release_channel","status","mandatory","min_supported_version_code","title","summary","changelog","apk_r2_key","apk_size_bytes","sha256","signature_fingerprint","rollout_percentage","published_at","created_at","updated_at","github_release_tag") VALUES(3,5,'1.4','stable','active',1,1,'Melhorias Gerais','','[""]',NULL,20172787,NULL,NULL,100,1781500550770,1781483044000,1781500550770,'1.4');
INSERT INTO "app_versions" ("id","version_code","version_name","release_channel","status","mandatory","min_supported_version_code","title","summary","changelog","apk_r2_key","apk_size_bytes","sha256","signature_fingerprint","rollout_percentage","published_at","created_at","updated_at","github_release_tag") VALUES(4,1,'1.5','stable','draft',0,1,'Correções e melhorias','','[""]',NULL,25057845,NULL,NULL,0,NULL,1781500484000,1781500484000,'1.5');
INSERT INTO "app_versions" ("id","version_code","version_name","release_channel","status","mandatory","min_supported_version_code","title","summary","changelog","apk_r2_key","apk_size_bytes","sha256","signature_fingerprint","rollout_percentage","published_at","created_at","updated_at","github_release_tag") VALUES(5,7,'1.6','stable','active',1,1,'Melhorias','','[""]',NULL,25057845,NULL,NULL,0,1781501284459,1781501276000,1781501286960,'1.6');
CREATE TABLE ota_attempts (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  installation_id TEXT NOT NULL,
  version_code    INTEGER NOT NULL,
  event_type      TEXT NOT NULL,  
  error_reason    TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(1,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501012000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(2,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501015000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(3,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501237000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(4,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501290000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(5,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501295000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(6,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501297000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(7,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501299000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(8,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501460000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(9,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501467000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(10,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501695000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(11,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501702000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(12,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501772000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(13,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781501774000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(14,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781509056000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(15,'6af60fef-5aca-4bb2-98e7-eba5e1146f6f',5,'check',NULL,1781512523000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(16,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781530661000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(17,'e8b5e913-795d-4509-8e7a-a9608c30627f',5,'check',NULL,1781530744000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(18,'6af60fef-5aca-4bb2-98e7-eba5e1146f6f',5,'check',NULL,1781534885000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(19,'7bb0e422-fb10-452a-8005-e5e892fc1fc5',5,'check',NULL,1781535027000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(20,'10d3d67e-4f5a-4695-827b-8a5a727917bb',5,'check',NULL,1781554109000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(21,'6af60fef-5aca-4bb2-98e7-eba5e1146f6f',5,'check',NULL,1781556778000);
INSERT INTO "ota_attempts" ("id","installation_id","version_code","event_type","error_reason","created_at") VALUES(22,'7bb0e422-fb10-452a-8005-e5e892fc1fc5',5,'check',NULL,1781559648000);
CREATE TABLE installations (
  installation_id   TEXT PRIMARY KEY,
  version_code      INTEGER NOT NULL,
  version_name      TEXT NOT NULL,
  android_version   TEXT,
  architecture      TEXT,
  language          TEXT,
  manufacturer      TEXT,
  model             TEXT,
  theme             TEXT,
  release_channel   TEXT NOT NULL DEFAULT 'stable',
  first_seen_at     INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  last_seen_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  is_active         INTEGER NOT NULL DEFAULT 1
);
INSERT INTO "installations" ("installation_id","version_code","version_name","android_version","architecture","language","manufacturer","model","theme","release_channel","first_seen_at","last_seen_at","is_active") VALUES('e8b5e913-795d-4509-8e7a-a9608c30627f',1,'1.0','15','aarch64','pt','samsung','SM-A145M',NULL,'stable',1781486941522,1781542146105,1);
CREATE TABLE analytics_events (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  installation_id TEXT NOT NULL,
  event_name      TEXT NOT NULL,
  properties      TEXT NOT NULL DEFAULT '{}',  
  version_code    INTEGER,
  session_id      TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(1,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781317932948);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(2,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781318062720);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(3,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_submitted','{}',1,NULL,1781318071651);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(4,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781318298144);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(5,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781318308476);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(6,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_submitted','{}',1,NULL,1781318322145);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(7,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781372769253);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(8,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781372784105);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(9,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_submitted','{}',1,NULL,1781372797582);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(10,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781394456698);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(11,'7bb0e422-fb10-452a-8005-e5e892fc1fc5','app_opened','{}',1,NULL,1781394579946);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(12,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781411416273);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(13,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781411495155);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(14,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_submitted','{}',1,NULL,1781411506378);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(15,'7bb0e422-fb10-452a-8005-e5e892fc1fc5','feedback_opened','{}',1,NULL,1781394599662);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(16,'7bb0e422-fb10-452a-8005-e5e892fc1fc5','feedback_submitted','{}',1,NULL,1781394616085);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(17,'7bb0e422-fb10-452a-8005-e5e892fc1fc5','app_opened','{}',1,NULL,1781472254581);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(18,'6af60fef-5aca-4bb2-98e7-eba5e1146f6f','app_opened','{}',1,NULL,1781475917872);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(19,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781476873182);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(20,'10d3d67e-4f5a-4695-827b-8a5a727917bb','app_opened','{}',1,NULL,1781476997793);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(21,'10d3d67e-4f5a-4695-827b-8a5a727917bb','app_opened','{}',1,NULL,1781477048567);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(22,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781476885238);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(23,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781481076156);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(24,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781481086937);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(25,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781481164527);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(26,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781481193556);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(27,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781481534634);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(28,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781481642870);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(29,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781481663557);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(30,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781481684958);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(31,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781483104968);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(32,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781483153849);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(33,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781483508772);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(34,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781484272047);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(35,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781485082188);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(36,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781485497650);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(37,'4fc0650b-f94b-490f-8cb6-586f1f04783e','app_opened','{}',1,NULL,1781485509369);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(38,'4fc0650b-f94b-490f-8cb6-586f1f04783e','app_opened','{}',1,NULL,1781485674483);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(39,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781486940959);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(40,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781487410808);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(41,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781487519369);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(42,'e8b5e913-795d-4509-8e7a-a9608c30627f','feedback_opened','{}',1,NULL,1781487984189);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(43,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781488062386);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(44,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781488067055);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(45,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781489330941);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(46,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781489558559);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(47,'6af60fef-5aca-4bb2-98e7-eba5e1146f6f','feedback_opened','{}',1,NULL,1781475943477);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(48,'6af60fef-5aca-4bb2-98e7-eba5e1146f6f','feedback_submitted','{}',1,NULL,1781475969918);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(49,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781493572725);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(50,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781495520097);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(51,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781498353485);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(52,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781498361438);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(53,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781501002413);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(54,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781501683574);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(55,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781530733780);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(56,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781532705279);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(57,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781534128617);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(58,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781534822779);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(59,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781534822779);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(60,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781534844066);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(61,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781535157496);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(62,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781535799796);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(63,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781536531303);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(64,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781542076683);
INSERT INTO "analytics_events" ("id","installation_id","event_name","properties","version_code","session_id","created_at") VALUES(65,'e8b5e913-795d-4509-8e7a-a9608c30627f','app_opened','{}',1,NULL,1781542145231);
CREATE TABLE daily_metrics (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  date            TEXT NOT NULL,  
  metric_name     TEXT NOT NULL,
  metric_value    REAL NOT NULL DEFAULT 0,
  dimension_key   TEXT,           
  dimension_value TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  UNIQUE(date, metric_name, dimension_key, dimension_value)
);
CREATE TABLE performance_metrics (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  installation_id TEXT NOT NULL,
  metric_type     TEXT NOT NULL,  
  value_ms        REAL NOT NULL,
  screen          TEXT,
  version_code    INTEGER,
  android_version TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);
CREATE TABLE feedback (
  id              TEXT PRIMARY KEY,  
  installation_id TEXT NOT NULL,
  rating          INTEGER,           
  category        TEXT NOT NULL,     
  message         TEXT NOT NULL,
  include_tech    INTEGER NOT NULL DEFAULT 0,
  tech_data       TEXT,              
  version_code    INTEGER,
  android_version TEXT,
  model           TEXT,
  language        TEXT,
  theme           TEXT,
  source_screen   TEXT,
  status          TEXT NOT NULL DEFAULT 'new',  
  priority        TEXT NOT NULL DEFAULT 'normal',
  admin_notes     TEXT,
  tags            TEXT NOT NULL DEFAULT '[]',   
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  updated_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
, screenshot_base64 TEXT);
INSERT INTO "feedback" ("id","installation_id","rating","category","message","include_tech","tech_data","version_code","android_version","model","language","theme","source_screen","status","priority","admin_notes","tags","created_at","updated_at","screenshot_base64") VALUES('76d62b8c-dffe-4c3e-8e0c-0a3454e28b53','123e4567-e89b-12d3-a456-426614174000',NULL,'suggestion','Test message',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'new','normal',NULL,'[]',1781318086000,1781318086000,NULL);
INSERT INTO "feedback" ("id","installation_id","rating","category","message","include_tech","tech_data","version_code","android_version","model","language","theme","source_screen","status","priority","admin_notes","tags","created_at","updated_at","screenshot_base64") VALUES('613f3eea-d2b7-4646-bc78-006ed316ef0f','e8b5e913-795d-4509-8e7a-a9608c30627f',5,'other','tp',1,'{"versionCode":1,"androidVersion":"15","model":"samsung SM-A145M","language":"pt","sourceScreen":"more_screen"}',1,'15','samsung SM-A145M','pt',NULL,'more_screen','new','normal',NULL,'[]',1781318321000,1781318321000,NULL);
INSERT INTO "feedback" ("id","installation_id","rating","category","message","include_tech","tech_data","version_code","android_version","model","language","theme","source_screen","status","priority","admin_notes","tags","created_at","updated_at","screenshot_base64") VALUES('7dc709b2-edd4-49e5-acb6-6b045f825977','e8b5e913-795d-4509-8e7a-a9608c30627f',5,'compliment','top',1,'{"versionCode":1,"androidVersion":"15","model":"samsung SM-A145M","language":"pt","sourceScreen":"more_screen"}',1,'15','samsung SM-A145M','pt',NULL,'more_screen','new','normal',NULL,'[]',1781372798000,1781372798000,NULL);
INSERT INTO "feedback" ("id","installation_id","rating","category","message","include_tech","tech_data","version_code","android_version","model","language","theme","source_screen","status","priority","admin_notes","tags","created_at","updated_at","screenshot_base64") VALUES('de94f403-cc36-4081-bf5f-fd8d864d242c','7bb0e422-fb10-452a-8005-e5e892fc1fc5',5,'suggestion','teste celular robs',1,'{"versionCode":1,"androidVersion":"15","model":"realme RMX3760","language":"pt","sourceScreen":"more_screen"}',1,'15','realme RMX3760','pt',NULL,'more_screen','new','normal',NULL,'[]',1781394616000,1781394616000,NULL);
INSERT INTO "feedback" ("id","installation_id","rating","category","message","include_tech","tech_data","version_code","android_version","model","language","theme","source_screen","status","priority","admin_notes","tags","created_at","updated_at","screenshot_base64") VALUES('ac0c068e-9f78-4cf7-8a1c-c70d4df9b735','e8b5e913-795d-4509-8e7a-a9608c30627f',5,'other','top',1,'{"versionCode":1,"androidVersion":"15","model":"samsung SM-A145M","language":"pt","sourceScreen":"more_screen"}',1,'15','samsung SM-A145M','pt',NULL,'more_screen','new','normal',NULL,'[]',1781411506000,1781411506000,NULL);
INSERT INTO "feedback" ("id","installation_id","rating","category","message","include_tech","tech_data","version_code","android_version","model","language","theme","source_screen","status","priority","admin_notes","tags","created_at","updated_at","screenshot_base64") VALUES('19d9c611-96a7-4dd5-b8ed-1a59496803d5','6af60fef-5aca-4bb2-98e7-eba5e1146f6f',5,'compliment',replace('aplicativo maravilhoso \nfacilita mtooo a minha vida','\n',char(10)),0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'new','normal',NULL,'[]',1781475969000,1781475969000,NULL);
INSERT INTO "feedback" ("id","installation_id","rating","category","message","include_tech","tech_data","version_code","android_version","model","language","theme","source_screen","status","priority","admin_notes","tags","created_at","updated_at","screenshot_base64") VALUES('80e89bf5-7e7a-469b-89a8-9b0d0e0613d3','e8b5e913-795d-4509-8e7a-a9608c30627f',5,'suggestion','Teste Ph',1,'{"versionCode":1,"androidVersion":"15","model":"samsung SM-A145M","language":"pt","sourceScreen":"smart_prompt"}',1,'15','samsung SM-A145M','pt',NULL,'smart_prompt','new','normal',NULL,'[]',1781484291000,1781484291000,NULL);
CREATE TABLE feedback_attachments (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  feedback_id     TEXT NOT NULL REFERENCES feedback(id) ON DELETE CASCADE,
  r2_key          TEXT NOT NULL,    
  file_size_bytes INTEGER,
  mime_type       TEXT,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);
CREATE TABLE crash_reports (
  id              TEXT PRIMARY KEY,  
  installation_id TEXT NOT NULL,
  error_type      TEXT NOT NULL,
  error_message   TEXT NOT NULL,     
  screen          TEXT,
  version_code    INTEGER,
  android_version TEXT,
  resolved        INTEGER NOT NULL DEFAULT 0,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);
CREATE TABLE admin_users (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  username        TEXT NOT NULL UNIQUE,
  password_hash   TEXT NOT NULL,   
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  last_login_at   INTEGER
);
INSERT INTO "admin_users" ("id","username","password_hash","created_at","last_login_at") VALUES(1,'admin','pbkdf2:sha256:100000:edb2d822bb7535900cccc2905fc44145:2a00d5ebdb714eeeeb68df44468378299b2c599dec9d4efceea0638f2464fc3c',1781311880000,1781559883156);
CREATE TABLE admin_sessions (
  id              TEXT PRIMARY KEY,  
  admin_user_id   INTEGER NOT NULL REFERENCES admin_users(id),
  expires_at      INTEGER NOT NULL,
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
  ip_hash         TEXT              
);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('a1677cbe6862337a6821f224c723095ea2418a946e67261e4d4c4647c1cbeed1',1,1781340959538,1781312159000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('bed4ac314d593e33a3e37d5e87b22ff266b22ce0fa00d9c32a41daba8f199d89',1,1781341065945,1781312266000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('8727b581ad96957afb0aa6fd8090702344ac2a4caf9235cdeff328f212cc54a2',1,1781424265888,1781395465000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('261b33ba60038395a83f964c082bb193d0c9d434152ce42104a47b933ca115fd',1,1781505616807,1781476816000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('b81e5396f13a0d89eaa40f18a8dfd6f601a414bab445a13899bbca2424b27e45',1,1781561091024,1781532291000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('6ecca1498b1cdb5c361aa983e7957b96b54c2b34139a477e74cacf92fdf66abc',1,1781571425522,1781542625000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('d3863e059edd2af57556f1d34507f28f6a09d63999cacbe328d50270d5d89c26',1,1781571456092,1781542656000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('f8f673eddaf58a02277a7bef22ec887312c9c420dabe788430e6a5a6348eeb92',1,1781571536504,1781542736000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('c308f631d9941d6c2cd7a03dece6e15fdd3da8e03ef8f78923e46a0cdc8896e0',1,1781571724357,1781542924000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('91f75d072fc73e52a1cecee07497ad1e19699b0050c462230028a1e927dc4402',1,1781577252759,1781548452000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('e4f5f0bbaabdbb572676a442dfcd2888c3f7ff5960add092394857897eee9b20',1,1781586683029,1781557883000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('50df430cb4e8f73fef06bb3b9d2299cf3db5ff05954485a8f61908575bab236b',1,1781588596652,1781559796000,NULL);
INSERT INTO "admin_sessions" ("id","admin_user_id","expires_at","created_at","ip_hash") VALUES('373099e42ca60ba716e1abf3c7282d4220d4bf23349e6cd7262db82621d27c86',1,1781588683014,1781559883000,NULL);
CREATE TABLE audit_logs (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  admin_user_id   INTEGER,
  action          TEXT NOT NULL,
  target_type     TEXT,
  target_id       TEXT,
  details         TEXT,            
  created_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(1,1,'login',NULL,NULL,'{"username":"admin"}',1781312159000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(2,1,'login',NULL,NULL,'{"username":"admin"}',1781312266000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(3,1,'setting_updated','system_setting','active_icon_mode','{"key":"active_icon_mode","value":"force_copa"}',1781313664000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(4,1,'setting_updated','system_setting','active_icon_mode','{"key":"active_icon_mode","value":"force_padrao"}',1781313679000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(5,1,'setting_updated','system_setting','active_icon_mode','{"key":"active_icon_mode","value":"force_copa"}',1781313685000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(6,1,'setting_updated','system_setting','telemetry_max_queue_size','{"key":"telemetry_max_queue_size","value":"999999"}',1781314135000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(7,1,'login',NULL,NULL,'{"username":"admin"}',1781395466000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(8,1,'setting_updated','system_setting','maintenance_mode','{"key":"maintenance_mode","value":"true"}',1781411406000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(9,1,'setting_updated','system_setting','maintenance_mode','{"key":"maintenance_mode","value":"false"}',1781411407000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(10,1,'setting_updated','system_setting','maintenance_mode','{"key":"maintenance_mode","value":"true"}',1781411563000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(11,1,'login',NULL,NULL,'{"username":"admin"}',1781476817000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(12,1,'setting_updated','system_setting','active_icon_mode','{"key":"active_icon_mode","value":"force_padrao"}',1781476857000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(13,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781479417000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(14,1,'version_created_github','app_version','2',NULL,1781481061000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(15,1,'version_updated','app_version','2','{"mandatory":true}',1781481067000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(16,1,'version_created_github','app_version','3',NULL,1781481583000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(17,1,'version_updated','app_version','3','{"mandatory":true}',1781481586000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(18,1,'version_updated','app_version','3','{"status":"active"}',1781482416000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(19,1,'version_updated','app_version','3','{"rolloutPercentage":100}',1781482810000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(20,1,'version_created_github','app_version','5',NULL,1781483045000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(21,1,'version_updated','app_version','3','{"status":"paused"}',1781483049000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(22,1,'version_updated','app_version','5','{"status":"active"}',1781483050000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(23,1,'version_updated','app_version','5','{"rolloutPercentage":100}',1781483052000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(24,1,'version_updated','app_version','5','{"status":"paused"}',1781483054000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(25,1,'version_updated','app_version','5','{"status":"paused"}',1781483055000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(26,1,'version_updated','app_version','5','{"status":"active"}',1781483057000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(27,1,'version_updated','app_version','5','{"mandatory":true}',1781483138000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(28,1,'version_updated','app_version','5','{"mandatory":false}',1781483139000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(29,1,'version_updated','app_version','5','{"mandatory":true}',1781483299000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(30,1,'version_updated','app_version','5','{"mandatory":true}',1781483300000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(31,1,'setting_updated','system_setting','active_icon_mode','{"key":"active_icon_mode","value":"force_copa"}',1781487885000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(32,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781487969000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(33,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781487970000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(34,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781487970000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(35,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781487970000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(36,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781487971000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(37,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781487974000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(38,1,'tester_updated','tester','1','{"display_name":"Rafaela Tasso"}',1781487995000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(39,1,'setting_updated','system_setting','maintenance_mode','{"key":"maintenance_mode","value":"false"}',1781496264000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(40,1,'setting_updated','system_setting','maintenance_mode','{"key":"maintenance_mode","value":"true"}',1781496265000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(41,1,'setting_updated','system_setting','maintenance_mode','{"key":"maintenance_mode","value":"false"}',1781496267000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(42,1,'setting_updated','system_setting','active_icon_mode','{"key":"active_icon_mode","value":"force_padrao"}',1781496285000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(43,1,'version_updated','app_version','5','{"status":"paused"}',1781499075000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(44,1,'version_created_github','app_version','1',NULL,1781500484000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(45,1,'version_updated','app_version','3','{"status":"active"}',1781500547000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(46,1,'version_updated','app_version','5','{"status":"active"}',1781500551000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(47,1,'version_updated','app_version','3','{"status":"paused"}',1781500554000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(48,1,'version_created_github','app_version','7',NULL,1781501276000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(49,1,'version_updated','app_version','7','{"status":"active"}',1781501284000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(50,1,'version_updated','app_version','7','{"mandatory":true}',1781501287000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(51,1,'login',NULL,NULL,'{"username":"admin"}',1781532291000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(52,1,'login',NULL,NULL,'{"username":"admin"}',1781542625000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(53,1,'login',NULL,NULL,'{"username":"admin"}',1781542656000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(54,1,'login',NULL,NULL,'{"username":"admin"}',1781542736000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(55,1,'login',NULL,NULL,'{"username":"admin"}',1781542924000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(56,1,'login',NULL,NULL,'{"username":"admin"}',1781548453000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(57,1,'login',NULL,NULL,'{"username":"admin"}',1781557883000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(58,1,'login',NULL,NULL,'{"username":"admin"}',1781559797000);
INSERT INTO "audit_logs" ("id","admin_user_id","action","target_type","target_id","details","created_at") VALUES(59,1,'login',NULL,NULL,'{"username":"admin"}',1781559883000);
CREATE TABLE system_settings (
  key             TEXT PRIMARY KEY,
  value           TEXT NOT NULL,
  description     TEXT,
  updated_at      INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('ota_check_interval_hours','6','Horas entre verificações OTA no app',1781310830000);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('ota_modal_cooldown_hours','24','Horas entre exibições do modal OTA',1781310830000);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('feedback_contextual_enabled','true','Habilitar solicitação contextual de feedback',1781310830000);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('feedback_contextual_cooldown_days','14','Dias entre solicitações contextuais',1781310830000);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('maintenance_mode','false','Modo de manutenção ativo',1781496266855);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('maintenance_message','','Mensagem exibida em modo de manutenção',1781310830000);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('telemetry_max_queue_size','999999','Tamanho máximo da fila de telemetria local',1781314135146);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('min_supported_version_code','1','Versão mínima suportada (abaixo = atualização obrigatória',1781310830000);
INSERT INTO "system_settings" ("key","value","description","updated_at") VALUES('active_icon_mode','force_padrao','Ícone ativo do app: auto, force_copa, force_padrao',1781496285168);
CREATE TABLE push_tokens (
    token TEXT PRIMARY KEY,
    user_id TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE testers (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  nickname TEXT,
  avatar_key TEXT,
  badge_key TEXT,
  message TEXT,
  participation_version TEXT,
  participation_period TEXT,
  display_order INTEGER DEFAULT 0,
  is_active INTEGER DEFAULT 1,
  is_featured INTEGER DEFAULT 0,
  consent_confirmed INTEGER DEFAULT 0,
  published_at INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
INSERT INTO "testers" ("id","display_name","nickname","avatar_key","badge_key","message","participation_version","participation_period","display_order","is_active","is_featured","consent_confirmed","published_at","created_at","updated_at") VALUES('1','Rafaela Tasso',NULL,NULL,'initial_tester','Primeira a dar feedback e utilizar o app. Agradecimento especial.','1.0',NULL,0,1,1,1,1781487994941,1781479300000,1781487994941);
CREATE TABLE monetization_products (
  id TEXT PRIMARY KEY,
  play_product_id TEXT NOT NULL,
  product_type TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  features TEXT, 
  display_order INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);
CREATE TABLE monetization_purchases (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  installation_id TEXT,
  platform TEXT NOT NULL,
  product_id TEXT NOT NULL,
  base_plan_id TEXT,
  offer_id TEXT,
  purchase_token_hash TEXT NOT NULL UNIQUE,
  order_id TEXT,
  purchase_state TEXT NOT NULL,
  purchased_at INTEGER NOT NULL,
  expires_at INTEGER,
  auto_renewing BOOLEAN DEFAULT 0,
  country_code TEXT,
  currency_code TEXT,
  amount REAL,
  environment TEXT NOT NULL DEFAULT 'PRODUCTION',
  last_verified_at INTEGER,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  FOREIGN KEY (product_id) REFERENCES monetization_products(id)
);
CREATE TABLE monetization_campaigns (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  starts_at INTEGER,
  ends_at INTEGER,
  target_audience TEXT,
  eligible_products TEXT, 
  features TEXT, 
  paywall_variant TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);
CREATE TABLE premium_codes (
  id TEXT PRIMARY KEY,
  code_hash TEXT NOT NULL UNIQUE,
  code_prefix TEXT NOT NULL,
  internal_name TEXT NOT NULL,
  description TEXT,
  campaign_id TEXT,
  benefit_type TEXT NOT NULL,
  features TEXT, 
  duration_type TEXT NOT NULL,
  duration_value INTEGER,
  valid_from INTEGER,
  valid_until INTEGER,
  max_redemptions INTEGER NOT NULL DEFAULT 1,
  redemption_count INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_by TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  revoked_at INTEGER,
  revoked_by TEXT,
  revocation_reason TEXT,
  FOREIGN KEY (campaign_id) REFERENCES monetization_campaigns(id)
);
INSERT INTO "premium_codes" ("id","code_hash","code_prefix","internal_name","description","campaign_id","benefit_type","features","duration_type","duration_value","valid_from","valid_until","max_redemptions","redemption_count","status","created_by","created_at","updated_at","revoked_at","revoked_by","revocation_reason") VALUES('07a26e65-45bf-4c03-af7f-3f6f7ad69794','Teste-PB22KY','Teste','teste',NULL,NULL,'PREMIUM_ALL',NULL,'DAYS',1,1781500451,NULL,1,0,'ACTIVE',NULL,1781500451,1781500451,NULL,NULL,NULL);
INSERT INTO "premium_codes" ("id","code_hash","code_prefix","internal_name","description","campaign_id","benefit_type","features","duration_type","duration_value","valid_from","valid_until","max_redemptions","redemption_count","status","created_by","created_at","updated_at","revoked_at","revoked_by","revocation_reason") VALUES('42813ea7-301d-46ff-ba2f-21197ae36b74','QF-S8G0XE','QF','teste',NULL,NULL,'PREMIUM_ALL',NULL,'DAYS',1,1781536880,NULL,1,0,'ACTIVE',NULL,1781536880,1781536880,NULL,NULL,NULL);
INSERT INTO "premium_codes" ("id","code_hash","code_prefix","internal_name","description","campaign_id","benefit_type","features","duration_type","duration_value","valid_from","valid_until","max_redemptions","redemption_count","status","created_by","created_at","updated_at","revoked_at","revoked_by","revocation_reason") VALUES('f1e5bd02-9cf7-4e1e-856d-46bb202c8e3a','5b0cc1f16cc06606e9582e3cda9eecb8a4f19f708dc3ab279d060cfb14fffbd8','QF','teste',NULL,NULL,'MENSAL',NULL,'MONTHS',1,1781542951,NULL,1,0,'ACTIVE',NULL,1781542951,1781542951,NULL,NULL,NULL);
INSERT INTO "premium_codes" ("id","code_hash","code_prefix","internal_name","description","campaign_id","benefit_type","features","duration_type","duration_value","valid_from","valid_until","max_redemptions","redemption_count","status","created_by","created_at","updated_at","revoked_at","revoked_by","revocation_reason") VALUES('b8889db5-4042-4fd0-ae71-57b661107147','6c6c44aa84e46a3a2e2571e033e9aeddc1acb5b635eeddba41a49353e4b01ffd','teste','teste',NULL,NULL,'PERSONALIZADO',NULL,'DAYS',1,1781543011,NULL,1,0,'ACTIVE',NULL,1781543011,1781543011,NULL,NULL,NULL);
INSERT INTO "premium_codes" ("id","code_hash","code_prefix","internal_name","description","campaign_id","benefit_type","features","duration_type","duration_value","valid_from","valid_until","max_redemptions","redemption_count","status","created_by","created_at","updated_at","revoked_at","revoked_by","revocation_reason") VALUES('21b08690-f655-4805-a182-22a2a00ce05e','6af72f11082939aaf54616a4bb3778a680a217c10a961502acc89be87b5a864e','TEXTE','TESTE',NULL,NULL,'MENSAL',NULL,'MONTHS',1,1781543333,NULL,1,1,'ACTIVE',NULL,1781543333,1781543347,NULL,NULL,NULL);
CREATE TABLE monetization_entitlements (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  type TEXT NOT NULL,
  source TEXT NOT NULL,
  product_id TEXT,
  campaign_id TEXT,
  code_id TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  features TEXT, 
  starts_at INTEGER,
  expires_at INTEGER,
  offline_valid_until INTEGER,
  last_synced_at INTEGER,
  granted_by_admin BOOLEAN DEFAULT 0,
  grant_reason TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  FOREIGN KEY (product_id) REFERENCES monetization_products(id),
  FOREIGN KEY (campaign_id) REFERENCES monetization_campaigns(id),
  FOREIGN KEY (code_id) REFERENCES premium_codes(id)
);
INSERT INTO "monetization_entitlements" ("id","user_id","type","source","product_id","campaign_id","code_id","status","features","starts_at","expires_at","offline_valid_until","last_synced_at","granted_by_admin","grant_reason","created_at","updated_at") VALUES('1e346bb3-0ab4-4c71-8fa9-911b52f96517','3ea5d2b6afc1a2cd','PREMIUM_CODE','CODE',NULL,NULL,'21b08690-f655-4805-a182-22a2a00ce05e','ACTIVE',NULL,1781543347,1784135347,NULL,NULL,0,NULL,1781543348,1781543348);
CREATE TABLE premium_code_redemptions (
  id TEXT PRIMARY KEY,
  code_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  installation_id TEXT,
  entitlement_id TEXT NOT NULL,
  redeemed_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  benefit_starts_at INTEGER,
  benefit_expires_at INTEGER,
  ip_hash TEXT,
  app_version TEXT,
  platform TEXT,
  status TEXT NOT NULL DEFAULT 'SUCCESS',
  FOREIGN KEY (code_id) REFERENCES premium_codes(id),
  FOREIGN KEY (entitlement_id) REFERENCES monetization_entitlements(id)
);
INSERT INTO "premium_code_redemptions" ("id","code_id","user_id","installation_id","entitlement_id","redeemed_at","benefit_starts_at","benefit_expires_at","ip_hash","app_version","platform","status") VALUES('131f6fef-1207-4c9d-95fe-b259681fa86f','21b08690-f655-4805-a182-22a2a00ce05e','3ea5d2b6afc1a2cd','3ea5d2b6afc1a2cd','1e346bb3-0ab4-4c71-8fa9-911b52f96517',1781543348,NULL,NULL,NULL,'1.0','ANDROID','SUCCESS');
CREATE TABLE monetization_audit_logs (
  id TEXT PRIMARY KEY,
  administrator_id TEXT NOT NULL,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  previous_state TEXT, 
  new_state TEXT, 
  reason TEXT,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);
DELETE FROM sqlite_sequence;
INSERT INTO "sqlite_sequence" ("name","seq") VALUES('d1_migrations',7);
INSERT INTO "sqlite_sequence" ("name","seq") VALUES('admin_users',1);
INSERT INTO "sqlite_sequence" ("name","seq") VALUES('audit_logs',59);
INSERT INTO "sqlite_sequence" ("name","seq") VALUES('analytics_events',65);
INSERT INTO "sqlite_sequence" ("name","seq") VALUES('app_versions',5);
INSERT INTO "sqlite_sequence" ("name","seq") VALUES('ota_attempts',22);
CREATE INDEX idx_versions_channel_status
  ON app_versions(release_channel, status);
CREATE INDEX idx_versions_version_code
  ON app_versions(version_code);
CREATE INDEX idx_ota_installation_id ON ota_attempts(installation_id);
CREATE INDEX idx_ota_version_code ON ota_attempts(version_code);
CREATE INDEX idx_ota_event_type ON ota_attempts(event_type);
CREATE INDEX idx_ota_created_at ON ota_attempts(created_at);
CREATE INDEX idx_installations_version_code ON installations(version_code);
CREATE INDEX idx_installations_last_seen ON installations(last_seen_at);
CREATE INDEX idx_installations_channel ON installations(release_channel);
CREATE INDEX idx_analytics_installation_id ON analytics_events(installation_id);
CREATE INDEX idx_analytics_event_name ON analytics_events(event_name);
CREATE INDEX idx_analytics_created_at ON analytics_events(created_at);
CREATE INDEX idx_analytics_version_code ON analytics_events(version_code);
CREATE INDEX idx_daily_metrics_date ON daily_metrics(date);
CREATE INDEX idx_daily_metrics_name ON daily_metrics(metric_name);
CREATE INDEX idx_perf_type ON performance_metrics(metric_type);
CREATE INDEX idx_perf_created_at ON performance_metrics(created_at);
CREATE INDEX idx_perf_version_code ON performance_metrics(version_code);
CREATE INDEX idx_feedback_status ON feedback(status);
CREATE INDEX idx_feedback_category ON feedback(category);
CREATE INDEX idx_feedback_rating ON feedback(rating);
CREATE INDEX idx_feedback_created_at ON feedback(created_at);
CREATE INDEX idx_feedback_version_code ON feedback(version_code);
CREATE INDEX idx_crash_error_type ON crash_reports(error_type);
CREATE INDEX idx_crash_version_code ON crash_reports(version_code);
CREATE INDEX idx_crash_created_at ON crash_reports(created_at);
CREATE INDEX idx_sessions_expires_at ON admin_sessions(expires_at);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_admin_user_id ON audit_logs(admin_user_id);
CREATE INDEX idx_push_tokens_user_id ON push_tokens(user_id);
CREATE INDEX idx_testers_active_order ON testers(is_active, display_order);
CREATE INDEX idx_testers_published ON testers(published_at);
CREATE UNIQUE INDEX idx_daily_metrics_unique
  ON daily_metrics(date, metric_name, COALESCE(dimension_key, ''), COALESCE(dimension_value, ''));

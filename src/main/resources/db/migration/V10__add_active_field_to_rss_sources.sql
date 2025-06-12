-- 为 rss_sources 表添加 active 字段
-- 该字段用于控制RSS源是否启用/禁用

ALTER TABLE rss_sources ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- 为现有数据设置默认值
UPDATE rss_sources SET active = TRUE WHERE active IS NULL;

-- 添加注释
COMMENT ON COLUMN rss_sources.active IS 'RSS源是否启用，true为启用，false为禁用'; 
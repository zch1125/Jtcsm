-- ========================================================
-- 修复：ai_generated_recipe 表缺少 updated_at 列
-- BaseEntity 统一要求该字段
-- ========================================================
ALTER TABLE ai_generated_recipe
ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 确认修复
DESC ai_generated_recipe;

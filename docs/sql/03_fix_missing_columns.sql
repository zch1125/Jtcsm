-- ========================================================
-- 完整修复：BaseEntity 缺字段统一修复脚本
-- 所有 extends BaseEntity 的表必须有 updated_at 列
-- ========================================================

-- 1. ai_generated_recipe（AI生成记录）
ALTER TABLE ai_generated_recipe
ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
AFTER `created_at`;



-- 4. search_history（搜索历史）
ALTER TABLE search_history
ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
AFTER `created_at`;

-- 5. user_favorite（收藏表）
ALTER TABLE user_favorite
ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
AFTER `created_at`;


-- 验证所有 BaseEntity 子表都已有 updated_at
SELECT TABLE_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'jtcsm'
  AND TABLE_NAME IN (
  )
  AND COLUMN_NAME = 'updated_at'
ORDER BY TABLE_NAME;

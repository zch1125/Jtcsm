-- ========================================================
-- 完整修复：BaseEntity 缺字段统一修复脚本
-- 所有 extends BaseEntity 的表必须有 updated_at 列
-- ========================================================

-- 1. ai_generated_recipe（AI生成记录）
ALTER TABLE ai_generated_recipe
ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
AFTER `created_at`;

-- 2. membership_plan（会员套餐）
ALTER TABLE membership_plan
ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
AFTER `created_at`;

-- 3. payment_record（支付流水）
ALTER TABLE payment_record
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

-- 6. user_membership（用户会员表）
ALTER TABLE user_membership
ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
AFTER `created_at`;

-- 验证所有 BaseEntity 子表都已有 updated_at
SELECT TABLE_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'jtcsm'
  AND TABLE_NAME IN (
    'user', 'recipe', 'order', 'user_preference',
    'ai_generated_recipe', 'membership_plan',
    'payment_record', 'search_history',
    'user_favorite', 'user_membership'
  )
  AND COLUMN_NAME = 'updated_at'
ORDER BY TABLE_NAME;

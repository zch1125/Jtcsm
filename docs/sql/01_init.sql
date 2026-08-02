-- ========================================================
-- 浠婂ぉ鍚冧粈涔?路 寤鸿〃鍒濆鍖栬剼鏈?-- 鏁版嵁搴擄細MySQL 8.x / InnoDB / utf8mb4
-- 鐗堟湰锛歷1.0
-- ========================================================

CREATE DATABASE IF NOT EXISTS jtcsm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jtcsm;

-- ========================================================
-- 1. 鐢ㄦ埛琛?-- ========================================================
CREATE TABLE `user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '鐢ㄦ埛ID',
    `openid`          VARCHAR(64)  NOT NULL                 COMMENT '寰俊OpenID',
    `nickname`        VARCHAR(64)  DEFAULT NULL             COMMENT '鏄电О',
    `avatar`          VARCHAR(512) DEFAULT NULL             COMMENT '澶村儚URL',
    `phone`           VARCHAR(20)  DEFAULT NULL             COMMENT '鎵嬫満鍙?,
    `gender`          TINYINT      DEFAULT 0                COMMENT '0鏈煡 1鐢?2濂?,
    `status`          TINYINT      DEFAULT 1                COMMENT '0绂佺敤 1姝ｅ父',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛琛?;

-- ========================================================
-- 2. 鐢ㄦ埛鍋忓ソ琛?-- ========================================================
CREATE TABLE `user_preference` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL                 COMMENT '鐢ㄦ埛ID',
    `taste`       VARCHAR(128) DEFAULT NULL             COMMENT '鍙ｅ懗鍋忓ソ锛堥夯杈?娓呮贰/閰哥敎绛夛級',
    `taboo`       VARCHAR(256) DEFAULT NULL             COMMENT '蹇屽彛椋熸潗锛圝SON鏁扮粍锛?,
    `cuisine`     VARCHAR(128) DEFAULT NULL             COMMENT '鍋忓ソ鑿滅郴锛圝SON鏁扮粍锛?,
    `difficulty`  VARCHAR(32)  DEFAULT NULL             COMMENT '闅惧害鍋忓ソ锛堢畝鍗?鏅€?鍥伴毦锛?,
    `cook_method` VARCHAR(128) DEFAULT NULL             COMMENT '鐑归オ鏂瑰紡鍋忓ソ锛圝SON鏁扮粍锛?,
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛鍋忓ソ琛?;

-- ========================================================
-- 3. 鑿滆氨涓昏〃
-- ========================================================
CREATE TABLE `recipe` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(128) NOT NULL                COMMENT '鑿滃悕',
    `cover_image`    VARCHAR(512) DEFAULT NULL             COMMENT '灏侀潰鍥綰RL',
    `description`    VARCHAR(512) DEFAULT NULL             COMMENT '绠€浠?,
    `cuisine`        VARCHAR(64)  DEFAULT NULL             COMMENT '鑿滅郴',
    `difficulty`     VARCHAR(16)  DEFAULT NULL             COMMENT '闅惧害',
    `cook_method`    VARCHAR(64)  DEFAULT NULL             COMMENT '鐑归オ鏂瑰紡',
    `cook_time`      INT          DEFAULT NULL             COMMENT '鐑归オ鏃堕棿锛堝垎閽燂級',
    `calories`       INT          DEFAULT NULL             COMMENT '鐑噺锛堝崈鍗★級',
    `view_count`     INT          DEFAULT 0                COMMENT '娴忚閲?,
    `favorite_count` INT          DEFAULT 0                COMMENT '鏀惰棌鏁?,
    `status`         TINYINT      DEFAULT 1                COMMENT '0涓嬫灦 1涓婃灦',
    `source`         VARCHAR(32)  DEFAULT 'system'         COMMENT 'system/user/import',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_cuisine` (`cuisine`),
    INDEX `idx_difficulty` (`difficulty`),
    INDEX `idx_cook_method` (`cook_method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鑿滆氨涓昏〃';

-- ========================================================
-- 4. 鐢ㄦ枡娓呭崟琛?-- ========================================================
CREATE TABLE `recipe_ingredient` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `recipe_id`     BIGINT      NOT NULL                COMMENT '鑿滆氨ID',
    `ingredient_id` BIGINT      DEFAULT NULL             COMMENT '椋熸潗ID锛堝叧鑱攊ngredient琛級',
    `name`          VARCHAR(64) NOT NULL                COMMENT '椋熸潗鍚嶇О',
    `amount`        VARCHAR(32) DEFAULT NULL             COMMENT '鐢ㄩ噺',
    `sort_order`    INT         DEFAULT 0               COMMENT '鎺掑簭',
    `created_at`    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ枡娓呭崟琛?;

-- ========================================================
-- 5. 鏁欑▼姝ラ琛?-- ========================================================
CREATE TABLE `recipe_step` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `recipe_id`  BIGINT       NOT NULL                COMMENT '鑿滆氨ID',
    `step_no`    INT          NOT NULL                COMMENT '姝ラ搴忓彿',
    `content`    TEXT         NOT NULL                COMMENT '姝ラ璇存槑',
    `image`      VARCHAR(512) DEFAULT NULL             COMMENT '姝ラ閰嶅浘URL',
    `duration`   INT          DEFAULT NULL             COMMENT '棰勮鑰楁椂锛堝垎閽燂級',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_step_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鏁欑▼姝ラ琛?;

-- ========================================================
-- 6. 椋熸潗瀛楀吀琛?-- ========================================================
CREATE TABLE `ingredient` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(64)  NOT NULL                COMMENT '椋熸潗鍚?,
    `category`   VARCHAR(32)  DEFAULT NULL             COMMENT '鍒嗙被锛堣敩鑿?鑲夌被/娴烽矞/璋冩枡绛夛級',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='椋熸潗瀛楀吀琛?;

-- ========================================================
-- 7. 鏀惰棌琛?-- ========================================================
CREATE TABLE `user_favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `recipe_id`  BIGINT   NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
,
    `updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_recipe` (`user_id`, `recipe_id`),
    INDEX `idx_fav_user_id` (`user_id`),
    INDEX `idx_fav_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鏀惰棌琛?;

-- ========================================================
-- 8. 娴忚鍘嗗彶琛?-- ========================================================
CREATE TABLE `user_history` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `recipe_id`  BIGINT   NOT NULL,
    `viewed_at`  DATETIME DEFAULT CURRENT_TIMESTAMP    COMMENT '娴忚鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_history_user` (`user_id`),
    INDEX `idx_viewed_at` (`viewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='娴忚鍘嗗彶琛?;





-- ========================================================
-- 13. AI 鐢熸垚璁板綍琛?-- ========================================================
CREATE TABLE `ai_generated_recipe` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       DEFAULT NULL            COMMENT '鐢ㄦ埛ID',
    `mode`         VARCHAR(16)  DEFAULT NULL            COMMENT 'ingredients/name/creative',
    `input_content` VARCHAR(512) DEFAULT NULL            COMMENT '鐢ㄦ埛杈撳叆',
    `result_json`  JSON         DEFAULT NULL            COMMENT 'AI鐢熸垚缁撴灉',
    `rating`       TINYINT      DEFAULT NULL            COMMENT '鐢ㄦ埛璇勫垎 1-5',
    `feedback`     VARCHAR(512) DEFAULT NULL            COMMENT '鐢ㄦ埛鍙嶉',
    `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
,
    `updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI鐢熸垚璁板綍琛?;

-- ========================================================
-- 14. 鎼滅储鍘嗗彶琛?-- ========================================================
CREATE TABLE `search_history` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `keyword`    VARCHAR(128) NOT NULL              COMMENT '鎼滅储鍏抽敭璇?,
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_search_user_id` (`user_id`),
    INDEX `idx_search_created_at` (`created_at`)
,
`updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鎼滅储鍘嗗彶琛?;



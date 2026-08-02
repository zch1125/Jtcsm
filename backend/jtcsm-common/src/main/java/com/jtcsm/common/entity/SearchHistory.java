package com.jtcsm.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 搜索历史实体，对应表 search_history
 */
@TableName("search_history")
public class SearchHistory extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 搜索关键词 */
    private String keyword;

    // ==================== Getter / Setter ====================

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}

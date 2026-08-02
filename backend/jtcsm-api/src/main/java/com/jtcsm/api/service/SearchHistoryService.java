package com.jtcsm.api.service;

import com.jtcsm.common.entity.SearchHistory;

import java.util.List;

/**
 * 搜索历史服务接口
 */
public interface SearchHistoryService {

    /** 记录一次搜索 */
    void record(Long userId, String keyword);

    /** 获取最近搜索历史（最多 20 条） */
    List<SearchHistory> getRecent(Long userId);

    /** 清空搜索历史 */
    void clearAll(Long userId);
}

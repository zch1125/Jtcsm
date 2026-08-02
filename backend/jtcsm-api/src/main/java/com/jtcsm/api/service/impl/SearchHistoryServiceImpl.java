package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.SearchHistoryMapper;
import com.jtcsm.api.service.SearchHistoryService;
import com.jtcsm.common.entity.SearchHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索历史服务实现
*/
@Service
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SearchHistoryServiceImpl.class);
    /** 最近搜索历史最大条数 */
    private static final int MAX_RECENT = 20;

    @Autowired
    private SearchHistoryMapper searchHistoryMapper;

    @Override
    public void record(Long userId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        // 同一用户相同关键词，先删旧记录再插新记录（保持时间最新）
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getKeyword, keyword.trim()));
        SearchHistory sh = new SearchHistory();
        sh.setUserId(userId);
        sh.setKeyword(keyword.trim());
        searchHistoryMapper.insert(sh);
        // 如果历史记录超过 50 条，删除最早的
        Long total = searchHistoryMapper.selectCount(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId));
        if (total > 50) {
            List<SearchHistory> all = searchHistoryMapper.selectList(
                    new LambdaQueryWrapper<SearchHistory>()
                            .eq(SearchHistory::getUserId, userId)
                            .orderByDesc(SearchHistory::getCreateTime));
            if (all.size() > 50) {
                List<Long> deleteIds = all.subList(50, all.size()).stream()
                        .map(SearchHistory::getId)
                        .collect(Collectors.toList());
                searchHistoryMapper.deleteBatchIds(deleteIds);
                log.debug("清理搜索历史: userId={}, 删除了{}条旧记录", userId, deleteIds.size());
            }
        }
    }

    @Override
    public List<SearchHistory> getRecent(Long userId) {
        // 按创建时间倒序查询最近 MAX_RECENT 条记录
        return searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .orderByDesc(SearchHistory::getCreateTime)
                        .last("LIMIT " + MAX_RECENT));
    }

    @Override
    public void clearAll(Long userId) {
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId));
        log.info("清空搜索历史: userId={}", userId);
    }
}

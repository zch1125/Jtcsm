package com.jtcsm.api.service;

import com.jtcsm.common.dto.HistoryListVO;

import java.util.List;

/**
 * 浏览历史服务接口
 */
public interface HistoryService {

    /** 记录浏览历史 */
    void recordView(Long userId, Long recipeId);

    /** 分页查询浏览历史列表 */
    List<HistoryListVO> listHistory(Long userId, int page, int size);
}

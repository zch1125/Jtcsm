package com.jtcsm.api.controller;

import com.jtcsm.api.service.SearchHistoryService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.entity.SearchHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索历史控制器
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchHistoryController {

    @Autowired
    private SearchHistoryService searchHistoryService;

    /**
     * 获取最近搜索历史（最多 20 条）
     * GET /api/v1/search/history
     */
    @GetMapping("/history")
    public Result<List<SearchHistory>> getRecent() {
        Long userId = UserContext.getUserId();
        List<SearchHistory> list = searchHistoryService.getRecent(userId);
        return Result.ok(list);
    }

    /**
     * 清空当前用户的所有搜索历史
     * DELETE /api/v1/search/history
     */
    @DeleteMapping("/history")
    public Result<Void> clearAll() {
        Long userId = UserContext.getUserId();
        searchHistoryService.clearAll(userId);
        return Result.ok("已清空搜索历史", null);
    }
}

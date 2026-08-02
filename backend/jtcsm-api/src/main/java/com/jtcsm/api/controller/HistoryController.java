package com.jtcsm.api.controller;

import com.jtcsm.api.service.HistoryService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.HistoryListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 浏览历史控制器
 */
@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    /**
     * 分页查询菜谱浏览历史（按浏览时间倒序）
     * GET /api/v1/history/list?page=1&size=20
     */
    @GetMapping("/list")
    public Result<List<HistoryListVO>> listHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        List<HistoryListVO> list = historyService.listHistory(userId, page, size);
        return Result.ok(list);
    }
}

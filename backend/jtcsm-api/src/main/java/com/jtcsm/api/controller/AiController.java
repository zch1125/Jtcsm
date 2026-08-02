package com.jtcsm.api.controller;

import com.jtcsm.api.service.AiService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.AiFeedbackRequest;
import com.jtcsm.common.dto.AiGenerateRecordVO;
import com.jtcsm.common.dto.AiGenerateRequest;
import com.jtcsm.common.dto.AiGenerateResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 智能菜谱控制器 —— 菜谱生成、历史记录、收藏和反馈
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * AI 生成菜谱
     * POST /api/v1/ai/generate
     */
    @PostMapping("/generate")
    public Result<AiGenerateResponse> generate(@Valid @RequestBody AiGenerateRequest request) {
        Long userId = UserContext.getUserId();
        return Result.ok(aiService.generate(userId, request));
    }

    /**
     * AI 生成历史列表
     * GET /api/v1/ai/history?page=1&size=20
     */
    @GetMapping("/history")
    public Result<List<AiGenerateRecordVO>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiService.getHistory(UserContext.getUserId(), page, size));
    }

    /**
     * AI 生成历史详情
     * GET /api/v1/ai/history/{id}
     */
    @GetMapping("/history/{id}")
    public Result<AiGenerateRecordVO> historyDetail(@PathVariable Long id) {
        return Result.ok(aiService.getHistoryDetail(UserContext.getUserId(), id));
    }

    /**
     * 将 AI 生成记录保存到收藏
     * POST /api/v1/ai/history/{id}/save
     */
    @PostMapping("/history/{id}/save")
    public Result<Void> saveToFavorite(@PathVariable Long id) {
        aiService.saveToFavorite(UserContext.getUserId(), id);
        return Result.ok("已收藏", null);
    }

    /**
     * 提交 AI 生成反馈（评分 + 评价）
     * POST /api/v1/ai/feedback
     */
    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody AiFeedbackRequest request) {
        aiService.submitFeedback(UserContext.getUserId(), request);
        return Result.ok("反馈已提交", null);
    }
}

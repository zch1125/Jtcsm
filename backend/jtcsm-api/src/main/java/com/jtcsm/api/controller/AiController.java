package com.jtcsm.api.controller;

import com.jtcsm.api.service.AiService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.AiFeedbackRequest;
import com.jtcsm.common.dto.AiGenerateRecordVO;
import com.jtcsm.common.dto.AiGenerateRequest;
import com.jtcsm.common.dto.AiGenerateResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 智能菜谱控制器 —— 菜谱生成、历史记录、收藏和反馈
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Autowired
    private AiService aiService;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private TaskExecutor taskExecutor;

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
     * AI 流式生成菜谱（SSE：先输出推荐文字，最后下发菜谱卡片）
     * POST /api/v1/ai/generate/stream
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Valid @RequestBody AiGenerateRequest request) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(120_000L);
        taskExecutor.execute(() -> {
            try {
                aiService.generateStream(userId, request, emitter);
            } catch (Exception e) {
                log.warn("AI 流式生成异常: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"message\":\"AI 生成失败，请重试\"}"));
                } catch (Exception ignored) {
                    // 客户端可能已断开连接
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
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

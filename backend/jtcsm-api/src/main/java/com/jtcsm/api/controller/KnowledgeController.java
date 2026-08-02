package com.jtcsm.api.controller;

import com.jtcsm.api.service.HybridSearchService;
import com.jtcsm.api.service.KnowledgeBaseMdService;
import com.jtcsm.api.service.RecipeIndexService;
import com.jtcsm.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 食谱知识库控制器 —— 语义搜索与索引管理
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    @Autowired
    private HybridSearchService hybridSearchService;

    @Autowired
    private RecipeIndexService recipeIndexService;

    /**
     * 语义搜索食谱知识库
     * GET /api/v1/knowledge/search?q=番茄鸡蛋&size=10
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "10") int size) {
        if (query == null || query.trim().isEmpty()) {
            return Result.ok(List.of());
        }
        List<Map<String, Object>> results = hybridSearchService.hybridSearch(query.trim(), null);
        // 限制返回数量
        List<Map<String, Object>> limited = results.size() > size ? results.subList(0, size) : results;
        return Result.ok(limited);
    }

    /**
     * 手动触发食谱知识库全量同步
     * POST /api/v1/knowledge/sync
     */
    @PostMapping("/sync")
    public Result<String> sync() {
        try {
            long start = System.currentTimeMillis();
            recipeIndexService.initIndex();
            recipeIndexService.fullSync();
            knowledgeBaseMdService.initIndex();
            knowledgeBaseMdService.fullSync();
            long elapsed = System.currentTimeMillis() - start;
            log.info("知识库手动同步完成，耗时={}ms", elapsed);
            return Result.ok("全量同步完成，耗时 " + elapsed + "ms");
        } catch (Exception e) {
            log.error("知识库同步失败", e);
            return Result.error("同步失败：" + e.getMessage());
        }
    }
    @Autowired
    private KnowledgeBaseMdService knowledgeBaseMdService;

    /**
     * Markdown 知识库全文检索
     * GET /api/v1/knowledge/md-search?q=番茄&size=5
     */
    @GetMapping("/md-search")
    public Result<List<Map<String, Object>>> mdSearch(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "5") int size) {
        if (query == null || query.trim().isEmpty()) {
            return Result.ok(List.of());
        }
        List<Map<String, Object>> results = knowledgeBaseMdService.search(query.trim(), size);
        return Result.ok(results);
    }

    /**
     * 初始化 Markdown 知识库索引
     * POST /api/v1/knowledge/init-md-index
     */
    @PostMapping("/init-md-index")
    public Result<String> initMdIndex() {
        try {
            knowledgeBaseMdService.initIndex();
            return Result.ok("Markdown 知识库索引初始化成功");
        } catch (Exception e) {
            log.error("Markdown 知识库索引初始化失败", e);
            return Result.error("初始化失败：" + e.getMessage());
        }
    }
}

package com.jtcsm.api.controller;

import com.jtcsm.api.service.RecommendService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.entity.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 推荐控制器 —— 个性化推荐、每日推荐和按食材推荐
 */
@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 个性化推荐
     * GET /api/v1/recommend/personal
     */
    @GetMapping("/personal")
    public Result<List<Recipe>> personal() {
        return Result.ok(recommendService.personalRecommend(UserContext.getUserId()));
    }

    /**
     * 每日推荐
     * GET /api/v1/recommend/daily
     */
    @GetMapping("/daily")
    public Result<List<Recipe>> daily() {
        return Result.ok(recommendService.dailyRecommend(UserContext.getUserId()));
    }

    /**
     * 按食材推荐
     * GET /api/v1/recommend/by-ingredients?ingredients=土豆,胡萝卜
     */
    @GetMapping("/by-ingredients")
    public Result<List<Recipe>> byIngredients(@RequestParam List<String> ingredients) {
        return Result.ok(recommendService.byIngredients(UserContext.getUserId(), ingredients));
    }
}

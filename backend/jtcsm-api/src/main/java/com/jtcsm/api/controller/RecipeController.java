package com.jtcsm.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jtcsm.api.service.RecipeService;
import com.jtcsm.common.Result;
import com.jtcsm.common.dto.RecipeDetailVO;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.RecipeStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜谱控制器 —— 搜索、热门推荐、详情和步骤查询
 */
@RestController
@RequestMapping("/api/v1/recipe")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    /**
     * 多条件搜索菜谱
     * GET /api/v1/recipe/search?keyword=&cuisine=&difficulty=&cookMethod=&page=1&size=20
     */
    @GetMapping("/search")
    public Result<IPage<Recipe>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String cookMethod,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Recipe> result = recipeService.search(keyword, cuisine, difficulty, cookMethod, page, size);
        return Result.ok(result);
    }

    /**
     * 热门菜谱 Top10
     * GET /api/v1/recipe/hot
     */
    @GetMapping("/hot")
    public Result<List<Recipe>> hot() {
        List<Recipe> hotList = recipeService.getHot();
        return Result.ok(hotList);
    }

    /**
     * 菜谱详情（含用料和步骤）
     * GET /api/v1/recipe/{id}
     */
    @GetMapping("/{id}")
    public Result<RecipeDetailVO> detail(@PathVariable Long id) {
        RecipeDetailVO vo = recipeService.getDetail(id);
        return Result.ok(vo);
    }

    /**
     * 获取菜谱步骤列表
     * GET /api/v1/recipe/{id}/steps
     */
    @GetMapping("/{id}/steps")
    public Result<List<RecipeStep>> steps(@PathVariable Long id) {
        List<RecipeStep> steps = recipeService.getSteps(id);
        return Result.ok(steps);
    }
}

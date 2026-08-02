package com.jtcsm.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jtcsm.admin.util.PageHelper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.common.Result;
import com.jtcsm.common.entity.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/recipes")
public class AdminRecipeController {

    @Autowired private RecipeMapper recipeMapper;

    @GetMapping
    public Result<IPage<Recipe>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        // 默认按 ID 升序排列
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>()
                .orderByAsc(Recipe::getId);
        // 关键字模糊搜索菜名
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Recipe::getName, keyword);
        }
        return Result.ok(PageHelper.selectPage(recipeMapper, wrapper, page, size));
    }

    @GetMapping("/{id}")
    public Result<Recipe> get(@PathVariable Long id) { return Result.ok(recipeMapper.selectById(id)); }

    @PostMapping
    public Result<Void> create(@RequestBody Recipe recipe) {
        recipe.setViewCount(0); recipe.setFavoriteCount(0); recipe.setStatus(1);
        recipeMapper.insert(recipe);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Recipe recipe) {
        recipe.setId(id); recipeMapper.updateById(recipe);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recipeMapper.deleteById(id);
        return Result.ok();
    }
}

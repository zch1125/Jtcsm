package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jtcsm.api.mapper.RecipeIngredientMapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.RecipeStepMapper;
import com.jtcsm.api.service.RecipeService;
import com.jtcsm.common.dto.RecipeDetailVO;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.RecipeIngredient;
import com.jtcsm.common.entity.RecipeStep;
import com.jtcsm.api.service.HistoryService;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipeServiceImpl implements RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeServiceImpl.class);
    private static final String HOT_CACHE_KEY = "jtcsm:recipe:hot";

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private RecipeIngredientMapper ingredientMapper;

    @Autowired
    private RecipeStepMapper stepMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<Recipe> search(String keyword, String cuisine, String difficulty,
                                String cookMethod, int pageNum, int pageSize) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();

        // 关键词：模糊匹配菜名或简介
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Recipe::getName, keyword.trim())
                    .or().like(Recipe::getDescription, keyword.trim()));
        }
        // 菜系
        if (StringUtils.hasText(cuisine)) {
            wrapper.eq(Recipe::getCuisine, cuisine.trim());
        }
        // 难度
        if (StringUtils.hasText(difficulty)) {
            wrapper.eq(Recipe::getDifficulty, difficulty.trim());
        }
        // 烹饪方式
        if (StringUtils.hasText(cookMethod)) {
            wrapper.eq(Recipe::getCookMethod, cookMethod.trim());
        }

        // 仅查询上架菜谱，按浏览量降序
        wrapper.eq(Recipe::getStatus, 1)
               .orderByDesc(Recipe::getViewCount);

        Page<Recipe> page = new Page<>(pageNum, pageSize);
        return recipeMapper.selectPage(page, wrapper);
    }

    @Override
    public RecipeDetailVO getDetail(Long id) {
        // 1. 查询菜谱
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null || recipe.getStatus() != 1) {
            throw new BusinessException(404, "菜谱不存在");
        }

        // 2. 查询用料清单（按 sort_order 排序）
        List<RecipeIngredient> ingredients = ingredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, id)
                        .orderByAsc(RecipeIngredient::getSortOrder));

        // 3. 查询步骤列表（按 step_no 排序）
        List<RecipeStep> steps = stepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>()
                        .eq(RecipeStep::getRecipeId, id)
                        .orderByAsc(RecipeStep::getStepNo));

        // 4. 浏览量 +1
        recipe.setViewCount(recipe.getViewCount() == null ? 1 : recipe.getViewCount() + 1);
        recipeMapper.updateById(recipe);

        // 6. 记录浏览历史（仅登录用户）
        if (UserContext.isLogin()) {
            historyService.recordView(UserContext.getUserId(), id);
        }

        // 5. 组装 VO
        RecipeDetailVO vo = RecipeDetailVO.from(recipe);
        vo.setIngredients(ingredients);
        vo.setSteps(steps);
        return vo;
    }

    @Override
    public List<Recipe> getHot() {
        // 1. 尝试从 Redis ZSet 读取缓存的 hot 列表
        Set<String> cachedIds = stringRedisTemplate.opsForZSet()
                .reverseRange(HOT_CACHE_KEY, 0, 9);

        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Long> ids = cachedIds.stream()
                    .map(Long::valueOf).collect(Collectors.toList());
            List<Recipe> recipes = recipeMapper.selectBatchIds(ids);
            // 按 Redis 中的顺序排列
            recipes.sort((a, b) -> ids.indexOf(a.getId()) - ids.indexOf(b.getId()));
            return recipes;
        }

        // 2. 缓存未命中，从 DB 查询并回填缓存
        List<Recipe> hotList = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>()
                        .eq(Recipe::getStatus, 1)
                        .orderByDesc(Recipe::getFavoriteCount)
                        .last("LIMIT 10"));

        // 回填 Redis ZSet（score = favorite_count）
        for (Recipe r : hotList) {
            stringRedisTemplate.opsForZSet()
                    .add(HOT_CACHE_KEY, String.valueOf(r.getId()),
                         r.getFavoriteCount() != null ? r.getFavoriteCount() : 0);
        }
        // 设置缓存过期时间 1 小时（匹配设计文档）
        stringRedisTemplate.expire(HOT_CACHE_KEY, java.time.Duration.ofHours(1));

        return hotList;
    }

    @Override
    public List<RecipeStep> getSteps(Long recipeId) {
        // 先校验菜谱存在
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new BusinessException(404, "菜谱不存在");
        }
        return stepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>()
                        .eq(RecipeStep::getRecipeId, recipeId)
                        .orderByAsc(RecipeStep::getStepNo));
    }
}

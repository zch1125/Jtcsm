package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.UserMapper;
import com.jtcsm.api.mapper.UserPreferenceMapper;
import com.jtcsm.api.service.RecommendService;
import com.jtcsm.common.annotation.VipRequired;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.User;
import com.jtcsm.common.entity.UserPreference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 推荐服务实现 —— 个性化推荐、每日推荐和按食材推荐
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired private RecipeMapper recipeMapper;
    @Autowired private UserPreferenceMapper preferenceMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private StringRedisTemplate redis;

    @Override
    @VipRequired
    public List<Recipe> personalRecommend(Long userId) {
        // 基于用户偏好查询菜品：匹配菜系和难度，最多返回 10 条
        UserPreference pref = preferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));
        if (pref == null) return dailyRecommend(userId);

        LambdaQueryWrapper<Recipe> w = new LambdaQueryWrapper<Recipe>().eq(Recipe::getStatus, 1);
        if (pref.getCuisine() != null && !pref.getCuisine().isEmpty()) w.eq(Recipe::getCuisine, pref.getCuisine());
        if (pref.getDifficulty() != null && !pref.getDifficulty().isEmpty()) w.eq(Recipe::getDifficulty, pref.getDifficulty());

        List<Recipe> list = recipeMapper.selectList(w.last("LIMIT 10"));
        if (list.isEmpty()) list = dailyRecommend(userId);
        return list;
    }

    @Override
    public List<Recipe> dailyRecommend(Long userId) {
        // 每日推荐：优先读取缓存，未命中则从数据库随机选取 5 条
        String today = LocalDate.now().toString();
        String cacheKey = "jtcsm:daily:rec:" + today;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            String[] ids = cached.split(",");
            return java.util.Arrays.stream(ids).map(id -> recipeMapper.selectById(Long.parseLong(id))).collect(Collectors.toList());
        }

        User user = userMapper.selectById(userId);
        boolean isVip = user != null && user.getIsVip() == 1;
        // 非会员每日只能刷新一次推荐
        if (!isVip) {
            String limitKey = "jtcsm:daily:limit:" + userId + ":" + today;
            String val = redis.opsForValue().get(limitKey);
            if (val != null) return dailyCache();
            redis.opsForValue().set(limitKey, "1", Duration.ofDays(1));
        }

        // 从所有上架菜谱中随机抽取 5 条并缓存
        List<Recipe> all = recipeMapper.selectList(
            new LambdaQueryWrapper<Recipe>().eq(Recipe::getStatus, 1).orderByDesc(Recipe::getFavoriteCount));
        Collections.shuffle(all, new Random());
        List<Recipe> top = all.stream().limit(5).collect(Collectors.toList());
        String ids = top.stream().map(r -> String.valueOf(r.getId())).collect(Collectors.joining(","));
        redis.opsForValue().set(cacheKey, ids, Duration.ofDays(1));
        return top;
    }

    private List<Recipe> dailyCache() {
        // 读取缓存的每日推荐列表
        String cached = redis.opsForValue().get("jtcsm:daily:rec:" + LocalDate.now().toString());
        if (cached != null) {
            String[] ids = cached.split(",");
            return java.util.Arrays.stream(ids).map(id -> recipeMapper.selectById(Long.parseLong(id))).collect(Collectors.toList());
        }
        return recipeMapper.selectList(new LambdaQueryWrapper<Recipe>().eq(Recipe::getStatus, 1).last("LIMIT 5"));
    }

    @Override
    public List<Recipe> byIngredients(Long userId, List<String> ingredients) {
        // 按食材推荐（当前简化实现直接返回每日推荐）
        if (ingredients == null || ingredients.isEmpty()) return dailyRecommend(userId);
        return recipeMapper.selectList(
            new LambdaQueryWrapper<Recipe>().eq(Recipe::getStatus, 1).last("LIMIT 10"));
    }
}

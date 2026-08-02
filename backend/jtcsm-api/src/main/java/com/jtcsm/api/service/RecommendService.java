package com.jtcsm.api.service;

import com.jtcsm.common.entity.Recipe;
import java.util.List;

/**
 * 推荐服务接口 —— 个性化推荐、每日推荐和按食材推荐
 */
public interface RecommendService {
    List<Recipe> personalRecommend(Long userId);
    List<Recipe> dailyRecommend(Long userId);
    List<Recipe> byIngredients(Long userId, List<String> ingredients);
}

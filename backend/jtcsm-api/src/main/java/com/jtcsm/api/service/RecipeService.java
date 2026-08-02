package com.jtcsm.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jtcsm.common.dto.RecipeDetailVO;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.RecipeStep;

import java.util.List;

/**
 * 菜谱服务接口 —— 搜索、详情、热门推荐和步骤查询
 */
public interface RecipeService {

    /** 多条件搜索 */
    IPage<Recipe> search(String keyword, String cuisine, String difficulty, String cookMethod, int page, int size);

    /** 菜谱详情（含用料+步骤） */
    RecipeDetailVO getDetail(Long id);

    /** 热门推荐 Top10 */
    List<Recipe> getHot();

    /** 获取步骤列表 */
    List<RecipeStep> getSteps(Long recipeId);
}

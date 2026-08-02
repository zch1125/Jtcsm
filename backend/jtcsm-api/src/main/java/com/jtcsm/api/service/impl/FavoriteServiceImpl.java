package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.UserFavoriteMapper;
import com.jtcsm.api.service.FavoriteService;
import com.jtcsm.common.dto.FavoriteListVO;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.UserFavorite;
import com.jtcsm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    @Autowired
    private UserFavoriteMapper favoriteMapper;

    @Autowired
    private RecipeMapper recipeMapper;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long recipeId) {
        // 校验菜谱是否存在且已上架
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || recipe.getStatus() != 1) {
            throw new BusinessException(404, "菜谱不存在");
        }

        // 检查是否已收藏（唯一约束兜底，避免重复插入报错）
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getRecipeId, recipeId));
        if (count > 0) {
            log.info("用户 {} 重复收藏菜谱 {}", userId, recipeId);
            return; // 幂等：已收藏则直接返回
        }

        // 新增收藏记录
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setRecipeId(recipeId);
        favoriteMapper.insert(favorite);

        // 更新菜谱收藏数 +1
        recipe.setFavoriteCount(recipe.getFavoriteCount() == null ? 1 : recipe.getFavoriteCount() + 1);
        recipeMapper.updateById(recipe);

        log.info("用户 {} 收藏菜谱 {}", userId, recipeId);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long recipeId) {
        // 删除收藏记录
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getRecipeId, recipeId);
        int deleted = favoriteMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException(404, "该收藏不存在");
        }

        // 更新菜谱收藏数 -1（不小于 0）
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null) {
            int count = recipe.getFavoriteCount() == null ? 0 : recipe.getFavoriteCount();
            recipe.setFavoriteCount(Math.max(0, count - 1));
            recipeMapper.updateById(recipe);
        }

        log.info("用户 {} 取消收藏菜谱 {}", userId, recipeId);
    }

    @Override
    public boolean isFavorited(Long userId, Long recipeId) {
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getRecipeId, recipeId)) > 0;
    }

    @Override
    public List<FavoriteListVO> listFavorites(Long userId, int page, int size) {
        // 分页查询收藏记录（按收藏时间倒序）
        int offset = (page - 1) * size;
        List<UserFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime)
                        .last("LIMIT " + offset + "," + size));

        if (favorites.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取菜谱 ID 列表，批量查询菜谱基本信息
        List<Long> recipeIds = favorites.stream()
                .map(UserFavorite::getRecipeId)
                .collect(Collectors.toList());
        List<Recipe> recipes = recipeMapper.selectBatchIds(recipeIds);
        Map<Long, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r));

        // 组装 VO
        return favorites.stream().map(fav -> {
            FavoriteListVO vo = new FavoriteListVO();
            vo.setFavoriteId(fav.getId());
            vo.setRecipeId(fav.getRecipeId());
            vo.setCreatedAt(fav.getCreateTime());

            Recipe r = recipeMap.get(fav.getRecipeId());
            if (r != null) {
                vo.setName(r.getName());
                vo.setCoverImage(r.getCoverImage());
                vo.setCuisine(r.getCuisine());
                vo.setDifficulty(r.getDifficulty());
                vo.setCookMethod(r.getCookMethod());
                vo.setCookTime(r.getCookTime());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}

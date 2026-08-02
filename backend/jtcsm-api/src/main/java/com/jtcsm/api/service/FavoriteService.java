package com.jtcsm.api.service;

import com.jtcsm.common.dto.FavoriteListVO;

import java.util.List;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /** 添加收藏 */
    void addFavorite(Long userId, Long recipeId);

    /** 取消收藏 */
    void removeFavorite(Long userId, Long recipeId);

    /** 检查是否已收藏 */
    boolean isFavorited(Long userId, Long recipeId);

    /** 分页查询收藏列表 */
    List<FavoriteListVO> listFavorites(Long userId, int page, int size);
}

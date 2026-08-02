package com.jtcsm.api.controller;

import com.jtcsm.api.service.FavoriteService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.FavoriteAddRequest;
import com.jtcsm.common.dto.FavoriteListVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/api/v1/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 添加菜谱收藏
     * POST /api/v1/favorite
     */
    @PostMapping
    public Result<Void> addFavorite(@Valid @RequestBody FavoriteAddRequest request) {
        Long userId = UserContext.getUserId();
        favoriteService.addFavorite(userId, request.getRecipeId());
        return Result.ok("收藏成功", null);
    }

    /**
     * 取消菜谱收藏
     * DELETE /api/v1/favorite/{recipeId}
     */
    @DeleteMapping("/{recipeId}")
    public Result<Void> removeFavorite(@PathVariable Long recipeId) {
        Long userId = UserContext.getUserId();
        favoriteService.removeFavorite(userId, recipeId);
        return Result.ok("已取消收藏", null);
    }

    /**
     * 分页查询收藏列表（按收藏时间倒序）
     * GET /api/v1/favorite/list?page=1&size=20
     */
    @GetMapping("/list")
    public Result<List<FavoriteListVO>> listFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        List<FavoriteListVO> list = favoriteService.listFavorites(userId, page, size);
        return Result.ok(list);
    }

    /**
     * 检查指定菜谱是否已被当前用户收藏
     * GET /api/v1/favorite/check/{recipeId}
     */
    @GetMapping("/check/{recipeId}")
    public Result<Boolean> checkFavorited(@PathVariable Long recipeId) {
        Long userId = UserContext.getUserId();
        boolean favorited = favoriteService.isFavorited(userId, recipeId);
        return Result.ok(favorited);
    }
}

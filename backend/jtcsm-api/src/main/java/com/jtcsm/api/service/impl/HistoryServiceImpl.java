package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.UserHistoryMapper;
import com.jtcsm.api.service.HistoryService;
import com.jtcsm.common.dto.HistoryListVO;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.UserHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 浏览历史服务实现
 */
@Service
public class HistoryServiceImpl implements HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryServiceImpl.class);

    @Autowired
    private UserHistoryMapper historyMapper;

    @Autowired
    private RecipeMapper recipeMapper;

    @Override
    public void recordView(Long userId, Long recipeId) {
        // 同一用户对同一菜谱只保留最新浏览记录：先删后插
        historyMapper.delete(
                new LambdaQueryWrapper<UserHistory>()
                        .eq(UserHistory::getUserId, userId)
                        .eq(UserHistory::getRecipeId, recipeId));

        UserHistory history = new UserHistory();
        history.setUserId(userId);
        history.setRecipeId(recipeId);
        history.setViewedAt(LocalDateTime.now());
        historyMapper.insert(history);

        log.debug("记录浏览历史: userId={}, recipeId={}", userId, recipeId);
    }

    @Override
    public List<HistoryListVO> listHistory(Long userId, int page, int size) {
        // 分页查询浏览历史（按浏览时间倒序）
        int offset = (page - 1) * size;
        List<UserHistory> histories = historyMapper.selectList(
                new LambdaQueryWrapper<UserHistory>()
                        .eq(UserHistory::getUserId, userId)
                        .orderByDesc(UserHistory::getViewedAt)
                        .last("LIMIT " + offset + "," + size));

        if (histories.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取菜谱 ID 列表，批量查询菜谱基本信息
        List<Long> recipeIds = histories.stream()
                .map(UserHistory::getRecipeId)
                .collect(Collectors.toList());
        List<Recipe> recipes = recipeMapper.selectBatchIds(recipeIds);
        Map<Long, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r));

        // 组装 VO
        return histories.stream().map(h -> {
            HistoryListVO vo = new HistoryListVO();
            vo.setHistoryId(h.getId());
            vo.setRecipeId(h.getRecipeId());
            vo.setViewedAt(h.getViewedAt());

            Recipe r = recipeMap.get(h.getRecipeId());
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

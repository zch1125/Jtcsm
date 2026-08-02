package com.jtcsm.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.UserMapper;
import com.jtcsm.common.Result;
import com.jtcsm.common.dto.AdminDashboardVO;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    @Autowired private UserMapper userMapper;
    @Autowired private RecipeMapper recipeMapper;

    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard() {
        AdminDashboardVO vo = new AdminDashboardVO();

        // 用户总数（状态正常的）
        vo.setTotalUsers(userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));

        // 菜谱总数（已上架的）
        vo.setTotalRecipes(recipeMapper.selectCount(
            new LambdaQueryWrapper<Recipe>().eq(Recipe::getStatus, 1)));

        return Result.ok(vo);
    }
}

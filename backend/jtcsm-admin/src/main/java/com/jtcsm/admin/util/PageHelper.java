package com.jtcsm.admin.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * MyBatis-Plus 手动分页工具
 * <p>
 * 3.5.9 移除了 PaginationInnerInterceptor，selectPage 无法正确计算 total。
 * 此工具通过 selectCount + selectList(LIMIT) 实现分页。
 * </p>
 */
public class PageHelper {

    /**
     * 手动分页查询
     *
     * @param mapper  BaseMapper 实例
     * @param wrapper 查询条件（可含排序）
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param <T>     实体类型
     * @return 分页结果 IPage
     */
    public static <T> IPage<T> selectPage(BaseMapper<T> mapper, LambdaQueryWrapper<T> wrapper, long page, long size) {
        // 执行 COUNT 查询
        long total = mapper.selectCount(wrapper);

        // 构造 Page 对象
        Page<T> result = new Page<>(page, size, total);

        if (total > 0) {
            // 执行带 LIMIT 的数据查询
            long offset = (page - 1) * size;
            wrapper.last("LIMIT " + size + " OFFSET " + offset);
            List<T> records = mapper.selectList(wrapper);
            result.setRecords(records);
        }

        return result;
    }
}

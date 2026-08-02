package com.jtcsm.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置
 * Mapper 扫描路径统一在此声明。
 * 注：3.5.9 移除了 PaginationInnerInterceptor，分页采用手动方式（见 AdminRecipeController）
 */
@Configuration
@MapperScan("com.jtcsm.**.mapper")
public class MyBatisPlusConfig {
}

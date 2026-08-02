package com.jtcsm.admin.controller;

import com.jtcsm.admin.service.SystemMonitorService;
import com.jtcsm.common.Result;
import com.jtcsm.common.dto.SystemMonitorVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统监控控制器 —— 提供系统/JVM/数据库/Redis 监控数据
 */
@RestController
@RequestMapping("/api/admin")
public class MonitorController {

    @Autowired
    private SystemMonitorService monitorService;

    /**
     * 获取全量系统监控数据
     * GET /api/admin/monitor
     */
    @GetMapping("/monitor")
    public Result<SystemMonitorVO> monitor() {
        return Result.ok(monitorService.collect());
    }
}

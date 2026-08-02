package com.jtcsm.admin.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.jtcsm.common.dto.SystemMonitorVO;
import com.jtcsm.common.dto.SystemMonitorVO.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

/**
 * 系统监控服务 —— 采集 JVM/系统/数据库/Redis 各项指标
 */
@Service
public class SystemMonitorService {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitorService.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat PCT = new DecimalFormat("#.0");

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private StringRedisTemplate redis;

    private final ObjectMapper esMapper = new ObjectMapper();

    /**
     * 采集全部监控指标
     */
    public SystemMonitorVO collect() {
        SystemMonitorVO vo = new SystemMonitorVO();
        try { vo.setSystem(collectSystemInfo()); } catch (Exception e) { log.warn("采集系统信息失败", e); }
        try { vo.setJvm(collectJvmInfo()); } catch (Exception e) { log.warn("采集 JVM 信息失败", e); }
        try { vo.setMemory(collectMemoryInfo()); } catch (Exception e) { log.warn("采集内存信息失败", e); }
        try { vo.setDisk(collectDiskInfo()); } catch (Exception e) { log.warn("采集磁盘信息失败", e); }
        try { vo.setThreads(collectThreadInfo()); } catch (Exception e) { log.warn("采集线程信息失败", e); }
        try { vo.setClasses(collectClassInfo()); } catch (Exception e) { log.warn("采集类加载信息失败", e); }
        try { vo.setDb(collectDbPoolInfo()); } catch (Exception e) { log.warn("采集数据库连接池信息失败", e); }
        try { vo.setRedis(collectRedisInfo()); } catch (Exception e) { log.warn("采集 Redis 信息失败", e); }
        try { vo.setEs(collectEsInfo()); } catch (Exception e) { log.warn("采集 ES 信息失败", e); }
        return vo;
    }

    /** 系统信息 */
    private SystemInfo collectSystemInfo() {
        SystemInfo info = new SystemInfo();
        info.setOsName(System.getProperty("os.name", "-"));
        info.setOsVersion(System.getProperty("os.version", "-"));
        info.setOsArch(System.getProperty("os.arch", "-"));
        info.setAvailableProcessors(Runtime.getRuntime().availableProcessors());

        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            info.setSystemCpuLoad(roundPct(osBean.getCpuLoad()));
            info.setProcessCpuLoad(roundPct(osBean.getProcessCpuLoad()));
        } catch (Exception e) {
            info.setSystemCpuLoad(-1);
            info.setProcessCpuLoad(-1);
        }
        return info;
    }

    /** JVM 信息 */
    private JvmInfo collectJvmInfo() {
        JvmInfo info = new JvmInfo();
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();

        info.setJavaVersion(System.getProperty("java.version", "-"));
        info.setJvmName(rt.getVmName());
        info.setJvmVendor(rt.getVmVendor());

        long startMs = rt.getStartTime();
        info.setStartTimeMillis(startMs);
        info.setStartTime(Instant.ofEpochMilli(startMs)
                .atZone(ZoneId.systemDefault()).toLocalDateTime().format(DTF));

        long upMs = rt.getUptime();
        info.setUptimeMillis(upMs);
        info.setUptime(formatUptime(upMs));

        // PID
        String vmName = rt.getName();
        if (vmName != null && vmName.contains("@")) {
            info.setPid(Long.parseLong(vmName.split("@")[0]));
        }

        // 启动参数
        List<String> args = rt.getInputArguments();
        info.setInputArgs(String.join(" ", args));

        return info;
    }

    /** 内存信息 */
    private MemoryInfo collectMemoryInfo() {
        MemoryInfo info = new MemoryInfo();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        // Heap
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        info.setHeapUsed(heap.getUsed());
        info.setHeapMax(heap.getMax() > 0 ? heap.getMax() : Runtime.getRuntime().maxMemory());
        info.setHeapCommitted(heap.getCommitted());
        info.setHeapUsagePercent(roundPct((double) heap.getUsed() / (heap.getMax() > 0 ? heap.getMax() : 1)));

        // Non-Heap
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();
        info.setNonHeapUsed(nonHeap.getUsed());
        info.setNonHeapCommitted(nonHeap.getCommitted());

        // 物理内存
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            info.setPhysicalTotal(osBean.getTotalMemorySize());
            info.setPhysicalFree(osBean.getFreeMemorySize());
            info.setPhysicalUsagePercent(roundPct(
                    1.0 - (double) osBean.getFreeMemorySize() / osBean.getTotalMemorySize()));
        } catch (Exception e) {
            info.setPhysicalTotal(0);
            info.setPhysicalFree(0);
        }
        return info;
    }

    /** 磁盘信息 */
    private List<DiskInfo> collectDiskInfo() {
        List<DiskInfo> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            DiskInfo d = new DiskInfo();
            d.setPath(root.getAbsolutePath());
            d.setTotal(root.getTotalSpace());
            d.setFree(root.getFreeSpace());
            d.setUsable(root.getUsableSpace());
            d.setUsagePercent(root.getTotalSpace() > 0
                    ? roundPct(1.0 - (double) root.getFreeSpace() / root.getTotalSpace()) : 0);
            list.add(d);
        }
        return list;
    }

    /** 线程信息 */
    private SystemMonitorVO.ThreadInfo collectThreadInfo() {
        SystemMonitorVO.ThreadInfo info = new SystemMonitorVO.ThreadInfo();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        info.setLiveCount(threadBean.getThreadCount());
        info.setDaemonCount(threadBean.getDaemonThreadCount());
        info.setPeakCount(threadBean.getPeakThreadCount());
        return info;
    }

    /** 类加载信息 */
    private ClassInfo collectClassInfo() {
        ClassInfo info = new ClassInfo();
        ClassLoadingMXBean clBean = ManagementFactory.getClassLoadingMXBean();
        info.setLoadedCount(clBean.getLoadedClassCount());
        info.setUnloadedCount(clBean.getUnloadedClassCount());
        return info;
    }

    /** 数据库连接池 */
    private DbPoolInfo collectDbPoolInfo() {
        DbPoolInfo info = new DbPoolInfo();
        if (dataSource instanceof DruidDataSource ds) {
            info.setActiveCount(ds.getActiveCount());
            info.setIdleCount(ds.getPoolingCount());
            info.setTotalCount(ds.getActiveCount() + ds.getPoolingCount());
            info.setMaxActive(ds.getMaxActive());
        }
        return info;
    }

    /** Redis 状态 */
    private RedisInfo collectRedisInfo() {
        RedisInfo info = new RedisInfo();
        if (redis != null) {
            try {
                redis.getConnectionFactory().getConnection().ping();
                info.setConnected(true);
                info.setInfo("已连接");
            } catch (Exception e) {
                info.setConnected(false);
                info.setInfo("连接失败: " + e.getMessage());
            }
        } else {
            info.setConnected(false);
            info.setInfo("未配置");
        }
        return info;
    }


    /** ES 状态（通过 REST API 采集） */
    private EsInfo collectEsInfo() {
        EsInfo info = new EsInfo();
        String esHost = System.getenv().getOrDefault("ES_HOST", "localhost");
        String esPort = System.getenv().getOrDefault("ES_PORT", "9200");
        String baseUrl = "http://" + esHost + ":" + esPort;

        try {
            // 1. 获取集群信息
            String response = httpGet(baseUrl + "/");
            if (response != null) {
                Map<String, Object> root = esMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                info.setConnected(true);
                Object versionObj = root.get("version");
                if (versionObj instanceof Map) {
                    info.setVersion((String) ((Map) versionObj).getOrDefault("number", "-"));
                }
                info.setClusterName((String) root.getOrDefault("cluster_name", "-"));
                info.setNodeName((String) root.getOrDefault("name", "-"));
            }

            // 2. 集群健康
            String healthResp = httpGet(baseUrl + "/_cluster/health");
            if (healthResp != null) {
                Map<String, Object> health = esMapper.readValue(healthResp, new TypeReference<Map<String, Object>>() {});
                info.setStatus((String) health.getOrDefault("status", "unknown"));
            }

            // 3. 索引信息
            String indicesResp = httpGet(baseUrl + "/_cat/indices?format=json");
            if (indicesResp != null) {
                java.util.List<Map<String, Object>> indices = esMapper.readValue(indicesResp, new TypeReference<java.util.List<Map<String, Object>>>() {});
                java.util.List<EsIndexInfo> indexInfos = new ArrayList<>();
                long totalDocs = 0;
                long totalStore = 0;

                for (Map<String, Object> idx : indices) {
                    String idxName = (String) idx.getOrDefault("index", "");
                    if (idxName.isEmpty()) continue;

                    EsIndexInfo idxInfo = new EsIndexInfo();
                    idxInfo.setName(idxName);
                    idxInfo.setHealth((String) idx.getOrDefault("health", "?"));
                    idxInfo.setDocCount(parseLong(idx.get("docs.count")));
                    idxInfo.setStoreSizeBytes(parseStoreSize((String) idx.getOrDefault("store.size", "0")));

                    indexInfos.add(idxInfo);
                    totalDocs += idxInfo.getDocCount();
                    totalStore += idxInfo.getStoreSizeBytes();
                }

                info.setIndexCount(indexInfos.size());
                info.setTotalDocCount(totalDocs);
                info.setTotalStoreSizeBytes(totalStore);
                info.setIndices(indexInfos);
            }
        } catch (Exception e) {
            info.setConnected(false);
            info.setStatus(e.getMessage());
            log.warn("ES REST API 调用失败: {}", e.getMessage());
        }

        return info;
    }

    private String httpGet(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code == 200) {
                byte[] data = conn.getInputStream().readAllBytes();
                return new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("HTTP GET {} failed: {}", url, e.getMessage());
        }
        return null;
    }

    private long parseLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0; }
    }

    private long parseStoreSize(String size) {
        if (size == null || size.isEmpty()) return 0;
        try {
            size = size.trim().toUpperCase();
            if (size.endsWith("TB")) return (long)(Double.parseDouble(size.replace("TB","").trim()) * 1024L * 1024 * 1024 * 1024);
            if (size.endsWith("GB")) return (long)(Double.parseDouble(size.replace("GB","").trim()) * 1024L * 1024 * 1024);
            if (size.endsWith("MB")) return (long)(Double.parseDouble(size.replace("MB","").trim()) * 1024L * 1024);
            if (size.endsWith("KB")) return (long)(Double.parseDouble(size.replace("KB","").trim()) * 1024L);
            if (size.endsWith("B")) return Long.parseLong(size.replace("B","").trim());
            return Long.parseLong(size);
        } catch (Exception e) { return 0; }
    }

    // ==================== 工具方法 ====================

    private double roundPct(double value) {
        if (value < 0) return -1;
        return Math.round(value * 1000.0) / 10.0;
    }

    private String formatUptime(long millis) {
        long sec = millis / 1000;
        long min = sec / 60;
        long hr = min / 60;
        long day = hr / 24;
        if (day > 0) return day + "d " + (hr % 24) + "h " + (min % 60) + "m";
        if (hr > 0) return hr + "h " + (min % 60) + "m " + (sec % 60) + "s";
        if (min > 0) return min + "m " + (sec % 60) + "s";
        return sec + "s";
    }
}

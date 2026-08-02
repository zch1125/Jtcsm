package com.jtcsm.common.dto;

import java.util.List;

/**
 * 系统监控数据 VO —— 涵盖系统、JVM、内存、磁盘、线程、数据库、Redis 指标
 */
public class SystemMonitorVO {

    /** 系统信息 */
    private SystemInfo system;
    /** JVM 信息 */
    private JvmInfo jvm;
    /** 内存信息 */
    private MemoryInfo memory;
    /** 磁盘分区信息 */
    private List<DiskInfo> disk;
    /** 线程信息 */
    private ThreadInfo threads;
    /** 类加载信息 */
    private ClassInfo classes;
    /** 数据库连接池 */
    private DbPoolInfo db;
    /** Redis 状态 */
    private RedisInfo redis;
    /** ES 状态 */
    private EsInfo es;

    // ==================== 内嵌静态类 ====================

    /** 系统信息 */
    public static class SystemInfo {
        private String osName;
        private String osVersion;
        private String osArch;
        private int availableProcessors;
        private double systemCpuLoad;
        private double processCpuLoad;
        // getter/setter
        public String getOsName() { return osName; }
        public void setOsName(String osName) { this.osName = osName; }
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        public String getOsArch() { return osArch; }
        public void setOsArch(String osArch) { this.osArch = osArch; }
        public int getAvailableProcessors() { return availableProcessors; }
        public void setAvailableProcessors(int v) { this.availableProcessors = v; }
        public double getSystemCpuLoad() { return systemCpuLoad; }
        public void setSystemCpuLoad(double v) { this.systemCpuLoad = v; }
        public double getProcessCpuLoad() { return processCpuLoad; }
        public void setProcessCpuLoad(double v) { this.processCpuLoad = v; }
    }

    /** JVM 信息 */
    public static class JvmInfo {
        private String javaVersion;
        private String jvmName;
        private String jvmVendor;
        private String startTime;
        private long startTimeMillis;
        private String uptime;
        private long uptimeMillis;
        private long pid;
        private String inputArgs;
        // getter/setter
        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String v) { this.javaVersion = v; }
        public String getJvmName() { return jvmName; }
        public void setJvmName(String v) { this.jvmName = v; }
        public String getJvmVendor() { return jvmVendor; }
        public void setJvmVendor(String v) { this.jvmVendor = v; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String v) { this.startTime = v; }
        public long getStartTimeMillis() { return startTimeMillis; }
        public void setStartTimeMillis(long v) { this.startTimeMillis = v; }
        public String getUptime() { return uptime; }
        public void setUptime(String v) { this.uptime = v; }
        public long getUptimeMillis() { return uptimeMillis; }
        public void setUptimeMillis(long v) { this.uptimeMillis = v; }
        public long getPid() { return pid; }
        public void setPid(long v) { this.pid = v; }
        public String getInputArgs() { return inputArgs; }
        public void setInputArgs(String v) { this.inputArgs = v; }
    }

    /** 内存信息 */
    public static class MemoryInfo {
        private long heapUsed;
        private long heapMax;
        private long heapCommitted;
        private double heapUsagePercent;
        private long nonHeapUsed;
        private long nonHeapCommitted;
        private long physicalTotal;
        private long physicalFree;
        private double physicalUsagePercent;
        // getter/setter
        public long getHeapUsed() { return heapUsed; }
        public void setHeapUsed(long v) { this.heapUsed = v; }
        public long getHeapMax() { return heapMax; }
        public void setHeapMax(long v) { this.heapMax = v; }
        public long getHeapCommitted() { return heapCommitted; }
        public void setHeapCommitted(long v) { this.heapCommitted = v; }
        public double getHeapUsagePercent() { return heapUsagePercent; }
        public void setHeapUsagePercent(double v) { this.heapUsagePercent = v; }
        public long getNonHeapUsed() { return nonHeapUsed; }
        public void setNonHeapUsed(long v) { this.nonHeapUsed = v; }
        public long getNonHeapCommitted() { return nonHeapCommitted; }
        public void setNonHeapCommitted(long v) { this.nonHeapCommitted = v; }
        public long getPhysicalTotal() { return physicalTotal; }
        public void setPhysicalTotal(long v) { this.physicalTotal = v; }
        public long getPhysicalFree() { return physicalFree; }
        public void setPhysicalFree(long v) { this.physicalFree = v; }
        public double getPhysicalUsagePercent() { return physicalUsagePercent; }
        public void setPhysicalUsagePercent(double v) { this.physicalUsagePercent = v; }
    }

    /** 磁盘分区 */
    public static class DiskInfo {
        private String path;
        private long total;
        private long free;
        private long usable;
        private double usagePercent;
        // getter/setter
        public String getPath() { return path; }
        public void setPath(String v) { this.path = v; }
        public long getTotal() { return total; }
        public void setTotal(long v) { this.total = v; }
        public long getFree() { return free; }
        public void setFree(long v) { this.free = v; }
        public long getUsable() { return usable; }
        public void setUsable(long v) { this.usable = v; }
        public double getUsagePercent() { return usagePercent; }
        public void setUsagePercent(double v) { this.usagePercent = v; }
    }

    /** 线程信息 */
    public static class ThreadInfo {
        private int liveCount;
        private int daemonCount;
        private int peakCount;
        // getter/setter
        public int getLiveCount() { return liveCount; }
        public void setLiveCount(int v) { this.liveCount = v; }
        public int getDaemonCount() { return daemonCount; }
        public void setDaemonCount(int v) { this.daemonCount = v; }
        public int getPeakCount() { return peakCount; }
        public void setPeakCount(int v) { this.peakCount = v; }
    }

    /** 类加载信息 */
    public static class ClassInfo {
        private long loadedCount;
        private long unloadedCount;
        // getter/setter
        public long getLoadedCount() { return loadedCount; }
        public void setLoadedCount(long v) { this.loadedCount = v; }
        public long getUnloadedCount() { return unloadedCount; }
        public void setUnloadedCount(long v) { this.unloadedCount = v; }
    }

    /** 数据库连接池 */
    public static class DbPoolInfo {
        private int activeCount;
        private int idleCount;
        private int totalCount;
        private int maxActive;
        // getter/setter
        public int getActiveCount() { return activeCount; }
        public void setActiveCount(int v) { this.activeCount = v; }
        public int getIdleCount() { return idleCount; }
        public void setIdleCount(int v) { this.idleCount = v; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int v) { this.totalCount = v; }
        public int getMaxActive() { return maxActive; }
        public void setMaxActive(int v) { this.maxActive = v; }
    }

    /** Redis 状态 */
    public static class RedisInfo {
        private boolean connected;
        private String info;
        // getter/setter
        public boolean isConnected() { return connected; }
        public void setConnected(boolean v) { this.connected = v; }
        public String getInfo() { return info; }
        public void setInfo(String v) { this.info = v; }
    }

    /** ES 状态 */
    public static class EsInfo {
        private boolean connected;
        private String clusterName;
        private String nodeName;
        private String version;
        private String status;
        private int indexCount;
        private long totalDocCount;
        private long totalStoreSizeBytes;
        private java.util.List<EsIndexInfo> indices;
        public boolean isConnected() { return connected; }
        public void setConnected(boolean v) { this.connected = v; }
        public String getClusterName() { return clusterName; }
        public void setClusterName(String v) { this.clusterName = v; }
        public String getNodeName() { return nodeName; }
        public void setNodeName(String v) { this.nodeName = v; }
        public String getVersion() { return version; }
        public void setVersion(String v) { this.version = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public int getIndexCount() { return indexCount; }
        public void setIndexCount(int v) { this.indexCount = v; }
        public long getTotalDocCount() { return totalDocCount; }
        public void setTotalDocCount(long v) { this.totalDocCount = v; }
        public long getTotalStoreSizeBytes() { return totalStoreSizeBytes; }
        public void setTotalStoreSizeBytes(long v) { this.totalStoreSizeBytes = v; }
        public java.util.List<EsIndexInfo> getIndices() { return indices; }
        public void setIndices(java.util.List<EsIndexInfo> v) { this.indices = v; }
    }

    /** ES 索引信息 */
    public static class EsIndexInfo {
        private String name;
        private long docCount;
        private long storeSizeBytes;
        private String health;
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public long getDocCount() { return docCount; }
        public void setDocCount(long v) { this.docCount = v; }
        public long getStoreSizeBytes() { return storeSizeBytes; }
        public void setStoreSizeBytes(long v) { this.storeSizeBytes = v; }
        public String getHealth() { return health; }
        public void setHealth(String v) { this.health = v; }
    }

    // ==================== 顶层 getter/setter ====================

    public SystemInfo getSystem() { return system; }
    public void setSystem(SystemInfo v) { this.system = v; }
    public JvmInfo getJvm() { return jvm; }
    public void setJvm(JvmInfo v) { this.jvm = v; }
    public MemoryInfo getMemory() { return memory; }
    public void setMemory(MemoryInfo v) { this.memory = v; }
    public List<DiskInfo> getDisk() { return disk; }
    public void setDisk(List<DiskInfo> v) { this.disk = v; }
    public ThreadInfo getThreads() { return threads; }
    public void setThreads(ThreadInfo v) { this.threads = v; }
    public ClassInfo getClasses() { return classes; }
    public void setClasses(ClassInfo v) { this.classes = v; }
    public DbPoolInfo getDb() { return db; }
    public void setDb(DbPoolInfo v) { this.db = v; }
    public RedisInfo getRedis() { return redis; }
    public void setRedis(RedisInfo v) { this.redis = v; }
    public EsInfo getEs() { return es; }
    public void setEs(EsInfo v) { this.es = v; }
}

<template>
  <div class="monitor-page">
    <h3>系统监控</h3>

    <!-- 概览卡片 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="6" v-for="c in overviewCards" :key="c.label">
        <el-card shadow="hover" class="overview-card">
          <div class="oc-label">{{ c.label }}</div>
          <div class="oc-value" :style="{ color: c.color }">{{ c.value }}</div>
          <div class="oc-sub" v-if="c.sub">{{ c.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统信息 -->
    <el-card class="section-card">
      <template #header><span>系统信息</span></template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="操作系统">{{ data?.system?.osName }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ data?.system?.osVersion }}</el-descriptions-item>
        <el-descriptions-item label="架构">{{ data?.system?.osArch }}</el-descriptions-item>
        <el-descriptions-item label="CPU 核心数">{{ data?.system?.availableProcessors }}</el-descriptions-item>
        <el-descriptions-item label="系统 CPU 负载">
          <el-progress :percentage="cpuPct(data?.system?.systemCpuLoad)" :stroke-width="12" />
        </el-descriptions-item>
        <el-descriptions-item label="进程 CPU 负载">
          <el-progress :percentage="cpuPct(data?.system?.processCpuLoad)" :stroke-width="12" />
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- JVM 信息 -->
    <el-card class="section-card">
      <template #header><span>JVM 运行时</span></template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="Java 版本">{{ data?.jvm?.javaVersion }}</el-descriptions-item>
        <el-descriptions-item label="JVM 名称">{{ data?.jvm?.jvmName }}</el-descriptions-item>
        <el-descriptions-item label="JVM 厂商">{{ data?.jvm?.jvmVendor }}</el-descriptions-item>
        <el-descriptions-item label="进程 ID">{{ data?.jvm?.pid }}</el-descriptions-item>
        <el-descriptions-item label="启动时间">{{ data?.jvm?.startTime }}</el-descriptions-item>
        <el-descriptions-item label="运行时长">{{ data?.jvm?.uptime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 内存 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card class="section-card">
          <template #header><span>堆内存</span></template>
          <el-progress type="dashboard" :percentage="heapPct" :width="140" :stroke-width="12">
            <template #default>{{ heapPct }}%</template>
          </el-progress>
          <div class="mem-detail">
            <p>已用: {{ fmtBytes(data?.memory?.heapUsed) }}</p>
            <p>最大: {{ fmtBytes(data?.memory?.heapMax) }}</p>
            <p>已分配: {{ fmtBytes(data?.memory?.heapCommitted) }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card">
          <template #header><span>物理内存</span></template>
          <el-progress type="dashboard" :percentage="physPct" :width="140" :stroke-width="12">
            <template #default>{{ physPct }}%</template>
          </el-progress>
          <div class="mem-detail">
            <p>已用: {{ fmtBytes(data?.memory?.physicalTotal - data?.memory?.physicalFree) }}</p>
            <p>总计: {{ fmtBytes(data?.memory?.physicalTotal) }}</p>
            <p>空闲: {{ fmtBytes(data?.memory?.physicalFree) }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 磁盘 -->
    <el-card class="section-card">
      <template #header><span>磁盘分区</span></template>
      <el-table :data="data?.disk || []" size="small" stripe>
        <el-table-column prop="path" label="路径" width="120" />
        <el-table-column label="总计" width="140">
          <template #default="{ row }">{{ fmtBytes(row.total) }}</template>
        </el-table-column>
        <el-table-column label="已用" width="140">
          <template #default="{ row }">{{ fmtBytes(row.total - row.free) }}</template>
        </el-table-column>
        <el-table-column label="可用" width="140">
          <template #default="{ row }">{{ fmtBytes(row.usable) }}</template>
        </el-table-column>
        <el-table-column label="使用率">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.usagePercent)" :stroke-width="16"
              :status="row.usagePercent > 85 ? 'exception' : row.usagePercent > 60 ? 'warning' : 'success'" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 线程 & 类加载 & DB & Redis -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="section-card">
          <template #header><span>线程</span></template>
          <div class="mini-stat">
            <div>活跃: <b>{{ data?.threads?.liveCount }}</b></div>
            <div>守护: <b>{{ data?.threads?.daemonCount }}</b></div>
            <div>峰值: <b>{{ data?.threads?.peakCount }}</b></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="section-card">
          <template #header><span>类加载</span></template>
          <div class="mini-stat">
            <div>已加载: <b>{{ data?.classes?.loadedCount }}</b></div>
            <div>已卸载: <b>{{ data?.classes?.unloadedCount }}</b></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="section-card">
          <template #header><span>数据库连接池</span></template>
          <div class="mini-stat">
            <div>活跃: <b>{{ data?.db?.activeCount }}</b></div>
            <div>空闲: <b>{{ data?.db?.idleCount }}</b></div>
            <div>最大: <b>{{ data?.db?.maxActive }}</b></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="section-card">
          <template #header><span>Redis</span></template>
          <div class="mini-stat">
            <div>
              状态:
              <el-tag :type="data?.redis?.connected ? 'success' : 'danger'" size="small">
                {{ data?.redis?.connected ? '已连接' : '未连接' }}
              </el-tag>
            </div>
            <div>{{ data?.redis?.info }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ES 向量数据库 -->
    <el-card class="section-card">
      <template #header>
        <span>
          ES 向量数据库
          <el-tag :type="data?.es?.connected ? 'success' : 'danger'" size="small" style="margin-left:8px">
            {{ data?.es?.connected ? "已连接" : "未连接" }}
          </el-tag>
          <el-tag v-if="data?.es?.status === 'green'" type="success" size="small" style="margin-left:4px">Green</el-tag>
          <el-tag v-else-if="data?.es?.status === 'yellow'" type="warning" size="small" style="margin-left:4px">Yellow</el-tag>
          <el-tag v-else-if="data?.es?.status === 'red'" type="danger" size="small" style="margin-left:4px">Red</el-tag>
        </span>
      </template>

      <el-descriptions :column="4" border size="small" style="margin-bottom:12px">
        <el-descriptions-item label="集群名称">{{ data?.es?.clusterName }}</el-descriptions-item>
        <el-descriptions-item label="节点名称">{{ data?.es?.nodeName }}</el-descriptions-item>
        <el-descriptions-item label="ES 版本">{{ data?.es?.version }}</el-descriptions-item>
        <el-descriptions-item label="索引总数">{{ data?.es?.indexCount }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="data?.es?.indices || []" size="small" stripe max-height="300">
        <el-table-column prop="name" label="索引名称" min-width="180" />
        <el-table-column label="文档数" width="100">
          <template #default="{ row }">{{ fmtCount(row.docCount) }}</template>
        </el-table-column>
        <el-table-column label="存储大小" width="120">
          <template #default="{ row }">{{ fmtBytes(row.storeSizeBytes) }}</template>
        </el-table-column>
        <el-table-column label="健康状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.health === 'green' ? 'success' : row.health === 'yellow' ? 'warning' : 'danger'" size="small">
              {{ row.health?.toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:8px; font-size:13px; color:#909399;">
        总计: {{ data?.es?.indexCount || 0 }} 个索引,
        {{ fmtCount(data?.es?.totalDocCount || 0) }} 篇文档,
        {{ fmtBytes(data?.es?.totalStoreSizeBytes || 0) }}
        <template v-if="data?.es?.indices">
          <el-tag v-if="hasIdx('knowledge_md')" type="primary" size="small" style="margin-left:8px">
            知识库: {{ getIdxDoc('knowledge_md') }}
          </el-tag>
          <el-tag v-if="hasIdx('jtcsm_recipe')" type="primary" size="small" style="margin-left:4px">
            食谱索引: {{ getIdxDoc('jtcsm_recipe') }}
          </el-tag>
          <el-tag v-if="hasIdx('jtcsm_knowledge_md')" type="primary" size="small" style="margin-left:4px">
            MD 知识库: {{ getIdxDoc('jtcsm_knowledge_md') }}
          </el-tag>
        </template>
      </div>

      <!-- ES 搜索 -->
      <el-divider />
      <div class="es-search-bar">
        <el-select v-model="searchIndex" placeholder="选择索引" size="small" style="width:200px;margin-right:8px">
          <el-option label="全部索引" value="" />
          <el-option v-for="idx in data?.es?.indices || []" :key="idx.name" :label="idx.name" :value="idx.name" />
        </el-select>
        <el-input v-model="searchQuery" placeholder="输入搜索关键词..." size="small"
          clearable style="width:300px;margin-right:8px" @keyup.enter="doSearch" />
        <el-button type="primary" size="small" @click="doSearch" :loading="searchLoading">搜索</el-button>
        <el-button size="small" @click="clearSearch">清空</el-button>
      </div>

      <!-- 搜索统计 -->
      <div v-if="searchResult" style="margin:8px 0;font-size:13px;color:#909399;">
        查询耗时 {{ searchResult.took }}ms，共 {{ searchResult.total }} 条结果
        <el-button text size="small" @click="searchResult = null">关闭</el-button>
      </div>

      <!-- 搜索结果 -->
      <el-table v-if="searchResult?.hits?.length" :data="searchResult.hits" size="small" stripe max-height="500">
        <el-table-column label="#" width="50" type="index" />
        <el-table-column prop="_id" label="ID" width="80" />
        <el-table-column label="评分" width="80">
          <template #default="{ row }">{{ row._score?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="内容" min-width="300">
          <template #default="{ row }">
            <div v-if="highlightText(row)" v-html="highlightText(row)" class="hl-content"></div>
            <div v-else class="src-content">{{ truncate(row._source?.content || row._source?.name || JSON.stringify(row._source), 200) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button text size="small" @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 详情弹窗 -->
      <el-dialog v-model="detailVisible" :title="'文档 ' + detailDoc?._id" width="60%">
        <pre class="json-view">{{ JSON.stringify(detailDoc?._source, null, 2) }}</pre>
      </el-dialog>
    </el-card>

    <el-button type="primary" @click="refresh" :loading="loading" class="refresh-btn">
      刷新
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getSystemMonitor } from '@/api/monitor'
import { esSearch } from '@/api/es'
import type { SystemMonitor } from '@/api/types'

const data = ref<SystemMonitor | null>(null)
const loading = ref(false)
const searchQuery = ref('')
const searchIndex = ref('')
const searchLoading = ref(false)
const searchResult = ref<any>(null)
const detailVisible = ref(false)
const detailDoc = ref<any>(null)

const overviewCards = computed(() => [
  { label: '运行时长', value: data.value?.jvm?.uptime || '-', color: '#409eff', sub: 'PID: ' + (data.value?.jvm?.pid || '-') },
  { label: '堆内存', value: heapPct.value + '%', color: heapPct.value > 80 ? '#e74c3c' : '#67c23a', sub: fmtBytes(data.value?.memory?.heapUsed) + ' / ' + fmtBytes(data.value?.memory?.heapMax) },
  { label: '物理内存', value: physPct.value + '%', color: physPct.value > 80 ? '#e74c3c' : '#67c23a', sub: fmtBytes((data.value?.memory?.physicalTotal || 0) - (data.value?.memory?.physicalFree || 0)) + ' / ' + fmtBytes(data.value?.memory?.physicalTotal) },
  { label: '活跃线程', value: String(data.value?.threads?.liveCount || 0), color: '#e6a23c', sub: '峰值: ' + (data.value?.threads?.peakCount || 0) },
])

const heapPct = computed(() => Math.round(data.value?.memory?.heapUsagePercent || 0))
const physPct = computed(() => Math.round(data.value?.memory?.physicalUsagePercent || 0))

function cpuPct(v: number | undefined): number {
  if (v === undefined || v < 0) return 0
  return Math.round(v * 100)
}

function fmtCount(n: number | undefined): string {
  if (!n || n <= 0) return "0"
  if (n >= 10000) return (n / 10000).toFixed(1) + " 万"
  return String(n)
}

function fmtBytes(bytes: number | undefined): string {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let b = bytes
  while (b >= 1024 && i < units.length - 1) { b /= 1024; i++ }
  return b.toFixed(b >= 100 ? 0 : 1) + ' ' + units[i]
}

function hasIdx(name: string): boolean {
  return data.value?.es?.indices?.some(i => i.name.includes(name)) || false
}

function getIdxDoc(name: string): string {
  const idx = data.value?.es?.indices?.find(i => i.name.includes(name))
  return idx ? fmtCount(idx.docCount) : "0"
}

function truncate(s: string, max: number): string {
  return s?.length > max ? s.substring(0, max) + '...' : (s || '')
}

function highlightText(row: any): string {
  if (row.highlight?.content) return row.highlight.content.join('...')
  if (row.highlight?.name) return row.highlight.name.join('...')
  if (row.highlight?.recipe_name) return row.highlight.recipe_name.join('...')
  return ''
}

function showDetail(row: any) {
  detailDoc.value = row
  detailVisible.value = true
}

async function doSearch() {
  if (!searchQuery.value.trim()) return
  searchLoading.value = true
  try {
    searchResult.value = await esSearch({
      index: searchIndex.value || undefined,
      query: searchQuery.value.trim(),
      size: 20,
    })
  } catch {
    searchResult.value = null
  }
  searchLoading.value = false
}

function clearSearch() {
  searchQuery.value = ''
  searchIndex.value = ''
  searchResult.value = null
}

async function refresh() {
  loading.value = true
  try {
    data.value = await getSystemMonitor()
  } catch { /* ignore */ }
  loading.value = false
}

onMounted(refresh)
</script>

<style scoped>
.monitor-page { max-width: 1400px; }
.overview-row { margin-bottom: 16px; }
.overview-card { text-align: center; }
.oc-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.oc-value { font-size: 26px; font-weight: 700; }
.oc-sub { font-size: 12px; color: #c0c4cc; margin-top: 2px; }
.section-card { margin-bottom: 16px; }
.mem-detail { text-align: center; margin-top: 12px; }
.mem-detail p { margin: 4px 0; font-size: 13px; color: #606266; }
.mini-stat { font-size: 14px; line-height: 1.8; }
.mini-stat div { margin: 4px 0; }
.refresh-btn { margin-top: 8px; }
.es-search-bar { display: flex; align-items: center; flex-wrap: wrap; margin-bottom: 8px; }
.hl-content { line-height: 1.6; font-size: 13px; }
.hl-content :deep(em) { color: #e74c3c; font-style: normal; background: #fef0f0; padding: 0 2px; border-radius: 2px; }
.src-content { font-size: 13px; color: #606266; line-height: 1.5; }
.json-view { background: #f8f9fa; padding: 16px; border-radius: 4px; font-size: 12px; overflow: auto; max-height: 500px; }
</style>

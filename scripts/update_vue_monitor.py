import re

with open("D:\\Project\\Jtcsm\\frontend\\jtcsm-web\\src\\views\\SystemMonitor.vue", "r", encoding="utf-8") as f:
    content = f.read()

# Add ES card after Redis section
old = '      </el-col>\n    </el-row>\n\n    <el-button'

new = '''      </el-col>
    </el-row>

    <!-- ES 向量数据库 -->
    <el-card class="section-card">
      <template #header>
        <span>
          ES 向量数据库
          <el-tag :type="data?.es?.connected ? \'success\' : \'danger\'" size="small" style="margin-left:8px">
            {{ data?.es?.connected ? "已连接" : "未连接" }}
          </el-tag>
          <el-tag v-if="data?.es?.status === \'green\'" type="success" size="small" style="margin-left:4px">Green</el-tag>
          <el-tag v-else-if="data?.es?.status === \'yellow\'" type="warning" size="small" style="margin-left:4px">Yellow</el-tag>
          <el-tag v-else-if="data?.es?.status === \'red\'" type="danger" size="small" style="margin-left:4px">Red</el-tag>
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
            <el-tag :type="row.health === \'green\' ? \'success\' : row.health === \'yellow\' ? \'warning\' : \'danger\'" size="small">
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
          <el-tag v-if="hasIdx(\'knowledge_md\')" type="primary" size="small" style="margin-left:8px">
            知识库: {{ getIdxDoc(\'knowledge_md\') }}
          </el-tag>
          <el-tag v-if="hasIdx(\'jtcsm_recipe\')" type="primary" size="small" style="margin-left:4px">
            食谱索引: {{ getIdxDoc(\'jtcsm_recipe\') }}
          </el-tag>
          <el-tag v-if="hasIdx(\'jtcsm_knowledge_md\')" type="primary" size="small" style="margin-left:4px">
            MD 知识库: {{ getIdxDoc(\'jtcsm_knowledge_md\') }}
          </el-tag>
        </template>
      </div>
    </el-card>

    <el-button'''

content = content.replace(old, new)

# Add helper functions
old_fn = '''function fmtBytes(bytes: number | undefined): string {'''
new_fn = '''function fmtCount(n: number | undefined): string {
  if (!n || n <= 0) return "0"
  if (n >= 10000) return (n / 10000).toFixed(1) + " 万"
  return String(n)
}

function fmtBytes(bytes: number | undefined): string {'''

content = content.replace(old_fn, new_fn)

# Add Vue helper methods
old_script = '''async function refresh() {'''

new_script = '''function hasIdx(name: string): boolean {
  return data.value?.es?.indices?.some(i => i.name.includes(name)) || false
}

function getIdxDoc(name: string): string {
  const idx = data.value?.es?.indices?.find(i => i.name.includes(name))
  return idx ? fmtCount(idx.docCount) : "0"
}

async function refresh() {'''

content = content.replace(old_script, new_script)

with open("D:\\Project\\Jtcsm\\frontend\\jtcsm-web\\src\\views\\SystemMonitor.vue", "w", encoding="utf-8") as f:
    f.write(content)
print("Vue page updated successfully")

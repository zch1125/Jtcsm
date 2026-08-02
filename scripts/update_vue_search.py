with open("D:\\Project\\Jtcsm\\frontend\\jtcsm-web\\src\\views\\SystemMonitor.vue", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add search form inside ES card, after summary div and before </el-card>
old = """      </div>
    </el-card>"""

new = """      </div>

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
    </el-card>"""

content = content.replace(old, new)

# 2. Add imports in script
old_import = """import { getSystemMonitor } from '@/api/monitor'"""
new_import = """import { getSystemMonitor } from '@/api/monitor'
import { esSearch } from '@/api/es'"""
content = content.replace(old_import, new_import)

# 3. Add search state variables
old_state = """const data = ref<SystemMonitor | null>(null)
const loading = ref(false)"""
new_state = """const data = ref<SystemMonitor | null>(null)
const loading = ref(false)
const searchQuery = ref('')
const searchIndex = ref('')
const searchLoading = ref(false)
const searchResult = ref<any>(null)
const detailVisible = ref(false)
const detailDoc = ref<any>(null)"""
content = content.replace(old_state, new_state)

# 4. Add search functions after refresh()
old_fn = """async function refresh() {"""
new_fn = """function truncate(s: string, max: number): string {
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

async function refresh() {"""
content = content.replace(old_fn, new_fn)

with open("D:\\Project\\Jtcsm\\frontend\\jtcsm-web\\src\\views\\SystemMonitor.vue", "w", encoding="utf-8") as f:
    f.write(content)
print("Vue page updated with ES search")

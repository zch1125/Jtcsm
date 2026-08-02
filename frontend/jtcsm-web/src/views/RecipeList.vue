<template>
  <div>
    <div class="page-header">
      <h3>菜谱管理</h3>
      <div class="header-right">
        <el-input v-model="keyword" placeholder="搜索菜名" clearable style="width:220px" @keyup.enter="search" />
        <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增菜谱</el-button>
      </div>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
      <el-table-column type="index" label="#" width="60" :index="calcIndex" />
      <el-table-column prop="name" label="菜名" min-width="140" />
      <el-table-column prop="cuisine" label="菜系" width="100" />
      <el-table-column prop="difficulty" label="难度" width="80" />
      <el-table-column prop="cookMethod" label="烹饪方式" width="100" />
      <el-table-column prop="cookTime" label="烹饪时间（分）" width="110" sortable  />
      <el-table-column prop="calories" label="热量（千卡）" width="100" sortable  />
      <el-table-column prop="viewCount" label="浏览量" width="80" sortable  />
      <el-table-column prop="favoriteCount" label="收藏数" width="80" sortable  />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="handleSizeChange"
      />
    </div>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜谱' : '新增菜谱'" width="600px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="菜名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="封面图"><el-input v-model="form.coverImage" placeholder="图片URL" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="菜系"><el-input v-model="form.cuisine" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度">
              <el-select v-model="form.difficulty">
                <el-option label="简单" value="简单" />
                <el-option label="普通" value="普通" />
                <el-option label="困难" value="困难" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="烹饪方式"><el-input v-model="form.cookMethod" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="烹饪时间"><el-input-number v-model="form.cookTime" :min="1" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="热量"><el-input-number v-model="form.calories" :min="0" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRecipeList, createRecipe, updateRecipe, deleteRecipe } from '@/api/recipe'
import type { RecipeItem } from '@/api/types'

const loading = ref(false)
const tableData = ref<RecipeItem[]>([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const size = ref(20)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<RecipeItem>>({})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getRecipeList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    tableData.value = res.records
    total.value = res.total
  } catch { }
  finally { loading.value = false }
}

function search() {
  page.value = 1
  fetchData()
}

function handleSizeChange(val: number) {
  size.value = val
  page.value = 1
  fetchData()
}

function calcIndex(index: number): number {
  return (page.value - 1) * size.value + index + 1
}

function resetForm() {
  form.value = { name: '', cuisine: '', difficulty: '简单', cookTime: 30, calories: 0 }
  editId.value = null
}

function openCreate() { isEdit.value = false; resetForm(); dialogVisible.value = true }

function openEdit(row: RecipeItem) {
  isEdit.value = true; editId.value = row.id ?? null; form.value = { ...row }; dialogVisible.value = true
}

async function handleSave() {
  try {
    if (isEdit.value && editId.value) { await updateRecipe(editId.value, form.value); ElMessage.success('更新成功') }
    else { await createRecipe(form.value); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch { }
}

async function handleDelete(row: RecipeItem) {
  await ElMessageBox.confirm(`确认删除菜谱「${row.name}」？`, '提示', { type: 'warning' })
  try { await deleteRecipe(row.id!); ElMessage.success('删除成功'); fetchData() } catch { }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-right { display: flex; gap: 12px; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>

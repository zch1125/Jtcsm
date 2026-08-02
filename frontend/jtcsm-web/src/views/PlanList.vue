<template>
  <div>
    <div class="page-header">
      <h3>套餐管理</h3>
      <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增套餐</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="套餐名称" width="120" />
      <el-table-column prop="price" label="售价" width="100">
        <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="originalPrice" label="原价" width="100">
        <template #default="{ row }">¥{{ Number(row.originalPrice).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="days" label="有效期(天)" width="110" />
      <el-table-column prop="description" label="套餐说明" min-width="180" />
      <el-table-column prop="isEnabled" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'">{{ row.isEnabled === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170">
        <template #default="{ row }">{{ row.createTime?.replace('T', ' ').substring(0, 19) }}</template>
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
        @change="fetchData"
      />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑套餐' : '新增套餐'" width="500px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="套餐名称"><el-input v-model="form.name" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="售价"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价"><el-input-number v-model="form.originalPrice" :min="0" :precision="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="有效期(天)"><el-input-number v-model="form.days" :min="1" /></el-form-item>
        <el-form-item label="套餐说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
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
import { getPlanList, createPlan, updatePlan, deletePlan } from '@/api/plan'
import type { PlanItem } from '@/api/types'

const loading = ref(false)
const tableData = ref<PlanItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<PlanItem>>({})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getPlanList({ page: page.value, size: size.value })
    tableData.value = res.records
    total.value = res.total
  } catch { /* 错误已在拦截器中处理 */ }
  finally { loading.value = false }
}

function resetForm() {
  form.value = { name: '', price: 0, originalPrice: 0, days: 30, description: '', isEnabled: 1 }
  editId.value = null
}

function openCreate() { isEdit.value = false; resetForm(); dialogVisible.value = true }

function openEdit(row: PlanItem) {
  isEdit.value = true
  editId.value = row.id ?? null
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (isEdit.value && editId.value) {
      await updatePlan(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createPlan(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* 错误已在拦截器中处理 */ }
}

async function handleDelete(row: PlanItem) {
  await ElMessageBox.confirm(`确认删除套餐「${row.name}」？`, '提示', { type: 'warning' })
  try {
    await deletePlan(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* 错误已在拦截器中处理 */ }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>

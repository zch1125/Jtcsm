<template>
  <div>
    <div class="page-header">
      <h3>用户管理</h3>
      <el-input v-model="keyword" placeholder="搜索昵称/手机号" clearable style="width:240px" @keyup.enter="search" />
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="nickname" label="昵称" min-width="120">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar v-if="row.avatar" :size="32" :src="row.avatar" />
            <span>{{ row.nickname || '未设置' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="gender" label="性别" width="70">
        <template #default="{ row }">{{ { 0: '未知', 1: '男', 2: '女' }[row.gender ?? 0] }}</template>
      </el-table-column>
      <el-table-column prop="isVip" label="VIP" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isVip === 1 ? 'warning' : 'info'">{{ row.isVip === 1 ? 'VIP' : '普通' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170">
        <template #default="{ row }">{{ row.createTime?.replace('T', ' ').substring(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'"
            @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, toggleUserStatus } from '@/api/user'
import type { UserItem } from '@/api/types'

const loading = ref(false)
const tableData = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserList({ page: page.value, size: size.value, keyword: keyword.value })
    tableData.value = res.records
    total.value = res.total
  } catch { /* 错误已在拦截器中处理 */ }
  finally { loading.value = false }
}

function search() { page.value = 1; fetchData() }

async function toggleStatus(row: UserItem) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户「${row.nickname || row.id}」？`, '提示', { type: 'warning' })
  try {
    await toggleUserStatus(row.id!, newStatus)
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch { /* 错误已在拦截器中处理 */ }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.user-cell { display: flex; align-items: center; gap: 8px; }
</style>

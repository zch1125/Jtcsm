<template>
  <div>
    <div class="page-header">
      <h3>订单管理</h3>
      <div class="header-right">
        <el-select v-model="statusFilter" placeholder="订单状态" clearable style="width:140px" @change="search">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索订单号" clearable style="width:220px" @keyup.enter="search" />
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="planId" label="套餐ID" width="100" />
      <el-table-column prop="amount" label="金额" width="100">
        <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payTime" label="支付时间" width="170">
        <template #default="{ row }">{{ formatTime(row.payTime) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
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
import { getOrderList } from '@/api/order'
import type { OrderItem } from '@/api/types'

const loading = ref(false)
const tableData = ref<OrderItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const statusFilter = ref<number | undefined>()

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderList({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value
    })
    tableData.value = res.records
    total.value = res.total
  } catch { /* 错误已在拦截器中处理 */ }
  finally { loading.value = false }
}

function search() { page.value = 1; fetchData() }

function statusLabel(s?: number): string {
  const map: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已取消' }
  return map[s ?? 0] ?? '未知'
}

function statusTagType(s?: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[s ?? 0] ?? 'info'
}

function formatTime(t?: string): string {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-right { display: flex; gap: 12px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>

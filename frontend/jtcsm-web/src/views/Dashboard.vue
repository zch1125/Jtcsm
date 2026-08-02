<template>
  <div>
    <h3>仪表盘</h3>
    <el-row :gutter="20">
      <el-col :span="6" v-for="s in stats" :key="s.title">
        <el-card class="stat-card">
          <div class="stat-title">{{ s.title }}</div>
          <div class="stat-value" :style="{ color: s.color }">{{ s.display }}</div>
          <div class="stat-sub" v-if="s.sub">{{ s.sub }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboard } from '@/api/dashboard'
import type { AdminDashboard } from '@/api/types'

const stats = ref<
  { title: string; display: string; color: string; sub?: string }[]
>([])

onMounted(async () => {
  try {
    const data: AdminDashboard = await getDashboard()
    stats.value = [
      { title: '菜谱总数', display: String(data.totalRecipes), color: '#e74c3c' },
      { title: '注册用户', display: String(data.totalUsers), color: '#3498db' },
      {
        title: 'VIP 会员',
        display: String(data.vipUsers),
        color: '#f39c12',
        sub: `共 ${data.totalUsers} 注册`
      },
      {
        title: '今日订单',
        display: String(data.todayOrders),
        color: '#2ecc71',
        sub: `¥${Number(data.todayRevenue).toFixed(2)}`
      },
      { title: '总订单数', display: String(data.totalOrders), color: '#9b59b6' },
      { title: '总营收', display: `¥${Number(data.totalRevenue).toFixed(2)}`, color: '#1abc9c' }
    ]
  } catch {
    stats.value = []
  }
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 20px;
  text-align: center;
}
.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
}
.stat-sub {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>

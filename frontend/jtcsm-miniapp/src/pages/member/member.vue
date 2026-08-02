<template>
  <view class="page">
    <!-- VIP 状态卡片 -->
    <view class="status-card" :class="{ vip: memberStatus.isVip }">
      <view class="vip-icon">{{ memberStatus.isVip ? '👑' : '🎯' }}</view>
      <view class="status-text">
        <text class="status-title">{{ memberStatus.isVip ? 'VIP 会员' : '普通用户' }}</text>
        <text v-if="memberStatus.isVip && memberStatus.remainingDays != null" class="status-desc">
          剩余 {{ memberStatus.remainingDays }} 天
        </text>
        <text v-if="memberStatus.isVip && memberStatus.planName" class="status-desc">
          {{ memberStatus.planName }}
        </text>
        <text v-if="!memberStatus.isVip" class="status-desc">开通会员解锁更多功能</text>
      </view>
    </view>

    <!-- 套餐列表 -->
    <view class="section">
      <text class="section-title">选择套餐</text>
      <view class="plan-list">
        <view
          v-for="plan in plans"
          :key="plan.id"
          class="plan-card"
          :class="{ selected: selectedPlanId === plan.id }"
          @tap="selectPlan(plan.id)"
        >
          <view class="plan-header">
            <text class="plan-name">{{ plan.name }}</text>
            <view class="plan-price-box">
              <text class="plan-price-symbol">¥</text>
              <text class="plan-price">{{ plan.price }}</text>
            </view>
          </view>
          <text class="plan-original">原价 ¥{{ plan.originalPrice }}</text>
          <text class="plan-days">有效期 {{ plan.days }} 天</text>
          <text v-if="plan.description" class="plan-desc">{{ plan.description }}</text>
        </view>
      </view>
    </view>

    <!-- 购买按钮 -->
    <view class="bottom-bar">
      <button class="buy-btn" :disabled="!selectedPlanId || buying" @tap="handleBuy">
        {{ buying ? '处理中...' : (selectedPlanId ? '立即开通' : '请选择套餐') }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPlans, getMembershipStatus, createOrder, simulatePay } from '../../api/index'

interface MembershipPlan {
  id: number
  name: string
  price: number
  originalPrice: number
  days: number
  description: string
}

interface MembershipStatus {
  isVip: boolean
  vipExpireTime: string
  planName: string
  remainingDays: number
}

const plans = ref<MembershipPlan[]>([])
const selectedPlanId = ref<number | null>(null)
const memberStatus = ref<MembershipStatus>({ isVip: false, vipExpireTime: '', planName: '', remainingDays: 0 })
const buying = ref(false)

function selectPlan(planId: number) {
  selectedPlanId.value = planId
}

async function handleBuy() {
  if (!selectedPlanId.value || buying.value) return
  buying.value = true
  try {
    const orderNo: any = await createOrder(selectedPlanId.value)
    // 开发环境模拟支付
    await simulatePay(orderNo)
    uni.showToast({ title: '开通成功！', icon: 'success' })
    // 刷新会员状态
    await fetchMemberStatus()
    selectedPlanId.value = null
  } catch (e) {
    // 错误已在 request 中统一处理
  } finally {
    buying.value = false
  }
}

async function fetchPlans() {
  try {
    plans.value = await getPlans()
  } catch (e) { /* */ }
}

async function fetchMemberStatus() {
  try {
    memberStatus.value = await getMembershipStatus()
  } catch (e) { /* */ }
}

onMounted(() => {
  fetchPlans()
  fetchMemberStatus()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 120rpx;
}

.status-card {
  display: flex;
  align-items: center;
  margin: 24rpx;
  padding: 32rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 16rpx;
  color: #fff;

  &.vip {
    background: linear-gradient(135deg, #f093fb, #f5576c);
  }

  .vip-icon {
    font-size: 64rpx;
    margin-right: 24rpx;
  }

  .status-text {
    display: flex;
    flex-direction: column;

    .status-title {
      font-size: 36rpx;
      font-weight: bold;
    }

    .status-desc {
      font-size: 26rpx;
      opacity: 0.85;
      margin-top: 8rpx;
    }
  }
}

.section {
  margin: 0 24rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 16rpx;
  }
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.plan-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 28rpx;
  border: 2rpx solid transparent;

  &.selected {
    border-color: #e74c3c;
    background: #fef5f5;
  }

  .plan-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .plan-name {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
    }

    .plan-price-box {
      display: flex;
      align-items: baseline;

      .plan-price-symbol {
        font-size: 28rpx;
        color: #e74c3c;
      }

      .plan-price {
        font-size: 48rpx;
        font-weight: bold;
        color: #e74c3c;
      }
    }
  }

  .plan-original {
    font-size: 24rpx;
    color: #999;
    text-decoration: line-through;
  }

  .plan-days {
    font-size: 26rpx;
    color: #666;
    margin-top: 4rpx;
  }

  .plan-desc {
    font-size: 24rpx;
    color: #999;
    margin-top: 4rpx;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 32rpx;
  padding-bottom: 40rpx;
  background: #fff;
  box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.06);

  .buy-btn {
    width: 100%;
    height: 88rpx;
    line-height: 88rpx;
    background: #e74c3c;
    color: #fff;
    border-radius: 44rpx;
    font-size: 32rpx;
    font-weight: bold;
    border: none;

    &[disabled] {
      background: #ccc;
      color: #fff;
    }
  }
}
</style>
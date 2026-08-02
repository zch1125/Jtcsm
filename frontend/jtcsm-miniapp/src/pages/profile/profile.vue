<template>
  <view class="page">
    <!-- 用户头部 -->
    <view class="header">
      <image :src="avatar || '/static/tab-profile.png'" class="avatar" />
      <text class="nickname">{{ nickname || "未登录" }}</text>
      <text class="vip-badge" v-if="vipStatus.isVip">VIP · {{ vipStatus.planName }}</text>
      <text class="vip-badge guest" v-else @tap="goMember">开通会员</text>
    </view>

    <!-- 会员信息卡片 -->
    <view class="vip-card" v-if="vipStatus.isVip">
      <text class="vip-label">会员有效期至：</text>
      <text class="vip-time">{{ vipStatus.vipExpireTime }}</text>
      <text class="vip-remaining">剩余 {{ vipStatus.remainingDays }} 天</text>
    </view>

    <!-- 菜单列表 -->
    <view class="menu">
      <view class="menu-item" @tap="goMember">
        <text>会员中心</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="goFavorites">
        <text>我的收藏</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="goAI">
        <text>AI 生成</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="goOrderHistory">
        <text>订单记录</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <button class="logout-btn" @tap="handleLogout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import {
  getUserProfile,
  getMembershipStatus,
} from "../../api/index"
import { useUserStore } from "../../store/index"

const userStore = useUserStore()

const nickname = ref(userStore.nickname || "")
const avatar = ref(userStore.avatar || "")
const vipStatus = ref<any>({ isVip: false, vipExpireTime: "", planName: "", remainingDays: 0 })

async function loadProfile() {
  try {
    const user: any = await getUserProfile()
    nickname.value = user.nickname || "美食家"
    avatar.value = user.avatar || ""
  } catch (e) {
    // 未登录时忽略
  }
}

async function loadVipStatus() {
  try {
    vipStatus.value = await getMembershipStatus()
    // 同步到 store
    if (vipStatus.value.isVip) {
      userStore.isVip = true
    }
  } catch (e) {
    // 忽略
  }
}

function goMember() {
  uni.navigateTo({ url: "/pages/member/member" })
}
function goFavorites() {
  uni.navigateTo({ url: "/pages/favorites/favorites" })
}
function goAI() {
  uni.switchTab({ url: "/pages/ai/ai" })
}
function goOrderHistory() {
  uni.showToast({ title: "订单功能开发中", icon: "none" })
}

function handleLogout() {
  uni.showModal({
    title: "提示",
    content: "确定退出登录吗？",
    success: (res) => {
      if (res.confirm) {
        userStore.clearLoginInfo()
        uni.showToast({ title: "已退出", icon: "success" })
        // 重新自动登录
        setTimeout(() => {
          uni.reLaunch({ url: "/pages/index/index" })
        }, 1000)
      }
    },
  })
}

onMounted(() => {
  loadProfile()
  loadVipStatus()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f8f8f8;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.2);
}
.nickname {
  font-size: 36rpx;
  font-weight: bold;
  margin-top: 16rpx;
}
.vip-badge {
  font-size: 24rpx;
  color: #ffd700;
  background: rgba(255, 255, 255, 0.2);
  padding: 4rpx 20rpx;
  border-radius: 20rpx;
  margin-top: 8rpx;
  &.guest {
    color: #fff;
    background: rgba(255, 255, 255, 0.15);
  }
}

.vip-card {
  margin: 24rpx;
  padding: 24rpx;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  border-radius: 16rpx;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.vip-label {
  font-size: 26rpx;
  opacity: 0.85;
}
.vip-time {
  font-size: 28rpx;
  margin-top: 4rpx;
}
.vip-remaining {
  font-size: 32rpx;
  font-weight: bold;
  margin-top: 8rpx;
}

.menu {
  margin: 16rpx 24rpx;
  background: #fff;
  border-radius: 12rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 24rpx;
  font-size: 30rpx;
  color: #333;
  border-bottom: 1rpx solid #f5f5f5;
}
.arrow {
  color: #ccc;
  font-size: 36rpx;
}

.logout-btn {
  margin: 40rpx 24rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #fff;
  color: #e74c3c;
  border: 1rpx solid #e74c3c;
  border-radius: 40rpx;
  font-size: 28rpx;
}
</style>

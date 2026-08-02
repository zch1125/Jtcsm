<template>
  <view class="page">
    <!-- 用户头部 -->
    <view class="header">
      <image :src="avatar || '/static/tab-profile.png'" class="avatar" />
      <text class="nickname">{{ nickname || "未登录" }}</text>
    </view>

    <!-- 菜单列表 -->
    <view class="menu">
      <view class="menu-item" @tap="goFavorites">
        <text>我的收藏</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="goAI">
        <text>AI 生成</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <button class="logout-btn" @tap="handleLogout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { getUserProfile } from "../../api/index"
import { useUserStore } from "../../store/index"

const userStore = useUserStore()

const nickname = ref(userStore.nickname || "")
const avatar = ref(userStore.avatar || "")

async function loadProfile() {
  try {
    const user: any = await getUserProfile()
    nickname.value = user.nickname || "美食家"
    avatar.value = user.avatar || ""
  } catch (e) {
    // 未登录时忽略
  }
}

function goFavorites() {
  uni.navigateTo({ url: "/pages/favorites/favorites" })
}
function goAI() {
  uni.switchTab({ url: "/pages/ai/ai" })
}

function handleLogout() {
  uni.showModal({
    title: "提示",
    content: "确定退出登录吗？",
    success: (res) => {
      if (res.confirm) {
        userStore.clearLoginInfo()
        uni.showToast({ title: "已退出", icon: "success" })
        setTimeout(() => {
          uni.reLaunch({ url: "/pages/index/index" })
        }, 1000)
      }
    },
  })
}

onMounted(() => {
  loadProfile()
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

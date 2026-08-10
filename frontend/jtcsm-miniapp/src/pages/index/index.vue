<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索菜谱、食材、菜系..."
        confirm-type="search"
        @confirm="doSearch"
        @focus="showHistory = true"
      />
      <button class="search-btn" size="mini" @tap="doSearch">搜索</button>
    </view>

    <!-- 搜索历史（获得焦点时显示） -->
    <view class="search-history" v-if="showHistory && searchHistoryList.length > 0">
      <view class="history-header">
        <text class="history-title">搜索历史</text>
        <text class="history-clear" @tap="handleClearHistory">清空</text>
      </view>
      <view class="history-tags">
        <text
          v-for="item in searchHistoryList"
          :key="item.id"
          class="history-tag"
          @tap="tapHistory(item.keyword)"
        >{{ item.keyword }}</text>
      </view>
    </view>

    <!-- 每日推荐 -->
    <view class="section" v-if="dailyList.length">
      <text class="section-title">每日推荐</text>
      <scroll-view scroll-x class="h-scroll">
        <view v-for="r in dailyList" :key="r.id" class="h-card" @tap="goDetail(r.id)">
          <image :src="resolveImageUrl(r.coverImage)" mode="aspectFill" class="h-cover" />
          <text class="h-name">{{ r.name }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- 热门菜谱 -->
    <view class="section">
      <text class="section-title">热门菜谱</text>
      <view v-for="r in hotList" :key="r.id" class="recipe-row" @tap="goDetail(r.id)">
        <image :src="resolveImageUrl(r.coverImage)" mode="aspectFill" class="row-cover" />
        <view class="row-info">
          <text class="row-name">{{ r.name }}</text>
          <view class="row-tags">
            <text class="tag">{{ r.cuisine }}</text>
            <text class="tag">{{ r.difficulty }}</text>
          </view>
        </view>
        <text class="row-count">{{ r.favoriteCount }} 收藏</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import {
  getHotRecipes,
  getDailyRecommend,
  getSearchHistory,
  clearSearchHistory,
} from "../../api/index"
import { resolveImageUrl } from "../../utils/request"

const keyword = ref("")
const hotList = ref<any[]>([])
const dailyList = ref<any[]>([])
const searchHistoryList = ref<any[]>([])
const showHistory = ref(false)

async function loadData() {
  hotList.value = await getHotRecipes()
  try {
    dailyList.value = await getDailyRecommend()
  } catch (e) {
    // 每日推荐接口异常时静默处理
  }
  loadSearchHistory()
}

async function loadSearchHistory() {
  try {
    searchHistoryList.value = await getSearchHistory()
  } catch (e) {
    // 未登录时忽略
  }
}

function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  showHistory.value = false
  // 跳转到搜索结果页（使用 recipe 页面展示搜索结果）
  uni.navigateTo({
    url: "/pages/recipe/recipe?keyword=" + encodeURIComponent(kw),
  })
}

function tapHistory(kw: string) {
  keyword.value = kw
  showHistory.value = false
  uni.navigateTo({
    url: "/pages/recipe/recipe?keyword=" + encodeURIComponent(kw),
  })
}

async function handleClearHistory() {
  try {
    await clearSearchHistory()
    searchHistoryList.value = []
    uni.showToast({ title: "已清空", icon: "success" })
  } catch (e) {
    // 错误已统一处理
  }
}

function goDetail(id: number) {
  uni.navigateTo({ url: "/pages/recipe/recipe?id=" + id })
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page {
  background: #f8f8f8;
  min-height: 100vh;
  padding-bottom: 20rpx;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  padding: 20rpx;
  background: #fff;
}
.search-input {
  flex: 1;
  height: 64rpx;
  background: #f5f5f5;
  border-radius: 32rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}
.search-btn {
  margin-left: 16rpx;
  height: 64rpx;
  line-height: 64rpx;
  background: #e74c3c;
  color: #fff;
  border-radius: 32rpx;
  border: none;
  font-size: 26rpx;
}

/* 搜索历史 */
.search-history {
  background: #fff;
  padding: 0 24rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.history-title {
  font-size: 26rpx;
  color: #999;
}
.history-clear {
  font-size: 24rpx;
  color: #e74c3c;
}
.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.history-tag {
  font-size: 24rpx;
  color: #666;
  background: #f5f5f5;
  padding: 6rpx 20rpx;
  border-radius: 24rpx;
}

/* 通用区块 */
.section {
  margin: 24rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 16rpx;
  display: block;
}

/* 横向滚动推荐 */
.h-scroll {
  white-space: nowrap;
}
.h-card {
  display: inline-block;
  width: 200rpx;
  margin-right: 16rpx;
}
.h-cover {
  width: 200rpx;
  height: 140rpx;
  border-radius: 12rpx;
  background: #eee;
}
.h-name {
  font-size: 26rpx;
  color: #333;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 200rpx;
  display: block;
}

/* 菜谱行 */
.recipe-row {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 20rpx;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}
.row-cover {
  width: 120rpx;
  height: 80rpx;
  border-radius: 8rpx;
  background: #eee;
  flex-shrink: 0;
}
.row-info {
  flex: 1;
  margin-left: 16rpx;
}
.row-name {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}
.row-tags {
  margin-top: 6rpx;
  display: flex;
  gap: 12rpx;
}
.tag {
  font-size: 22rpx;
  color: #999;
  background: #f5f5f5;
  padding: 2rpx 8rpx;
  border-radius: 4rpx;
}
.row-count {
  font-size: 24rpx;
  color: #e74c3c;
  flex-shrink: 0;
}
</style>

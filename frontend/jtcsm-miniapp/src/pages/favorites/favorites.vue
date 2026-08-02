<template>
  <view class="page">
    <!-- 顶部 Tab 切换 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: currentTab === 'favorites' }"
        @tap="switchTab('favorites')"
      >
        我的收藏
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'history' }"
        @tap="switchTab('history')"
      >
        浏览历史
      </view>
    </view>

    <!-- 收藏列表 -->
    <scroll-view
      v-if="currentTab === 'favorites'"
      class="list-container"
      scroll-y
      @scrolltolower="loadMoreFavorites"
      refresher-enabled
      :refresher-triggered="favoritesRefreshing"
      @refresherrefresh="onFavoritesRefresh"
    >
      <view v-if="favoriteList.length === 0 && !favoritesLoading" class="empty">
        <image src="/static/empty-fav.png" class="empty-img" mode="aspectFit" />
        <text class="empty-text">还没有收藏菜谱，去首页看看吧</text>
      </view>
      <view
        v-for="item in favoriteList"
        :key="item.favoriteId"
        class="recipe-card"
        @tap="goDetail(item.recipeId)"
      >
        <image :src="item.coverImage" class="card-cover" mode="aspectFill" />
        <view class="card-info">
          <text class="card-name">{{ item.name }}</text>
          <view class="card-tags">
            <text class="tag">{{ item.cuisine }}</text>
            <text class="tag">{{ item.difficulty }}</text>
            <text class="tag">{{ item.cookTime }}分钟</text>
          </view>
        </view>
        <view class="card-actions">
          <text class="fav-time">{{ formatTime(item.createdAt) }}</text>
          <button class="unfav-btn" size="mini" @tap.stop="handleUnfavorite(item)">取消收藏</button>
        </view>
      </view>
      <view v-if="favoritesLoading" class="loading">
        <text>加载中...</text>
      </view>
    </scroll-view>

    <!-- 浏览历史列表 -->
    <scroll-view
      v-if="currentTab === 'history'"
      class="list-container"
      scroll-y
      @scrolltolower="loadMoreHistory"
      refresher-enabled
      :refresher-triggered="historyRefreshing"
      @refresherrefresh="onHistoryRefresh"
    >
      <view v-if="historyList.length === 0 && !historyLoading" class="empty">
        <image src="/static/empty-history.png" class="empty-img" mode="aspectFit" />
        <text class="empty-text">还没有浏览记录</text>
      </view>
      <view
        v-for="item in historyList"
        :key="item.historyId"
        class="recipe-card"
        @tap="goDetail(item.recipeId)"
      >
        <image :src="item.coverImage" class="card-cover" mode="aspectFill" />
        <view class="card-info">
          <text class="card-name">{{ item.name }}</text>
          <view class="card-tags">
            <text class="tag">{{ item.cuisine }}</text>
            <text class="tag">{{ item.difficulty }}</text>
            <text class="tag">{{ item.cookTime }}分钟</text>
          </view>
        </view>
        <view class="card-actions">
          <text class="fav-time">浏览于 {{ formatTime(item.viewedAt) }}</text>
        </view>
      </view>
      <view v-if="historyLoading" class="loading">
        <text>加载中...</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getFavorites, removeFavorite, getHistory } from '../../api/index'

// ==================== Tab 状态 ====================
const currentTab = ref<'favorites' | 'history'>('favorites')

function switchTab(tab: 'favorites' | 'history') {
  currentTab.value = tab
}

// ==================== 收藏列表 ====================
interface FavoriteItem {
  favoriteId: number
  recipeId: number
  name: string
  coverImage: string
  cuisine: string
  difficulty: string
  cookMethod: string
  cookTime: number
  createdAt: string
}

const favoriteList = ref<FavoriteItem[]>([])
const favoritesLoading = ref(false)
const favoritesRefreshing = ref(false)
let favoritesPage = 1
let favoritesHasMore = true

/** 加载收藏列表 */
async function fetchFavorites(reset = false) {
  if (favoritesLoading.value) return
  if (reset) {
    favoritesPage = 1
    favoritesHasMore = true
    favoriteList.value = []
  }
  if (!favoritesHasMore) return

  favoritesLoading.value = true
  try {
    const data = await getFavorites(favoritesPage, 20)
    if (reset) {
      favoriteList.value = data
    } else {
      favoriteList.value = [...favoriteList.value, ...data]
    }
    favoritesHasMore = data.length >= 20
    favoritesPage++
  } catch (e) {
    // 错误已在 request 中统一处理
  } finally {
    favoritesLoading.value = false
    favoritesRefreshing.value = false
  }
}

function loadMoreFavorites() {
  fetchFavorites(false)
}

function onFavoritesRefresh() {
  favoritesRefreshing.value = true
  fetchFavorites(true)
}

/** 取消收藏 */
async function handleUnfavorite(item: FavoriteItem) {
  try {
    await removeFavorite(item.recipeId)
    // 从列表中移除
    favoriteList.value = favoriteList.value.filter((f) => f.favoriteId !== item.favoriteId)
    uni.showToast({ title: '已取消收藏', icon: 'success' })
  } catch (e) {
    // 错误已在 request 中统一处理
  }
}

// ==================== 浏览历史 ====================
interface HistoryItem {
  historyId: number
  recipeId: number
  name: string
  coverImage: string
  cuisine: string
  difficulty: string
  cookMethod: string
  cookTime: number
  viewedAt: string
}

const historyList = ref<HistoryItem[]>([])
const historyLoading = ref(false)
const historyRefreshing = ref(false)
let historyPage = 1
let historyHasMore = true

/** 加载浏览历史 */
async function fetchHistory(reset = false) {
  if (historyLoading.value) return
  if (reset) {
    historyPage = 1
    historyHasMore = true
    historyList.value = []
  }
  if (!historyHasMore) return

  historyLoading.value = true
  try {
    const data = await getHistory(historyPage, 20)
    if (reset) {
      historyList.value = data
    } else {
      historyList.value = [...historyList.value, ...data]
    }
    historyHasMore = data.length >= 20
    historyPage++
  } catch (e) {
    // 错误已在 request 中统一处理
  } finally {
    historyLoading.value = false
    historyRefreshing.value = false
  }
}

function loadMoreHistory() {
  fetchHistory(false)
}

function onHistoryRefresh() {
  historyRefreshing.value = true
  fetchHistory(true)
}

// ==================== 通用 ====================

/** 跳转到菜谱详情 */
function goDetail(recipeId: number) {
  uni.navigateTo({ url: '/pages/recipe/recipe?id=' + recipeId })
}

/** 格式化时间 */
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => (n < 10 ? '0' + n : '' + n)
  return pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

// ==================== 初始化 ====================
fetchFavorites(true)
</script>

<style lang="scss" scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f8f8f8;
}

.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    font-size: 28rpx;
    color: #666;
    position: relative;

    &.active {
      color: #e74c3c;
      font-weight: bold;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: #e74c3c;
        border-radius: 2rpx;
      }
    }
  }
}

.list-container {
  flex: 1;
}

.recipe-card {
  display: flex;
  align-items: center;
  background: #fff;
  margin: 16rpx 24rpx;
  border-radius: 12rpx;
  padding: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);

  .card-cover {
    width: 140rpx;
    height: 100rpx;
    border-radius: 8rpx;
    flex-shrink: 0;
    background: #eee;
  }

  .card-info {
    flex: 1;
    margin-left: 16rpx;
    overflow: hidden;

    .card-name {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
      display: block;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .card-tags {
      margin-top: 8rpx;
      display: flex;
      gap: 12rpx;

      .tag {
        font-size: 22rpx;
        color: #999;
        background: #f5f5f5;
        padding: 2rpx 12rpx;
        border-radius: 4rpx;
      }
    }
  }

  .card-actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    flex-shrink: 0;
    margin-left: 16rpx;

    .fav-time {
      font-size: 22rpx;
      color: #bbb;
      margin-bottom: 8rpx;
    }

    .unfav-btn {
      font-size: 22rpx;
      color: #e74c3c;
      background: #fff;
      border: 1px solid #e74c3c;
      border-radius: 20rpx;
      padding: 4rpx 16rpx;
      line-height: 1.4;
    }
  }
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;

  .empty-img {
    width: 240rpx;
    height: 240rpx;
    margin-bottom: 24rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999;
  }
}

.loading {
  text-align: center;
  padding: 32rpx;
  color: #999;
  font-size: 26rpx;
}
</style>

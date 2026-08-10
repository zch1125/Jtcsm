<template>
  <!-- 搜索模式 -->
  <view class="page" v-if="mode === 'search'">
    <view class="search-header">
      <input
        class="search-input"
        v-model="searchKeyword"
        placeholder="搜索菜谱..."
        confirm-type="search"
        @confirm="doSearch"
      />
      <button class="search-btn" size="mini" @tap="doSearch">搜索</button>
    </view>

    <view class="filter-bar">
      <picker mode="selector" :range="cuisineList" @change="onCuisineFilter">
        <view class="filter-item">{{ cuisineFilter || "全部菜系" }}</view>
      </picker>
      <picker mode="selector" :range="difficultyList" @change="onDifficultyFilter">
        <view class="filter-item">{{ difficultyFilter || "全部难度" }}</view>
      </picker>
    </view>

    <view class="result-count" v-if="searchTotal">共 {{ searchTotal }} 个结果</view>

    <view v-for="r in searchResults" :key="r.id" class="recipe-row" @tap="goDetail(r.id)">
      <image :src="resolveImageUrl(r.coverImage)" mode="aspectFill" class="row-cover" />
      <view class="row-info">
        <text class="row-name">{{ r.name }}</text>
        <view class="row-tags">
          <text class="tag">{{ r.cuisine }}</text>
          <text class="tag">{{ r.difficulty }}</text>
          <text class="tag">{{ r.cookTime }}分</text>
        </view>
        <text class="row-desc">{{ r.description }}</text>
      </view>
    </view>

    <view class="nomore" v-if="searchResults.length > 0 && !hasMore">没有更多了</view>
    <view class="empty" v-if="searchResults.length === 0 && !searchLoading">
      <text class="empty-text">试试其他关键词吧</text>
    </view>
  </view>

  <!-- 详情模式 -->
  <view class="page" v-else-if="recipe">
    <image :src="resolveImageUrl(recipe.coverImage)" mode="aspectFill" class="cover" />
    <view class="info">
      <text class="name">{{ recipe.name }}</text>
      <view class="meta">
        <text class="tag">{{ recipe.cuisine }}</text>
        <text class="tag">{{ recipe.difficulty }}</text>
        <text class="tag">{{ recipe.cookTime }}分钟</text>
        <text class="tag" v-if="recipe.calories">{{ recipe.calories }}千卡</text>
      </view>
      <text class="desc" v-if="recipe.description">{{ recipe.description }}</text>
    </view>

    <view class="section">
      <text class="s-title">用料清单</text>
      <view v-for="ing in recipe.ingredients" :key="ing.id" class="ing-row">
        <text class="ing-name">{{ ing.name }}</text>
        <text class="ing-amount">{{ ing.amount }}</text>
      </view>
    </view>

    <view class="section">
      <text class="s-title">烹饪步骤</text>
      <view v-for="step in recipe.steps" :key="step.id" class="step-row">
        <view class="step-no">{{ step.stepNo }}</view>
        <view class="step-body">
          <text class="step-content">{{ step.content }}</text>
          <text class="step-duration" v-if="step.duration">约 {{ step.duration }} 分钟</text>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <button
        class="fav-btn"
        :class="{ active: isFav }"
        @tap="toggleFav"
      >{{ isFav ? "✓ 已收藏" : "♥ 收藏" }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import {
  getRecipeDetail,
  searchRecipes,
  addFavorite,
  removeFavorite,
  checkFavorited,
} from "../../api/index"
import { resolveImageUrl } from "../../utils/request"

// ==================== 模式判断 ====================
const mode = ref<"detail" | "search">("detail")
const recipeId = ref(0)

// ==================== 详情模式 ====================
const recipe = ref<any>(null)
const isFav = ref(false)

// ==================== 搜索模式 ====================
const searchKeyword = ref("")
const searchResults = ref<any[]>([])
const searchLoading = ref(false)
const searchTotal = ref(0)
const cuisineFilter = ref("")
const difficultyFilter = ref("")
const cuisineList = ["", "川菜", "粤菜", "湘菜", "鲁菜", "苏菜", "浙菜", "闽菜", "徽菜", "东北菜", "家常菜"]
const difficultyList = ["", "简单", "普通", "困难"]
const searchPage = ref(1)
const hasMore = ref(true)

onMounted(async () => {
  const pages = getCurrentPages()
  const opts = (pages[pages.length - 1] as any).options || {}
  const id = Number(opts.id)
  const keyword = opts.keyword || ""

  if (id) {
    // 详情模式
    mode.value = "detail"
    recipeId.value = id
    recipe.value = await getRecipeDetail(id)
    try {
      isFav.value = await checkFavorited(id)
    } catch (e) {
      // 未登录时忽略
    }
  } else if (keyword) {
    // 搜索模式
    mode.value = "search"
    searchKeyword.value = keyword
    await doSearch()
  }
})

// ==================== 搜索逻辑 ====================

async function doSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  searchLoading.value = true
  searchPage.value = 1
  hasMore.value = true

  try {
    const res = await searchRecipes({
      keyword: kw,
      cuisine: cuisineFilter.value || undefined,
      difficulty: difficultyFilter.value || undefined,
      page: 1,
      size: 20,
    })
    searchResults.value = res.records || []
    searchTotal.value = res.total || 0
    hasMore.value = searchResults.value.length >= 20
  } catch (e) {
    // 错误已处理
  } finally {
    searchLoading.value = false
  }
}

function onCuisineFilter(e: any) {
  cuisineFilter.value = cuisineList[e.detail.value]
  if (searchKeyword.value.trim()) doSearch()
}

function onDifficultyFilter(e: any) {
  difficultyFilter.value = difficultyList[e.detail.value]
  if (searchKeyword.value.trim()) doSearch()
}

// ==================== 收藏逻辑 ====================

async function toggleFav() {
  if (!recipeId.value) return
  if (isFav.value) {
    await removeFavorite(recipeId.value)
    isFav.value = false
    uni.showToast({ title: "已取消收藏", icon: "success" })
  } else {
    await addFavorite(recipeId.value)
    isFav.value = true
    uni.showToast({ title: "已收藏", icon: "success" })
  }
}

// ==================== 通用 ====================

function goDetail(id: number) {
  uni.navigateTo({ url: "/pages/recipe/recipe?id=" + id })
}
</script>

<style lang="scss" scoped>
.page {
  background: #f8f8f8;
  min-height: 100vh;
  padding-bottom: 120rpx;
}

/* ==================== 搜索头部 ==================== */
.search-header {
  display: flex;
  padding: 16rpx 20rpx;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
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

/* ==================== 筛选栏 ==================== */
.filter-bar {
  display: flex;
  background: #fff;
  padding: 12rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 16rpx;
}
.filter-item {
  padding: 8rpx 20rpx;
  background: #f5f5f5;
  border-radius: 4rpx;
  font-size: 24rpx;
  color: #666;
}

.result-count {
  padding: 12rpx 24rpx;
  font-size: 24rpx;
  color: #999;
}

/* ==================== 搜索结果行 ==================== */
.recipe-row {
  display: flex;
  background: #fff;
  margin: 12rpx 16rpx;
  padding: 16rpx;
  border-radius: 12rpx;
}
.row-cover {
  width: 160rpx;
  height: 120rpx;
  border-radius: 8rpx;
  background: #eee;
  flex-shrink: 0;
}
.row-info {
  flex: 1;
  margin-left: 16rpx;
  overflow: hidden;
}
.row-name {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}
.row-tags {
  margin-top: 6rpx;
  display: flex;
  gap: 8rpx;
}
.tag {
  font-size: 22rpx;
  color: #e74c3c;
  background: #fef0f0;
  padding: 2rpx 10rpx;
  border-radius: 4rpx;
}
.row-desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nomore {
  text-align: center;
  padding: 32rpx;
  font-size: 24rpx;
  color: #ccc;
}
.empty {
  display: flex;
  justify-content: center;
  padding-top: 200rpx;
}
.empty-text {
  font-size: 28rpx;
  color: #999;
}

/* ==================== 详情模式 ==================== */
.cover {
  width: 100%;
  height: 400rpx;
}
.info {
  padding: 24rpx;
  background: #fff;
}
.name {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
}
.meta {
  margin-top: 12rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.meta .tag {
  font-size: 24rpx;
  color: #e74c3c;
  background: #fef0f0;
  padding: 4rpx 16rpx;
  border-radius: 4rpx;
}
.desc {
  font-size: 28rpx;
  color: #666;
  margin-top: 12rpx;
  display: block;
}

.section {
  margin: 16rpx 16rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
}
.s-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 16rpx;
  display: block;
}
.ing-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.ing-name {
  font-size: 28rpx;
  color: #333;
}
.ing-amount {
  font-size: 26rpx;
  color: #999;
}
.step-row {
  display: flex;
  margin-bottom: 20rpx;
}
.step-no {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  background: #e74c3c;
  color: #fff;
  border-radius: 50%;
  font-size: 24rpx;
  flex-shrink: 0;
  margin-right: 16rpx;
}
.step-body {
  flex: 1;
}
.step-content {
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
}
.step-duration {
  font-size: 22rpx;
  color: #aaa;
  margin-top: 4rpx;
  display: block;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 32rpx 40rpx;
  background: #fff;
  box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.06);
}
.fav-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #e74c3c;
  color: #fff;
  border-radius: 40rpx;
  font-size: 30rpx;
  border: none;
  &.active {
    background: #95a5a6;
  }
}
</style>

<template>
  <div class="page-body fav-page">
    <div class="container">
      <div class="tab-bar card">
        <button class="tab-btn" :class="{ active: currentTab === 'favorites' }" @click="switchTab('favorites')">
          我的收藏
        </button>
        <button class="tab-btn" :class="{ active: currentTab === 'history' }" @click="switchTab('history')">
          浏览历史
        </button>
      </div>

      <div v-if="currentTab === 'favorites'">
        <div v-if="favoriteList.length" class="list">
          <article
            v-for="item in favoriteList"
            :key="item.favoriteId"
            class="row card"
            @click="goDetail(item.recipeId)"
          >
            <div class="row-cover">
              <img v-if="item.coverImage" :src="resolveImageUrl(item.coverImage)" :alt="item.name" />
              <div v-else class="cover-fallback">{{ (item.name || "菜").slice(0, 1) }}</div>
            </div>
            <div class="row-info">
              <h3>{{ item.name }}</h3>
              <div class="row-tags">
                <span class="tag">{{ item.cuisine }}</span>
                <span class="tag tag-hot">{{ item.difficulty }}</span>
                <span class="tag tag-muted">{{ item.cookTime }} 分钟</span>
              </div>
              <span class="row-time">收藏于 {{ formatTime(item.createdAt) }}</span>
            </div>
            <button class="unfav-btn" @click.stop="handleUnfavorite(item)">取消收藏</button>
          </article>
        </div>
        <div v-else-if="!favoritesLoading" class="empty card">还没有收藏菜谱，去首页看看吧</div>
        <div v-if="favoritesLoading" class="empty">加载中...</div>
      </div>

      <div v-else>
        <div v-if="historyList.length" class="list">
          <article
            v-for="item in historyList"
            :key="item.historyId"
            class="row card"
            @click="goDetail(item.recipeId)"
          >
            <div class="row-cover">
              <img v-if="item.coverImage" :src="resolveImageUrl(item.coverImage)" :alt="item.name" />
              <div v-else class="cover-fallback">{{ (item.name || "菜").slice(0, 1) }}</div>
            </div>
            <div class="row-info">
              <h3>{{ item.name }}</h3>
              <div class="row-tags">
                <span class="tag">{{ item.cuisine }}</span>
                <span class="tag tag-hot">{{ item.difficulty }}</span>
                <span class="tag tag-muted">{{ item.cookTime }} 分钟</span>
              </div>
              <span class="row-time">浏览于 {{ formatTime(item.viewedAt) }}</span>
            </div>
          </article>
        </div>
        <div v-else-if="!historyLoading" class="empty card">还没有浏览记录</div>
        <div v-if="historyLoading" class="empty">加载中...</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { getFavorites, getHistory, removeFavorite } from "@/api"
import { resolveImageUrl } from "@/api/request"

const router = useRouter()
const currentTab = ref<"favorites" | "history">("favorites")

const favoriteList = ref<any[]>([])
const favoritesLoading = ref(false)
let favoritesPage = 1
let favoritesHasMore = true

const historyList = ref<any[]>([])
const historyLoading = ref(false)
let historyPage = 1
let historyHasMore = true

function switchTab(tab: "favorites" | "history") {
  currentTab.value = tab
  if (tab === "favorites" && !favoriteList.value.length) {
    fetchFavorites()
  } else if (tab === "history" && !historyList.value.length) {
    fetchHistory()
  }
}

async function fetchFavorites() {
  if (favoritesLoading.value || !favoritesHasMore) return
  favoritesLoading.value = true
  try {
    const data = await getFavorites(favoritesPage, 20)
    favoriteList.value = [...favoriteList.value, ...data]
    favoritesHasMore = data.length >= 20
    favoritesPage += 1
  } catch {
    favoriteList.value = []
  } finally {
    favoritesLoading.value = false
  }
}

async function fetchHistory() {
  if (historyLoading.value || !historyHasMore) return
  historyLoading.value = true
  try {
    const data = await getHistory(historyPage, 20)
    historyList.value = [...historyList.value, ...data]
    historyHasMore = data.length >= 20
    historyPage += 1
  } catch {
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

async function handleUnfavorite(item: any) {
  try {
    await removeFavorite(item.recipeId)
    favoriteList.value = favoriteList.value.filter((f) => f.favoriteId !== item.favoriteId)
  } catch {
    // 错误已在请求层处理
  }
}

function goDetail(id: number) {
  router.push({ path: "/recipes", query: { id: String(id) } })
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ""
  const d = new Date(dateStr)
  const pad = (n: number) => (n < 10 ? "0" + n : String(n))
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(fetchFavorites)
</script>

<style scoped>
.tab-bar {
  display: flex;
  padding: 6px;
  margin-bottom: 18px;
  max-width: 360px;
}

.tab-btn {
  flex: 1;
  border: 0;
  background: none;
  border-radius: 999px;
  padding: 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #7d857f;
}

.tab-btn.active {
  background: var(--green);
  color: #fff;
}

.list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.row:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(47, 93, 58, 0.12);
}

.row-cover {
  width: 110px;
  height: 82px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #eee8dc;
}

.row-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--leaf), var(--green));
}

.row-info {
  flex: 1;
  min-width: 0;
}

.row-info h3 {
  margin: 0 0 8px;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.row-time {
  font-size: 12px;
  color: #a6ada7;
}

.unfav-btn {
  flex-shrink: 0;
  border: 1px solid #f0cfc4;
  background: #fff;
  color: var(--tomato);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
}

@media (max-width: 900px) {
  .list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .row-cover {
    width: 96px;
    height: 74px;
  }
}
</style>

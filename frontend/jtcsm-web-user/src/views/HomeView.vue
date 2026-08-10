<template>
  <div class="page-body home">
    <div class="container">
      <section class="hero">
        <div class="hero-copy">
          <p class="eyebrow">今晚吃什么 · 交给 AI</p>
          <h1>把冰箱里的食材，变成一桌好菜</h1>
          <p class="hero-sub">
            输入食材、菜名或口味要求，RAG 语义检索 + AI 厨艺导师为你推荐合适的做法。
          </p>
          <div class="hero-actions">
            <router-link to="/ai" class="btn btn-primary">开始 AI 对话</router-link>
            <router-link to="/recipes" class="btn btn-ghost">浏览全部菜谱</router-link>
          </div>
        </div>

        <div class="hero-plate">
          <div class="plate-ring">
            <div class="plate-item plate-a">番茄炒蛋</div>
            <div class="plate-item plate-b">麻婆豆腐</div>
            <div class="plate-item plate-c">土豆炖牛腩</div>
            <div class="plate-center">今天<br />吃什么</div>
          </div>
        </div>
      </section>

      <section class="search-panel card">
        <div class="search-row">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索菜谱、食材、菜系..."
            @keyup.enter="doSearch"
            @focus="showHistory = true"
            @blur="delayHideHistory"
          />
          <button class="btn btn-primary" @click="doSearch">搜索</button>
        </div>

        <div v-if="showHistory && historyList.length" class="history-panel">
          <div class="history-head">
            <span>搜索历史</span>
            <button class="link-btn" @mousedown.prevent="handleClearHistory">清空</button>
          </div>
          <div class="history-tags">
            <button
              v-for="item in historyList"
              :key="item.id"
              class="history-tag"
              @mousedown.prevent="tapHistory(item.keyword)"
            >
              {{ item.keyword }}
            </button>
          </div>
        </div>
      </section>

      <section v-if="dailyList.length" class="daily">
        <div class="section-head">
          <h2 class="section-title">每日推荐</h2>
          <div class="carousel-controls">
            <button class="carousel-btn" aria-label="上一页" @click="dailyPrev">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6" /></svg>
            </button>
            <button class="carousel-btn" aria-label="下一页" @click="dailyNext">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          </div>
        </div>
        <div class="carousel card" @mouseenter="pauseCarousel" @mouseleave="startCarousel">
          <div class="carousel-track" :style="{ transform: `translateX(-${dailyIndex * (100 / visibleCount)}%)` }">
            <article
              v-for="r in dailyList"
              :key="r.id"
              class="recipe-card"
              @click="goDetail(r.id)"
            >
              <div class="card-cover">
                <img v-if="r.coverImage" :src="resolveImageUrl(r.coverImage)" :alt="r.name" />
                <div v-else class="cover-fallback">{{ (r.name || "菜").slice(0, 1) }}</div>
              </div>
              <div class="card-info">
                <h3>{{ r.name }}</h3>
                <div class="card-meta">
                  <span class="tag">{{ r.cuisine }}</span>
                  <span class="tag tag-muted">{{ r.cookTime }} 分钟</span>
                </div>
              </div>
            </article>
          </div>
          <div class="carousel-dots">
            <button
              v-for="(_, di) in dailyPages"
              :key="di"
              class="dot"
              :class="{ active: dailyIndex === di }"
              @click="dailyIndex = di"
            ></button>
          </div>
        </div>
      </section>

      <section class="hot-section">
        <div class="section-head">
          <h2 class="section-title">热门菜谱</h2>
          <router-link to="/recipes" class="more-link">查看全部</router-link>
        </div>
        <div v-if="hotList.length" class="hot-grid">
          <article
            v-for="r in hotList"
            :key="r.id"
            class="recipe-card card"
            @click="goDetail(r.id)"
          >
            <div class="card-cover">
              <img v-if="r.coverImage" :src="resolveImageUrl(r.coverImage)" :alt="r.name" />
              <div v-else class="cover-fallback">{{ (r.name || "菜").slice(0, 1) }}</div>
            </div>
            <div class="card-info">
              <h3>{{ r.name }}</h3>
              <p v-if="r.description" class="card-desc">{{ r.description }}</p>
              <div class="card-meta">
                <span class="tag">{{ r.cuisine }}</span>
                <span class="tag tag-hot">{{ r.difficulty }}</span>
                <span class="tag tag-muted">{{ r.cookTime }} 分钟</span>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="empty">正在加载菜谱...</div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import {
  getDailyRecommend,
  getHotRecipes,
  getSearchHistory,
  clearSearchHistory
} from "@/api"
import { resolveImageUrl } from "@/api/request"

const router = useRouter()
const keyword = ref("")
const hotList = ref<any[]>([])
const dailyList = ref<any[]>([])
const historyList = ref<any[]>([])
const showHistory = ref(false)
const dailyIndex = ref(0)
const visibleCount = ref(4)
let carouselTimer: ReturnType<typeof setInterval> | null = null

const dailyPages = computed(() => {
  return Math.max(1, Math.ceil(dailyList.value.length / visibleCount.value))
})

async function loadData() {
  try {
    hotList.value = await getHotRecipes()
  } catch {
    hotList.value = []
  }
  try {
    dailyList.value = await getDailyRecommend()
  } catch {
    dailyList.value = []
  }
  loadHistory()
}

async function loadHistory() {
  try {
    historyList.value = await getSearchHistory()
  } catch {
    historyList.value = []
  }
}

function dailyNext() {
  dailyIndex.value = (dailyIndex.value + 1) % dailyPages.value
}

function dailyPrev() {
  dailyIndex.value = (dailyIndex.value - 1 + dailyPages.value) % dailyPages.value
}

function startCarousel() {
  if (carouselTimer) return
  carouselTimer = setInterval(() => {
    dailyNext()
  }, 4000)
}

function updateVisibleCount() {
  visibleCount.value = window.innerWidth <= 640 ? 2 : 4
}

function pauseCarousel() {
  if (carouselTimer) {
    clearInterval(carouselTimer)
    carouselTimer = null
  }
}

function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  showHistory.value = false
  router.push({ path: "/recipes", query: { keyword: kw } })
}

function tapHistory(kw: string) {
  keyword.value = kw
  showHistory.value = false
  router.push({ path: "/recipes", query: { keyword: kw } })
}

function delayHideHistory() {
  setTimeout(() => {
    showHistory.value = false
  }, 200)
}

async function handleClearHistory() {
  try {
    await clearSearchHistory()
    historyList.value = []
  } catch {
    // 未登录时忽略
  }
}

function goDetail(id: number) {
  router.push({ path: "/recipes", query: { id: String(id) } })
}

onMounted(() => {
  loadData()
  updateVisibleCount()
  window.addEventListener("resize", updateVisibleCount)
  startCarousel()
})

onBeforeUnmount(() => {
  pauseCarousel()
  window.removeEventListener("resize", updateVisibleCount)
})
</script>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  align-items: center;
  gap: 28px;
  padding: 40px 34px;
  border-radius: 22px;
  background:
    radial-gradient(circle at 85% 15%, rgba(242, 177, 52, 0.25), transparent 34%),
    linear-gradient(135deg, #eef3e9 0%, #f8f1e3 100%);
  border: 1px solid var(--line);
  overflow: hidden;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--tomato);
}

.hero h1 {
  margin: 0 0 14px;
  font-size: 34px;
  line-height: 1.25;
  color: var(--green-dark);
}

.hero-sub {
  margin: 0 0 22px;
  font-size: 15px;
  line-height: 1.7;
  color: #5c655e;
  max-width: 520px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-plate {
  display: flex;
  justify-content: center;
  align-items: center;
}

.plate-ring {
  position: relative;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: #fffdf8;
  border: 2px solid #e3d9c4;
  box-shadow: 0 18px 40px rgba(47, 93, 58, 0.14);
}

.plate-ring::before {
  content: "";
  position: absolute;
  inset: 22px;
  border-radius: 50%;
  border: 1px dashed #d9cfb8;
}

.plate-center {
  position: absolute;
  inset: 66px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--green);
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  text-align: center;
  line-height: 1.35;
  box-shadow: 0 8px 20px rgba(47, 93, 58, 0.28);
}

.plate-item {
  position: absolute;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  white-space: nowrap;
}

.plate-a {
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  background: #ffe3c2;
  color: #9a5b00;
}

.plate-b {
  right: -18px;
  top: 104px;
  background: #fbd5cf;
  color: #a33a24;
}

.plate-c {
  left: -34px;
  top: 150px;
  background: #dbe8d6;
  color: #2c5a38;
}

.search-panel {
  margin: 20px 0 32px;
  padding: 18px 20px;
  position: relative;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted);
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 42px;
  border: 0;
  outline: 0;
  background: #f4efe4;
  border-radius: 999px;
  padding: 0 18px;
  font-size: 15px;
  color: var(--ink);
}

.history-panel {
  position: absolute;
  left: 16px;
  right: 16px;
  top: calc(100% - 8px);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow);
  padding: 14px 16px;
  z-index: 20;
}

.history-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 10px;
}

.link-btn {
  border: 0;
  background: none;
  color: var(--tomato);
  font-size: 13px;
  padding: 0;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  border: 0;
  background: #f2ede2;
  color: #5c655e;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 13px;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--leaf), var(--green));
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.recipe-card {
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.recipe-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 34px rgba(47, 93, 58, 0.14);
}

.card-cover {
  aspect-ratio: 16 / 10;
  background: #eee8dc;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-info {
  padding: 12px 14px 14px;
}

.card-info h3 {
  margin: 0 0 8px;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.carousel {
  position: relative;
  overflow: hidden;
  padding: 18px 18px 40px;
  margin-bottom: 34px;
}

.carousel-track {
  display: flex;
  transition: transform 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.carousel .recipe-card {
  flex: 0 0 25%;
  min-width: 0;
  padding: 0 7px;
  cursor: pointer;
}

.carousel .recipe-card .card-cover {
  border-radius: 12px;
  overflow: hidden;
}

.carousel-controls {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.carousel-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: #fff;
  color: var(--green);
  transition: background 0.15s ease, color 0.15s ease;
}

.carousel-btn:hover {
  background: var(--green);
  color: #fff;
}

.carousel-dots {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 14px;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border: 0;
  border-radius: 999px;
  background: #d9d2c2;
  padding: 0;
  transition: width 0.2s ease, background 0.2s ease;
}

.dot.active {
  width: 22px;
  background: var(--green);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.hot-section {
  margin-top: 6px;
}

.more-link {
  font-size: 13px;
  font-weight: 600;
  color: var(--green);
}

@media (max-width: 1000px) {
  .hot-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .carousel .recipe-card {
    flex-basis: 33.3333%;
  }
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    padding: 28px 22px;
  }

  .hero-plate {
    display: none;
  }
}

@media (max-width: 640px) {
  .hero {
    padding: 24px 18px;
  }

  .hero h1 {
    font-size: 26px;
  }

  .hot-grid {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .carousel .recipe-card {
    flex-basis: 50%;
  }

  .card-info {
    padding: 10px;
  }

  .card-info h3 {
    font-size: 14px;
  }
}
</style>

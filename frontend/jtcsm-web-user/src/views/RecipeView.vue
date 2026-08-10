<template>
  <div class="page-body recipe-page">
    <div class="container">
      <!-- 搜索 / 菜谱列表模式 -->
      <template v-if="mode === 'search'">
        <section class="search-head card">
          <div class="search-row">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            <input
              v-model="searchKeyword"
              class="search-input"
              placeholder="搜索菜谱..."
              @keyup.enter="doSearch"
            />
            <button class="btn btn-primary" @click="doSearch">搜索</button>
          </div>
          <div class="filter-row">
            <select v-model="cuisineFilter" class="filter-select" @change="applyFilters">
              <option value="">全部菜系</option>
              <option v-for="c in cuisineList" :key="c" :value="c">{{ c }}</option>
            </select>
            <select v-model="difficultyFilter" class="filter-select" @change="applyFilters">
              <option value="">全部难度</option>
              <option v-for="d in difficultyList" :key="d" :value="d">{{ d }}</option>
            </select>
            <select v-model="cookMethodFilter" class="filter-select" @change="applyFilters">
              <option value="">全部方式</option>
              <option v-for="m in cookMethodList" :key="m" :value="m">{{ m }}</option>
            </select>
          </div>
        </section>

        <p v-if="searchTotal" class="result-count">
          {{ searchKeyword.trim() ? `找到 ${searchTotal} 个结果` : `共 ${searchTotal} 道菜谱` }}
        </p>

        <div class="recipe-grid">
          <article
            v-for="r in pageRecords"
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
              <p v-if="r.description">{{ r.description }}</p>
              <div class="card-tags">
                <span class="tag">{{ r.cuisine }}</span>
                <span class="tag tag-hot">{{ r.difficulty }}</span>
                <span class="tag tag-muted">{{ r.cookTime }} 分钟</span>
              </div>
            </div>
          </article>
        </div>

        <div v-if="searchLoading" class="empty">加载中...</div>
        <div v-else-if="!searchResults.length && searched" class="empty">
          没有找到相关菜谱，换个关键词试试
        </div>

        <!-- 分页：全部菜谱模式显示页码，关键词搜索模式显示加载更多 -->
        <div v-if="searchResults.length && allMode" class="pagination">
          <button class="page-btn" :disabled="page === 1" @click="gotoPage(page - 1)">上一页</button>
          <button
            v-for="p in pageNumbers"
            :key="p"
            class="page-btn"
            :class="{ active: p === page }"
            @click="gotoPage(p)"
          >
            {{ p }}
          </button>
          <button class="page-btn" :disabled="page >= totalPages" @click="gotoPage(page + 1)">
            下一页
          </button>
        </div>
        <div v-else-if="hasMore" class="load-more">
          <button class="btn btn-ghost" @click="loadMore">加载更多</button>
        </div>
      </template>

      <!-- 详情模式 -->
      <template v-else-if="recipe">
        <div class="detail-hero card">
          <div class="detail-cover">
            <img v-if="recipe.coverImage" :src="resolveImageUrl(recipe.coverImage)" :alt="recipe.name" />
            <div v-else class="cover-fallback big">{{ (recipe.name || "菜").slice(0, 1) }}</div>
          </div>
          <div class="detail-head">
            <div class="detail-title-row">
              <h1>{{ recipe.name }}</h1>
              <button class="fav-btn" :class="{ active: isFav }" @click="toggleFav">
                <svg viewBox="0 0 24 24" width="18" height="18" :fill="isFav ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round">
                  <path d="M12 20.5 4.8 13a4.4 4.4 0 0 1 6.2-6.2L12 8l1-1.2A4.4 4.4 0 0 1 19.2 13Z" />
                </svg>
                {{ isFav ? "已收藏" : "收藏" }}
              </button>
            </div>
            <div class="detail-meta">
              <span class="tag tag-hot">{{ recipe.cuisine }}</span>
              <span class="tag">{{ recipe.difficulty }}</span>
              <span class="tag tag-muted">{{ recipe.cookTime }} 分钟</span>
              <span v-if="recipe.calories" class="tag tag-muted">{{ recipe.calories }} 千卡</span>
            </div>
            <p v-if="recipe.description" class="detail-desc">{{ recipe.description }}</p>
          </div>
        </div>

        <div class="detail-grid">
          <section class="card detail-section">
            <h2 class="section-title">用料清单</h2>
            <div v-if="recipe.ingredients?.length" class="ing-list">
              <div v-for="ing in recipe.ingredients" :key="ing.id ?? ing.name" class="ing-row">
                <span>{{ ing.name }}</span>
                <span class="ing-amount">{{ ing.amount }}</span>
              </div>
            </div>
            <div v-else class="empty">暂无用料信息</div>
          </section>

          <section class="card detail-section">
            <h2 class="section-title">烹饪步骤</h2>
            <div v-if="recipe.steps?.length" class="step-list">
              <div v-for="step in recipe.steps" :key="step.id ?? step.stepNo" class="step-row">
                <div class="step-no">{{ step.stepNo }}</div>
                <div class="step-body">
                  <p>{{ step.content }}</p>
                  <span v-if="step.duration" class="step-duration">约 {{ step.duration }} 分钟</span>
                </div>
              </div>
            </div>
            <div v-else class="empty">暂无步骤信息</div>
          </section>
        </div>
      </template>

      <div v-else class="empty">加载中...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  addFavorite,
  checkFavorited,
  getRecipeDetail,
  removeFavorite,
  searchRecipes
} from "@/api"
import { resolveImageUrl } from "@/api/request"

const route = useRoute()
const router = useRouter()

const mode = ref<"search" | "detail">("detail")
const recipe = ref<any>(null)
const isFav = ref(false)

const searchKeyword = ref("")
const searchResults = ref<any[]>([])
const searchLoading = ref(false)
const searchTotal = ref(0)
const searched = ref(false)
const page = ref(1)
const pageSize = 8
const hasMore = ref(false)
const allMode = ref(false)
const cuisineFilter = ref("")
const difficultyFilter = ref("")
const cookMethodFilter = ref("")
const cuisineList = ["川菜", "粤菜", "湘菜", "鲁菜", "苏菜", "浙菜", "闽菜", "徽菜", "东北菜", "家常菜"]
const difficultyList = ["简单", "普通", "困难"]
const cookMethodList = ["炒", "炖", "蒸", "煮", "炸", "烤", "凉拌", "煎"]

const totalPages = computed(() => Math.max(1, Math.ceil(searchTotal.value / pageSize)))
const pageRecords = computed(() => {
  if (!allMode.value) return searchResults.value
  const start = (page.value - 1) * pageSize
  return searchResults.value.slice(start, start + pageSize)
})
const pageNumbers = computed(() => {
  const nums: number[] = []
  const total = totalPages.value
  const current = page.value
  const start = Math.max(1, Math.min(current - 2, total - 4))
  const end = Math.min(total, start + 4)
  for (let i = start; i <= end; i++) nums.push(i)
  return nums
})

onMounted(async () => {
  const id = Number(route.query.id)
  const keyword = String(route.query.keyword || "")
  if (id) {
    mode.value = "detail"
    try {
      recipe.value = await getRecipeDetail(id)
    } catch {
      recipe.value = null
    }
    try {
      isFav.value = await checkFavorited(id)
    } catch {
      isFav.value = false
    }
  } else {
    mode.value = "search"
    if (keyword) {
      searchKeyword.value = keyword
    }
    await doSearch()
  }
})

async function doSearch() {
  const kw = searchKeyword.value.trim()
  allMode.value = !kw
  searchLoading.value = true
  page.value = 1
  hasMore.value = false
  try {
    const res = await searchRecipes({
      keyword: kw || undefined,
      cuisine: cuisineFilter.value || undefined,
      difficulty: difficultyFilter.value || undefined,
      cookMethod: cookMethodFilter.value || undefined,
      page: 1,
      size: allMode.value ? 500 : pageSize
    })
    searchResults.value = res.records || []
    searchTotal.value = res.total || searchResults.value.length
    hasMore.value = !allMode.value && searchResults.value.length < searchTotal.value
  } catch {
    searchResults.value = []
    searchTotal.value = 0
  } finally {
    searchLoading.value = false
    searched.value = true
  }
}

function applyFilters() {
  doSearch()
}

async function loadMore() {
  if (searchLoading.value || !hasMore.value) return
  searchLoading.value = true
  page.value += 1
  try {
    const res = await searchRecipes({
      keyword: searchKeyword.value.trim() || undefined,
      cuisine: cuisineFilter.value || undefined,
      difficulty: difficultyFilter.value || undefined,
      cookMethod: cookMethodFilter.value || undefined,
      page: page.value,
      size: pageSize
    })
    searchResults.value = [...searchResults.value, ...(res.records || [])]
    hasMore.value = searchResults.value.length < (res.total || 0)
  } catch {
    // 错误已在请求层处理
  } finally {
    searchLoading.value = false
  }
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  window.scrollTo({ top: 0, behavior: "smooth" })
}

async function toggleFav() {
  const id = recipe.value?.id
  if (!id) return
  try {
    if (isFav.value) {
      await removeFavorite(id)
      isFav.value = false
    } else {
      await addFavorite(id)
      isFav.value = true
    }
  } catch {
    // 未登录等错误已在请求层处理
  }
}

function goDetail(id: number) {
  router.push({ path: "/recipes", query: { id: String(id) } })
}
</script>

<style scoped>
.search-head {
  padding: 18px 20px;
  margin-bottom: 18px;
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
}

.filter-row {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.filter-select {
  appearance: none;
  border: 1px solid var(--line);
  background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%237d857f' stroke-width='2.4'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E") no-repeat right 12px center;
  border-radius: 999px;
  padding: 8px 34px 8px 16px;
  font-size: 13px;
  color: var(--ink);
  cursor: pointer;
}

.result-count {
  font-size: 13px;
  color: var(--muted);
  margin: 0 0 12px;
}

.recipe-grid {
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

.cover-fallback.big {
  font-size: 56px;
}

.card-info {
  padding: 12px 14px 14px;
}

.card-info h3 {
  margin: 0 0 6px;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-info p {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 22px;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--ink);
  transition: background 0.15s ease, color 0.15s ease;
}

.page-btn:hover:not(:disabled):not(.active) {
  background: #f4efe4;
}

.page-btn.active {
  background: var(--green);
  border-color: var(--green);
  color: #fff;
  font-weight: 700;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.detail-hero {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 26px;
  overflow: hidden;
  margin-bottom: 18px;
}

.detail-cover {
  height: 300px;
  background: #eee8dc;
}

.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-head {
  padding: 26px 26px 26px 0;
}

.detail-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.detail-title-row h1 {
  margin: 0;
  font-size: 30px;
  color: var(--green-dark);
}

.fav-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  border: 1px solid #f0cfc4;
  background: #fff;
  color: var(--tomato);
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 700;
}

.fav-btn.active {
  background: var(--tomato);
  border-color: var(--tomato);
  color: #fff;
}

.detail-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.detail-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: #5c655e;
}

.detail-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 18px;
  align-items: start;
}

.detail-section {
  padding: 22px;
}

.ing-list {
  display: flex;
  flex-direction: column;
}

.ing-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid #f0ead9;
  font-size: 15px;
}

.ing-amount {
  color: var(--muted);
  flex-shrink: 0;
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-row {
  display: flex;
  gap: 14px;
}

.step-no {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--tomato);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

.step-body {
  flex: 1;
  min-width: 0;
}

.step-body p {
  margin: 0 0 4px;
  font-size: 15px;
  line-height: 1.7;
}

.step-duration {
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 1000px) {
  .recipe-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .detail-cover {
    height: 240px;
  }

  .detail-head {
    padding: 0 20px 20px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .recipe-grid {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .card-info {
    padding: 10px;
  }

  .card-info h3 {
    font-size: 14px;
  }

  .detail-title-row h1 {
    font-size: 24px;
  }

  .detail-cover {
    height: 200px;
  }
}
</style>

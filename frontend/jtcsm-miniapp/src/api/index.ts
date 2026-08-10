/**
 * API 接口封装 - 所有后端 API 的调用入口
 */
import request, { StreamEventHandlers } from '../utils/request'

// ==================== 通用类型 ====================

interface RecipeSummary {
  recipeId: number
  name: string
  coverImage: string
  cuisine: string
  difficulty: string
  cookMethod: string
  cookTime: number
}

interface FavoriteItem extends RecipeSummary {
  favoriteId: number
  createdAt: string
}

interface HistoryItem extends RecipeSummary {
  historyId: number
  viewedAt: string
}

interface SearchHistoryItem {
  id: number
  userId: number
  keyword: string
  created_at: string
}

// ==================== 登录模块 ====================

interface LoginResponseData {
  token: string
  userId: number
  nickname: string
  avatar: string
}

/** 微信登录 / 模拟登录 */
export function login(code?: string, nickname?: string, avatar?: string): Promise<LoginResponseData> {
  return request.post("/api/v1/user/login", { code, nickname, avatar })
}

/** 获取用户个人信息 */
export function getUserProfile(): Promise<any> {
  return request.get("/api/v1/user/profile")
}

// ==================== 菜谱模块 ====================

/** 获取热门菜谱 Top10 */
export function getHotRecipes(): Promise<any[]> {
  return request.get("/api/v1/recipe/hot")
}

/** 多条件搜索菜谱 */
export function searchRecipes(params: any): Promise<any> {
  return request.get("/api/v1/recipe/search", params)
}

/** 获取菜谱详情（含用料+步骤） */
export function getRecipeDetail(id: number): Promise<any> {
  return request.get("/api/v1/recipe/" + id)
}

// ==================== 推荐模块 ====================

/** 获取每日推荐 */
export function getDailyRecommend(): Promise<any[]> {
  return request.get("/api/v1/recommend/daily")
}

/** 个性化推荐 */
export function getPersonalRecommend(): Promise<any[]> {
  return request.get("/api/v1/recommend/personal")
}

/** 按食材匹配菜谱 */
export function getRecommendByIngredients(ingredients: string[]): Promise<any[]> {
  return request.get("/api/v1/recommend/by-ingredients", { ingredients })
}

// ==================== AI 模块 ====================

interface AiGenerateResponseData {
  id: number | null
  recipes: AiRecipeItemData[]
  fromCache: boolean
}

interface AiRecipeItemData {
  name: string
  cuisine: string
  difficulty: string
  cookTime: number
  ingredients: { name: string; amount: string }[]
  steps: { stepNo: number; content: string; duration: number }[]
}

interface AiHistoryItem {
  id: number
  mode: string
  inputContent: string
  resultJson: string
  rating: number | null
  feedback: string | null
  createdAt: string
}

/** AI 生成菜谱 */
export function aiGenerate(params: {
  mode: string
  ingredients?: string[]
  name?: string
  cuisineA?: string
  cuisineB?: string
  conditions?: string
}): Promise<AiGenerateResponseData> {
  return request.post("/api/v1/ai/generate", params)
}

/** AI 流式生成菜谱（SSE：先输出推荐文字，再下发菜谱卡片） */
export function aiGenerateStream(
  params: {
    mode: string
    ingredients?: string[]
    name?: string
    cuisineA?: string
    cuisineB?: string
    conditions?: string
  },
  handlers: StreamEventHandlers,
) {
  return request.stream("/api/v1/ai/generate/stream", params, handlers)
}

/** AI 生成历史列表 */
export function getAiHistory(page = 1, size = 20): Promise<AiHistoryItem[]> {
  return request.get("/api/v1/ai/history", { page, size })
}

/** AI 生成历史详情 */
export function getAiHistoryDetail(id: number): Promise<AiHistoryItem> {
  return request.get("/api/v1/ai/history/" + id)
}

/** 保存AI生成结果到收藏 */
export function saveAiToFavorite(historyId: number) {
  return request.post("/api/v1/ai/history/" + historyId + "/save")
}

/** 提交AI生成反馈 */
export function aiFeedback(historyId: number, rating: number, feedback?: string) {
  return request.post("/api/v1/ai/feedback", { historyId, rating, feedback })
}

// ==================== 收藏模块 ====================

/** 添加收藏 */
export function addFavorite(recipeId: number) {
  return request.post("/api/v1/favorite", { recipeId })
}

/** 取消收藏 */
export function removeFavorite(recipeId: number) {
  return request.delete("/api/v1/favorite/" + recipeId)
}

/** 收藏列表 */
export function getFavorites(page = 1, size = 20): Promise<FavoriteItem[]> {
  return request.get("/api/v1/favorite/list", { page, size })
}

/** 检查是否已收藏 */
export function checkFavorited(recipeId: number): Promise<boolean> {
  return request.get("/api/v1/favorite/check/" + recipeId)
}

// ==================== 浏览历史模块 ====================

/** 浏览历史列表 */
export function getHistory(page = 1, size = 20): Promise<HistoryItem[]> {
  return request.get("/api/v1/history/list", { page, size })
}

// ==================== 搜索历史模块 ====================

/** 获取搜索历史 */
export function getSearchHistory(): Promise<SearchHistoryItem[]> {
  return request.get("/api/v1/search/history")
}

/** 清空搜索历史 */
export function clearSearchHistory() {
  return request.delete("/api/v1/search/history")
}

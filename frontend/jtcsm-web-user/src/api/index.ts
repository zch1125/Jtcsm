import request from "./request"
import { aiGenerateStream, type StreamEventHandlers } from "./stream"

export { aiGenerateStream, type StreamEventHandlers }

// ==================== 登录 / 用户 ====================

export interface LoginResult {
  token: string
  userId: number
  nickname: string
  avatar: string
}

export interface UserProfile {
  id: number
  nickname: string
  avatar: string
  phone?: string
  gender?: number
}

export function login(nickname?: string, avatar?: string): Promise<LoginResult> {
  return request.post("/api/v1/user/login", { code: "mock", nickname, avatar })
}

export function getUserProfile(): Promise<UserProfile> {
  return request.get("/api/v1/user/profile")
}

export function updateUserProfile(data: { nickname?: string; avatar?: string }): Promise<void> {
  return request.put("/api/v1/user/profile", data)
}

// ==================== 菜谱 ====================

export interface RecipeSummary {
  id: number
  name: string
  coverImage: string
  description?: string
  cuisine: string
  difficulty: string
  cookMethod?: string
  cookTime: number
  calories?: number
  favoriteCount?: number
}

export interface RecipeDetail extends RecipeSummary {
  viewCount?: number
  ingredients: Array<{ id?: number; name: string; amount: string }>
  steps: Array<{ id?: number; stepNo: number; content: string; duration?: number }>
}

export function getHotRecipes(): Promise<RecipeSummary[]> {
  return request.get("/api/v1/recipe/hot")
}

export function searchRecipes(params: {
  keyword?: string
  cuisine?: string
  difficulty?: string
  cookMethod?: string
  page?: number
  size?: number
}): Promise<{ records: RecipeSummary[]; total: number; page: number; size: number }> {
  return request.get("/api/v1/recipe/search", { params })
}

export function getRecipeDetail(id: number): Promise<RecipeDetail> {
  return request.get(`/api/v1/recipe/${id}`)
}

// ==================== 推荐 ====================

export function getDailyRecommend(): Promise<RecipeSummary[]> {
  return request.get("/api/v1/recommend/daily")
}

export function getPersonalRecommend(): Promise<RecipeSummary[]> {
  return request.get("/api/v1/recommend/personal")
}

export function getRecommendByIngredients(ingredients: string[]): Promise<RecipeSummary[]> {
  return request.get("/api/v1/recommend/by-ingredients", {
    params: { ingredients }
  })
}

// ==================== AI ====================

export interface AiIngredient {
  name: string
  amount: string
}

export interface AiStep {
  stepNo: number
  content: string
  duration?: number
}

export interface AiRecipeItem {
  recipeId?: number
  coverImage?: string
  name: string
  cuisine: string
  difficulty: string
  cookTime: number
  ingredients: AiIngredient[]
  steps: AiStep[]
}

export interface AiGenerateParams {
  mode: "ingredients" | "name" | "creative"
  ingredients?: string[]
  name?: string
  cuisineA?: string
  cuisineB?: string
  conditions?: string
}

export interface AiHistoryItem {
  id: number
  mode: string
  inputContent: string
  resultJson: string
  rating: number | null
  feedback: string | null
  createdAt: string
}

export function aiGenerate(params: AiGenerateParams): Promise<{ id: number | null; recipes: AiRecipeItem[]; fromCache: boolean }> {
  return request.post("/api/v1/ai/generate", params)
}

export function getAiHistory(page = 1, size = 20): Promise<AiHistoryItem[]> {
  return request.get("/api/v1/ai/history", { params: { page, size } })
}

export function getAiHistoryDetail(id: number): Promise<AiHistoryItem> {
  return request.get(`/api/v1/ai/history/${id}`)
}

export function saveAiToFavorite(historyId: number): Promise<void> {
  return request.post(`/api/v1/ai/history/${historyId}/save`)
}

export function aiFeedback(historyId: number, rating: number, feedback?: string): Promise<void> {
  return request.post("/api/v1/ai/feedback", { historyId, rating, feedback })
}

// ==================== 收藏 ====================

export interface FavoriteItem {
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

export function addFavorite(recipeId: number): Promise<void> {
  return request.post("/api/v1/favorite", { recipeId })
}

export function removeFavorite(recipeId: number): Promise<void> {
  return request.delete(`/api/v1/favorite/${recipeId}`)
}

export function getFavorites(page = 1, size = 20): Promise<FavoriteItem[]> {
  return request.get("/api/v1/favorite/list", { params: { page, size } })
}

export function checkFavorited(recipeId: number): Promise<boolean> {
  return request.get(`/api/v1/favorite/check/${recipeId}`)
}

// ==================== 浏览历史 ====================

export interface HistoryItem {
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

export function getHistory(page = 1, size = 20): Promise<HistoryItem[]> {
  return request.get("/api/v1/history/list", { params: { page, size } })
}

// ==================== 搜索历史 ====================

export interface SearchHistoryItem {
  id: number
  keyword: string
  created_at?: string
}

export function getSearchHistory(): Promise<SearchHistoryItem[]> {
  return request.get("/api/v1/search/history")
}

export function clearSearchHistory(): Promise<void> {
  return request.delete("/api/v1/search/history")
}

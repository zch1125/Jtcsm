import request from './request'
import type { PageResult, RecipeItem } from './types'

/**
 * 菜谱列表（分页）
 */
export function getRecipeList(params: { page: number; size: number; keyword?: string }) {
  return request.get<any, PageResult<RecipeItem>>('/recipes', { params })
}

/**
 * 菜谱详情
 */
export function getRecipe(id: number) {
  return request.get<any, RecipeItem>(`/recipes/${id}`)
}

/**
 * 新增菜谱
 */
export function createRecipe(data: Partial<RecipeItem>) {
  return request.post('/recipes', data)
}

/**
 * 更新菜谱
 */
export function updateRecipe(id: number, data: Partial<RecipeItem>) {
  return request.put(`/recipes/${id}`, data)
}

/**
 * 删除菜谱
 */
export function deleteRecipe(id: number) {
  return request.delete(`/recipes/${id}`)
}

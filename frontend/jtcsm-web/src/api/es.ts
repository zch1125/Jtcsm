import request from './request'

/**
 * ES 向量数据库搜索
 * @param params { index, query, size, from }
 */
export function esSearch(params: {
  index?: string
  query: string
  size?: number
  from?: number
}) {
  return request.post<any, any>('/es/search', params)
}

/**
 * 浏览 ES 索引文档
 */
export function esBrowse(index: string, size = 10, from = 0) {
  return request.get<any, any>('/es/browse', { index, size, from })
}

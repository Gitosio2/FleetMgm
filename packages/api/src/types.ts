export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ApiError = {
  status: number
  code: string
  message: string
  correlationId: string
}

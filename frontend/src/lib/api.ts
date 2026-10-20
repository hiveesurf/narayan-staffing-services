import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

export const api = axios.create({ baseURL })

const ACCESS = 'nss_access'
const REFRESH = 'nss_refresh'

export function getAccessToken() {
  return localStorage.getItem(ACCESS)
}

export function setSession(access: string, refresh: string) {
  localStorage.setItem(ACCESS, access)
  localStorage.setItem(REFRESH, refresh)
}

export function clearSession() {
  localStorage.removeItem(ACCESS)
  localStorage.removeItem(REFRESH)
}

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing: Promise<string | null> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (error.response?.status !== 401 || original?._retry || original?.url?.includes('/api/auth/login') || original?.url?.includes('/api/auth/refresh')) {
      return Promise.reject(error)
    }
    original._retry = true
    const refresh = localStorage.getItem(REFRESH)
    if (!refresh) {
      clearSession()
      return Promise.reject(error)
    }
    refreshing ??= api.post('/api/auth/refresh', { refreshToken: refresh })
      .then((res) => {
        setSession(res.data.accessToken, res.data.refreshToken)
        return res.data.accessToken as string
      })
      .catch(() => {
        clearSession()
        return null
      })
      .finally(() => {
        refreshing = null
      })
    const token = await refreshing
    if (!token) return Promise.reject(error)
    original.headers.Authorization = `Bearer ${token}`
    return api(original)
  },
)

export function errorMessage(error: unknown, fallback = 'Something went wrong.') {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

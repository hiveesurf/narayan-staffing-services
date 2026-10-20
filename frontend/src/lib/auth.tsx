import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, clearSession, getAccessToken, setSession } from './api'

export type UserProfile = {
  id: string
  email: string
  fullName: string
  phone?: string
  active: boolean
  roles: string[]
  permissions: string[]
}

type AuthState = {
  user: UserProfile | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  can: (permission: string) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!getAccessToken()) {
      setLoading(false)
      return
    }
    api.get('/api/auth/me').then((res) => setUser(res.data)).catch(() => clearSession()).finally(() => setLoading(false))
  }, [])

  const value = useMemo<AuthState>(() => ({
    user,
    loading,
    async login(email, password) {
      const res = await api.post('/api/auth/login', { email, password })
      setSession(res.data.accessToken, res.data.refreshToken)
      setUser(res.data.user)
    },
    async logout() {
      const refresh = localStorage.getItem('nss_refresh')
      if (refresh) {
        await api.post('/api/auth/logout', { refreshToken: refresh }).catch(() => undefined)
      }
      clearSession()
      setUser(null)
    },
    can(permission) {
      return !!user?.permissions.includes(permission) || !!user?.roles.includes('SUPER_ADMIN')
    },
  }), [user, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthProvider missing')
  return ctx
}

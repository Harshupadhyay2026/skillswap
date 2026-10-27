import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import authService from '../services/authService';

const TOKEN_KEY = 'skillswap_access_token';
const USER_KEY = 'skillswapUser';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem(USER_KEY);
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [booting, setBooting] = useState(() => Boolean(localStorage.getItem(TOKEN_KEY)));

  const persistSession = useCallback((authData) => {
    localStorage.setItem(TOKEN_KEY, authData.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(authData.user));
    setUser(authData.user);
    return authData.user;
  }, []);

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  // Validate a stored token on boot; drop it if the backend rejects it.
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return;

    let cancelled = false;
    authService
      .getCurrentUser()
      .then((res) => {
        if (cancelled) return;
        if (res?.success && res.data) {
          setUser(res.data);
          localStorage.setItem(USER_KEY, JSON.stringify(res.data));
        } else {
          clearSession();
        }
      })
      .catch(() => {
        if (!cancelled) clearSession();
      })
      .finally(() => {
        if (!cancelled) setBooting(false);
      });

    return () => {
      cancelled = true;
    };
  }, [clearSession]);

  // Global logout hook used by the axios interceptor on a 401.
  useEffect(() => {
    const onUnauthorized = () => clearSession();
    window.addEventListener('skillswap:unauthorized', onUnauthorized);
    return () => window.removeEventListener('skillswap:unauthorized', onUnauthorized);
  }, [clearSession]);

  const login = useCallback(
    async (credentials) => {
      const res = await authService.login(credentials);
      if (!res?.success || !res.data) {
        throw new Error(res?.message || 'Login failed');
      }
      return persistSession(res.data);
    },
    [persistSession]
  );

  const register = useCallback(
    async (payload) => {
      const res = await authService.register(payload);
      if (!res?.success || !res.data) {
        throw new Error(res?.message || 'Registration failed');
      }
      return persistSession(res.data);
    },
    [persistSession]
  );

  const logout = useCallback(() => clearSession(), [clearSession]);

  const updateUser = useCallback((nextUser) => {
    setUser(nextUser);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      booting,
      login,
      register,
      logout,
      updateUser,
    }),
    [user, booting, login, register, logout, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside an <AuthProvider>');
  }
  return ctx;
}

export default AuthContext;

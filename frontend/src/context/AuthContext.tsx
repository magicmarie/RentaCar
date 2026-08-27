import { createContext, useContext, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { TOKEN_STORAGE_KEY } from '../api/client';
import type { CurrentUser } from '../types';

const USER_STORAGE_KEY = 'rentacar_user';

interface AuthContextValue {
  user: CurrentUser | null;
  login: (usernameOrEmail: string, password: string) => Promise<CurrentUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(loadStoredUser);

  async function login(usernameOrEmail: string, password: string): Promise<CurrentUser> {
    const response = await authApi.login(usernameOrEmail, password);
    const currentUser: CurrentUser = {
      token: response.token,
      userId: response.userId,
      firstName: response.firstName,
      lastName: response.lastName,
      email: response.email,
      role: response.role,
    };
    localStorage.setItem(TOKEN_STORAGE_KEY, currentUser.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    setUser(currentUser);
    return currentUser;
  }

  function logout() {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  }

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}

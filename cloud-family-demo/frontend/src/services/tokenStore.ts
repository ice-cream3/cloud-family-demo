import type { LoginResponse } from '../types/api';

const SESSION_KEY = 'cloud-family-session';
export const AUTH_UNAUTHORIZED_EVENT = 'cloud-family-auth-unauthorized';

export type Session = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  username?: string;
  userType?: string;
  roles: string[];
  permissions: string[];
};

export function readSession(): Session | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as Session;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function saveSession(response: LoginResponse): Session {
  const session: Session = {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    tokenType: response.tokenType || 'Bearer',
    username: response.username,
    userType: response.userType,
    roles: response.roles || [],
    permissions: response.permissions || [],
  };
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

export function notifyUnauthorized() {
  window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT));
}

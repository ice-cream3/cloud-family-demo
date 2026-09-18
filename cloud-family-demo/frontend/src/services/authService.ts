import type { LoginMode, LoginResponse } from '../types/api';
import { clearSession, readSession, saveSession } from './tokenStore';
import { post } from './apiClient';

export async function login(mode: LoginMode, username: string, password: string) {
  const path = mode === 'manager' ? '/auth/manager/login' : '/auth/api/login';
  const response = await post<LoginResponse>(path, {
    auth: false,
    body: { username, password },
  });
  return saveSession(response);
}

export async function refreshAccessToken() {
  const session = readSession();
  if (!session?.refreshToken) {
    throw new Error('No refresh token');
  }

  const response = await post<LoginResponse>('/auth/refresh', {
    auth: false,
    body: { refreshToken: session.refreshToken },
  });
  return saveSession(response);
}

export async function logout() {
  try {
    await post<void>('/auth/logout');
  } finally {
    clearSession();
  }
}

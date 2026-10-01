import type { LoginResponse, PartnerHealth, UserProfile } from '../types/api';
import { post } from './apiClient';
import { clearSession, saveSession } from './tokenStore';

export async function loginPartner(username: string, password: string) {
  const response = await post<LoginResponse>('/auth/api/login', {
    auth: false,
    body: { username, password },
  });
  return saveSession(response);
}

export async function logoutPartner() {
  try {
    await post<void>('/auth/logout');
  } finally {
    clearSession();
  }
}

export function loadCurrentUser() {
  return post<UserProfile>('/api/users/me');
}

export function loadPartnerHealth() {
  return post<PartnerHealth>('/api/users/health');
}

import type {
  AccountSecurity,
  AccountSecurityUpdateRequest,
  AppDocument,
  AppVersion,
  FavoriteItem,
  LoginResponse,
  Membership,
  NotificationSettings,
  PageResult,
  PartnerHealth,
  PasswordChangeRequest,
  ProfileDynamic,
  RegisterRequest,
  TripOwner,
  TripPublishRequest,
  TripSlot,
  UserHistoryItem,
  UserProfile,
} from '../types/api';
import { post } from './apiClient';
import { clearSession, saveSession } from './tokenStore';

export async function loginPartner(username: string, password: string, remember = true) {
  const response = await post<LoginResponse>('/auth/api/login', {
    auth: false,
    body: { username, password },
  });
  return saveSession(response, remember);
}

export async function registerPartner(request: RegisterRequest) {
  const response = await post<LoginResponse>('/auth/api/register', {
    auth: false,
    body: request,
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

export function loadAccountSecurity() {
  return post<AccountSecurity>('/api/users/security');
}

export function updateAccountSecurity(request: AccountSecurityUpdateRequest) {
  return post<AccountSecurity>('/api/users/security/update', {
    body: request,
  });
}

export function changeAccountPassword(request: PasswordChangeRequest) {
  return post<void>('/api/users/security/password', {
    body: request,
  });
}

export function loadNotificationSettings() {
  return post<NotificationSettings>('/api/users/settings/notifications');
}

export function updateNotificationSettings(request: NotificationSettings) {
  return post<NotificationSettings>('/api/users/settings/notifications/update', {
    body: request,
  });
}

export function loadAppDocument(documentType: 'about' | 'privacy' | 'agreement') {
  return post<AppDocument>(`/api/users/settings/document/${documentType}`);
}

export function loadAppVersion() {
  return post<AppVersion>('/api/users/settings/version');
}

export function loadMembership() {
  return post<Membership>('/api/users/membership');
}

export function loadHistory(category = '全部', pageNum = 1, pageSize = 20) {
  return post<PageResult<UserHistoryItem>>('/api/users/history/page', {
    body: { category, pageNum, pageSize },
  });
}

export function loadFavorites(pageNum = 1, pageSize = 20) {
  return post<PageResult<FavoriteItem>>('/api/users/favorites/page', {
    body: { pageNum, pageSize },
  });
}

export function refreshProfileDynamic() {
  return post<ProfileDynamic>('/api/users/dynamic/refresh');
}

export function loadTripOwners() {
  return post<TripOwner[]>('/api/users/trips/bookable/owners');
}

export function loadBookableTripSlots(ownerUsername: string) {
  return post<TripSlot[]>(`/api/users/trips/bookable/slots/${encodeURIComponent(ownerUsername)}`);
}

export function loadMyTrips() {
  return post<TripSlot[]>('/api/users/trips/my');
}

export function loadMyTripReservations() {
  return post<TripSlot[]>('/api/users/trips/my-reservations');
}

export function publishTrip(request: TripPublishRequest) {
  return post<TripSlot>('/api/users/trips/publish', {
    body: request,
  });
}

export function applyTrip(slotId: number, applyNote = '希望预约这个时段', confirmConflict = false) {
  return post<TripSlot>('/api/users/trips/apply', {
    body: { slotId, applyNote, confirmConflict },
  });
}

export function reviewTrip(slotId: number, approved: boolean) {
  return post<TripSlot>('/api/users/trips/review', {
    body: { slotId, approved },
  });
}

export function loadPartnerHealth() {
  return post<PartnerHealth>('/api/users/health');
}

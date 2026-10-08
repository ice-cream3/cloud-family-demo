export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
  timestamp: string;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresAt?: string;
  refreshTokenExpiresAt?: string;
  username?: string;
  userType?: string;
  roles?: string[];
  permissions?: string[];
};

export type RegisterRequest = {
  username: string;
  password: string;
  displayName: string;
  email?: string;
  phone?: string;
};

export type UserProfile = {
  username?: string;
  displayName?: string;
  roles?: string[];
  requestTime?: string;
  [key: string]: unknown;
};

export type AccountSecurity = {
  username: string;
  displayName: string;
  email?: string | null;
  phone?: string | null;
  vipLevel?: string | null;
  status?: string | null;
  updatedAt?: string | null;
};

export type AccountSecurityUpdateRequest = {
  displayName: string;
  email?: string | null;
  phone?: string | null;
};

export type PasswordChangeRequest = {
  currentPassword: string;
  newPassword: string;
};

export type PartnerHealth = {
  status?: string;
  service?: string;
  [key: string]: unknown;
};

export type NotificationSettings = {
  systemEnabled: boolean;
  activityEnabled: boolean;
  taskEnabled: boolean;
};

export type AppDocument = {
  documentType: string;
  title: string;
  content: string;
  updatedAt?: string | null;
};

export type AppVersion = {
  versionName: string;
  latest: boolean;
  releaseNote?: string | null;
};

export type Membership = {
  username: string;
  planName: string;
  status: string;
  expireAt?: string | null;
  benefits?: string | null;
};

export type UserHistoryItem = {
  id: number;
  category: string;
  title: string;
  description?: string | null;
  iconTone?: string | null;
  occurredAt?: string | null;
};

export type FavoriteItem = {
  id: number;
  itemType: string;
  title: string;
  description?: string | null;
  iconTone?: string | null;
  createdAt?: string | null;
};

export type PageResult<T> = {
  total: number;
  pageNum: number;
  pageSize: number;
  records: T[];
};

export type ProfileDynamic = {
  favoriteCount: number;
  historyCount: number;
  fileCount: number;
  couponCount: number;
  serviceStatus: string;
  refreshedAt: string;
};

export type TripOwner = {
  ownerUsername: string;
  ownerDisplayName: string;
  slotCount: number;
  nextDate: string;
  nextTime: string;
};

export type TripSlot = {
  id: number;
  ownerUsername: string;
  ownerDisplayName: string;
  title: string;
  place: string;
  tripDate: string;
  startTime: string;
  endTime: string;
  status: 'OPEN' | 'PENDING' | 'BOOKED';
  applicantUsername?: string | null;
  applicantDisplayName?: string | null;
  applyNote?: string | null;
  appliedAt?: string | null;
  reviewedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type TripPublishRequest = {
  title: string;
  place: string;
  tripDate: string;
  startTime: string;
  endTime: string;
};

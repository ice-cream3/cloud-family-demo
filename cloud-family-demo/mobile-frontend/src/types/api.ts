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

export type UserProfile = {
  username?: string;
  displayName?: string;
  roles?: string[];
  requestTime?: string;
  [key: string]: unknown;
};

export type PartnerHealth = {
  status?: string;
  service?: string;
  [key: string]: unknown;
};

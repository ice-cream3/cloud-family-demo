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

export type ManagerDashboard = {
  username?: string;
  displayName?: string;
  roles?: string[];
  permissions?: string[];
  requestTime?: string;
  [key: string]: unknown;
};

export type UserProfile = {
  username?: string;
  displayName?: string;
  roles?: string[];
  requestTime?: string;
  [key: string]: unknown;
};

export type PageResult<T> = {
  total: number;
  pageNum: number;
  pageSize: number;
  records: T[];
};

export type PageQuery = {
  keyword?: string;
  status?: string;
};

export type SysMenu = {
  id: number;
  parentId?: number | null;
  menuCode?: string;
  menuName?: string;
  path?: string;
  component?: string;
  icon?: string;
  sortOrder?: number | null;
  visible?: boolean;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type MenuTreeNode = SysMenu & {
  children: MenuTreeNode[];
};

export type SysUser = {
  id: number;
  username: string;
  displayName?: string;
  email?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type SysRole = {
  id: number;
  roleCode?: string;
  roleName?: string;
  description?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type SysPermission = {
  id: number;
  permissionCode?: string;
  permissionName?: string;
  description?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type SystemPageRecord = SysUser | SysRole | SysPermission | SysMenu;

export type SystemRecordPayload = {
  [key: string]: string | number | boolean | null | undefined;
};

export type LoginMode = 'partner' | 'manager';

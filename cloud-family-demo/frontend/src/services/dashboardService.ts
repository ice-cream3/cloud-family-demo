import type {
  ManagerDashboard,
  MenuTreeNode,
  PageQuery,
  PageResult,
  SysMenu,
  SysPermission,
  SysRole,
  SysUser,
  SystemPageRecord,
  SystemRecordPayload,
  UserProfile,
  VipUser,
} from '../types/api';
import { post } from './apiClient';

export function loadManagerDashboard() {
  return post<ManagerDashboard>('/api/manager/dashboard');
}

export function loadCurrentUser() {
  return post<UserProfile>('/api/users/me');
}

export function loadMenus() {
  return post<MenuTreeNode[]>('/api/manager/system/menus/current-user/tree');
}

export function loadSystemMenuTree() {
  return post<MenuTreeNode[]>('/api/manager/system/menus/tree');
}

export function loadSystemUsers(pageNum = 1, pageSize = 10) {
  return post<PageResult<SysUser>>('/api/manager/system/users/page', {
    form: {
      pageNum,
      pageSize,
    },
  });
}

export function loadSystemRoles(pageNum = 1, pageSize = 10) {
  return post<PageResult<SysRole>>('/api/manager/system/roles/page', {
    form: {
      pageNum,
      pageSize,
    },
  });
}

export function loadSystemPermissions(pageNum = 1, pageSize = 10) {
  return post<PageResult<SysPermission>>('/api/manager/system/permissions/page', {
    form: {
      pageNum,
      pageSize,
    },
  });
}

export function loadSystemMenuPage(pageNum = 1, pageSize = 10, query: PageQuery = {}) {
  return post<PageResult<SysMenu>>('/api/manager/system/menus/page', {
    form: {
      pageNum,
      pageSize,
      keyword: query.keyword,
      status: query.status,
    },
  });
}

export function loadSystemOperationLogs(pageNum = 1, pageSize = 10, query: PageQuery = {}) {
  return post<PageResult<SystemPageRecord>>('/api/manager/system/operation-logs/page', {
    form: {
      pageNum,
      pageSize,
      keyword: query.keyword,
      status: query.status,
      operatorUsername: query.operatorUsername,
      businessName: query.businessName,
      businessModule: query.businessModule,
      businessType: query.businessType,
    },
  });
}

export function loadManagerVipUsers(pageNum = 1, pageSize = 10, query: PageQuery = {}) {
  return post<PageResult<VipUser>>('/api/manager/vip-users/page', {
    form: {
      pageNum,
      pageSize,
      username: query.keyword,
      displayName: query.businessName,
      vipLevel: query.businessType,
      status: query.status,
    },
  });
}

export function loadPartnerInfo(pageNum = 1, pageSize = 10, query: PageQuery = {}) {
  return post<PageResult<VipUser>>('/api/manager/partner/info/page', {
    form: {
      pageNum,
      pageSize,
      username: query.keyword,
      displayName: query.businessName,
      vipLevel: query.businessType,
      status: query.status,
    },
  });
}

export function loadPartnerFeaturePage(feature: string, pageNum = 1, pageSize = 10, query: PageQuery = {}) {
  return post<PageResult<SystemPageRecord>>(`/api/manager/partner/features/${feature}/page`, {
    form: {
      pageNum,
      pageSize,
      keyword: query.keyword,
      status: query.status,
      businessType: query.businessType,
    },
  });
}

export function createSystemRecord(path: string, payload: SystemRecordPayload) {
  return post<SystemPageRecord>(getSystemEndpoint(path), {
    body: payload,
  });
}

export function updateSystemRecord(path: string, id: number, payload: SystemRecordPayload) {
  return post<SystemPageRecord>(`${getSystemEndpoint(path)}/update/${id}`, {
    body: payload,
  });
}

export function deleteSystemRecord(path: string, id: number) {
  return post<void>(`${getSystemEndpoint(path)}/delete/${id}`);
}

export function resetSystemUserPassword(id: number, password: string) {
  return post<SystemPageRecord>(`/api/manager/system/users/password/reset/${id}`, {
    body: { password },
  });
}

export function resetPartnerInfoPassword(id: number, password: string) {
  return post<SystemPageRecord>(`/api/manager/partner/info/password/reset/${id}`, {
    body: { password },
  });
}

export function loadSystemMenuPermissions(id: number) {
  return post<SysPermission[]>(`/api/manager/system/menus/permissions/${id}`);
}

export function replaceSystemMenuPermissions(id: number, ids: number[]) {
  return post<SysPermission[]>(`/api/manager/system/menus/permissions/replace/${id}`, {
    body: { ids },
  });
}

export function loadSystemUserRoles(id: number) {
  return post<SysRole[]>(`/api/manager/system/users/roles/${id}`);
}

export function replaceSystemUserRoles(id: number, ids: number[]) {
  return post<SysRole[]>(`/api/manager/system/users/roles/replace/${id}`, {
    body: { ids },
  });
}

export function loadSystemRolePermissions(id: number) {
  return post<SysPermission[]>(`/api/manager/system/roles/permissions/${id}`);
}

export function replaceSystemRolePermissions(id: number, ids: number[]) {
  return post<SysPermission[]>(`/api/manager/system/roles/permissions/replace/${id}`, {
    body: { ids },
  });
}

export function loadSystemRoleMenus(id: number) {
  return post<SysMenu[]>(`/api/manager/system/roles/menus/${id}`);
}

export function replaceSystemRoleMenus(id: number, ids: number[]) {
  return post<SysMenu[]>(`/api/manager/system/roles/menus/replace/${id}`, {
    body: { ids },
  });
}

function getSystemEndpoint(path: string) {
  const endpoints: Record<string, string> = {
    '/api/manager/system/users': '/api/manager/system/users',
    '/api/manager/system/roles': '/api/manager/system/roles',
    '/api/manager/system/permissions': '/api/manager/system/permissions',
    '/api/manager/system/menus': '/api/manager/system/menus',
    '/api/manager/vip-users': '/api/manager/vip-users',
    '/api/manager/partner/info': '/api/manager/partner/info',
    '/api/manager/partner/settings/notifications': '/api/manager/partner/features/notifications',
    '/api/manager/partner/settings/documents': '/api/manager/partner/features/documents',
    '/api/manager/partner/settings/versions': '/api/manager/partner/features/versions',
    '/api/manager/partner/memberships': '/api/manager/partner/features/memberships',
    '/api/manager/partner/histories': '/api/manager/partner/features/histories',
    '/api/manager/partner/favorites': '/api/manager/partner/features/favorites',
  };
  const endpoint = endpoints[path];
  if (!endpoint) {
    throw new Error(`Unsupported system path: ${path}`);
  }
  return endpoint;
}

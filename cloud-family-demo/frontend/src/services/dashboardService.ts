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

export function loadSystemMenuPermissions(id: number) {
  return post<SysPermission[]>(`/api/manager/system/menus/permissions/${id}`);
}

export function replaceSystemMenuPermissions(id: number, ids: number[]) {
  return post<SysPermission[]>(`/api/manager/system/menus/permissions/replace/${id}`, {
    body: { ids },
  });
}

function getSystemEndpoint(path: string) {
  const endpoints: Record<string, string> = {
    '/api/manager/system/users': '/api/manager/system/users',
    '/api/manager/system/roles': '/api/manager/system/roles',
    '/api/manager/system/permissions': '/api/manager/system/permissions',
    '/api/manager/system/menus': '/api/manager/system/menus',
  };
  const endpoint = endpoints[path];
  if (!endpoint) {
    throw new Error(`Unsupported system path: ${path}`);
  }
  return endpoint;
}

import type { ManagerDashboard, MenuTreeNode, PageResult, SysUser, UserProfile } from '../types/api';
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

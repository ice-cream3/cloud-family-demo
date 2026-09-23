import { useEffect, useState } from 'react';
import { LoginPanel } from './components/LoginPanel';
import { Dashboard } from './components/Dashboard';
import { login, logout } from './services/authService';
import {
  createSystemRecord,
  deleteSystemRecord,
  loadSystemMenuPermissions,
  loadCurrentUser,
  loadManagerDashboard,
  loadMenus,
  loadSystemMenuPage,
  loadSystemOperationLogs,
  loadSystemPermissions,
  loadSystemRoleMenus,
  loadSystemRoles,
  loadSystemUserRoles,
  loadSystemUsers,
  resetSystemUserPassword,
  replaceSystemMenuPermissions,
  replaceSystemRoleMenus,
  replaceSystemUserRoles,
  updateSystemRecord,
} from './services/dashboardService';
import { ApiError } from './services/apiClient';
import { AUTH_UNAUTHORIZED_EVENT, readSession, type Session } from './services/tokenStore';
import type { LoginMode, ManagerDashboard, MenuTreeNode, PageQuery, PageResult, SystemPageRecord, SystemRecordPayload, UserProfile } from './types/api';

export default function App() {
  const [session, setSession] = useState<Session | null>(() => readSession());
  const [managerDashboard, setManagerDashboard] = useState<ManagerDashboard | null>(null);
  const [userProfile, setUserProfile] = useState<UserProfile | null>(null);
  const [menuTree, setMenuTree] = useState<MenuTreeNode[]>([]);
  const [activeMenu, setActiveMenu] = useState<MenuTreeNode | null>(null);
  const [systemPage, setSystemPage] = useState<PageResult<SystemPageRecord> | null>(null);
  const [systemPageQuery, setSystemPageQuery] = useState<PageQuery>({});
  const [systemPageSize, setSystemPageSize] = useState(20);
  const [systemPageLoading, setSystemPageLoading] = useState(false);
  const [systemPageError, setSystemPageError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [menuError, setMenuError] = useState<string | null>(null);

  useEffect(() => {
    if (session) {
      void reloadData();
    }
  }, [session?.accessToken]);

  useEffect(() => {
    function handleUnauthorized() {
      resetAuthenticatedState();
      setError('登录状态已失效，请重新登录。');
    }

    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
  }, []);

  async function handleLogin(mode: LoginMode, username: string, password: string) {
    setLoading(true);
    setError(null);
    setMenuError(null);
    try {
      const nextSession = await login(mode, username, password);
      setSession(nextSession);
    } catch (err) {
      setError(readError(err));
    } finally {
      setLoading(false);
    }
  }

  async function reloadData() {
    setLoading(true);
    setError(null);
    try {
      const [profileResult, managerResult, menuResult] = await Promise.allSettled([
        loadCurrentUser(),
        loadManagerDashboard(),
        loadMenus(),
      ]);

      setUserProfile(profileResult.status === 'fulfilled' ? profileResult.value : null);
      setManagerDashboard(managerResult.status === 'fulfilled' ? managerResult.value : null);
      setMenuTree(menuResult.status === 'fulfilled' ? menuResult.value : []);
      setMenuError(menuResult.status === 'rejected' ? '当前登录用户菜单加载失败，请确认账号有管理端菜单权限。' : null);

      if (profileResult.status === 'rejected' && managerResult.status === 'rejected') {
        setError('后端接口请求失败，请确认 gateway 已启动且令牌有效。');
      }
    } catch (err) {
      setError(readError(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleMenuSelect(menu: MenuTreeNode) {
    setActiveMenu(menu);
    setSystemPageError(null);
    setSystemPage(null);
    setSystemPageQuery({});

    if (isSystemPageMenu(menu.path)) {
      await loadSystemPage(menu, 1, {});
    }
  }

  async function loadSystemPage(menu: MenuTreeNode | null, pageNum: number, query = systemPageQuery, pageSize = systemPageSize) {
    if (!menu || !isSystemPageMenu(menu.path)) {
      return;
    }

    setSystemPageLoading(true);
    setSystemPageError(null);
    try {
      setSystemPage(await getSystemPageLoader(menu.path)(pageNum, pageSize, query));
    } catch (err) {
      setSystemPage(null);
      setSystemPageError(readError(err));
    } finally {
      setSystemPageLoading(false);
    }
  }

  async function handleCreateSystemRecord(payload: SystemRecordPayload) {
    if (!activeMenu?.path) {
      return;
    }
    await createSystemRecord(activeMenu.path, payload);
    await reloadCurrentSystemPage(1);
    if (activeMenu.path === '/api/manager/system/menus') {
      await refreshMenus();
    }
  }

  async function handleUpdateSystemRecord(id: number, payload: SystemRecordPayload) {
    if (!activeMenu?.path) {
      return;
    }
    await updateSystemRecord(activeMenu.path, id, payload);
    await reloadCurrentSystemPage(systemPage?.pageNum || 1);
    if (activeMenu.path === '/api/manager/system/menus') {
      await refreshMenus();
    }
  }

  async function handleDeleteSystemRecord(id: number) {
    if (!activeMenu?.path) {
      return;
    }
    await deleteSystemRecord(activeMenu.path, id);
    await reloadCurrentSystemPage(systemPage?.pageNum || 1);
    if (activeMenu.path === '/api/manager/system/menus') {
      await refreshMenus();
    }
  }

  async function handleResetUserPassword(id: number, password: string) {
    await resetSystemUserPassword(id, password);
    await reloadCurrentSystemPage(systemPage?.pageNum || 1);
  }

  async function handleLoadMenuPermissionOptions() {
    const page = await loadSystemPermissions(1, 100);
    return page.records;
  }

  async function handleLoadMenuPermissions(id: number) {
    return loadSystemMenuPermissions(id);
  }

  async function handleReplaceMenuPermissions(id: number, ids: number[]) {
    await replaceSystemMenuPermissions(id, ids);
  }

  async function handleLoadUserRoleOptions() {
    const page = await loadSystemRoles(1, 100);
    return page.records;
  }

  async function handleLoadUserRoles(id: number) {
    return loadSystemUserRoles(id);
  }

  async function handleReplaceUserRoles(id: number, ids: number[]) {
    await replaceSystemUserRoles(id, ids);
  }

  async function handleLoadRoleMenus(id: number) {
    return loadSystemRoleMenus(id);
  }

  async function handleReplaceRoleMenus(id: number, ids: number[]) {
    await replaceSystemRoleMenus(id, ids);
  }

  async function reloadCurrentSystemPage(pageNum: number) {
    await loadSystemPage(activeMenu, pageNum, systemPageQuery, systemPageSize);
  }

  async function handleSystemPageSizeChange(pageSize: number) {
    setSystemPageSize(pageSize);
    await loadSystemPage(activeMenu, 1, systemPageQuery, pageSize);
  }

  async function refreshMenus() {
    try {
      setMenuTree(await loadMenus());
      setMenuError(null);
    } catch {
      setMenuError('当前登录用户菜单加载失败，请确认账号有管理端菜单权限。');
    }
  }

  async function handleLogout() {
    await logout();
    resetAuthenticatedState();
  }

  function resetAuthenticatedState() {
    setSession(null);
    setManagerDashboard(null);
    setUserProfile(null);
    setMenuTree([]);
    setActiveMenu(null);
    setSystemPage(null);
    setSystemPageQuery({});
    setSystemPageSize(20);
    setSystemPageError(null);
    setSystemPageLoading(false);
    setMenuError(null);
  }

  if (!session) {
    return <LoginPanel loading={loading} error={error} onLogin={handleLogin} />;
  }

  return (
    <Dashboard
      session={session}
      managerDashboard={managerDashboard}
      userProfile={userProfile}
      menuTree={menuTree}
      menuError={menuError}
      activeMenu={activeMenu}
      systemPage={systemPage}
      systemPageQuery={systemPageQuery}
      systemPageLoading={systemPageLoading}
      systemPageError={systemPageError}
      onMenuSelect={handleMenuSelect}
      onSystemPageChange={(pageNum) => loadSystemPage(activeMenu, pageNum)}
      onSystemPageSizeChange={handleSystemPageSizeChange}
      onSystemPageSearch={(query) => {
        setSystemPageQuery(query);
        void loadSystemPage(activeMenu, 1, query, systemPageSize);
      }}
      onCreateSystemRecord={handleCreateSystemRecord}
      onUpdateSystemRecord={handleUpdateSystemRecord}
      onDeleteSystemRecord={handleDeleteSystemRecord}
      onResetUserPassword={handleResetUserPassword}
      onLoadMenuPermissionOptions={handleLoadMenuPermissionOptions}
      onLoadMenuPermissions={handleLoadMenuPermissions}
      onReplaceMenuPermissions={handleReplaceMenuPermissions}
      onLoadUserRoleOptions={handleLoadUserRoleOptions}
      onLoadUserRoles={handleLoadUserRoles}
      onReplaceUserRoles={handleReplaceUserRoles}
      onLoadRoleMenus={handleLoadRoleMenus}
      onReplaceRoleMenus={handleReplaceRoleMenus}
      loading={loading}
      error={error}
      onReload={reloadData}
      onLogout={handleLogout}
    />
  );
}

function isSystemPageMenu(path?: string) {
  return Boolean(path && getSystemPageLoaders()[path]);
}

function getSystemPageLoader(path?: string) {
  return getSystemPageLoaders()[path || ''];
}

function getSystemPageLoaders(): Record<string, (pageNum: number, pageSize: number, query?: PageQuery) => Promise<PageResult<SystemPageRecord>>> {
  return {
    '/api/manager/system/users': loadSystemUsers,
    '/api/manager/system/roles': loadSystemRoles,
    '/api/manager/system/permissions': loadSystemPermissions,
    '/api/manager/system/menus': loadSystemMenuPage,
    '/api/manager/system/operation-logs': loadSystemOperationLogs,
  };
}

function readError(error: unknown) {
  if (error instanceof ApiError) {
    return `${error.message} (${error.status})`;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '请求失败';
}

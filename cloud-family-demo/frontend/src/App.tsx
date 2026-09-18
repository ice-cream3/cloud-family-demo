import { useEffect, useState } from 'react';
import { LoginPanel } from './components/LoginPanel';
import { Dashboard } from './components/Dashboard';
import { login, logout } from './services/authService';
import { loadCurrentUser, loadManagerDashboard, loadMenus, loadSystemUsers } from './services/dashboardService';
import { ApiError } from './services/apiClient';
import { AUTH_UNAUTHORIZED_EVENT, readSession, type Session } from './services/tokenStore';
import type { LoginMode, ManagerDashboard, MenuTreeNode, PageResult, SysUser, UserProfile } from './types/api';

export default function App() {
  const [session, setSession] = useState<Session | null>(() => readSession());
  const [managerDashboard, setManagerDashboard] = useState<ManagerDashboard | null>(null);
  const [userProfile, setUserProfile] = useState<UserProfile | null>(null);
  const [menuTree, setMenuTree] = useState<MenuTreeNode[]>([]);
  const [activeMenu, setActiveMenu] = useState<MenuTreeNode | null>(null);
  const [userPage, setUserPage] = useState<PageResult<SysUser> | null>(null);
  const [userPageLoading, setUserPageLoading] = useState(false);
  const [userPageError, setUserPageError] = useState<string | null>(null);
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
    setUserPageError(null);

    if (menu.path === '/api/manager/system/users') {
      await loadUserPage(1);
    }
  }

  async function loadUserPage(pageNum: number) {
    setUserPageLoading(true);
    setUserPageError(null);
    try {
      setUserPage(await loadSystemUsers(pageNum, userPage?.pageSize || 10));
    } catch (err) {
      setUserPage(null);
      setUserPageError(readError(err));
    } finally {
      setUserPageLoading(false);
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
    setUserPage(null);
    setUserPageError(null);
    setUserPageLoading(false);
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
      userPage={userPage}
      userPageLoading={userPageLoading}
      userPageError={userPageError}
      onMenuSelect={handleMenuSelect}
      onUserPageChange={loadUserPage}
      loading={loading}
      error={error}
      onReload={reloadData}
      onLogout={handleLogout}
    />
  );
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

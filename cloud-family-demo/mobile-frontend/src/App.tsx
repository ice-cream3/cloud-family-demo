import { useEffect, useState } from 'react';
import { HomePage } from './components/HomePage';
import { LoginPage } from './components/LoginPage';
import { ApiError } from './services/apiClient';
import { loadCurrentUser, loadPartnerHealth, loginPartner, logoutPartner, registerPartner } from './services/partnerService';
import { AUTH_UNAUTHORIZED_EVENT, readSession, type Session } from './services/tokenStore';
import type { PartnerHealth, RegisterRequest, UserProfile } from './types/api';

export default function App() {
  const [session, setSession] = useState<Session | null>(() => readSession());
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [health, setHealth] = useState<PartnerHealth | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (session) {
      void reloadData();
    }
  }, [session?.accessToken]);

  useEffect(() => {
    function handleUnauthorized() {
      setSession(null);
      setProfile(null);
      setHealth(null);
      setError('登录状态已失效，请重新登录。');
    }

    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
  }, []);

  async function handleLogin(username: string, password: string, remember: boolean) {
    setLoading(true);
    setError(null);
    try {
      setSession(await loginPartner(username, password, remember));
    } catch (err) {
      setError(readError(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleRegister(request: RegisterRequest) {
    setLoading(true);
    setError(null);
    try {
      setSession(await registerPartner(request));
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
      const [profileResult, healthResult] = await Promise.allSettled([
        loadCurrentUser(),
        loadPartnerHealth(),
      ]);

      setProfile(profileResult.status === 'fulfilled' ? profileResult.value : null);
      setHealth(healthResult.status === 'fulfilled' ? healthResult.value : null);

      if (profileResult.status === 'rejected') {
        setError('用户端接口请求失败，请确认 gateway 和 partner-service 已启动且令牌有效。');
      }
    } catch (err) {
      setError(readError(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleLogout() {
    await logoutPartner();
    setSession(null);
    setProfile(null);
    setHealth(null);
    setError(null);
  }

  if (!session) {
    return <LoginPage loading={loading} error={error} onLogin={handleLogin} onRegister={handleRegister} />;
  }

  return (
    <HomePage
      session={session}
      profile={profile}
      health={health}
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

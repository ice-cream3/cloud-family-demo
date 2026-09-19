import { FormEvent, useState } from 'react';
import { KeyRound, LockKeyhole, LogIn, Network, ServerCog, ShieldCheck, UserRound } from 'lucide-react';
import type { LoginMode } from '../types/api';

type LoginPanelProps = {
  loading: boolean;
  error: string | null;
  onLogin: (mode: LoginMode, username: string, password: string) => Promise<void>;
};

export function LoginPanel({ loading, error, onLogin }: LoginPanelProps) {
  const [mode, setMode] = useState<LoginMode>('manager');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onLogin(mode, username.trim(), password);
  }

  return (
    <main className="login-surface">
      <section className="login-panel" aria-label="登录">
        <div className="login-visual" aria-hidden="true">
          <div className="login-brand">
            <div className="icon-box login-logo">
              <KeyRound size={24} />
            </div>
            <div>
              <span>Cloud Family</span>
              <strong>Unified Gateway</strong>
            </div>
          </div>
          <div className="login-hero-copy">
            <p className="login-kicker">Secure access console</p>
            <h1>统一云家庭业务入口</h1>
            <p>面向用户端与管理端的轻量控制台，集中呈现认证、网关转发与后端服务状态。</p>
          </div>
          <div className="login-status-grid">
            <div>
              <ShieldCheck size={18} />
              <span>JWT Token</span>
              <strong>Active</strong>
            </div>
            <div>
              <Network size={18} />
              <span>Gateway</span>
              <strong>Proxy</strong>
            </div>
            <div>
              <ServerCog size={18} />
              <span>Services</span>
              <strong>Ready</strong>
            </div>
          </div>
        </div>

        <div className="login-card">
          <div className="panel-header">
            <div>
              <p className="login-kicker">Welcome back</p>
              <h2>登录控制台</h2>
              <p>选择身份后使用账号密码登录。</p>
            </div>
          </div>

          <div className="segmented" role="tablist" aria-label="登录类型">
            <button className={mode === 'partner' ? 'active' : ''} type="button" onClick={() => setMode('partner')}>
              用户端
            </button>
            <button className={mode === 'manager' ? 'active' : ''} type="button" onClick={() => setMode('manager')}>
              管理端
            </button>
          </div>

          <form className="login-form" onSubmit={handleSubmit}>
            <label>
              账号
              <span className="input-wrap">
                <UserRound size={18} />
                <input
                  autoComplete="username"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="请输入账号"
                />
              </span>
            </label>
            <label>
              密码
              <span className="input-wrap">
                <LockKeyhole size={18} />
                <input
                  autoComplete="current-password"
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="请输入密码"
                />
              </span>
            </label>
            {error ? <div className="error-banner">{error}</div> : null}
            <button className="primary-button" type="submit" disabled={loading || !username.trim() || !password}>
              <LogIn size={18} />
              {loading ? '登录中' : '登录'}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}

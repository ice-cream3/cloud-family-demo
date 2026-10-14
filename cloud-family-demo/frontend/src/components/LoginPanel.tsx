import { FormEvent, useState } from 'react';
import { Activity, Cpu, KeyRound, LockKeyhole, LogIn, Network, ServerCog, ShieldCheck, UserRound, Zap } from 'lucide-react';
import type { LoginMode } from '../types/api';

type LoginPanelProps = {
  loading: boolean;
  error: string | null;
  onLogin: (mode: LoginMode, username: string, password: string) => Promise<void>;
};

export function LoginPanel({ loading, error, onLogin }: LoginPanelProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onLogin('manager', username.trim(), password);
  }

  return (
    <main className="login-surface">
      <section className="login-panel" aria-label="登录">
        <div className="login-visual" aria-hidden="true">
          <div className="tech-orbit one" />
          <div className="tech-orbit two" />
          <div className="tech-node node-a" />
          <div className="tech-node node-b" />
          <div className="tech-node node-c" />
          <div className="login-brand">
            <div className="icon-box login-logo">
              <KeyRound size={24} />
            </div>
            <div>
              <span>Cloud Family</span>
              <strong>Manager Console</strong>
            </div>
          </div>
          <div className="login-hero-copy">
            <p className="login-kicker">Secure access console</p>
            <h1>统一管理端业务入口</h1>
            <p>面向管理端的轻量控制台，集中呈现认证、网关转发、系统菜单和业务管理能力。</p>
            <div className="login-signal-row">
              <span><Activity size={15} /> Live telemetry</span>
              <span><Cpu size={15} /> 24 services online</span>
              <span><Zap size={15} /> 12ms gateway</span>
            </div>
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
              <h2>登录管理端</h2>
              <p>使用具备管理端权限的账号密码登录。</p>
            </div>
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

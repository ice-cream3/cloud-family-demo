import { FormEvent, useState } from 'react';
import { KeyRound, LockKeyhole, LogIn, Network, ShieldCheck, Smartphone, UserRound } from 'lucide-react';

type LoginPageProps = {
  loading: boolean;
  error: string | null;
  onLogin: (username: string, password: string) => Promise<void>;
};

export function LoginPage({ loading, error, onLogin }: LoginPageProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onLogin(username.trim(), password);
  }

  return (
    <main className="login-surface">
      <section className="login-panel" aria-label="Partner 用户登录">
        <div className="login-visual" aria-hidden="true">
          <div className="login-brand">
            <div className="icon-box login-logo">
              <KeyRound size={24} />
            </div>
            <div>
              <span>Cloud Family</span>
              <strong>Partner Portal</strong>
            </div>
          </div>

          <div className="login-hero-copy">
            <p className="login-kicker">Responsive partner access</p>
            <h1>PC Web 与 H5 共用入口</h1>
            <p>一套用户端工程同时适配桌面和移动端，登录后进入 partner 首页。</p>
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
              <Smartphone size={18} />
              <span>H5</span>
              <strong>Ready</strong>
            </div>
          </div>
        </div>

        <div className="login-card">
          <div className="panel-header">
            <div>
              <p className="login-kicker">Welcome back</p>
              <h2>Partner 用户登录</h2>
              <p>使用 partner 账号密码访问用户端首页。</p>
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

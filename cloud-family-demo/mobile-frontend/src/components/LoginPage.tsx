import { FormEvent, useState } from 'react';
import { KeyRound, LockKeyhole, LogIn, Mail, Network, Phone, ShieldCheck, Smartphone, UserRound } from 'lucide-react';
import type { RegisterRequest } from '../types/api';

type LoginPageProps = {
  loading: boolean;
  error: string | null;
  onLogin: (username: string, password: string) => Promise<void>;
  onRegister: (request: RegisterRequest) => Promise<void>;
};

export function LoginPage({ loading, error, onLogin, onRegister }: LoginPageProps) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (mode === 'register') {
      await onRegister({
        username: username.trim(),
        password,
        displayName: displayName.trim(),
        email: email.trim() || undefined,
        phone: phone.trim() || undefined,
      });
      return;
    }
    await onLogin(username.trim(), password);
  }

  const submitDisabled = loading
    || !username.trim()
    || !password
    || (mode === 'register' && !displayName.trim());

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
              <strong>Partner</strong>
            </div>
          </div>

          <div className="login-hero-copy">
            <p className="login-kicker">Partner Access</p>
            <h1>移动端用户中心</h1>
            <p>账号、权益、通知和服务状态集中在一个适合手机访问的入口。</p>
          </div>

          <div className="login-status-grid">
            <div>
              <ShieldCheck size={18} />
              <span>身份</span>
              <strong>安全</strong>
            </div>
            <div>
              <Network size={18} />
              <span>服务</span>
              <strong>在线</strong>
            </div>
            <div>
              <Smartphone size={18} />
              <span>体验</span>
              <strong>H5</strong>
            </div>
          </div>
        </div>

        <div className="login-card">
          <div className="panel-header">
            <div>
              <p className="login-kicker">Welcome</p>
              <h2>{mode === 'register' ? '创建账号' : '欢迎回来'}</h2>
              <p>{mode === 'register' ? '填写基础资料，注册后自动登录。' : '登录后查看你的 Partner 首页。'}</p>
            </div>
          </div>

          <div className="auth-switch" role="tablist" aria-label="账号入口">
            <button className={mode === 'login' ? 'active' : ''} type="button" onClick={() => setMode('login')}>
              登录
            </button>
            <button className={mode === 'register' ? 'active' : ''} type="button" onClick={() => setMode('register')}>
              注册
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
            {mode === 'register' ? (
              <>
                <label>
                  昵称
                  <span className="input-wrap">
                    <UserRound size={18} />
                    <input
                      autoComplete="name"
                      value={displayName}
                      onChange={(event) => setDisplayName(event.target.value)}
                      placeholder="请输入昵称"
                    />
                  </span>
                </label>
                <label>
                  邮箱
                  <span className="input-wrap">
                    <Mail size={18} />
                    <input
                      autoComplete="email"
                      type="email"
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                      placeholder="可选"
                    />
                  </span>
                </label>
                <label>
                  手机号
                  <span className="input-wrap">
                    <Phone size={18} />
                    <input
                      autoComplete="tel"
                      value={phone}
                      onChange={(event) => setPhone(event.target.value)}
                      placeholder="可选"
                    />
                  </span>
                </label>
              </>
            ) : null}
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
            <button className="primary-button" type="submit" disabled={submitDisabled}>
              <LogIn size={18} />
              {loading ? (mode === 'register' ? '注册中' : '登录中') : (mode === 'register' ? '注册并登录' : '登录')}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}

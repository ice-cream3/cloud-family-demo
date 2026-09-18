import { FormEvent, useState } from 'react';
import { KeyRound, LogIn } from 'lucide-react';
import type { LoginMode } from '../types/api';

type LoginPanelProps = {
  loading: boolean;
  error: string | null;
  onLogin: (mode: LoginMode, username: string, password: string) => Promise<void>;
};

export function LoginPanel({ loading, error, onLogin }: LoginPanelProps) {
  const [mode, setMode] = useState<LoginMode>('partner');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onLogin(mode, username.trim(), password);
  }

  return (
    <section className="login-panel" aria-label="登录">
      <div className="panel-header">
        <div className="icon-box">
          <KeyRound size={22} />
        </div>
        <div>
          <h1>Cloud Family Console</h1>
          <p>前端独立运行，所有请求通过 Gateway 转发到后端服务。</p>
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
          <input
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            placeholder="请输入账号"
          />
        </label>
        <label>
          密码
          <input
            autoComplete="current-password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="请输入密码"
          />
        </label>
        {error ? <div className="error-banner">{error}</div> : null}
        <button className="primary-button" type="submit" disabled={loading || !username.trim() || !password}>
          <LogIn size={18} />
          {loading ? '登录中' : '登录'}
        </button>
      </form>
    </section>
  );
}

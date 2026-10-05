import { FormEvent, useState } from 'react';
import { Check, ChevronLeft, Eye, EyeOff, KeyRound, LockKeyhole, Mail, MessageCircle } from 'lucide-react';
import type { RegisterRequest } from '../types/api';

type LoginPageProps = {
  loading: boolean;
  error: string | null;
  onLogin: (username: string, password: string, remember: boolean) => Promise<void>;
  onRegister: (request: RegisterRequest) => Promise<void>;
};

type AuthMode = 'login' | 'register';
type AuthMethod = 'phone' | 'email';

export function LoginPage({ loading, error, onLogin, onRegister }: LoginPageProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [method, setMethod] = useState<AuthMethod>('phone');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [confirmPasswordVisible, setConfirmPasswordVisible] = useState(false);
  const [code, setCode] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [agreed, setAgreed] = useState(true);
  const [formError, setFormError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const account = normalizeAuthAccount(method, username);
    const validationMessage = validateAuthForm(method, account, password, isRegister ? confirmPassword : undefined);
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }
    setFormError(null);
    if (mode === 'register') {
      const request: RegisterRequest = {
        username: account,
        password,
        displayName: buildDisplayName(account),
        email: method === 'email' ? account : undefined,
        phone: method === 'phone' ? account : undefined,
      };
      await onRegister(request);
      return;
    }
    await onLogin(account, password, rememberMe);
    if (!rememberMe) {
      setPassword('');
    }
  }

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setPassword('');
    setConfirmPassword('');
    setPasswordVisible(false);
    setConfirmPasswordVisible(false);
    setCode('');
    setFormError(null);
  }

  const isRegister = mode === 'register';
  const title = isRegister ? (method === 'phone' ? '手机号注册' : '邮箱注册') : '欢迎回来';
  const subtitle = isRegister
    ? (method === 'phone' ? '注册后即可使用全部工具功能' : '使用邮箱注册，方便快捷')
    : '登录账号，享受更多功能';
  const accountPlaceholder = method === 'phone' ? '请输入手机号' : '请输入邮箱地址';
  const submitDisabled = loading
    || !username.trim()
    || !password
    || !agreed
    || (isRegister && (!code.trim() || password !== confirmPassword));

  return (
    <main className="mobile-login-shell">
      <section className="mobile-auth-card" aria-label={isRegister ? '注册' : '登录'}>
        <LoginStatusBar />
        <header className="mobile-auth-nav">
          <button type="button" aria-label="返回">
            <ChevronLeft size={22} />
          </button>
          <button type="button">帮助</button>
        </header>

        <div className="mobile-auth-logo">
          <KeyRound size={42} />
        </div>

        <div className="mobile-auth-title">
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>

        <div className="mobile-auth-tabs" role="tablist" aria-label="登录方式">
          <button className={method === 'phone' ? 'active' : ''} type="button" onClick={() => {
            setMethod('phone');
            setFormError(null);
          }}>
            手机号{isRegister ? '' : '登录'}
          </button>
          <button className={method === 'email' ? 'active' : ''} type="button" onClick={() => {
            setMethod('email');
            setFormError(null);
          }}>
            邮箱{isRegister ? '' : '登录'}
          </button>
        </div>

        <form className="mobile-auth-form" onSubmit={handleSubmit}>
          <label className="mobile-auth-input">
            {method === 'phone' ? (
              <span className="country-code">+86</span>
            ) : (
              <Mail size={17} />
            )}
            <input
              autoComplete={method === 'phone' ? 'tel' : 'email'}
              inputMode={method === 'phone' ? 'tel' : 'email'}
              value={username}
              onChange={(event) => {
                setUsername(event.target.value);
                setFormError(null);
              }}
              placeholder={accountPlaceholder}
            />
          </label>

          {isRegister ? (
            <label className="mobile-auth-input">
              <LockKeyhole size={17} />
              <input
                autoComplete="one-time-code"
                inputMode="numeric"
                value={code}
                onChange={(event) => {
                  setCode(event.target.value);
                  setFormError(null);
                }}
                placeholder="请输入验证码"
              />
              <button className="code-button" type="button">获取验证码</button>
            </label>
          ) : null}

          <label className="mobile-auth-input">
            <LockKeyhole size={17} />
            <input
              autoComplete={isRegister || !rememberMe ? 'new-password' : 'current-password'}
              type={passwordVisible ? 'text' : 'password'}
              value={password}
              onChange={(event) => {
                setPassword(event.target.value);
                setFormError(null);
              }}
              placeholder={isRegister ? '请设置登录密码（6-20位）' : '请输入密码'}
            />
            <button
              className="mobile-password-toggle"
              type="button"
              onClick={() => setPasswordVisible((visible) => !visible)}
              aria-label={passwordVisible ? '隐藏密码' : '显示密码'}
              title={passwordVisible ? '隐藏密码' : '显示密码'}
            >
              {passwordVisible ? <Eye size={17} /> : <EyeOff size={17} />}
            </button>
          </label>
          <p className="mobile-auth-hint">密码需为 6-20 位，包含数字、大小写字母和特殊字符。</p>

          {isRegister ? (
            <label className="mobile-auth-input">
              <LockKeyhole size={17} />
              <input
                autoComplete="new-password"
                type={confirmPasswordVisible ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(event) => {
                  setConfirmPassword(event.target.value);
                  setFormError(null);
                }}
                placeholder="请再次输入密码"
              />
              <button
                className="mobile-password-toggle"
                type="button"
                onClick={() => setConfirmPasswordVisible((visible) => !visible)}
                aria-label={confirmPasswordVisible ? '隐藏密码' : '显示密码'}
                title={confirmPasswordVisible ? '隐藏密码' : '显示密码'}
              >
                {confirmPasswordVisible ? <Eye size={17} /> : <EyeOff size={17} />}
              </button>
            </label>
          ) : null}

          {!isRegister ? (
            <div className="mobile-auth-options">
              <label>
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(event) => setRememberMe(event.target.checked)}
                />
                <span><Check size={12} /></span>
                记住我
              </label>
              <button type="button">忘记密码?</button>
            </div>
          ) : (
            <label className="mobile-auth-agreement">
              <input type="checkbox" checked={agreed} onChange={(event) => setAgreed(event.target.checked)} />
              <span><Check size={12} /></span>
              我已阅读并同意 <a>《用户协议》</a> 和 <a>《隐私政策》</a>
            </label>
          )}

          {isRegister && password && confirmPassword && password !== confirmPassword ? (
            <div className="mobile-auth-error">两次输入的密码不一致</div>
          ) : null}
          {formError ? <div className="mobile-auth-error">{formError}</div> : null}
          {error ? <div className="mobile-auth-error">{error}</div> : null}

          <button className="mobile-auth-submit" type="submit" disabled={submitDisabled}>
            {loading ? (isRegister ? '注册中' : '登录中') : (isRegister ? '注册' : '登录')}
          </button>
        </form>

        {!isRegister ? (
          <>
            <div className="mobile-auth-divider"><span>或使用其他方式登录</span></div>
            <div className="social-login-row">
              <button className="wechat" type="button">
                <span><MessageCircle size={22} /></span>
                <em>微信登录</em>
              </button>
              <button className="qq" type="button">
                <span>Q</span>
                <em>QQ登录</em>
              </button>
              <button className="apple" type="button">
                <span>Apple</span>
                <em>Apple 登录</em>
              </button>
            </div>
          </>
        ) : null}

        <footer className="mobile-auth-footer">
          {isRegister ? '已有账号？' : '还没有账号？'}
          <button type="button" onClick={() => switchMode(isRegister ? 'login' : 'register')}>
            {isRegister ? '去登录' : '去注册'}
          </button>
        </footer>
      </section>
    </main>
  );
}

function LoginStatusBar() {
  return (
    <div className="mobile-login-status" aria-hidden="true">
      <span>9:41</span>
      <span>▮▮▮ ︎⌁ ▰</span>
    </div>
  );
}

function buildDisplayName(account: string) {
  if (!account) {
    return '新用户';
  }
  return account.includes('@') ? account.split('@')[0] : `用户${account.slice(-4)}`;
}

function normalizeAuthAccount(method: AuthMethod, value: string) {
  const trimmed = value.trim();
  if (method === 'email') {
    return trimmed;
  }
  return trimmed
    .replace(/[\s-]/g, '')
    .replace(/^\+86/, '')
    .replace(/^86(?=1[3-9]\d{9}$)/, '');
}

function validateAuthForm(method: AuthMethod, account: string, password: string, confirmPassword?: string) {
  if (method === 'phone' && !isValidPhone(account)) {
    return '请输入有效的手机号。';
  }
  if (method === 'email' && !isValidEmail(account)) {
    return '请输入有效的邮箱地址。';
  }
  if (!password) {
    return '请输入密码。';
  }
  if (confirmPassword !== undefined && !isStrongPassword(password)) {
    return '密码需为 6-20 位，并同时包含数字、小写字母、大写字母和特殊字符。';
  }
  if (confirmPassword !== undefined && password !== confirmPassword) {
    return '两次输入的密码不一致。';
  }
  return null;
}

function isValidPhone(value: string) {
  return /^\+?\d{6,20}$/.test(value);
}

function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value);
}

function isStrongPassword(value: string) {
  return /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{6,20}$/.test(value);
}

import { FocusEvent, FormEvent, useEffect, useState } from 'react';
import type { DragEvent, PointerEvent } from 'react';
import { Check, ChevronLeft, Eye, EyeOff, KeyRound, LockKeyhole, Mail, MessageCircle } from 'lucide-react';
import type { AntiAbuseChallenge, RegisterRequest } from '../types/api';
import { COUNTRY_CALLING_CODE_OPTIONS } from '../constants/countryCallingCodes';
import { createLoginChallenge } from '../services/partnerService';

type LoginPageProps = {
  loading: boolean;
  error: string | null;
  onLogin: (
    username: string,
    password: string,
    remember: boolean,
    antiAbuseChallengeId?: string,
    antiAbuseAnswer?: string,
  ) => Promise<void>;
  onRegister: (request: RegisterRequest) => Promise<void>;
};

type AuthMode = 'login' | 'register';
type AuthMethod = 'phone' | 'email';

export function LoginPage({ loading, error, onLogin, onRegister }: LoginPageProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [method, setMethod] = useState<AuthMethod>('phone');
  const [username, setUsername] = useState('');
  const [phoneCountryCode, setPhoneCountryCode] = useState('+86');
  const [countrySearch, setCountrySearch] = useState('');
  const [countryPickerOpen, setCountryPickerOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [confirmPasswordVisible, setConfirmPasswordVisible] = useState(false);
  const [code, setCode] = useState('');
  const [antiAbuseChallenge, setAntiAbuseChallenge] = useState<AntiAbuseChallenge | null>(null);
  const [antiAbuseAnswer, setAntiAbuseAnswer] = useState('');
  const [antiAbuseLoading, setAntiAbuseLoading] = useState(false);
  const [draggedPuzzleValue, setDraggedPuzzleValue] = useState<string | null>(null);
  const [rememberMe, setRememberMe] = useState(true);
  const [agreed, setAgreed] = useState(true);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    void reloadLoginChallenge(mode === 'register' ? 'api_register' : 'api_login');
  }, [mode]);

  useEffect(() => {
    if (error) {
      void reloadLoginChallenge(mode === 'register' ? 'api_register' : 'api_login');
    }
  }, [error, mode]);

  async function reloadLoginChallenge(purpose: 'api_login' | 'api_register' = mode === 'register' ? 'api_register' : 'api_login') {
    setAntiAbuseLoading(true);
    try {
      const challenge = await createLoginChallenge(purpose);
      setAntiAbuseChallenge(challenge);
      setAntiAbuseAnswer(buildInitialAntiAbuseAnswer(challenge));
      setDraggedPuzzleValue(null);
    } catch (err) {
      setFormError(readAuthFormError(err));
    } finally {
      setAntiAbuseLoading(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const account = normalizeAuthAccount(method, username);
    const normalizedCountryCode = normalizePhoneCountryCode(phoneCountryCode);
    const validationMessage = validateAuthForm(method, account, password, isRegister ? confirmPassword : undefined, normalizedCountryCode);
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }
    setFormError(null);
    if (!antiAbuseChallenge?.challengeId || selectedPatternValues.length !== requiredPatternSelections) {
      setFormError('请先完成人机验证。');
      return;
    }
    if (mode === 'register') {
      const request: RegisterRequest = {
        username: account,
        password,
        displayName: buildDisplayName(account),
        email: method === 'email' ? account : undefined,
        phoneCountryCode: method === 'phone' ? normalizedCountryCode : undefined,
        phone: method === 'phone' ? account : undefined,
        antiAbuseChallengeId: antiAbuseChallenge.challengeId,
        antiAbuseAnswer: antiAbuseAnswer.trim(),
      };
      try {
        await onRegister(request);
      } catch {
        void reloadLoginChallenge('api_register');
      }
      return;
    }
    try {
      await onLogin(
        method === 'phone' ? `${normalizedCountryCode}${account}` : account,
        password,
        rememberMe,
        antiAbuseChallenge.challengeId,
        antiAbuseAnswer.trim(),
      );
    } catch {
      void reloadLoginChallenge();
    }
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
    setAntiAbuseAnswer('');
    setAntiAbuseChallenge(null);
    setDraggedPuzzleValue(null);
    setFormError(null);
  }

  function closeCountryPickerOnBlur(event: FocusEvent<HTMLElement>) {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
      setCountryPickerOpen(false);
    }
  }

  const isRegister = mode === 'register';
  const title = isRegister ? (method === 'phone' ? '手机号注册' : '邮箱注册') : '欢迎回来';
  const subtitle = isRegister
    ? (method === 'phone' ? '注册后即可使用全部工具功能' : '使用邮箱注册，方便快捷')
    : '登录账号，享受更多功能';
  const accountPlaceholder = method === 'phone' ? '请输入手机号' : '请输入邮箱地址';
  const countryCodeOptions = COUNTRY_CALLING_CODE_OPTIONS
    .filter((option) => {
      const keyword = countrySearch.trim().toLowerCase();
      if (!keyword) {
        return true;
      }
      return option.code.includes(keyword) || option.label.toLowerCase().includes(keyword);
    })
    .slice(0, 40);
  const selectedPatternValues = antiAbuseAnswer ? antiAbuseAnswer.split(',').filter(Boolean) : [];
  const requiredPatternSelections = antiAbuseChallenge?.requiredSelectionCount || 3;
  const isPuzzleChallenge = antiAbuseChallenge?.scheme === 'image_reassemble';
  const submitDisabled = loading
    || !username.trim()
    || !password
    || !agreed
    || (isRegister && (!code.trim() || password !== confirmPassword))
    || !antiAbuseChallenge
    || selectedPatternValues.length !== requiredPatternSelections;

  function selectPatternOption(value: string) {
    if (isPuzzleChallenge) {
      return;
    }
    const nextValues = selectedPatternValues.includes(value)
      ? selectedPatternValues.filter((selectedValue) => selectedValue !== value)
      : [...selectedPatternValues, value].slice(0, requiredPatternSelections);
    setAntiAbuseAnswer(nextValues.join(','));
    setFormError(null);
  }

  function movePuzzleTile(fromValue: string, toValue: string) {
    if (!antiAbuseChallenge || !isPuzzleChallenge || fromValue === toValue) {
      return;
    }
    const orderedValues = selectedPatternValues.length
      ? selectedPatternValues
      : (antiAbuseChallenge.options || []).map((option) => option.value);
    const fromIndex = orderedValues.indexOf(fromValue);
    const toIndex = orderedValues.indexOf(toValue);
    if (fromIndex < 0 || toIndex < 0) {
      return;
    }
    const nextValues = [...orderedValues];
    [nextValues[fromIndex], nextValues[toIndex]] = [nextValues[toIndex], nextValues[fromIndex]];
    setAntiAbuseAnswer(nextValues.join(','));
    setFormError(null);
  }

  function handlePuzzleDragStart(event: DragEvent<HTMLButtonElement>, value: string) {
    setDraggedPuzzleValue(value);
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', value);
  }

  function handlePuzzleDrop(event: DragEvent<HTMLButtonElement>, toValue: string) {
    event.preventDefault();
    const fromValue = event.dataTransfer.getData('text/plain') || draggedPuzzleValue;
    if (fromValue) {
      movePuzzleTile(fromValue, toValue);
    }
    setDraggedPuzzleValue(null);
  }

  function handlePuzzlePointerDown(event: PointerEvent<HTMLButtonElement>, value: string) {
    if (!isPuzzleChallenge || event.pointerType === 'mouse') {
      return;
    }
    setDraggedPuzzleValue(value);
  }

  function handlePuzzlePointerUp(event: PointerEvent<HTMLButtonElement>) {
    if (!isPuzzleChallenge || event.pointerType === 'mouse' || !draggedPuzzleValue) {
      return;
    }
    const target = document
      .elementFromPoint(event.clientX, event.clientY)
      ?.closest<HTMLButtonElement>('[data-puzzle-value]');
    const toValue = target?.dataset.puzzleValue;
    if (toValue) {
      movePuzzleTile(draggedPuzzleValue, toValue);
    }
    setDraggedPuzzleValue(null);
  }

  const puzzleOptions = isPuzzleChallenge
    ? selectedPatternValues
        .map((value) => (antiAbuseChallenge?.options || []).find((option) => option.value === value))
        .filter((option): option is NonNullable<typeof option> => Boolean(option))
    : (antiAbuseChallenge?.options || []);

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
          <label className={`mobile-auth-input ${method === 'phone' ? 'phone-auth-input' : ''}`}>
            {method === 'phone' ? (
              <span className="country-code-control" onBlur={closeCountryPickerOnBlur}>
                <input
                  className="country-code-input"
                  autoComplete="tel-country-code"
                  inputMode="tel"
                  value={phoneCountryCode}
                  onFocus={() => setCountryPickerOpen(true)}
                  onChange={(event) => {
                    setPhoneCountryCode(event.target.value);
                    setCountrySearch(event.target.value);
                    setCountryPickerOpen(true);
                    setFormError(null);
                  }}
                  aria-label="国际区号"
                />
                <button
                  type="button"
                  aria-label="选择国际区号"
                  onClick={() => setCountryPickerOpen((open) => !open)}
                />
                {countryPickerOpen ? (
                  <div className="country-code-panel">
                    <input
                      value={countrySearch}
                      onChange={(event) => setCountrySearch(event.target.value)}
                      placeholder="搜索国家/地区或区号"
                    />
                    <div className="country-code-list">
                      {countryCodeOptions.map((option) => (
                        <button
                          key={`${option.code}-${option.label}`}
                          type="button"
                          onMouseDown={(event) => {
                            event.preventDefault();
                            setPhoneCountryCode(option.code);
                            setCountrySearch('');
                            setCountryPickerOpen(false);
                            setFormError(null);
                          }}
                          onClick={() => setCountryPickerOpen(false)}
                        >
                          <strong>{option.code}</strong>
                          <span>{option.label}</span>
                        </button>
                      ))}
                    </div>
                  </div>
                ) : null}
              </span>
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
              onFocus={() => setCountryPickerOpen(false)}
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

          <div className="mobile-auth-pattern">
            <div className="auth-pattern-head">
              <span>
                {antiAbuseChallenge?.questionImage ? (
                  <img
                    className={`auth-pattern-question-image ${antiAbuseChallenge.scheme === 'image_reassemble' ? 'puzzle' : ''}`}
                    src={antiAbuseChallenge.questionImage}
                    alt={antiAbuseChallenge.scheme === 'image_reassemble' ? '请按目标图片重组' : '请选择目标图形'}
                  />
                ) : (antiAbuseChallenge?.question || '加载中')}
              </span>
              <button className="code-button" type="button" onClick={() => reloadLoginChallenge()} disabled={antiAbuseLoading}>
                {antiAbuseLoading ? '刷新中' : '换一组'}
              </button>
            </div>
            <div
              className={`auth-pattern-options ${isPuzzleChallenge ? 'puzzle-board' : ''}`}
              role="group"
              aria-label={isPuzzleChallenge ? '拖拽图片碎片完成重组' : '人机验证图案'}
            >
              {puzzleOptions.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  className={`${selectedPatternValues.includes(option.value) && !isPuzzleChallenge ? 'active' : ''} ${draggedPuzzleValue === option.value ? 'dragging' : ''}`}
                  onClick={() => selectPatternOption(option.value)}
                  disabled={antiAbuseLoading}
                  draggable={isPuzzleChallenge && !antiAbuseLoading}
                  onDragStart={(event) => handlePuzzleDragStart(event, option.value)}
                  onDragEnd={() => setDraggedPuzzleValue(null)}
                  onDragOver={(event) => {
                    if (isPuzzleChallenge) {
                      event.preventDefault();
                      event.dataTransfer.dropEffect = 'move';
                    }
                  }}
                  onDrop={(event) => handlePuzzleDrop(event, option.value)}
                  onPointerDown={(event) => handlePuzzlePointerDown(event, option.value)}
                  onPointerUp={handlePuzzlePointerUp}
                  onPointerCancel={() => setDraggedPuzzleValue(null)}
                  data-puzzle-value={isPuzzleChallenge ? option.value : undefined}
                  role={isPuzzleChallenge ? undefined : 'checkbox'}
                  aria-checked={isPuzzleChallenge ? undefined : selectedPatternValues.includes(option.value)}
                  aria-label={isPuzzleChallenge ? '可拖拽图片碎片' : '验证选项'}
                >
                  {option.image ? (
                    <img className="auth-puzzle-tile" src={option.image} alt="图片碎片" />
                  ) : (
                    <span
                      className={`auth-pattern-shape ${option.fillMode === 'outline' ? 'outline' : 'solid'}`}
                      style={{
                        backgroundColor: option.color,
                        clipPath: option.shape ? `polygon(${option.shape.clipPath})` : undefined,
                      }}
                    />
                  )}
                </button>
              ))}
            </div>
          </div>
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
    .replace(/[\s\-()]/g, '')
    .replace(/^\+/, '');
}

function validateAuthForm(method: AuthMethod, account: string, password: string, confirmPassword?: string, phoneCountryCode = '+86') {
  if (method === 'phone' && !isValidPhone(phoneCountryCode, account)) {
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

function readAuthFormError(error: unknown) {
  if (error instanceof Error) {
    if (error.message === 'anti abuse verification is required') {
      return '请先完成人机验证。';
    }
    if (error.message === 'anti abuse verification expired') {
      return '人机验证已过期，请重新验证。';
    }
    if (error.message === 'anti abuse verification is invalid') {
      return '人机验证答案不正确。';
    }
    return error.message;
  }
  return '操作失败。';
}

function buildInitialAntiAbuseAnswer(challenge: AntiAbuseChallenge) {
  if (challenge.scheme !== 'image_reassemble') {
    return '';
  }
  return (challenge.options || []).map((option) => option.value).join(',');
}

function isValidPhone(phoneCountryCode: string, phone: string) {
  const countryCode = normalizePhoneCountryCode(phoneCountryCode);
  const localPhone = phone.replace(/[\s\-()]/g, '').replace(/^\+/, '');
  if (!/^\+[1-9]\d{0,3}$/.test(countryCode) || !/^\d{4,14}$/.test(localPhone)) {
    return false;
  }
  return /^[1-9]\d{7,14}$/.test(`${countryCode.slice(1)}${localPhone}`);
}

function normalizePhoneCountryCode(value: string) {
  const normalized = value.trim().replace(/[\s\-()]/g, '');
  if (!normalized) {
    return '+86';
  }
  return normalized.startsWith('+') ? normalized : `+${normalized}`;
}

function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value);
}

function isStrongPassword(value: string) {
  return /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{6,20}$/.test(value);
}

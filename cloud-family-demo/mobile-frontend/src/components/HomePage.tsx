import {
  Bell,
  ChevronRight,
  Home,
  LogOut,
  RefreshCw,
  ShieldCheck,
  Smartphone,
  Sparkles,
  TicketPercent,
  UserRound,
  WalletCards,
} from 'lucide-react';
import type { PartnerHealth, UserProfile } from '../types/api';
import type { Session } from '../services/tokenStore';

type HomePageProps = {
  session: Session;
  profile: UserProfile | null;
  health: PartnerHealth | null;
  loading: boolean;
  error: string | null;
  onReload: () => void;
  onLogout: () => void;
};

const quickActions = [
  { icon: WalletCards, title: '账户权益', desc: '等级、积分与权益包' },
  { icon: TicketPercent, title: '优惠中心', desc: '活动与可用券包' },
  { icon: ShieldCheck, title: '安全设置', desc: '登录与认证信息' },
];

const timelineItems = [
  { title: '账户资料同步', time: '今天', status: '已完成' },
  { title: 'Gateway 身份校验', time: '登录后', status: '生效中' },
  { title: '权益服务接入', time: '待配置', status: '预留' },
];

export function HomePage({ session, profile, health, loading, error, onReload, onLogout }: HomePageProps) {
  const displayName = profile?.displayName || session.username || 'Partner 用户';
  const roleText = (profile?.roles?.length ? profile.roles : session.roles).join(', ') || 'PARTNER';
  const serviceStatus = health?.status || (profile ? 'UP' : 'UNKNOWN');

  return (
    <main className="partner-shell">
      <aside className="partner-rail" aria-label="主导航">
        <div className="partner-brand">
          <div className="partner-brand-mark">CF</div>
          <div>
            <strong>Cloud Family</strong>
            <span>Mobile Portal</span>
          </div>
        </div>

        <nav className="partner-nav">
          <button className="active" type="button">
            <Home size={18} />
            首页
          </button>
          <button type="button">
            <WalletCards size={18} />
            权益
          </button>
          <button type="button">
            <Bell size={18} />
            消息
          </button>
          <button type="button">
            <UserRound size={18} />
            我的
          </button>
        </nav>
      </aside>

      <section className="partner-page">
        <header className="partner-topbar">
          <div>
            <span>今天</span>
            <h1>{displayName}</h1>
          </div>
          <div className="partner-topbar-actions">
            <button className="icon-button" type="button" onClick={onReload} disabled={loading} title="刷新">
              <RefreshCw size={18} />
            </button>
            <button className="ghost-button" type="button" onClick={onLogout}>
              <LogOut size={18} />
              退出
            </button>
          </div>
        </header>

        {error ? <div className="error-banner">{error}</div> : null}

        <section className="partner-hero">
          <div className="partner-hero-copy">
            <p>Partner 首页</p>
            <h2>服务在线，账户已同步</h2>
            <span>查看权益、消息和账号状态，继续完成用户端业务流程。</span>
            <div className="partner-hero-actions">
              <button className="primary-button" type="button">
                <Sparkles size={18} />
                查看权益
              </button>
              <button className="ghost-button" type="button">
                <Smartphone size={18} />
                H5 预览
              </button>
            </div>
          </div>
          <img
            src="https://images.unsplash.com/photo-1551650975-87deedd944c3?auto=format&fit=crop&w=900&q=80"
            alt="Partner portal responsive preview"
          />
        </section>

        <section className="partner-stat-grid" aria-label="账号概览">
          <article>
            <span>用户类型</span>
            <strong>{session.userType || 'API'}</strong>
            <p>认证服务</p>
          </article>
          <article>
            <span>角色</span>
            <strong>{roleText}</strong>
            <p>访问范围</p>
          </article>
          <article>
            <span>服务状态</span>
            <strong>{serviceStatus}</strong>
            <p>{health?.service || 'partner-service'}</p>
          </article>
        </section>

        <section className="partner-content-grid">
          <article className="partner-panel">
            <div className="partner-section-heading">
              <div>
                <h2>快捷入口</h2>
                <p>常用操作</p>
              </div>
            </div>
            <div className="quick-action-list">
              {quickActions.map((item) => {
                const Icon = item.icon;
                return (
                  <button key={item.title} type="button">
                    <span className="quick-action-icon">
                      <Icon size={20} />
                    </span>
                    <span>
                      <strong>{item.title}</strong>
                      <small>{item.desc}</small>
                    </span>
                    <ChevronRight size={18} />
                  </button>
                );
              })}
            </div>
          </article>

          <article className="partner-panel">
            <div className="partner-section-heading">
              <div>
                <h2>最近动态</h2>
                <p>账号状态</p>
              </div>
            </div>
            <div className="partner-timeline">
              {timelineItems.map((item) => (
                <div key={item.title}>
                  <span />
                  <div>
                    <strong>{item.title}</strong>
                    <p>{item.time}</p>
                  </div>
                  <em>{item.status}</em>
                </div>
              ))}
            </div>
          </article>
        </section>

        <nav className="partner-bottom-tabs" aria-label="移动端底部导航">
          <button className="active" type="button">
            <Home size={20} />
            <span>首页</span>
          </button>
          <button type="button">
            <WalletCards size={20} />
            <span>权益</span>
          </button>
          <button type="button">
            <Bell size={20} />
            <span>消息</span>
          </button>
          <button type="button">
            <UserRound size={20} />
            <span>我的</span>
          </button>
        </nav>
      </section>
    </main>
  );
}

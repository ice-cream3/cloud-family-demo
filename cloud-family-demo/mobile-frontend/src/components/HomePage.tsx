import { useEffect, useState } from 'react';
import type { FocusEvent, FormEvent, MouseEvent } from 'react';
import {
  Archive,
  Bell,
  Calculator,
  CalendarDays,
  CheckCircle2,
  Camera,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Crown,
  FileImage,
  FileText,
  Gift,
  Grid2X2,
  Home,
  Image,
  Images,
  LogOut,
  MessageCircle,
  PackageCheck,
  Palette,
  RefreshCw,
  ScanLine,
  Search,
  Settings,
  ShieldCheck,
  Sparkles,
  Star,
  UserCheck,
  UserRound,
  Wand2,
  Wrench,
  X,
  Zap,
} from 'lucide-react';
import type { AppDocument, FavoriteItem, Membership, PartnerHealth, ProfileDynamic, TripCredit, TripOwner, TripPublishRequest, TripSlot, UserHistoryItem, UserProfile } from '../types/api';
import type { AccountSecurity } from '../types/api';
import {
  changeAccountPassword,
  createAccountSecurityChallenge,
  loadAccountSecurity,
  loadAppDocument,
  loadAppVersion,
  loadFavorites,
  loadHistory,
  loadMembership,
  loadNotificationSettings,
  loadBookableTripSlots,
  loadMyTrips,
  loadMyTripReservations,
  loadTripCredit,
  loadTripOwners,
  markTripNoShow,
  publishTrip,
  refreshProfileDynamic,
  applyTrip,
  reviewTrip,
  sendEmailVerificationCode,
  sendPhoneVerificationCode,
  updateAccountSecurity,
  updateNotificationSettings,
} from '../services/partnerService';
import { ApiError } from '../services/apiClient';
import type { Session } from '../services/tokenStore';
import { COUNTRY_CALLING_CODE_OPTIONS } from '../constants/countryCallingCodes';

type HomePageProps = {
  session: Session;
  profile: UserProfile | null;
  health: PartnerHealth | null;
  loading: boolean;
  error: string | null;
  onReload: () => void;
  onLogout: () => void;
};

type TabKey = 'home' | 'tools' | 'activity' | 'message' | 'profile';
type ScreenKey = TabKey | 'compress' | 'history' | 'settings' | 'phoneVerification' | 'membership' | 'favorites' | 'trips' | 'myTrips';
type SettingDetail = 'account' | 'notifications' | 'about' | 'privacy' | 'agreement' | 'version';
type SearchableTool = {
  icon: typeof Image;
  label: string;
  tone: string;
  screen?: 'compress';
};

const TRIP_CONFLICT_CODE = 200002;
const defaultTripCredit: TripCredit = {
  creditScore: 100,
  creditLevel: 'S',
  minReservableScore: 60,
  noShowPenalty: 20,
  reservable: true,
};

const mainTools = [
  { icon: Image, label: '图片处理', tone: 'green' },
  { icon: FileText, label: '文件转换', tone: 'orange' },
  { icon: ScanLine, label: '扫描识别', tone: 'purple' },
  { icon: PackageCheck, label: '生活工具', tone: 'pink' },
];

const imageTools = [
  { icon: Image, label: '图片压缩', tone: 'green', screen: 'compress' as const },
  { icon: Wand2, label: '图片裁剪', tone: 'blue' },
  { icon: Archive, label: '图片格式转换', tone: 'orange' },
  { icon: Palette, label: '图片加水印', tone: 'purple' },
  { icon: FileImage, label: '图片转PDF', tone: 'red' },
  { icon: Images, label: '拼图', tone: 'blue' },
  { icon: Camera, label: '图片尺寸调整', tone: 'sky' },
  { icon: Sparkles, label: '图片修复', tone: 'green' },
];

const fileTools = [
  { icon: FileText, label: 'PDF转图片', tone: 'orange' },
  { icon: FileImage, label: '图片转PDF', tone: 'red' },
  { icon: FileText, label: 'Word转PDF', tone: 'blue' },
  { icon: FileText, label: 'PDF转Word', tone: 'indigo' },
  { icon: FileText, label: 'Excel转PDF', tone: 'green' },
  { icon: FileText, label: 'PPT转PDF', tone: 'orange' },
  { icon: FileText, label: 'TXT转PDF', tone: 'blue' },
  { icon: Archive, label: 'PDF合并', tone: 'red' },
];

const moreTools = [
  { icon: Calculator, label: '计算器', tone: 'blue' },
  { icon: Zap, label: '汇率换算', tone: 'green' },
  { icon: Wrench, label: '单位换算', tone: 'indigo' },
  { icon: FileText, label: '日期计算', tone: 'orange' },
  { icon: Gift, label: '随机数', tone: 'orange' },
  { icon: ShieldCheck, label: '密码生成器', tone: 'blue' },
  { icon: Palette, label: '颜色取值', tone: 'sky' },
  { icon: Camera, label: '屏幕录制', tone: 'red' },
  { icon: Bell, label: '音乐转换', tone: 'purple' },
  { icon: FileText, label: '视频转换', tone: 'blue' },
  { icon: MessageCircle, label: '网速测试', tone: 'green' },
  { icon: Wrench, label: '尺子测量', tone: 'purple' },
];

const activityCards = [
  { tag: '限时优惠', title: '年度会员 立减50%', desc: '高级工具无限用', action: '立即抢购', tone: 'gold' },
  { tag: '邀请好友', title: '邀请好友得7天会员', desc: '多邀多得，最高送90天', action: '去邀请', tone: 'warm' },
  { tag: '新功能上线', title: 'AI 图片修复', desc: '智能修复模糊图片', action: '立即体验', tone: 'purple' },
  { tag: '学生专享', title: '学生认证享5折', desc: '1分钟完成认证', action: '去认证', tone: 'green' },
];

const messages = [
  { icon: ShieldCheck, title: '系统通知', desc: '欢迎使用我们的工具 App!', time: '1小时前', tone: 'blue' },
  { icon: Star, title: '会员活动', desc: '年度会员限时5折，快来抢购!', time: '3小时前', tone: 'gold' },
  { icon: PackageCheck, title: '任务完成', desc: '图片压缩已完成，共节省 2.4MB', time: '5小时前', tone: 'green' },
  { icon: FileImage, title: '功能更新', desc: '新增 AI 图片修复功能', time: '1天前', tone: 'purple' },
  { icon: Gift, title: '订单通知', desc: '你的会员订单已支付成功', time: '2天前', tone: 'red' },
  { icon: MessageCircle, title: '客服消息', desc: '您好，有什么可以帮助您?', time: '2天前', tone: 'blue' },
];

const tripStartMinute = 0;
const tripEndMinute = 23 * 60 + 50;
const tripDurationLimitMinutes = 4 * 60;

export function HomePage({ session, profile, health, loading, error, onReload, onLogout }: HomePageProps) {
  const [screen, setScreen] = useState<ScreenKey>('home');
  const [showMoreTools, setShowMoreTools] = useState(false);
  const [dynamic, setDynamic] = useState<ProfileDynamic | null>(null);
  const [membership, setMembership] = useState<Membership | null>(null);
  const [accountSecurity, setAccountSecurity] = useState<AccountSecurity | null>(null);
  const [tripCredit, setTripCredit] = useState<TripCredit>(defaultTripCredit);
  const [dynamicLoading, setDynamicLoading] = useState(false);
  const displayName = profile?.displayName || session.username || '用户名';
  const serviceStatus = dynamic?.serviceStatus || health?.status || (profile ? 'UP' : 'UNKNOWN');
  const activeTab = ['history', 'settings', 'phoneVerification', 'membership', 'favorites', 'trips', 'myTrips'].includes(screen) ? 'profile' : screen === 'compress' ? 'tools' : screen as TabKey;

  useEffect(() => {
    void loadDynamic();
    void reloadAccountSecurity();
    void reloadMembership();
    void reloadTripCredit();
  }, []);

  async function loadDynamic() {
    setDynamicLoading(true);
    try {
      setDynamic(await refreshProfileDynamic());
    } finally {
      setDynamicLoading(false);
    }
  }

  async function refreshDynamic() {
    onReload();
    await Promise.all([loadDynamic(), reloadAccountSecurity(), reloadMembership(), reloadTripCredit()]);
  }

  async function reloadAccountSecurity() {
    try {
      const nextAccount = await loadAccountSecurity();
      setAccountSecurity(nextAccount);
      return nextAccount;
    } catch {
      setAccountSecurity(null);
      return null;
    }
  }

  async function reloadMembership() {
    try {
      setMembership(await loadMembership());
    } catch {
      setMembership(null);
    }
  }

  async function reloadTripCredit() {
    try {
      setTripCredit(await loadTripCredit());
    } catch {
      setTripCredit(defaultTripCredit);
    }
  }

  function setTab(tab: TabKey) {
    setScreen(tab);
    setShowMoreTools(false);
  }

  return (
    <main className="tool-app-shell">
      <section className="tool-phone" aria-label="移动端工具 App">
        <StatusBar />
        {error ? <div className="tool-error">{error}</div> : null}

        {screen === 'home' ? (
          <HomeScreen
            displayName={displayName}
            tripCredit={tripCredit}
            onOpenTools={() => setTab('tools')}
            onOpenCompress={() => setScreen('compress')}
            onOpenTrips={() => setScreen('trips')}
          />
        ) : null}
        {screen === 'tools' ? (
          <ToolsScreen onOpenCompress={() => setScreen('compress')} onOpenMore={() => setShowMoreTools(true)} />
        ) : null}
        {screen === 'compress' ? (
          <CompressScreen onBack={() => setScreen('tools')} />
        ) : null}
        {screen === 'activity' ? <ActivityScreen /> : null}
        {screen === 'message' ? <MessageScreen /> : null}
        {screen === 'profile' ? (
          <ProfileScreen
            displayName={displayName}
            serviceStatus={serviceStatus}
            loading={loading || dynamicLoading}
            dynamic={dynamic}
            membership={membership}
            tripCredit={tripCredit}
            onReload={refreshDynamic}
            onLogout={onLogout}
            onOpenMembership={() => setScreen('membership')}
            onOpenHistory={() => setScreen('history')}
            onOpenFavorites={() => setScreen('favorites')}
            onOpenTrips={() => setScreen('myTrips')}
            onOpenSettings={() => setScreen('settings')}
          />
        ) : null}
        {screen === 'history' ? <HistoryScreen onBack={() => setScreen('profile')} /> : null}
        {screen === 'membership' ? <MembershipScreen onBack={() => setScreen('profile')} /> : null}
        {screen === 'favorites' ? <FavoritesScreen onBack={() => setScreen('profile')} /> : null}
        {screen === 'trips' ? (
          <TripScheduleScreen
            mode="booking"
            displayName={displayName}
            username={session.username || ''}
            accountSecurity={accountSecurity}
            tripCredit={tripCredit}
            onRefreshAccountSecurity={reloadAccountSecurity}
            onRefreshTripCredit={reloadTripCredit}
            onOpenPhoneVerification={() => setScreen('phoneVerification')}
            onBack={() => setScreen('home')}
            onOpenMyTrips={() => setScreen('myTrips')}
          />
        ) : null}
        {screen === 'myTrips' ? (
          <TripScheduleScreen
            mode="mine"
            displayName={displayName}
            username={session.username || ''}
            accountSecurity={accountSecurity}
            tripCredit={tripCredit}
            onRefreshAccountSecurity={reloadAccountSecurity}
            onRefreshTripCredit={reloadTripCredit}
            onOpenPhoneVerification={() => setScreen('phoneVerification')}
            onBack={() => setScreen('profile')}
            onOpenBooking={() => setScreen('trips')}
          />
        ) : null}
        {screen === 'settings' ? (
          <SettingsScreen displayName={displayName} onAccountUpdated={setAccountSecurity} onBack={() => setScreen('profile')} onLogout={onLogout} />
        ) : null}
        {screen === 'phoneVerification' ? (
          <SettingsScreen displayName={displayName} initialDetail="account" focusPhone onAccountUpdated={setAccountSecurity} onBack={() => setScreen('myTrips')} onLogout={onLogout} />
        ) : null}

        <BottomTabs active={activeTab} onChange={setTab} hasMessageDot />
        {showMoreTools ? <MoreToolsSheet onClose={() => setShowMoreTools(false)} /> : null}
      </section>
    </main>
  );
}

function StatusBar() {
  return (
    <div className="tool-status-bar" aria-hidden="true">
      <span>9:41</span>
      <i />
      <span>▮▮▮ ︎⌁ ▰</span>
    </div>
  );
}

function HomeScreen({
  displayName,
  tripCredit,
  onOpenTools,
  onOpenCompress,
  onOpenTrips,
}: {
  displayName: string;
  tripCredit: TripCredit;
  onOpenTools: () => void;
  onOpenCompress: () => void;
  onOpenTrips: () => void;
}) {
  const [searchText, setSearchText] = useState('');
  const [searchActive, setSearchActive] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const searchResults = searchTools(searchText);

  function openTool(item: SearchableTool) {
    if (item.screen === 'compress') {
      onOpenCompress();
      return;
    }
    showToast(`${item.label}功能建设中`);
  }

  function submitSearch() {
    const firstResult = searchResults[0];
    if (!searchText.trim()) {
      setSearchActive(true);
      return;
    }
    if (firstResult) {
      openTool(firstResult);
      return;
    }
    showToast('暂无匹配工具');
  }

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(null), 1600);
  }

  return (
    <div className="tool-screen">
      <header className="tool-home-head">
        <div className="tool-avatar">
          <Sparkles size={20} />
        </div>
        <div>
          <strong>Hi，早上好!</strong>
          <span>{displayName} 的效率工具箱 ✨</span>
        </div>
        <CreditLevelPill credit={tripCredit} />
        <button type="button" aria-label="快速打开图片压缩" onClick={onOpenCompress}>
          <Zap size={16} />
        </button>
      </header>

      <div className={`tool-search-wrap ${searchActive ? 'active' : ''}`}>
        <form className="tool-search" onSubmit={(event) => {
          event.preventDefault();
          submitSearch();
        }}>
          <input
            value={searchText}
            placeholder="搜索工具，试试“图片压缩”"
            onChange={(event) => {
              setSearchText(event.target.value);
              setSearchActive(true);
            }}
            onFocus={() => setSearchActive(true)}
          />
          <button type="submit" aria-label="搜索工具">
            <Search size={18} />
          </button>
        </form>
        {searchActive ? (
          <div className="tool-search-results">
            {searchResults.length > 0 ? searchResults.slice(0, 5).map((item) => {
              const Icon = item.icon;
              return (
                <button key={item.label} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => openTool(item)}>
                  <span className={`tool-icon ${item.tone}`}>
                    <Icon size={18} />
                  </span>
                  <span>
                    <strong>{item.label}</strong>
                    <small>{toolCategoryLabel(item)}</small>
                  </span>
                  <ChevronRight size={16} />
                </button>
              );
            }) : <div className="tool-search-empty">暂无匹配工具</div>}
          </div>
        ) : null}
      </div>

      <section className="tool-home-banner">
        <div>
          <h1>高效工具<br />让生活更简单</h1>
          <p>多种实用工具，随时随地使用</p>
        </div>
        <button type="button" aria-label="进入工具" onClick={onOpenTools}>
          <ChevronRight size={20} />
        </button>
      </section>

      <div className="tool-shortcut-grid">
        <button type="button" onClick={onOpenTrips}>
          <span className="tool-icon blue">
            <CalendarDays size={24} />
          </span>
          <em>行程预约</em>
        </button>
        {mainTools.map((item) => {
          const Icon = item.icon;
          return (
            <button key={item.label} type="button" onClick={item.label === '图片处理' ? onOpenTools : undefined}>
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={24} />
              </span>
              <em>{item.label}</em>
            </button>
          );
        })}
      </div>

      <SectionTitle title="最近使用" action="查看全部" />
      <div className="tool-recent-grid">
        {imageTools.slice(0, 4).map((item) => {
          const Icon = item.icon;
          return (
            <button key={item.label} type="button" onClick={item.screen ? onOpenCompress : undefined}>
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={22} />
              </span>
              <em>{item.label}</em>
            </button>
          );
        })}
      </div>

      <SectionTitle title="推荐工具" />
      <button className="tool-recommend" type="button">
        <span className="tool-icon purple">
          <FileImage size={24} />
        </span>
        <span>
          <strong>图片转PDF</strong>
          <small>多张图片一键合并为PDF</small>
        </span>
        <ChevronRight size={18} />
      </button>
      {toast ? <div className="home-toast">{toast}</div> : null}
    </div>
  );
}

function CreditLevelPill({ credit }: { credit: TripCredit }) {
  const [showDescription, setShowDescription] = useState(false);
  const creditIcon = tripCreditIcon(credit.creditLevel);
  const Icon = creditIcon.icon;
  return (
    <>
      <button
        className={`tool-credit-pill ${credit.reservable ? 'available' : 'blocked'} ${creditIcon.tone}`}
        type="button"
        onClick={() => setShowDescription(true)}
        aria-label="查看信用级别说明"
      >
        <Icon size={16} />
        <span>
          <strong>{credit.creditLevel}</strong>
          <em>{credit.creditScore}分</em>
        </span>
      </button>
      {showDescription ? <CreditLevelDescription credit={credit} onClose={() => setShowDescription(false)} /> : null}
    </>
  );
}

function allSearchableTools(): SearchableTool[] {
  return [...imageTools, ...fileTools, ...moreTools];
}

function searchTools(keyword: string) {
  const normalizedKeyword = keyword.trim().toLowerCase();
  const tools = allSearchableTools();
  if (!normalizedKeyword) {
    return tools.slice(0, 5);
  }
  return tools.filter((item) => item.label.toLowerCase().includes(normalizedKeyword));
}

function toolCategoryLabel(item: SearchableTool) {
  if (imageTools.some((tool) => tool.label === item.label)) {
    return '图片处理';
  }
  if (fileTools.some((tool) => tool.label === item.label)) {
    return '文件转换';
  }
  return '生活工具';
}

function ToolsScreen({ onOpenCompress, onOpenMore }: { onOpenCompress: () => void; onOpenMore: () => void }) {
  return (
    <div className="tool-screen">
      <header className="tool-page-head">
        <h1>工具中心</h1>
        <Search size={20} />
      </header>
      <SegmentedTabs items={['全部', '图片处理', '文件转换', '扫描识别', '生活工具']} />
      <ToolSection title="图片处理" tools={imageTools} onOpenCompress={onOpenCompress} />
      <ToolSection title="文件转换" tools={fileTools} />
      <ToolSection title="扫描识别" tools={imageTools.slice(2, 6)} />
      <button className="more-tools-entry" type="button" onClick={onOpenMore}>
        <span>
          <strong>更多工具</strong>
          <small>计算器、汇率、日期、密码生成等实用工具</small>
        </span>
        <ChevronRight size={18} />
      </button>
    </div>
  );
}

function ToolSection({ title, tools, onOpenCompress }: { title: string; tools: typeof imageTools; onOpenCompress?: () => void }) {
  return (
    <section className="tool-section">
      <h2>{title}</h2>
      <div className="tool-catalog-grid">
        {tools.map((item) => {
          const Icon = item.icon;
          return (
            <button key={item.label} type="button" onClick={item.screen ? onOpenCompress : undefined}>
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={23} />
              </span>
              <em>{item.label}</em>
            </button>
          );
        })}
      </div>
    </section>
  );
}

function CompressScreen({ onBack }: { onBack: () => void }) {
  return (
    <div className="tool-screen compress-screen">
      <HeaderWithBack title="图片压缩" onBack={onBack} right="使用帮助" />
      <section className="upload-card">
        <span className="upload-art">
          <Image size={48} />
          <i>+</i>
        </span>
        <strong>选择图片</strong>
        <p>支持 JPG、PNG、HEIC 等格式</p>
        <button className="app-primary" type="button">选择图片</button>
      </section>
      <section className="compress-settings">
        <h2>压缩设置</h2>
        <div className="mode-pills">
          <button className="active" type="button">智能压缩<br /><small>推荐</small></button>
          <button type="button">清晰优先</button>
          <button type="button">体积优先</button>
          <button type="button">自定义</button>
        </div>
        <label>
          <span>压缩质量</span>
          <strong>80%</strong>
        </label>
        <input className="quality-slider" type="range" min="0" max="100" defaultValue="80" />
        <div className="quality-labels">
          <span>体积更小</span>
          <span>均衡</span>
          <span>质量更高</span>
        </div>
      </section>
      <button className="app-primary fixed-action" type="button">开始压缩</button>
    </div>
  );
}

function ActivityScreen() {
  return (
    <div className="tool-screen">
      <header className="tool-page-head">
        <h1>活动中心</h1>
        <Bell size={20} />
      </header>
      <section className="vip-banner">
        <div>
          <h2>开通会员<br />解锁更多高级工具</h2>
          <p>更高效率 · 无限次数 · 专属功能</p>
          <button type="button">立即开通</button>
        </div>
        <Crown size={78} />
      </section>
      <SegmentedTabs items={['全部', '会员活动', '限时福利', '新功能']} />
      <div className="activity-list">
        {activityCards.map((item) => (
          <article key={item.title} className={`activity-card ${item.tone}`}>
            <div>
              <em>{item.tag}</em>
              <strong>{item.title}</strong>
              <span>{item.desc}</span>
            </div>
            <button type="button">{item.action}</button>
          </article>
        ))}
      </div>
    </div>
  );
}

function MessageScreen() {
  return (
    <div className="tool-screen">
      <header className="tool-page-head">
        <h1>消息</h1>
        <Settings size={20} />
      </header>
      <SegmentedTabs items={['全部', '系统通知', '活动消息', '功能更新']} />
      <div className="message-list">
        {messages.map((item) => {
          const Icon = item.icon;
          return (
            <article key={item.title} className="message-item">
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={20} />
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.desc}</p>
              </div>
              <time>{item.time}</time>
            </article>
          );
        })}
      </div>
    </div>
  );
}

function ProfileScreen({
  displayName,
  serviceStatus,
  loading,
  dynamic,
  membership,
  tripCredit,
  onReload,
  onLogout,
  onOpenMembership,
  onOpenHistory,
  onOpenFavorites,
  onOpenTrips,
  onOpenSettings,
}: {
  displayName: string;
  serviceStatus: string;
  loading: boolean;
  dynamic: ProfileDynamic | null;
  membership: Membership | null;
  tripCredit: TripCredit;
  onReload: () => void;
  onLogout: () => void;
  onOpenMembership: () => void;
  onOpenHistory: () => void;
  onOpenFavorites: () => void;
  onOpenTrips: () => void;
  onOpenSettings: () => void;
}) {
  const [statsMessage, setStatsMessage] = useState<string | null>(null);
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);

  function showStatsMessage(message: string) {
    setStatsMessage(message);
    window.setTimeout(() => setStatsMessage(null), 1600);
  }

  function confirmLogout() {
    setLogoutConfirmOpen(false);
    onLogout();
  }

  return (
    <div className="tool-screen profile-screen">
      <header className="profile-head">
        <div className="profile-avatar">
          <UserRound size={34} />
        </div>
        <div>
          <h1>Hello, {displayName}</h1>
          <p>ID: 12345678 · {serviceStatus}</p>
        </div>
        <button type="button" aria-label="通知">
          <Bell size={22} />
          <i />
        </button>
      </header>
      <ProfileAssetCards membership={membership} credit={tripCredit} onOpenMembership={onOpenMembership} />
      <div className="profile-stats">
        <button type="button" onClick={onOpenFavorites} aria-label="打开我的收藏">
          <strong>{dynamic?.favoriteCount ?? '-'}</strong>
          <span>我的收藏</span>
        </button>
        <button type="button" onClick={onOpenHistory} aria-label="打开历史记录">
          <strong>{dynamic?.historyCount ?? '-'}</strong>
          <span>历史记录</span>
        </button>
        <button type="button" onClick={() => showStatsMessage('我的文件功能建设中')} aria-label="打开我的文件">
          <strong>{dynamic?.fileCount ?? '-'}</strong>
          <span>我的文件</span>
        </button>
        <button type="button" onClick={() => showStatsMessage('优惠券功能建设中')} aria-label="打开优惠券">
          <strong>{dynamic?.couponCount ?? '-'}</strong>
          <span>优惠券</span>
        </button>
      </div>
      {statsMessage ? <div className="profile-stats-toast">{statsMessage}</div> : null}
      <div className="profile-menu">
        <MenuRow icon={Crown} title="会员中心" onClick={onOpenMembership} />
        <MenuRow icon={CalendarDays} title="我的行程" onClick={onOpenTrips} />
        <MenuRow icon={FileText} title="历史记录" onClick={onOpenHistory} />
        <MenuRow icon={Star} title="我的收藏" onClick={onOpenFavorites} />
        <MenuRow icon={RefreshCw} title={loading ? '刷新中' : '刷新动态'} onClick={onReload} />
        <MenuRow icon={Settings} title="设置" onClick={onOpenSettings} />
        <MenuRow icon={LogOut} title="退出登录" onClick={() => setLogoutConfirmOpen(true)} />
      </div>
      <LogoutConfirmDialog
        open={logoutConfirmOpen}
        onCancel={() => setLogoutConfirmOpen(false)}
        onConfirm={confirmLogout}
      />
    </div>
  );
}

function ProfileAssetCards({
  membership,
  credit,
  onOpenMembership,
}: {
  membership: Membership | null;
  credit: TripCredit;
  onOpenMembership: () => void;
}) {
  const [showCreditDescription, setShowCreditDescription] = useState(false);
  const creditIcon = tripCreditIcon(credit.creditLevel);
  const CreditIcon = creditIcon.icon;
  const planName = membership?.planName || '普通会员';
  const membershipStatus = membership?.status === 'ACTIVE' ? '权益生效中' : membership?.status || '基础权益';

  return (
    <>
      <section className="profile-asset-grid">
        <button className="profile-asset-card membership" type="button" onClick={onOpenMembership}>
          <span className="profile-asset-icon">
            <Crown size={22} />
          </span>
          <span>
            <em>会员等级</em>
            <strong>{planName}</strong>
            <small>{membershipStatus}</small>
          </span>
        </button>
        <button className={`profile-asset-card credit ${credit.reservable ? 'available' : 'blocked'} ${creditIcon.tone}`} type="button" onClick={() => setShowCreditDescription(true)}>
          <span className="profile-asset-icon">
            <CreditIcon size={22} />
          </span>
          <span>
            <em>信用等级</em>
            <strong>{credit.creditLevel} · {credit.creditScore}分</strong>
            <small>{credit.reservable ? '可预约行程' : '暂不可预约'}</small>
          </span>
        </button>
      </section>
      {showCreditDescription ? <CreditLevelDescription credit={credit} onClose={() => setShowCreditDescription(false)} /> : null}
    </>
  );
}

function HistoryScreen({ onBack }: { onBack: () => void }) {
  const [items, setItems] = useState<UserHistoryItem[]>([]);
  const [category, setCategory] = useState('全部');
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);
  const categories = ['全部', '图片处理', '文件转换', '扫描识别', '生活工具'];

  useEffect(() => {
    void reload(category);
  }, [category]);

  async function reload(nextCategory: string) {
    setLoading(true);
    setMessage(null);
    try {
      const page = await loadHistory(nextCategory);
      setItems(page.records || []);
    } catch (err) {
      setMessage(readSettingError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="tool-screen">
      <HeaderWithBack title="历史记录" onBack={onBack} />
      <SegmentedTabs items={categories} active={category} onChange={setCategory} />
      <h2 className="history-date">今天</h2>
      {message ? <div className="settings-message error">{message}</div> : null}
      <div className="message-list history-list">
        {loading ? <article className="settings-info-card">加载中</article> : null}
        {!loading && items.length === 0 ? <article className="settings-info-card">暂无历史记录</article> : null}
        {items.map((item) => {
          const Icon = iconForTone(item.iconTone);
          return (
            <article key={item.id} className="message-item">
              <span className={`tool-icon ${item.iconTone || 'blue'}`}>
                <Icon size={20} />
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.description}</p>
              </div>
              <time>{formatShortTime(item.occurredAt)}</time>
            </article>
          );
        })}
      </div>
    </div>
  );
}

function MembershipScreen({ onBack }: { onBack: () => void }) {
  const [membership, setMembership] = useState<Membership | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    loadMembership()
      .then(setMembership)
      .catch((err) => setMessage(readSettingError(err)));
  }, []);

  return (
    <div className="tool-screen">
      <HeaderWithBack title="会员中心" onBack={onBack} />
      {message ? <div className="settings-message error">{message}</div> : null}
      <section className="vip-banner">
        <div>
          <h2>{membership?.planName || '普通会员'}</h2>
          <p>{membership?.benefits || '基础工具可用'}</p>
          <button type="button">立即开通</button>
        </div>
        <Crown size={78} />
      </section>
      <div className="settings-card-list">
        <article className="settings-info-card">
          <strong>会员状态</strong>
          <span>{membership?.status || '-'}</span>
        </article>
        <article className="settings-info-card">
          <strong>到期时间</strong>
          <span>{membership?.expireAt ? formatShortTime(membership.expireAt) : '长期有效'}</span>
        </article>
      </div>
    </div>
  );
}

function FavoritesScreen({ onBack }: { onBack: () => void }) {
  const [items, setItems] = useState<FavoriteItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    loadFavorites()
      .then((page) => setItems(page.records || []))
      .catch((err) => setMessage(readSettingError(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="tool-screen">
      <HeaderWithBack title="我的收藏" onBack={onBack} />
      {message ? <div className="settings-message error">{message}</div> : null}
      <div className="message-list">
        {loading ? <article className="settings-info-card">加载中</article> : null}
        {!loading && items.length === 0 ? <article className="settings-info-card">暂无收藏</article> : null}
        {items.map((item) => {
          const Icon = iconForTone(item.iconTone);
          return (
            <article key={item.id} className="message-item">
              <span className={`tool-icon ${item.iconTone || 'blue'}`}>
                <Icon size={20} />
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.description}</p>
              </div>
              <time>{formatShortTime(item.createdAt)}</time>
            </article>
          );
        })}
      </div>
    </div>
  );
}

function TripScheduleScreen({
  mode,
  displayName,
  username,
  accountSecurity,
  tripCredit,
  onRefreshAccountSecurity,
  onRefreshTripCredit,
  onOpenPhoneVerification,
  onBack,
  onOpenMyTrips,
  onOpenBooking,
}: {
  mode: 'booking' | 'mine';
  displayName: string;
  username: string;
  accountSecurity: AccountSecurity | null;
  tripCredit: TripCredit;
  onRefreshAccountSecurity: () => Promise<AccountSecurity | null>;
  onRefreshTripCredit: () => Promise<void>;
  onOpenPhoneVerification: () => void;
  onBack: () => void;
  onOpenMyTrips?: () => void;
  onOpenBooking?: () => void;
}) {
  const [tab, setTab] = useState(mode === 'mine' ? '我的行程' : '可预约');
  const [selectedTripOwner, setSelectedTripOwner] = useState<string | null>(null);
  const [owners, setOwners] = useState<TripOwner[]>([]);
  const [selectedOwnerSlots, setSelectedOwnerSlots] = useState<TripSlot[]>([]);
  const [mySlots, setMySlots] = useState<TripSlot[]>([]);
  const [reservedSlots, setReservedSlots] = useState<TripSlot[]>([]);
  const [loading, setLoading] = useState(false);
  const [title, setTitle] = useState('下午可约');
  const [place, setPlace] = useState('线上会议');
  const [date, setDate] = useState(nextTripDate(1));
  const [startTime, setStartTime] = useState('14:00');
  const [endTime, setEndTime] = useState('15:00');
  const [toast, setToast] = useState<string | null>(null);
  const pendingRequests = mySlots.filter((slot) => slot.status === 'PENDING');
  const phoneVerified = Boolean(accountSecurity?.phone?.trim());
  const startTimeOptions = buildTripStartTimeOptions(date);
  const endTimeOptions = buildTripEndTimeOptions(startTime);

  useEffect(() => {
    void reloadTrips();
  }, [mode]);

  async function reloadTrips(ownerUsername = selectedTripOwner) {
    setLoading(true);
    try {
      if (mode === 'booking') {
        const nextOwners = await loadTripOwners();
        const nextReservations = await loadMyTripReservations();
        const nextMySlots = await loadMyTrips();
        setOwners(nextOwners);
        setReservedSlots(nextReservations);
        setMySlots(nextMySlots);
        if (ownerUsername) {
          setSelectedOwnerSlots(await loadBookableTripSlots(ownerUsername));
        }
      } else {
        const [nextMySlots, nextReservations] = await Promise.all([
          loadMyTrips(),
          loadMyTripReservations(),
        ]);
        setMySlots(nextMySlots);
        setReservedSlots(nextReservations);
      }
      await onRefreshTripCredit();
    } catch (err) {
      showTripToast(readSettingError(err));
    } finally {
      setLoading(false);
    }
  }

  async function selectTripOwner(ownerUsername: string) {
    setSelectedTripOwner(ownerUsername);
    setLoading(true);
    try {
      setSelectedOwnerSlots(await loadBookableTripSlots(ownerUsername));
    } catch (err) {
      showTripToast(readSettingError(err));
    } finally {
      setLoading(false);
    }
  }

  async function publishSlot(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!(await ensurePhoneVerified())) {
      return;
    }
    if (!title.trim() || !place.trim() || !date || !startTime || !endTime) {
      showTripToast('请完整填写行程信息');
      return;
    }
    if (startTime >= endTime) {
      showTripToast('结束时间需晚于开始时间');
      return;
    }
    const request: TripPublishRequest = {
      title: title.trim(),
      place: place.trim(),
      tripDate: date,
      startTime,
      endTime,
    };
    try {
      await publishTrip(request);
      await reloadTrips();
      setTab('我的发布');
      setTitle('下午可约');
      setPlace('线上会议');
      showTripToast('行程已发布');
    } catch (err) {
      showTripToast(readSettingError(err));
    }
  }

  function changeTripDate(nextDate: string) {
    const nextStartOptions = buildTripStartTimeOptions(nextDate);
    const nextStartTime = nextStartOptions.includes(startTime) ? startTime : nextStartOptions[0] || startTime;
    const nextEndOptions = buildTripEndTimeOptions(nextStartTime);
    setDate(nextDate);
    setStartTime(nextStartTime);
    if (!nextEndOptions.includes(endTime)) {
      setEndTime(nextEndOptions[0] || endTime);
    }
  }

  function changeStartTime(nextStartTime: string) {
    const nextEndOptions = buildTripEndTimeOptions(nextStartTime);
    setStartTime(nextStartTime);
    if (!nextEndOptions.includes(endTime)) {
      setEndTime(nextEndOptions[0] || endTime);
    }
  }

  async function requestSlot(slot: TripSlot) {
    if (!(await ensurePhoneVerified())) {
      return;
    }
    if (!tripCredit.reservable) {
      showTripToast(`信用分低于 ${tripCredit.minReservableScore} 分，暂不可预约`);
      return;
    }
    if (isOwnTripSlot(slot, username)) {
      showTripToast('不能预约自己发布的行程');
      return;
    }
    try {
      await applyTrip(slot.id);
      await reloadTrips();
      setTab('我的预约');
      showTripToast('预约申请已发送');
    } catch (err) {
      if (err instanceof ApiError && err.code === TRIP_CONFLICT_CODE) {
        const confirmed = window.confirm('预约时间与您发布的行程冲突。继续预约将删除自己发布的冲突行程，是否继续？');
        if (!confirmed) {
          return;
        }
        try {
          await applyTrip(slot.id, '希望预约这个时段', true);
          await reloadTrips();
          setTab('我的预约');
          showTripToast('已删除冲突行程并提交预约');
        } catch (confirmErr) {
          showTripToast(readSettingError(confirmErr));
        }
        return;
      }
      showTripToast(readSettingError(err));
    }
  }

  async function ensurePhoneVerified() {
    const nextAccount = await onRefreshAccountSecurity();
    if (!nextAccount?.phone?.trim()) {
      showTripToast('请先在设置-账号与安全中认证手机号');
      onOpenPhoneVerification();
      return false;
    }
    return true;
  }

  function reviewSlot(slotId: number, approved: boolean) {
    reviewTrip(slotId, approved)
      .then(() => reloadTrips())
      .then(() => showTripToast(approved ? '已通过预约申请' : '已拒绝预约申请'))
      .catch((err) => showTripToast(readSettingError(err)));
  }

  function markNoShow(slot: TripSlot) {
    const applicant = slot.applicantDisplayName || slot.applicantUsername || '预约人';
    const confirmed = window.confirm(`确认将 ${applicant} 标记为未履约？系统将扣除 ${tripCredit.noShowPenalty} 信用分。`);
    if (!confirmed) {
      return;
    }
    markTripNoShow(slot.id)
      .then(() => reloadTrips())
      .then(() => showTripToast('已标记未履约并扣除信用分'))
      .catch((err) => showTripToast(readSettingError(err)));
  }

  function changeTripTab(nextTab: string) {
    setTab(nextTab);
    if (mode !== 'booking' || nextTab !== '可预约') {
      setSelectedTripOwner(null);
      setSelectedOwnerSlots([]);
    }
    void reloadTrips();
  }

  function showTripToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(null), 1600);
  }

  return (
    <div className="tool-screen trip-screen">
      <HeaderWithBack title={mode === 'mine' ? '我的行程' : '行程预约'} onBack={onBack} right={mode === 'mine' ? '管理' : '预约'} />
      <section className="trip-hero">
        <div>
          <span>最近一周</span>
          <h2>{mode === 'mine' ? '查看我的行程、预约和审核' : '先选择可预约人，再查看我的预约'}</h2>
        </div>
        <CalendarDays size={58} />
      </section>

      {!phoneVerified ? (
        <button className="trip-phone-alert" type="button" onClick={onOpenPhoneVerification}>
          <ShieldCheck size={18} />
          <span>发布行程和预约行程前，需要先认证手机号</span>
          <ChevronRight size={16} />
        </button>
      ) : null}

      <TripCreditCard credit={tripCredit} />

      <div className="trip-segment-row">
        {mode === 'mine' ? (
          <button className="trip-flow-jump backward" type="button" onClick={onOpenBooking} aria-label="返回行程预约">
            <ChevronLeft size={17} />
          </button>
        ) : null}
        <SegmentedTabs items={mode === 'mine' ? ['我的行程', '我的预约', '我的发布', '待审核'] : ['可预约', '我的预约', '行程预览']} active={tab} onChange={changeTripTab} />
        {mode === 'booking' ? (
          <button className="trip-flow-jump forward" type="button" onClick={onOpenMyTrips} aria-label="打开我的行程">
            <ChevronRight size={17} />
          </button>
        ) : null}
      </div>

      {tab === '我的行程' ? (
        <TripSlotList emptyText={loading ? '加载中' : '暂无我的行程'} slots={mySlots} currentUser={username} />
      ) : null}

      {tab === '我的发布' ? (
        <>
          <form className="trip-form" onSubmit={publishSlot}>
            <label>
              <span>行程标题</span>
              <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="例如 咖啡沟通 / 线上咨询" />
            </label>
            <label>
              <span>地点</span>
              <input value={place} onChange={(event) => setPlace(event.target.value)} placeholder="例如 公司会议室 / 线上会议" />
            </label>
            <div className="trip-time-grid">
              <label>
                <span>日期</span>
                <input type="date" min={nextTripDate(0)} max={nextTripDate(6)} value={date} onChange={(event) => changeTripDate(event.target.value)} />
              </label>
              <label>
                <span>开始</span>
                <TripTimePicker value={startTime} options={startTimeOptions} onChange={changeStartTime} hourMode={date === nextTripDate(0) ? 'options' : 'all'} />
              </label>
              <label>
                <span>结束</span>
                <TripTimePicker value={endTime} options={endTimeOptions} onChange={setEndTime} hourMode="options" />
              </label>
            </div>
            <button className="app-primary" type="submit">{phoneVerified ? '发布行程' : '认证手机号后发布'}</button>
          </form>
          <TripSlotList
            emptyText={loading ? '加载中' : '暂无已发布行程'}
            slots={mySlots}
            currentUser={username}
            actionLabel={(slot) => slot.status === 'BOOKED' && isTripSlotExpired(slot) ? '未履约' : null}
            onAction={markNoShow}
          />
        </>
      ) : null}

      {tab === '我的预约' ? (
        <TripSlotList emptyText={loading ? '加载中' : '暂无我的预约'} slots={reservedSlots} currentUser={username} statusContext="reservation" />
      ) : null}

      {tab === '行程预览' ? (
        <TripTimelinePreview slots={mergeTripTimelineSlots(mySlots, reservedSlots)} currentUser={username} />
      ) : null}

      {mode === 'booking' && tab === '可预约' ? (
        selectedTripOwner ? (
          <>
            <button className="trip-owner-back" type="button" onClick={() => setSelectedTripOwner(null)}>
              <ChevronLeft size={17} />
              返回可预约人列表
            </button>
            <TripSlotList
              emptyText="该用户暂无可预约时段"
              slots={selectedOwnerSlots}
              currentUser={username}
              actionLabel={phoneVerified ? '预约' : '需认证手机号'}
              actionDisabled={phoneVerified && !tripCredit.reservable}
              disabledActionLabel="信用不足"
              onAction={requestSlot}
            />
          </>
        ) : (
          <TripOwnerList owners={owners} loading={loading} onSelect={selectTripOwner} />
        )
      ) : null}

      {tab === '待审核' ? (
        <div className="trip-list">
          {pendingRequests.length === 0 ? <article className="settings-info-card">暂无待审核申请</article> : null}
          {pendingRequests.map((slot) => (
            <article key={slot.id} className="trip-card">
              <TripCardMain slot={slot} currentUser={username} />
              <div className="trip-request-note">
                <UserCheck size={16} />
                <span>{slot.applicantDisplayName || slot.applicantUsername} 申请预约：{slot.applyNote}</span>
              </div>
              <div className="trip-review-actions">
                <button type="button" onClick={() => reviewSlot(slot.id, false)}>拒绝</button>
                <button type="button" onClick={() => reviewSlot(slot.id, true)}>
                  <CheckCircle2 size={16} />
                  通过
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      {toast ? <div className="home-toast">{toast}</div> : null}
    </div>
  );
}

function TripCreditCard({ credit }: { credit: TripCredit }) {
  const [showDescription, setShowDescription] = useState(false);
  const progress = Math.max(0, Math.min(100, credit.creditScore));
  const creditIcon = tripCreditIcon(credit.creditLevel);
  const Icon = creditIcon.icon;
  return (
    <>
      <section className={`trip-credit-card ${credit.reservable ? 'available' : 'blocked'} ${creditIcon.tone}`}>
      <button
        className="trip-credit-score"
        type="button"
        onClick={() => setShowDescription(true)}
        aria-label="查看信用级别说明"
      >
        <Icon size={22} />
        <span>信用等级</span>
        <strong>{credit.creditLevel}</strong>
      </button>
      <div className="trip-credit-main">
        <div>
          <strong>{credit.creditScore} 分</strong>
          <span>低于 {credit.minReservableScore} 分不可预约 · 未履约扣 {credit.noShowPenalty} 分</span>
        </div>
        <div className="trip-credit-meter" aria-hidden="true">
          <i style={{ width: `${progress}%` }} />
        </div>
      </div>
    </section>
      {showDescription ? <CreditLevelDescription credit={credit} onClose={() => setShowDescription(false)} /> : null}
    </>
  );
}

function CreditLevelDescription({ credit, onClose }: { credit: TripCredit; onClose: () => void }) {
  return (
    <div className="credit-level-mask" role="dialog" aria-modal="true" aria-label="信用级别说明">
      <div className="credit-level-sheet">
        <header>
          <div>
            <strong>信用级别说明</strong>
            <span>当前 {credit.creditLevel} 级 · {credit.creditScore} 分</span>
          </div>
          <button type="button" onClick={onClose} aria-label="关闭信用级别说明">
            <X size={18} />
          </button>
        </header>
        <div className="credit-level-rules">
          {creditLevelRules().map((rule) => {
            const Icon = rule.icon;
            return (
              <article key={rule.level} className={rule.tone}>
                <Icon size={18} />
                <strong>{rule.level}</strong>
                <span>{rule.range}</span>
                <small>{rule.description}</small>
              </article>
            );
          })}
        </div>
        <p>低于 {credit.minReservableScore} 分将暂停预约；每次确认未履约扣 {credit.noShowPenalty} 分。</p>
      </div>
    </div>
  );
}

function creditLevelRules() {
  return [
    { level: 'S', range: '95-100分', description: '信用优秀，预约优先级最高', icon: Crown, tone: 'credit-s' },
    { level: 'A', range: '85-94分', description: '信用良好，可正常预约', icon: ShieldCheck, tone: 'credit-a' },
    { level: 'B', range: '75-84分', description: '信用稳定，建议按时履约', icon: Star, tone: 'credit-b' },
    { level: 'C', range: '60-74分', description: '信用偏低，需减少失约', icon: UserCheck, tone: 'credit-c' },
    { level: 'D', range: '0-59分', description: '暂不可预约行程', icon: X, tone: 'credit-d' },
  ];
}

function tripCreditIcon(level: string): { icon: typeof ShieldCheck; tone: string } {
  const normalizedLevel = level.toUpperCase();
  if (normalizedLevel === 'S') {
    return { icon: Crown, tone: 'credit-s' };
  }
  if (normalizedLevel === 'A') {
    return { icon: ShieldCheck, tone: 'credit-a' };
  }
  if (normalizedLevel === 'B') {
    return { icon: Star, tone: 'credit-b' };
  }
  if (normalizedLevel === 'C') {
    return { icon: UserCheck, tone: 'credit-c' };
  }
  return { icon: X, tone: 'credit-d' };
}


function TripOwnerList({ owners, loading, onSelect }: { owners: TripOwner[]; loading: boolean; onSelect: (ownerName: string) => void }) {
  return (
    <div className="trip-owner-list">
      {owners.length === 0 ? <article className="settings-info-card">{loading ? '加载中' : '暂无可预约用户'}</article> : null}
      {owners.map((owner) => (
        <button key={owner.ownerUsername} className="trip-owner-card" type="button" onClick={() => onSelect(owner.ownerUsername)}>
          <span className="trip-owner-avatar">
            <UserRound size={20} />
          </span>
          <span>
            <strong>{owner.ownerDisplayName || owner.ownerUsername}</strong>
            <small>{owner.slotCount} 个可预约时段 · 最近 {formatTripDate(owner.nextDate)} {owner.nextTime}</small>
          </span>
          <ChevronRight size={17} />
        </button>
      ))}
    </div>
  );
}

function TripTimePicker({
  value,
  options,
  onChange,
  hourMode = 'all',
}: {
  value: string;
  options: string[];
  onChange: (value: string) => void;
  hourMode?: 'all' | 'options';
}) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(value);
  const selectedHour = value.slice(0, 2);
  const hours = hourMode === 'all' ? buildTripHourOptions() : Array.from(new Set(options.map((time) => time.slice(0, 2))));
  const activeHour = hours.includes(selectedHour) ? selectedHour : hours[0] || selectedHour;
  const minutes = options
    .filter((time) => time.startsWith(`${activeHour}:`))
    .map((time) => time.slice(3, 5));

  useEffect(() => {
    setDraft(value);
  }, [value]);

  function closeWhenLeaving(event: FocusEvent<HTMLDivElement>) {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
      setOpen(false);
      if (!isAllowedTripTime(draft, options)) {
        setDraft(value);
      }
    }
  }

  function applyManualInput(nextDraft: string) {
    setDraft(nextDraft);
    const normalized = normalizeTripTimeInput(nextDraft);
    if (normalized && options.includes(normalized)) {
      onChange(normalized);
    }
  }

  function chooseHour(hour: string) {
    const nextTime = options.find((time) => time.startsWith(`${hour}:`));
    if (nextTime) {
      onChange(nextTime);
    }
  }

  function chooseMinute(minute: string) {
    const nextTime = `${activeHour}:${minute}`;
    if (options.includes(nextTime)) {
      onChange(nextTime);
    }
  }

  return (
    <div className="trip-time-picker" onBlur={closeWhenLeaving}>
      <div className="trip-time-input-wrap">
        <input
          value={draft}
          inputMode="numeric"
          placeholder="HH:mm"
          onFocus={() => setOpen(true)}
          onChange={(event) => applyManualInput(event.target.value)}
        />
        <button type="button" aria-label="选择时间" onClick={() => setOpen((next) => !next)}>
          <ChevronRight size={15} />
        </button>
      </div>
      {open ? (
        <div className="trip-time-panel">
          <div className="trip-time-column" aria-label="小时">
            {hours.map((hour) => (
              <button key={hour} className={hour === activeHour ? 'active' : ''} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => chooseHour(hour)}>
                {hour}
              </button>
            ))}
          </div>
          <div className="trip-time-column" aria-label="分钟">
            {minutes.map((minute) => (
              <button key={minute} className={`${activeHour}:${minute}` === value ? 'active' : ''} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => chooseMinute(minute)}>
                {minute}
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function TripTimelinePreview({ slots, currentUser }: { slots: TripSlot[]; currentUser: string }) {
  const now = new Date();
  const sortedSlots = [...slots].sort(compareTripSlots);
  const upcomingSlots = sortedSlots.filter((slot) => !isTripSlotExpired(slot, now));
  const expiredSlots = sortedSlots.filter((slot) => isTripSlotExpired(slot, now)).reverse();

  return (
    <section className="trip-timeline-preview">
      <header>
        <span>行程预览</span>
        <strong>{upcomingSlots.length} 个未出行 · {expiredSlots.length} 个已过期</strong>
      </header>
      <TripTimelineGroup
        title="未出行"
        description="今天未结束和未来行程"
        emptyText="暂无未出行行程"
        slots={upcomingSlots}
        currentUser={currentUser}
      />
      <TripTimelineGroup
        title="已过期"
        description="已超过结束时间的行程"
        emptyText="暂无已过期行程"
        slots={expiredSlots}
        currentUser={currentUser}
        expired
      />
    </section>
  );
}

function TripTimelineGroup({
  title,
  description,
  emptyText,
  slots,
  currentUser,
  expired = false,
}: {
  title: string;
  description: string;
  emptyText: string;
  slots: TripSlot[];
  currentUser: string;
  expired?: boolean;
}) {
  return (
    <div className="trip-timeline-group">
      <div className="trip-timeline-group-title">
        <strong>{title}</strong>
        <span>{description}</span>
      </div>
      {slots.length === 0 ? <article className="settings-info-card">{emptyText}</article> : null}
      {slots.map((slot) => {
        const ownSlot = isOwnTripSlot(slot, currentUser);
        return (
          <article key={`${slot.id}-${ownSlot ? 'owned' : 'reserved'}-${expired ? 'expired' : 'upcoming'}`} className={`trip-timeline-item ${ownSlot ? 'owned' : 'reserved'} ${expired ? 'expired' : 'upcoming'}`}>
            <time>
              <span>{formatTripDate(slot.tripDate)}</span>
              {formatTripTimeRange(slot.startTime, slot.endTime)}
            </time>
            <div>
              <strong>{slot.title}</strong>
              <span>{ownSlot ? tripSubtitle(slot, true) : tripSubtitle(slot, false, 'reservation')}</span>
              <small>{slot.place} · {tripStatusLabel(slot.status, ownSlot ? undefined : 'reservation')} · {expired ? '已过期' : '未出行'}</small>
            </div>
          </article>
        );
      })}
    </div>
  );
}

function TripSlotList({
  slots,
  currentUser,
  emptyText,
  actionLabel,
  actionDisabled,
  disabledActionLabel,
  onAction,
  statusContext,
}: {
  slots: TripSlot[];
  currentUser: string;
  emptyText: string;
  actionLabel?: string | ((slot: TripSlot) => string | null);
  actionDisabled?: boolean;
  disabledActionLabel?: string;
  onAction?: (slot: TripSlot) => void;
  statusContext?: 'reservation';
}) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    if (statusContext !== 'reservation') return;
    const timer = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timer);
  }, [statusContext]);

  const visibleSlots = statusContext === 'reservation'
    ? slots.filter((slot) => !isTripSlotExpired(slot, now))
    : slots;

  return (
    <div className="trip-list">
      {visibleSlots.length === 0 ? <article className="settings-info-card">{emptyText}</article> : null}
      {visibleSlots.map((slot) => {
        const resolvedActionLabel = typeof actionLabel === 'function' ? actionLabel(slot) : actionLabel;
        return (
          <article key={slot.id} className="trip-card">
            <TripCardMain slot={slot} currentUser={currentUser} statusContext={statusContext} />
            {resolvedActionLabel && onAction ? (
              <button className="trip-card-action" type="button" disabled={actionDisabled} onClick={() => onAction(slot)}>
                {actionDisabled ? disabledActionLabel || resolvedActionLabel : resolvedActionLabel}
              </button>
            ) : null}
          </article>
        );
      })}
    </div>
  );
}

function mergeTripTimelineSlots(mySlots: TripSlot[], reservedSlots: TripSlot[]) {
  const byKey = new Map<string, TripSlot>();
  [...mySlots, ...reservedSlots].forEach((slot) => {
    byKey.set(`${slot.id}-${slot.ownerUsername}-${slot.applicantUsername || ''}`, slot);
  });
  return Array.from(byKey.values());
}

function compareTripSlots(left: TripSlot, right: TripSlot) {
  return `${left.tripDate} ${left.startTime}`.localeCompare(`${right.tripDate} ${right.startTime}`);
}

function isTripSlotExpired(slot: TripSlot, now = new Date()) {
  const endAt = new Date(`${slot.tripDate}T${formatTripClock(slot.endTime)}:00`);
  return !Number.isNaN(endAt.getTime()) && endAt.getTime() < now.getTime();
}

function TripCardMain({ slot, currentUser, statusContext }: { slot: TripSlot; currentUser: string; statusContext?: 'reservation' }) {
  const ownSlot = isOwnTripSlot(slot, currentUser);

  return (
    <div className="trip-card-main">
      <span
        className={`trip-status ${slot.status}`}
        title={tripStatusHint(slot.status, statusContext)}
        tabIndex={statusContext === 'reservation' && slot.status === 'PENDING' ? 0 : undefined}
      >
        {tripStatusLabel(slot.status, statusContext)}
      </span>
      <div>
        <strong>{slot.title}</strong>
        <p>{tripSubtitle(slot, ownSlot, statusContext)}</p>
      </div>
      <div className="trip-meta">
        <span><CalendarDays size={15} />{formatTripDate(slot.tripDate)}</span>
        <span><Clock3 size={15} />{slot.startTime}-{slot.endTime}</span>
        <span>{slot.place}</span>
      </div>
    </div>
  );
}

function tripSubtitle(slot: TripSlot, ownSlot: boolean, context?: 'reservation') {
  if (context === 'reservation') {
    const owner = slot.ownerDisplayName || slot.ownerUsername;
    return owner ? `预约对象：${owner}` : '预约对象：-';
  }
  if (!ownSlot) {
    return `${slot.ownerDisplayName || slot.ownerUsername} 发布的行程`;
  }
  const applicant = slot.applicantDisplayName || slot.applicantUsername;
  return applicant ? `我发布的行程 · 预约人：${applicant}` : '我发布的行程';
}

function SettingsScreen({
  displayName,
  initialDetail = null,
  focusPhone = false,
  onAccountUpdated,
  onBack,
  onLogout,
}: {
  displayName: string;
  initialDetail?: SettingDetail | null;
  focusPhone?: boolean;
  onAccountUpdated?: (account: AccountSecurity) => void;
  onBack: () => void;
  onLogout: () => void;
}) {
  const [detail, setDetail] = useState<SettingDetail | null>(initialDetail);
  const [cacheSize, setCacheSize] = useState('12.4MB');
  const [toast, setToast] = useState<string | null>(null);
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(null), 1800);
  }

  function clearCache() {
    if (cacheSize === '0KB') {
      showToast('缓存已清理');
      return;
    }
    setCacheSize('0KB');
    showToast('缓存清理完成');
  }

  function checkVersion() {
    setDetail('version');
  }

  function logout() {
    setLogoutConfirmOpen(true);
  }

  function confirmLogout() {
    setLogoutConfirmOpen(false);
    onLogout();
  }

  if (detail) {
    return (
      <div className="tool-screen settings-detail-screen">
        <HeaderWithBack title={settingTitle(detail)} onBack={focusPhone && initialDetail === detail ? onBack : () => setDetail(null)} />
        {detail === 'account' ? (
          <AccountSecurityDetail fallbackDisplayName={displayName} focusPhone={focusPhone} onAccountUpdated={onAccountUpdated} />
        ) : (
          <SettingDetailContent
            detail={detail}
          />
        )}
      </div>
    );
  }

  return (
    <div className="tool-screen">
      <HeaderWithBack title="设置" onBack={onBack} />
      <div className="profile-menu settings-menu">
        <MenuRow icon={ShieldCheck} title="账号与安全" onClick={() => setDetail('account')} />
        <MenuRow icon={Bell} title="消息通知" onClick={() => setDetail('notifications')} />
        <MenuRow icon={Archive} title="清理缓存" meta={cacheSize} onClick={clearCache} />
        <MenuRow icon={FileText} title="关于我们" onClick={() => setDetail('about')} />
        <MenuRow icon={ShieldCheck} title="隐私政策" onClick={() => setDetail('privacy')} />
        <MenuRow icon={FileText} title="用户协议" onClick={() => setDetail('agreement')} />
        <MenuRow icon={RefreshCw} title="版本更新" meta="v1.2.0" onClick={checkVersion} />
      </div>
      <button className="logout-button" type="button" onClick={logout}>退出登录</button>
      <LogoutConfirmDialog
        open={logoutConfirmOpen}
        onCancel={() => setLogoutConfirmOpen(false)}
        onConfirm={confirmLogout}
      />
      {toast ? <div className="settings-toast">{toast}</div> : null}
    </div>
  );
}

function LogoutConfirmDialog({
  open,
  onCancel,
  onConfirm,
}: {
  open: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  if (!open) {
    return null;
  }

  function cancel(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
    event.stopPropagation();
    onCancel();
  }

  function confirm(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
    event.stopPropagation();
    onConfirm();
  }

  return (
    <div className="settings-confirm-mask" role="presentation" onClick={onCancel}>
      <section className="settings-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="logout-confirm-title" onClick={(event) => event.stopPropagation()}>
        <h2 id="logout-confirm-title">确认退出登录？</h2>
        <p>退出后需要重新登录才能继续使用账号相关功能。</p>
        <div className="settings-confirm-actions">
          <button type="button" onClick={cancel}>取消</button>
          <button type="button" className="danger" onClick={confirm}>退出登录</button>
        </div>
      </section>
    </div>
  );
}

function AccountSecurityDetail({
  fallbackDisplayName,
  focusPhone = false,
  onAccountUpdated,
}: {
  fallbackDisplayName: string;
  focusPhone?: boolean;
  onAccountUpdated?: (account: AccountSecurity) => void;
}) {
  type AccountFieldErrorKey = 'displayName' | 'email' | 'phone' | 'currentEmailCode' | 'newEmailCode' | 'currentPhoneCode' | 'newPhoneCode' | 'antiAbuse' | 'currentPassword' | 'newPassword' | 'confirmPassword';
  const [account, setAccount] = useState<AccountSecurity | null>(null);
  const [displayName, setDisplayName] = useState(fallbackDisplayName);
  const [email, setEmail] = useState('');
  const [phoneCountryCode, setPhoneCountryCode] = useState('+86');
  const [countrySearch, setCountrySearch] = useState('');
  const [countryPickerOpen, setCountryPickerOpen] = useState(false);
  const [phone, setPhone] = useState('');
  const [currentPhoneVerificationCode, setCurrentPhoneVerificationCode] = useState('');
  const [newPhoneVerificationCode, setNewPhoneVerificationCode] = useState('');
  const [currentEmailVerificationCode, setCurrentEmailVerificationCode] = useState('');
  const [newEmailVerificationCode, setNewEmailVerificationCode] = useState('');
  const [codeSending, setCodeSending] = useState<string | null>(null);
  const [codeCountdowns, setCodeCountdowns] = useState<Record<string, number>>({});
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [antiAbuseChallenge, setAntiAbuseChallenge] = useState<{ challengeId: string; question: string; expiresInSeconds: number } | null>(null);
  const [antiAbuseAnswer, setAntiAbuseAnswer] = useState('');
  const [antiAbuseLoading, setAntiAbuseLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<AccountFieldErrorKey, string>>>({});
  const countryCodeOptions = COUNTRY_CALLING_CODE_OPTIONS
    .filter((option) => {
      const keyword = countrySearch.trim().toLowerCase();
      if (!keyword) {
        return true;
      }
      return option.code.includes(keyword) || option.label.toLowerCase().includes(keyword);
    })
    .slice(0, 40);

  useEffect(() => {
    void reload();
    void reloadAntiAbuseChallenge();
  }, []);

  useEffect(() => {
    if (!Object.values(codeCountdowns).some((value) => value > 0)) {
      return;
    }
    const timer = window.setTimeout(() => {
      setCodeCountdowns((counts) => Object.fromEntries(
        Object.entries(counts).map(([key, value]) => [key, Math.max(0, value - 1)])
      ));
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [codeCountdowns]);

  async function reload() {
    setLoading(true);
    setMessage(null);
    setFieldErrors({});
    try {
      const nextAccount = await loadAccountSecurity();
      setAccount(nextAccount);
      setDisplayName(nextAccount.displayName || '');
      setEmail(nextAccount.email || '');
      setPhoneCountryCode(nextAccount.phoneCountryCode || '+86');
      setPhone(nextAccount.phone || '');
      clearVerificationCodes();
      onAccountUpdated?.(nextAccount);
    } catch (err) {
      setMessage({ type: 'error', text: readSettingError(err) });
    } finally {
      setLoading(false);
    }
  }

  async function reloadAntiAbuseChallenge() {
    setAntiAbuseLoading(true);
    try {
      setAntiAbuseChallenge(await createAccountSecurityChallenge());
      setAntiAbuseAnswer('');
    } catch (err) {
      setMessage({ type: 'error', text: readSettingError(err) });
    } finally {
      setAntiAbuseLoading(false);
    }
  }

  function clearVerificationCodes() {
    setCurrentPhoneVerificationCode('');
    setNewPhoneVerificationCode('');
    setCurrentEmailVerificationCode('');
    setNewEmailVerificationCode('');
  }

  function clearFieldError(key: AccountFieldErrorKey) {
    setFieldErrors((errors) => ({ ...errors, [key]: undefined }));
  }

  function showFieldError(key: AccountFieldErrorKey, text: string) {
    setMessage(null);
    setFieldErrors((errors) => ({ ...errors, [key]: text }));
  }

  function showServerFieldError(error: unknown) {
    if (!(error instanceof Error)) {
      return false;
    }
    const message = error.message;
    if (message === 'email already exists' || message === 'email format is invalid') {
      showFieldError('email', readSettingError(error));
      return true;
    }
    if (message === 'phone format is invalid' || message === 'phone already exists') {
      showFieldError('phone', readSettingError(error));
      return true;
    }
    if (message === 'phone verification code is required' || message === 'phone verification code expired' || message === 'phone verification code is invalid') {
      const key = phoneChanged && currentPhone ? 'currentPhoneCode' : 'newPhoneCode';
      showFieldError(key, readSettingError(error));
      return true;
    }
    if (message === 'email verification code is required' || message === 'email verification code expired' || message === 'email verification code is invalid') {
      const key = emailChanged && currentEmail ? 'currentEmailCode' : 'newEmailCode';
      showFieldError(key, readSettingError(error));
      return true;
    }
    if (message === 'anti abuse verification is required' || message === 'anti abuse verification expired' || message === 'anti abuse verification is invalid') {
      showFieldError('antiAbuse', readSettingError(error));
      return true;
    }
    if (message === 'Current password is incorrect') {
      showFieldError('currentPassword', readSettingError(error));
      return true;
    }
    if (message === 'newPassword is not strong enough') {
      showFieldError('newPassword', readSettingError(error));
      return true;
    }
    return false;
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    if (!displayName.trim()) {
      showFieldError('displayName', '请输入显示名称');
      return;
    }
    if (email.trim() && !isValidEmail(email.trim())) {
      showFieldError('email', '请输入有效的邮箱地址');
      return;
    }
    const nextEmail = email.trim() || null;
    const currentEmail = account?.email?.trim() || null;
    const emailChanged = nextEmail !== currentEmail;
    const nextPhone = phone.trim();
    const nextPhoneCountryCode = normalizePhoneCountryCode(phoneCountryCode);
    const currentPhone = account?.phone || '';
    const currentPhoneCountryCode = normalizePhoneCountryCode(account?.phoneCountryCode || '+86');
    const phoneChanged = nextPhone !== currentPhone || (nextPhone ? nextPhoneCountryCode !== currentPhoneCountryCode : Boolean(currentPhone));
    if (nextPhone && !isValidPhone(nextPhoneCountryCode, nextPhone)) {
      showFieldError('phone', '请输入有效的手机号');
      return;
    }
    if (emailChanged && currentEmail && !currentEmailVerificationCode.trim()) {
      showFieldError('currentEmailCode', '请输入当前邮箱验证码');
      return;
    }
    if (emailChanged && nextEmail && !newEmailVerificationCode.trim()) {
      showFieldError('newEmailCode', '请输入新邮箱验证码');
      return;
    }
    if (phoneChanged && currentPhone && !currentPhoneVerificationCode.trim()) {
      showFieldError('currentPhoneCode', '请输入当前手机号验证码');
      return;
    }
    if (phoneChanged && nextPhone && !newPhoneVerificationCode.trim()) {
      showFieldError('newPhoneCode', '请输入新手机号验证码');
      return;
    }
    if (!antiAbuseChallenge?.challengeId || !antiAbuseAnswer.trim()) {
      showFieldError('antiAbuse', '请先完成人机验证');
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const nextAccount = await updateAccountSecurity({
        displayName: displayName.trim(),
        email: nextEmail,
        phoneCountryCode: nextPhone ? nextPhoneCountryCode : null,
        phone: nextPhone || null,
        currentPhoneVerificationCode: phoneChanged && currentPhone ? currentPhoneVerificationCode.trim() : null,
        newPhoneVerificationCode: phoneChanged && nextPhone ? newPhoneVerificationCode.trim() : null,
        currentEmailVerificationCode: emailChanged && currentEmail ? currentEmailVerificationCode.trim() : null,
        newEmailVerificationCode: emailChanged && nextEmail ? newEmailVerificationCode.trim() : null,
        antiAbuseChallengeId: antiAbuseChallenge.challengeId,
        antiAbuseAnswer: antiAbuseAnswer.trim(),
      });
      setAccount(nextAccount);
      setDisplayName(nextAccount.displayName || '');
      setEmail(nextAccount.email || '');
      setPhoneCountryCode(nextAccount.phoneCountryCode || '+86');
      setPhone(nextAccount.phone || '');
      clearVerificationCodes();
      setFieldErrors({});
      await reloadAntiAbuseChallenge();
      onAccountUpdated?.(nextAccount);
      setMessage({ type: 'success', text: '账号资料已更新' });
    } catch (err) {
      if (!showServerFieldError(err)) {
        setMessage({ type: 'error', text: readSettingError(err) });
      }
      void reloadAntiAbuseChallenge();
    } finally {
      setSaving(false);
    }
  }

  async function sendPhoneCode(target: 'currentPhone' | 'newPhone') {
    const targetPhone = target === 'currentPhone' ? account?.phone || '' : phone.trim();
    const targetPhoneCountryCode = target === 'currentPhone'
      ? normalizePhoneCountryCode(account?.phoneCountryCode || '+86')
      : normalizePhoneCountryCode(phoneCountryCode);
    if (!isValidPhone(targetPhoneCountryCode, targetPhone)) {
      showFieldError('phone', '请输入有效的手机号');
      return;
    }
    setCodeSending(target);
    setMessage(null);
    try {
      const result = await sendPhoneVerificationCode(targetPhoneCountryCode, targetPhone);
      setCodeCountdowns((counts) => ({ ...counts, [target]: result.expiresInSeconds || 60 }));
      setMessage({ type: 'success', text: '验证码已发送，请查收短信' });
    } catch (err) {
      setMessage({ type: 'error', text: readSettingError(err) });
    } finally {
      setCodeSending(null);
    }
  }

  async function sendEmailCode(target: 'currentEmail' | 'newEmail') {
    const targetEmail = target === 'currentEmail' ? account?.email?.trim() || '' : email.trim();
    if (!isValidEmail(targetEmail)) {
      showFieldError('email', '请输入有效的邮箱地址');
      return;
    }
    setCodeSending(target);
    setMessage(null);
    try {
      const result = await sendEmailVerificationCode(targetEmail);
      setCodeCountdowns((counts) => ({ ...counts, [target]: result.expiresInSeconds || 60 }));
      setMessage({ type: 'success', text: '验证码已发送，请查收邮箱' });
    } catch (err) {
      setMessage({ type: 'error', text: readSettingError(err) });
    } finally {
      setCodeSending(null);
    }
  }

  function closeCountryPickerOnBlur(event: FocusEvent<HTMLElement>) {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
      setCountryPickerOpen(false);
    }
  }

  const currentEmail = account?.email?.trim() || '';
  const nextEmail = email.trim();
  const emailChanged = nextEmail !== currentEmail;
  const currentPhone = account?.phone || '';
  const currentPhoneCountryCode = normalizePhoneCountryCode(account?.phoneCountryCode || '+86');
  const nextPhone = phone.trim();
  const nextPhoneCountryCode = normalizePhoneCountryCode(phoneCountryCode);
  const phoneChanged = nextPhone !== currentPhone || (nextPhone ? nextPhoneCountryCode !== currentPhoneCountryCode : Boolean(currentPhone));

  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    if (!currentPassword || !newPassword) {
      showFieldError(!currentPassword ? 'currentPassword' : 'newPassword', '请输入当前密码和新密码');
      return;
    }
    if (newPassword !== confirmPassword) {
      showFieldError('confirmPassword', '两次输入的新密码不一致');
      return;
    }
    if (!isStrongPassword(newPassword)) {
      showFieldError('newPassword', '新密码需为 6-20 位，并包含数字、大小写字母和特殊字符');
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      await changeAccountPassword({ currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setFieldErrors({});
      setMessage({ type: 'success', text: '密码已修改' });
    } catch (err) {
      if (!showServerFieldError(err)) {
        setMessage({ type: 'error', text: readSettingError(err) });
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="settings-card-list">
      <article className="settings-info-card">
        <strong>{account?.username || '-'}</strong>
        <span>{loading ? '账号信息加载中' : `会员等级：${account?.vipLevel || '-'} · 状态：${account?.status || '-'}`}</span>
      </article>

      {message ? <div className={`settings-message ${message.type}`}>{message.text}</div> : null}

      <form className="settings-form-card" onSubmit={saveProfile}>
        <h2>基础信息</h2>
        <label>
          显示名称
          <input value={displayName} onChange={(event) => {
            setDisplayName(event.target.value);
            clearFieldError('displayName');
          }} disabled={loading || saving} />
          {fieldErrors.displayName ? <small className="settings-field-error">{fieldErrors.displayName}</small> : null}
        </label>
        <label>
          邮箱
          <input value={email} onChange={(event) => {
            setEmail(event.target.value);
            clearFieldError('email');
          }} disabled={loading || saving} inputMode="email" />
          {fieldErrors.email ? <small className="settings-field-error">{fieldErrors.email}</small> : null}
        </label>
        {emailChanged && currentEmail ? (
          <label>
            当前邮箱验证码
            <div className="settings-code-field">
              <input
                value={currentEmailVerificationCode}
                onChange={(event) => {
                  setCurrentEmailVerificationCode(event.target.value);
                  clearFieldError('currentEmailCode');
                }}
                disabled={loading || saving}
                inputMode="numeric"
                maxLength={6}
                placeholder="请输入当前邮箱验证码"
              />
              <button className="settings-code-button" type="button" onClick={() => sendEmailCode('currentEmail')} disabled={loading || saving || codeSending !== null || (codeCountdowns.currentEmail || 0) > 0}>
                {(codeCountdowns.currentEmail || 0) > 0 ? `${codeCountdowns.currentEmail}s` : codeSending === 'currentEmail' ? '发送中' : '获取验证码'}
              </button>
            </div>
            {fieldErrors.currentEmailCode ? <small className="settings-field-error">{fieldErrors.currentEmailCode}</small> : null}
          </label>
        ) : null}
        {emailChanged && nextEmail ? (
          <label>
            {currentEmail ? '新邮箱验证码' : '邮箱验证码'}
            <div className="settings-code-field">
              <input
                value={newEmailVerificationCode}
                onChange={(event) => {
                  setNewEmailVerificationCode(event.target.value);
                  clearFieldError('newEmailCode');
                }}
                disabled={loading || saving}
                inputMode="numeric"
                maxLength={6}
                placeholder={currentEmail ? '请输入新邮箱验证码' : '请输入邮箱验证码'}
              />
              <button className="settings-code-button" type="button" onClick={() => sendEmailCode('newEmail')} disabled={loading || saving || codeSending !== null || (codeCountdowns.newEmail || 0) > 0}>
                {(codeCountdowns.newEmail || 0) > 0 ? `${codeCountdowns.newEmail}s` : codeSending === 'newEmail' ? '发送中' : '获取验证码'}
              </button>
            </div>
            {fieldErrors.newEmailCode ? <small className="settings-field-error">{fieldErrors.newEmailCode}</small> : null}
          </label>
        ) : null}
        <label>
          手机号
          <div className="mobile-auth-input phone-auth-input settings-phone-auth-input">
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
                  clearFieldError('phone');
                }}
                disabled={loading || saving}
                aria-label="国际区号"
              />
              <button
                type="button"
                aria-label="选择国际区号"
                onClick={() => setCountryPickerOpen((open) => !open)}
                disabled={loading || saving}
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
            <input
              value={phone}
              onFocus={() => setCountryPickerOpen(false)}
              onChange={(event) => {
                setPhone(event.target.value);
                clearFieldError('phone');
              }}
              disabled={loading || saving}
              inputMode="tel"
              autoComplete="tel"
              autoFocus={focusPhone}
              placeholder="请输入手机号"
            />
          </div>
          {fieldErrors.phone ? <small className="settings-field-error">{fieldErrors.phone}</small> : null}
        </label>
        {phoneChanged && currentPhone ? (
          <label>
            当前手机号验证码
            <div className="settings-code-field">
              <input
                value={currentPhoneVerificationCode}
                onChange={(event) => {
                  setCurrentPhoneVerificationCode(event.target.value);
                  clearFieldError('currentPhoneCode');
                }}
                disabled={loading || saving}
                inputMode="numeric"
                maxLength={6}
                placeholder="请输入当前手机号验证码"
              />
              <button className="settings-code-button" type="button" onClick={() => sendPhoneCode('currentPhone')} disabled={loading || saving || codeSending !== null || (codeCountdowns.currentPhone || 0) > 0}>
                {(codeCountdowns.currentPhone || 0) > 0 ? `${codeCountdowns.currentPhone}s` : codeSending === 'currentPhone' ? '发送中' : '获取验证码'}
              </button>
            </div>
            {fieldErrors.currentPhoneCode ? <small className="settings-field-error">{fieldErrors.currentPhoneCode}</small> : null}
          </label>
        ) : null}
        {phoneChanged && nextPhone ? (
          <label>
            {currentPhone ? '新手机号验证码' : '手机验证码'}
            <div className="settings-code-field">
              <input
                value={newPhoneVerificationCode}
                onChange={(event) => {
                  setNewPhoneVerificationCode(event.target.value);
                  clearFieldError('newPhoneCode');
                }}
                disabled={loading || saving}
                inputMode="numeric"
                maxLength={6}
                placeholder={currentPhone ? '请输入新手机号验证码' : '请输入手机验证码'}
              />
              <button className="settings-code-button" type="button" onClick={() => sendPhoneCode('newPhone')} disabled={loading || saving || codeSending !== null || (codeCountdowns.newPhone || 0) > 0}>
                {(codeCountdowns.newPhone || 0) > 0 ? `${codeCountdowns.newPhone}s` : codeSending === 'newPhone' ? '发送中' : '获取验证码'}
              </button>
            </div>
            {fieldErrors.newPhoneCode ? <small className="settings-field-error">{fieldErrors.newPhoneCode}</small> : null}
          </label>
        ) : null}
        <label>
          人机验证
          <div className="settings-anti-abuse-field">
            <span>{antiAbuseChallenge?.question || '加载中'}</span>
            <input
              value={antiAbuseAnswer}
              onChange={(event) => {
                setAntiAbuseAnswer(event.target.value);
                clearFieldError('antiAbuse');
              }}
              disabled={loading || saving || antiAbuseLoading || !antiAbuseChallenge}
              inputMode="numeric"
              placeholder="答案"
            />
            <button type="button" onClick={reloadAntiAbuseChallenge} disabled={loading || saving || antiAbuseLoading}>
              {antiAbuseLoading ? '刷新中' : '换一题'}
            </button>
          </div>
          {fieldErrors.antiAbuse ? <small className="settings-field-error">{fieldErrors.antiAbuse}</small> : null}
        </label>
        <button className="app-primary" type="submit" disabled={loading || saving}>
          {saving ? '保存中' : '保存资料'}
        </button>
      </form>

      <form className="settings-form-card" onSubmit={savePassword}>
        <h2>修改密码</h2>
        <label>
          当前密码
          <input type="password" value={currentPassword} onChange={(event) => {
            setCurrentPassword(event.target.value);
            clearFieldError('currentPassword');
          }} disabled={saving} autoComplete="current-password" />
          {fieldErrors.currentPassword ? <small className="settings-field-error">{fieldErrors.currentPassword}</small> : null}
        </label>
        <label>
          新密码
          <input type="password" value={newPassword} onChange={(event) => {
            setNewPassword(event.target.value);
            clearFieldError('newPassword');
          }} disabled={saving} autoComplete="new-password" />
          {fieldErrors.newPassword ? <small className="settings-field-error">{fieldErrors.newPassword}</small> : null}
        </label>
        <label>
          确认新密码
          <input type="password" value={confirmPassword} onChange={(event) => {
            setConfirmPassword(event.target.value);
            clearFieldError('confirmPassword');
          }} disabled={saving} autoComplete="new-password" />
          {fieldErrors.confirmPassword ? <small className="settings-field-error">{fieldErrors.confirmPassword}</small> : null}
        </label>
        <button className="app-primary" type="submit" disabled={saving}>
          {saving ? '提交中' : '修改密码'}
        </button>
      </form>
    </div>
  );
}

function settingTitle(detail: SettingDetail) {
  return {
    account: '账号与安全',
    notifications: '消息通知',
    about: '关于我们',
    privacy: '隐私政策',
    agreement: '用户协议',
    version: '版本更新',
  }[detail];
}

function SettingDetailContent({
  detail,
}: {
  detail: SettingDetail;
}) {
  const [noticeSettings, setNoticeSettings] = useState({
    systemEnabled: true,
    activityEnabled: true,
    taskEnabled: false,
  });
  const [document, setDocument] = useState<AppDocument | null>(null);
  const [version, setVersion] = useState('检查中');
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    setMessage(null);
    if (detail === 'notifications') {
      loadNotificationSettings()
        .then(setNoticeSettings)
        .catch((err) => setMessage(readSettingError(err)));
      return;
    }
    if (detail === 'version') {
      loadAppVersion()
        .then((nextVersion) => setVersion(nextVersion.latest ? '已是最新' : nextVersion.versionName))
        .catch((err) => setMessage(readSettingError(err)));
      return;
    }
    if (detail === 'about' || detail === 'privacy' || detail === 'agreement') {
      loadAppDocument(detail)
        .then(setDocument)
        .catch((err) => setMessage(readSettingError(err)));
    }
  }, [detail]);

  async function saveNotice(key: keyof typeof noticeSettings, value: boolean) {
    const next = { ...noticeSettings, [key]: value };
    setNoticeSettings(next);
    try {
      setNoticeSettings(await updateNotificationSettings(next));
    } catch (err) {
      setMessage(readSettingError(err));
    }
  }

  if (detail === 'notifications') {
    return (
      <div className="settings-card-list">
        {message ? <div className="settings-message error">{message}</div> : null}
        <SwitchRow title="系统通知" desc="登录状态、账号安全和服务状态提醒" checked={noticeSettings.systemEnabled} onChange={(value) => saveNotice('systemEnabled', value)} />
        <SwitchRow title="活动消息" desc="会员活动、福利和优惠通知" checked={noticeSettings.activityEnabled} onChange={(value) => saveNotice('activityEnabled', value)} />
        <SwitchRow title="任务完成提醒" desc="文件处理、图片压缩完成后提醒" checked={noticeSettings.taskEnabled} onChange={(value) => saveNotice('taskEnabled', value)} />
      </div>
    );
  }

  if (detail === 'version') {
    return (
      <div className="settings-card-list">
        {message ? <div className="settings-message error">{message}</div> : null}
        <article className="settings-info-card centered">
          <RefreshCw size={34} />
          <strong>{version}</strong>
          <span>{version === '已是最新' ? '当前已是最新版本' : '当前版本'}</span>
        </article>
      </div>
    );
  }

  if (detail === 'account') {
    return null;
  }

  return (
    <article className="settings-document">
      {message ? <div className="settings-message error">{message}</div> : null}
      <h2>{document?.title || '加载中'}</h2>
      {(document?.content ? document.content.split('。').filter(Boolean).map((item) => `${item}。`) : ['内容加载中']).map((paragraph) => (
        <p key={paragraph}>{paragraph}</p>
      ))}
    </article>
  );
}

function readSettingError(error: unknown) {
  if (error instanceof Error) {
    if (error.message === 'email already exists') {
      return '该邮箱已经被占用';
    }
    if (error.message === 'email format is invalid') {
      return '请输入有效的邮箱地址';
    }
    if (error.message === 'Current password is incorrect') {
      return '当前密码不正确';
    }
    if (error.message === 'newPassword is not strong enough') {
      return '新密码需为 6-20 位，并包含数字、大小写字母和特殊字符';
    }
    if (error.message === 'trip slot time conflicts') {
      return '该时间段已有发布行程，请更换时间';
    }
    if (error.message === 'duplicate trip slot') {
      return '该时间段已有发布行程，请更换时间';
    }
    if (error.message === 'credit score is too low to book trips') {
      return '信用分低于预约限制，暂不可预约行程';
    }
    if (error.message === 'trip no-show is not available') {
      return '当前行程无法标记未履约';
    }
    if (error.message === 'trip is not finished yet') {
      return '行程结束后才可标记未履约';
    }
    if (error.message === 'phone verification is required') {
      return '请先认证手机号后再发布或预约行程';
    }
    if (error.message === 'phone format is invalid') {
      return '请输入有效的手机号';
    }
    if (error.message === 'phone already exists') {
      return '该手机号已被占用，请联系客服处理';
    }
    if (error.message === 'phone verification code is required') {
      return '请输入手机验证码';
    }
    if (error.message === 'phone verification code expired') {
      return '验证码已过期，请重新获取';
    }
    if (error.message === 'phone verification code is invalid') {
      return '验证码不正确';
    }
    if (error.message === 'email verification code is required') {
      return '请输入邮箱验证码';
    }
    if (error.message === 'email verification code expired') {
      return '邮箱验证码已过期，请重新获取';
    }
    if (error.message === 'email verification code is invalid') {
      return '邮箱验证码不正确';
    }
    if (error.message === 'anti abuse verification is required') {
      return '请先完成人机验证';
    }
    if (error.message === 'anti abuse verification expired') {
      return '人机验证已过期，请重新验证';
    }
    if (error.message === 'anti abuse verification is invalid') {
      return '人机验证答案不正确';
    }
    return error.message;
  }
  return '操作失败';
}

function isStrongPassword(value: string) {
  return /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{6,20}$/.test(value);
}

function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value);
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

function iconForTone(tone?: string | null) {
  if (tone === 'green') {
    return Image;
  }
  if (tone === 'orange') {
    return FileText;
  }
  if (tone === 'red') {
    return FileImage;
  }
  if (tone === 'purple') {
    return Sparkles;
  }
  return FileText;
}

function formatShortTime(value?: string | null) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function nextTripDate(offsetDays: number) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
}

function buildTripTimeOptions(startMinute: number, endMinute: number) {
  const options: string[] = [];
  for (let minute = startMinute; minute <= endMinute; minute += 10) {
    options.push(formatTripTimeMinute(minute));
  }
  return options;
}

function buildTripHourOptions() {
  return Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, '0'));
}

function buildTripStartTimeOptions(date: string) {
  const today = nextTripDate(0);
  const minMinute = date === today ? Math.max(tripStartMinute, nextTenMinuteAfter(new Date())) : tripStartMinute;
  return buildTripTimeOptions(minMinute, tripEndMinute - 10);
}

function buildTripEndTimeOptions(startTime: string) {
  const startMinute = parseTripTimeMinute(startTime);
  const minMinute = startMinute + 10;
  const maxMinute = Math.min(tripEndMinute, startMinute + tripDurationLimitMinutes);
  return buildTripTimeOptions(minMinute, maxMinute);
}

function formatTripTimeMinute(totalMinutes: number) {
  const hour = Math.floor(totalMinutes / 60);
  const minute = totalMinutes % 60;
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function formatTripTimeRange(startTime: string, endTime: string) {
  return `${formatTripClock(startTime)}-${formatTripClock(endTime)}`;
}

function formatTripClock(value: string) {
  return value.slice(0, 5);
}

function parseTripTimeMinute(value: string) {
  const [hour = '0', minute = '0'] = value.split(':');
  return Number(hour) * 60 + Number(minute);
}

function normalizeTripTimeInput(value: string) {
  const normalized = value.trim();
  const match = normalized.match(/^(\d{1,2}):?(\d{2})$/);
  if (!match) {
    return null;
  }
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    return null;
  }
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function isAllowedTripTime(value: string, options: string[]) {
  const normalized = normalizeTripTimeInput(value);
  return Boolean(normalized && options.includes(normalized));
}

function nextTenMinuteAfter(date: Date) {
  const minutes = date.getHours() * 60 + date.getMinutes();
  return Math.floor(minutes / 10) * 10 + 10;
}

function formatTripDate(value: string) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  return `${date.getMonth() + 1}/${date.getDate()} ${weekdays[date.getDay()]}`;
}

function tripStatusLabel(status: TripSlot['status'], context?: 'reservation') {
  const labels: Record<TripSlot['status'], string> = {
    OPEN: '可预约',
    PENDING: context === 'reservation' ? '预约中' : '待审核',
    BOOKED: '已预约',
    NO_SHOW: '未履约',
  };
  return labels[status];
}

function tripStatusHint(status: TripSlot['status'], context?: 'reservation') {
  if (context === 'reservation' && status === 'PENDING') {
    return '等待对方确认';
  }
  return undefined;
}

function isOwnTripSlot(slot: TripSlot, displayName: string) {
  return normalizeTripOwner(slot.ownerUsername) === normalizeTripOwner(displayName);
}

function normalizeTripOwner(value: string) {
  return value.trim().toLowerCase();
}

function SwitchRow({ title, desc, checked, onChange }: { title: string; desc: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="settings-switch-row">
      <span>
        <strong>{title}</strong>
        <small>{desc}</small>
      </span>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <i />
    </label>
  );
}

function MoreToolsSheet({ onClose }: { onClose: () => void }) {
  return (
    <div className="more-tools-mask">
      <section className="more-tools-sheet">
        <header>
          <h2>更多工具</h2>
          <button type="button" onClick={onClose} aria-label="关闭">
            <X size={20} />
          </button>
        </header>
        <div className="tool-catalog-grid">
          {moreTools.map((item) => {
            const Icon = item.icon;
            return (
              <button key={item.label} type="button">
                <span className={`tool-icon ${item.tone}`}>
                  <Icon size={22} />
                </span>
                <em>{item.label}</em>
              </button>
            );
          })}
        </div>
      </section>
    </div>
  );
}

function BottomTabs({ active, onChange, hasMessageDot }: { active: TabKey; onChange: (tab: TabKey) => void; hasMessageDot?: boolean }) {
  const tabs = [
    { key: 'home' as const, icon: Home, label: '首页' },
    { key: 'tools' as const, icon: Grid2X2, label: '工具' },
    { key: 'activity' as const, icon: Star, label: '活动' },
    { key: 'message' as const, icon: MessageCircle, label: '消息', dot: hasMessageDot },
    { key: 'profile' as const, icon: UserRound, label: '我的' },
  ];

  return (
    <nav className="app-bottom-tabs" aria-label="底部导航">
      {tabs.map((tab) => {
        const Icon = tab.icon;
        return (
          <button key={tab.key} className={active === tab.key ? 'active' : ''} type="button" onClick={() => onChange(tab.key)}>
            <span>
              <Icon size={21} />
              {tab.dot ? <i /> : null}
            </span>
            <em>{tab.label}</em>
          </button>
        );
      })}
    </nav>
  );
}

function HeaderWithBack({ title, onBack, right }: { title: string; onBack: () => void; right?: string }) {
  return (
    <header className="tool-detail-head">
      <button type="button" onClick={onBack} aria-label="返回">
        <ChevronLeft size={22} />
      </button>
      <h1>{title}</h1>
      <span>{right}</span>
    </header>
  );
}

function SectionTitle({ title, action }: { title: string; action?: string }) {
  return (
    <div className="tool-section-title">
      <h2>{title}</h2>
      {action ? <button type="button">{action}<ChevronRight size={14} /></button> : null}
    </div>
  );
}

function SegmentedTabs({ items, active, onChange }: { items: string[]; active?: string; onChange?: (item: string) => void }) {
  return (
    <div className="tool-segments">
      {items.map((item, index) => (
        <button key={item} className={(active ? active === item : index === 0) ? 'active' : ''} type="button" onClick={() => onChange?.(item)}>{item}</button>
      ))}
    </div>
  );
}

function MenuRow({ icon: Icon, title, meta, onClick }: { icon: typeof Settings; title: string; meta?: string; onClick?: () => void }) {
  return (
    <button type="button" onClick={onClick}>
      <Icon size={18} />
      <span>{title}</span>
      {meta ? <small>{meta}</small> : null}
      <ChevronRight size={17} />
    </button>
  );
}
